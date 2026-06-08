package ar.edu.itba.paw.services.reservation.view;


import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.util.search.ReservationHubStatusWhitelist;
import ar.edu.itba.paw.models.util.search.ReservationSearchCriteria;
import ar.edu.itba.paw.models.util.time.AppTimezone;
import ar.edu.itba.paw.models.util.time.WallDateTimeDisplayFormat;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarAvailability;
import ar.edu.itba.paw.models.domain.car.CarPicture;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.dto.reservation.CarReservationsListPageModel;
import ar.edu.itba.paw.models.dto.reservation.OwnerReservationsListPageModel;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.reservation.ReservationCard;
import ar.edu.itba.paw.models.dto.reservation.ReservationCardDisplayRow;
import ar.edu.itba.paw.models.dto.reservation.ReservationChatPageModel;
import ar.edu.itba.paw.models.dto.reservation.ReservationDetailPageModel;
import ar.edu.itba.paw.models.dto.reservation.ReservationEditPageModel;
import ar.edu.itba.paw.models.dto.reservation.RiderReservationsListPageModel;
import ar.edu.itba.paw.models.dto.car.BookableSegmentProjection;
import ar.edu.itba.paw.models.util.time.BookableWallRangesJson;
import ar.edu.itba.paw.models.util.time.WallDateTimeParsing;
import ar.edu.itba.paw.policy.PaymentReceiptUploadPolicy;
import ar.edu.itba.paw.util.CarAvailabilityAddressFormatter;
import ar.edu.itba.paw.util.format.MoneyFormat;

import ar.edu.itba.paw.services.car.CarAvailabilityService;
import ar.edu.itba.paw.services.car.CarPictureService;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.file.ImageService;
import ar.edu.itba.paw.services.reservation.ReservationAvailabilityService;
import ar.edu.itba.paw.services.reservation.ReservationMessageService;
import ar.edu.itba.paw.services.reservation.ReservationService;
import ar.edu.itba.paw.services.review.ReviewService;
import ar.edu.itba.paw.services.user.UserService;
/** Read-only reservation views; domain reads and billable-day math go through {@link ReservationService}. */
@Service
public final class ReservationViewServiceImpl implements ReservationViewService {

    private final ReservationService reservationService;
    private final CarService carService;
    private final CarAvailabilityService carAvailabilityService;
    private final ReservationAvailabilityService reservationAvailabilityService;
    private final CarPictureService carPictureService;
    private final CarAvailabilityAddressFormatter carAvailabilityAddressFormatter;
    private final UserService userService;
    private final ImageService imageService;
    private final PaymentReceiptUploadPolicy paymentReceiptUploadPolicy;
    private final ReviewService reviewService;
    private final ReservationMessageService reservationMessageService;
    private final MoneyFormat moneyFormat;

    @Autowired
    public ReservationViewServiceImpl(
            final ReservationService reservationService,
            final CarService carService,
            final CarAvailabilityService carAvailabilityService,
            final ReservationAvailabilityService reservationAvailabilityService,
            final CarPictureService carPictureService,
            final CarAvailabilityAddressFormatter carAvailabilityAddressFormatter,
            final UserService userService,
            final ImageService imageService,
            final PaymentReceiptUploadPolicy paymentReceiptUploadPolicy,
            @Lazy final ReviewService reviewService,
            final ReservationMessageService reservationMessageService,
            final MoneyFormat moneyFormat) {
        this.reservationService = reservationService;
        this.carService = carService;
        this.carAvailabilityService = carAvailabilityService;
        this.reservationAvailabilityService = reservationAvailabilityService;
        this.carPictureService = carPictureService;
        this.carAvailabilityAddressFormatter = carAvailabilityAddressFormatter;
        this.userService = userService;
        this.imageService = imageService;
        this.paymentReceiptUploadPolicy = paymentReceiptUploadPolicy;
        this.reviewService = reviewService;
        this.reservationMessageService = reservationMessageService;
        this.moneyFormat = moneyFormat;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReservationDetailPageModel> loadMyReservationDetailForViewer(
            final long viewerUserId,
            final long reservationId,
            final String role,
            final Locale locale) {
        final boolean viewerIsOwner = "owner".equals(role);
        final Optional<Reservation> reservationOpt = viewerIsOwner
                ? reservationService.getOwnerReservationById(viewerUserId, reservationId)
                : reservationService.getRiderReservationById(viewerUserId, reservationId);
        if (reservationOpt.isEmpty()) {
            return Optional.empty();
        }
        final Reservation reservation = reservationOpt.get();
        final long carId = reservation.getCarId();
        final Optional<Car> carOpt = carService.getCarById(carId);
        if (carOpt.isEmpty()) {
            return Optional.empty();
        }
        final Car car = carOpt.get();
        final User carOwner = car.getOwner();
        final Optional<User> counterpartyOpt =
                viewerIsOwner
                        ? userService.getUserById(reservation.getRiderId())
                        : Optional.ofNullable(carOwner);
        if (counterpartyOpt.isEmpty()) {
            return Optional.empty();
        }
        final User counterpartyMinimal = counterpartyOpt.get();
        final User counterparty = userService.getUserById(counterpartyMinimal.getId()).orElse(counterpartyMinimal);
        final Optional<CarAvailability> effectiveAvailability =
                reservationAvailabilityService.findEffectivePickupAvailabilityForReservation(reservation.getId());
        final String reservationPickupLocationDisplay = effectiveAvailability
                .map(av -> carAvailabilityAddressFormatter.formatPickupForReservationView(av, reservation, viewerIsOwner))
                .orElse("");
        final String pickupDisplay = WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(reservation.getStartDate(), locale);
        final String returnDisplay = WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(reservation.getEndDate(), locale);
        final String totalPrice = moneyFormat.format(reservation.getTotalPrice());
        final List<CarPicture> carPictures = carPictureService.getCarPicturesByCarId(carId);
        final long carImageId = carPictures.stream()
                .map(CarPicture::getImageId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(0L);
        final OffsetDateTime nowUtc = OffsetDateTime.now(ZoneOffset.UTC);
        final boolean periodEnded = nowUtc.isAfter(reservation.getEndDate());
        final boolean hasOwnerReview = reviewService.hasOwnerReview(reservation.getId());
        final boolean hasRiderReview = reviewService.hasRiderReview(reservation.getId());
        final Optional<String> paymentProofDeadlineDisplay = reservation.getPaymentProofDeadlineAt()
                .map(deadline -> WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(deadline, locale));
        final boolean pickupNotYetStarted = nowUtc.isBefore(reservation.getStartDate());
        final boolean canCancelReservation =
                (reservation.getStatus() == Reservation.Status.PENDING
                                && reservation.getPaymentReceiptFileId().isEmpty())
                        || (reservation.getStatus() == Reservation.Status.ACCEPTED && pickupNotYetStarted);
        final Optional<String> refundProofDeadlineDisplay = reservation.getRefundProofDeadlineAt()
                .map(deadline -> WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(deadline, locale));
        final boolean hasRefundReceipt = reservation.getPaymentRefundReceiptFileId().isPresent();
        return Optional.of(new ReservationDetailPageModel(
                reservation,
                reservationPickupLocationDisplay,
                car,
                carOwner,
                counterparty,
                counterparty.getProfilePictureId().orElse(null),
                counterparty.getPhoneNumber().orElse(""),
                carOwner.getCbu().orElse(""),
                pickupDisplay,
                returnDisplay,
                reservation.getStatus().name().toLowerCase(Locale.ROOT),
                totalPrice,
                carImageId,
                role,
                imageService.getMaxImageBytes(),
                imageService.getMaxImageMegabytesRoundedUp(),
                paymentReceiptUploadPolicy.getMaxBytes(),
                paymentReceiptUploadPolicy.getMaxMegabytesRoundedUp(),
                reviewService.getReviewCommentMaxLength(),
                periodEnded,
                viewerIsOwner && periodEnded && !reservation.isCarReturned(),
                viewerIsOwner && reservation.isCarReturned() && !hasOwnerReview,
                !viewerIsOwner && periodEnded && !hasRiderReview,
                paymentProofDeadlineDisplay,
                reservation.getPaymentReceiptFileId().isPresent(),
                canCancelReservation,
                hasRefundReceipt,
                refundProofDeadlineDisplay,
                reservationMessageService.isChatAvailable(reservation)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReservationEditPageModel> loadRiderEditReservationPage(
            final long riderUserId, final long reservationId, final Locale locale) {
        // The security filter chain rejects unauthorized callers via
        // ReservationWebAuthorization.isRiderUnpaidPending, but we re-load through the rider-scoped
        // accessor anyway so the view-builder cannot be hoisted out of context.
        final Optional<Reservation> reservationOpt =
                reservationService.getRiderReservationById(riderUserId, reservationId);
        if (reservationOpt.isEmpty()) {
            return Optional.empty();
        }
        final Reservation reservation = reservationOpt.get();
        if (reservation.getStatus() != Reservation.Status.PENDING
                || reservation.getPaymentReceiptFileId().isPresent()) {
            return Optional.empty();
        }
        final Optional<Car> carOpt = carService.getCarById(reservation.getCarId());
        if (carOpt.isEmpty()) {
            return Optional.empty();
        }
        final Car car = carOpt.get();
        // Excluding the reservation itself prevents it from blocking its own days in the bookable
        // calendar — riders editing a pending reservation must be able to keep or partly reuse
        // their currently-held window.
        final List<BookableSegmentProjection> bookableSegments =
                carAvailabilityService.getBookableSegmentsForRiderDatePickerByCarExcluding(
                        car.getId(), Instant.now(), reservation.getId());
        final String bookableWallRangesJson = BookableWallRangesJson.toJsonArray(bookableSegments);
        final boolean hasBookableDays = !bookableSegments.isEmpty();
        final List<CarPicture> carPictures =
                carPictureService.getCarPicturesByCarId(car.getId());
        final long carImageId = carPictures.stream()
                .map(CarPicture::getImageId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(0L);
        final String pickupDisplay =
                WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(reservation.getStartDate(), locale);
        final String returnDisplay =
                WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(reservation.getEndDate(), locale);
        final String totalPriceDisplay = moneyFormat.format(reservation.getTotalPrice());
        // Pre-fill the hidden Flatpickr inputs with the current wall-zone values so the form opens
        // pointing at the user's existing window (which they can then shrink/move/extend).
        final LocalDateTime startWall =
                reservation.getStartDate().atZoneSameInstant(AppTimezone.WALL_ZONE).toLocalDateTime();
        final LocalDateTime endWall =
                reservation.getEndDate().atZoneSameInstant(AppTimezone.WALL_ZONE).toLocalDateTime();
        final String currentFromDateTimeWall = WallDateTimeParsing.WALL_INPUT_DATE_TIME.format(startWall);
        final String currentUntilDateTimeWall = WallDateTimeParsing.WALL_INPUT_DATE_TIME.format(endWall);
        return Optional.of(new ReservationEditPageModel(
                reservation,
                car,
                carImageId,
                pickupDisplay,
                returnDisplay,
                totalPriceDisplay,
                currentFromDateTimeWall,
                currentUntilDateTimeWall,
                bookableWallRangesJson,
                hasBookableDays,
                reservationService.getConfiguredMaxReservationBillableDays(),
                car.getMinimumRentalDays()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReservationChatPageModel> loadReservationChatForParticipant(
            final long viewerUserId,
            final long reservationId,
            final String role,
            final Locale locale) {
        final Optional<Reservation> reservationOpt = "owner".equals(role)
                ? reservationService.getOwnerReservationById(viewerUserId, reservationId)
                : reservationService.getRiderReservationById(viewerUserId, reservationId);
        if (reservationOpt.isEmpty()) {
            return Optional.empty();
        }
        final Reservation reservation = reservationOpt.get();
        if (!reservationMessageService.isChatAvailable(reservation)) {
            return Optional.empty();
        }
        final long carId = reservation.getCarId();
        final Optional<Car> carOpt = carService.getCarById(carId);
        if (carOpt.isEmpty()) {
            return Optional.empty();
        }
        final Car car = carOpt.get();
        final Optional<User> counterpartyOpt = "owner".equals(role)
                ? userService.getUserById(reservation.getRiderId())
                : Optional.ofNullable(car.getOwner());
        if (counterpartyOpt.isEmpty()) {
            return Optional.empty();
        }
        final User counterpartyMinimal = counterpartyOpt.get();
        final User counterparty =
                userService.getUserById(counterpartyMinimal.getId()).orElse(counterpartyMinimal);
        final String counterpartyDisplayName = counterparty.getForename() + " " + counterparty.getSurname();
        final String vehicleLabel = car.getBrand() + " " + car.getModel();
        return Optional.of(new ReservationChatPageModel(
                reservation.getId(),
                carId,
                vehicleLabel,
                role,
                counterpartyDisplayName,
                counterparty.getProfilePictureId().orElse(null),
                viewerUserId,
                reservationMessageService.getMessageBodyMaxLength(),
                reservationMessageService.getMaxChatAttachmentMegabytes(),
                reservationMessageService.getHistoryPageSize()));
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationCardDisplayRow toReservationCardDisplayRow(final ReservationCard card, final Locale locale) {
        final String pickupDisplay = WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(card.getStartDate(), locale);
        final String returnDisplay = WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(card.getEndDate(), locale);
        final String totalPrice = card.getTotalPrice() != null
                ? moneyFormat.format(card.getTotalPrice())
                : "-";
        return new ReservationCardDisplayRow(
                card.getReservationId(),
                card.getCarId(),
                card.getImageId(),
                card.getBrand(),
                card.getModel(),
                pickupDisplay,
                returnDisplay,
                card.getStatus().name().toLowerCase(Locale.ROOT),
                totalPrice);
    }

    @Override
    @Transactional(readOnly = true)
    public String normalizeReservationStatusQueryParam(final String raw) {
        return ReservationHubStatusWhitelist.normalizeStatusQueryParam(raw);
    }

    @Override
    @Transactional(readOnly = true)
    public OwnerReservationsListPageModel loadOwnerReservationsListPage(
            final ReservationSearchCriteria criteria,
            final Car selectedCarOrNull,
            final String currentSort,
            final Locale locale) {
        final Page<ReservationCard> resultPage = reservationService.getOwnerReservationCards(criteria);
        final List<ReservationCardDisplayRow> rows = resultPage.getContent().stream()
                .map(card -> toReservationCardDisplayRow(card, locale))
                .toList();
        // Refund-proof obligation badges are part of the same view; pulling them inside the
        // page model keeps the controller free of an independent reservationService call.
        final Set<Long> pendingRefundReservationIds = criteria.getOwnerId() != null
                ? reservationService.findOwnerReservationIdsRequiringRefundProof(criteria.getOwnerId())
                : Set.of();
        return new OwnerReservationsListPageModel(
                rows, resultPage, selectedCarOrNull, currentSort, pendingRefundReservationIds);
    }

    @Override
    @Transactional(readOnly = true)
    public CarReservationsListPageModel loadCarReservationsListPage(
            final long ownerUserId,
            final Car car,
            final int page,
            final int pageSize,
            final String statusFilter,
            final Locale locale) {
        final Page<ReservationCard> resultPage = reservationService.getCarReservationCards(
                ownerUserId, car.getId(), page, pageSize, statusFilter);
        final List<ReservationCardDisplayRow> rows = resultPage.getContent().stream()
                .map(card -> toReservationCardDisplayRow(card, locale))
                .toList();
        return new CarReservationsListPageModel(car, rows, resultPage, statusFilter);
    }

    @Override
    @Transactional(readOnly = true)
    public RiderReservationsListPageModel loadRiderReservationsListPage(
            final ReservationSearchCriteria criteria,
            final String currentSort,
            final Locale locale) {
        final Page<ReservationCard> resultPage = reservationService.getRiderReservationCards(criteria);
        final List<ReservationCardDisplayRow> rows = resultPage.getContent().stream()
                .map(card -> toReservationCardDisplayRow(card, locale))
                .toList();
        return new RiderReservationsListPageModel(rows, resultPage, currentSort);
    }
}
