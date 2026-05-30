package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.util.ArsMoneyFormat;
import ar.edu.itba.paw.models.util.ReservationHubStatusWhitelist;
import ar.edu.itba.paw.models.util.WallDateTimeDisplayFormat;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.CarCard;
import ar.edu.itba.paw.models.dto.ReservationCard;
import ar.edu.itba.paw.models.dto.ReservationCardDisplayRow;
import ar.edu.itba.paw.models.dto.ReservationChatPageModel;
import ar.edu.itba.paw.models.dto.ReservationDetailPageModel;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.profile.CounterpartyActiveListingsLoadMore;
import ar.edu.itba.paw.models.dto.profile.CounterpartyHeaderDto;
import ar.edu.itba.paw.models.dto.profile.CounterpartyProfilePageModel;
import ar.edu.itba.paw.models.dto.profile.ReviewItemDto;
import ar.edu.itba.paw.services.policy.PaymentReceiptUploadPolicy;
import ar.edu.itba.paw.services.policy.PresentationLimitsPolicy;
import ar.edu.itba.paw.services.util.ListingAddressFormatter;

/** Read-only reservation views; domain reads and billable-day math go through {@link ReservationService}. */
@Service
public final class ReservationViewServiceImpl implements ReservationViewService {

    private final ReservationService reservationService;
    private final CarService carService;
    private final ListingAvailabilityService listingAvailabilityService;
    private final CarPictureService carPictureService;
    private final ListingAddressFormatter listingAddressFormatter;
    private final UserService userService;
    private final ImageService imageService;
    private final PaymentReceiptUploadPolicy paymentReceiptUploadPolicy;
    private final ReviewService reviewService;
    private final PresentationLimitsPolicy presentationLimitsPolicy;
    private final ReservationMessageService reservationMessageService;

    @Autowired
    public ReservationViewServiceImpl(
            final ReservationService reservationService,
            final CarService carService,
            final ListingAvailabilityService listingAvailabilityService,
            final CarPictureService carPictureService,
            final ListingAddressFormatter listingAddressFormatter,
            final UserService userService,
            final ImageService imageService,
            final PaymentReceiptUploadPolicy paymentReceiptUploadPolicy,
            @Lazy final ReviewService reviewService,
            final PresentationLimitsPolicy presentationLimitsPolicy,
            final ReservationMessageService reservationMessageService) {
        this.reservationService = reservationService;
        this.carService = carService;
        this.listingAvailabilityService = listingAvailabilityService;
        this.carPictureService = carPictureService;
        this.listingAddressFormatter = listingAddressFormatter;
        this.userService = userService;
        this.imageService = imageService;
        this.paymentReceiptUploadPolicy = paymentReceiptUploadPolicy;
        this.reviewService = reviewService;
        this.presentationLimitsPolicy = presentationLimitsPolicy;
        this.reservationMessageService = reservationMessageService;
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
        final Optional<ListingAvailability> effectiveAvailability =
                listingAvailabilityService.findEffectiveForDayByCar(
                        carId,
                        reservation.getStartDate().atZoneSameInstant(java.time.ZoneOffset.UTC).toLocalDate());
        final String reservationPickupLocationDisplay = effectiveAvailability
                .map(av -> listingAddressFormatter.formatPickupForReservationView(av, reservation, viewerIsOwner))
                .orElse("");
        final String pickupDisplay = WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(reservation.getStartDate(), locale);
        final String returnDisplay = WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(reservation.getEndDate(), locale);
        final String totalPrice = ArsMoneyFormat.format(reservation.getTotalPrice());
        final List<ar.edu.itba.paw.models.domain.CarPicture> carPictures = carPictureService.getCarPicturesByCarId(carId);
        final long carImageId = carPictures.isEmpty() ? 0L : carPictures.get(0).getImageId();
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
        final boolean refundReceiptApproved = reservation.isPaymentRefundApproved();
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
                refundReceiptApproved,
                refundProofDeadlineDisplay,
                reservationMessageService.isChatAvailable(reservation)));
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
                reservationMessageService.getMaxChatAttachmentMegabytes()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CounterpartyProfilePageModel> loadCounterpartyProfileForReservationParticipant(
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
        final long reservationCarId = reservation.getCarId();
        final Optional<User> counterpartyOpt = "owner".equals(role)
                ? userService.getUserById(reservation.getRiderId())
                : carService.getCarById(reservationCarId).map(Car::getOwner);
        if (counterpartyOpt.isEmpty()) {
            return Optional.empty();
        }
        final User counterparty = counterpartyOpt.get();
        final boolean counterpartyIsOwner = "rider".equals(role);
        final DateTimeFormatter memberSinceFormatter = DateTimeFormatter.ofPattern("LLLL uuuu").withLocale(locale);
        final BigDecimal averageRating =
                reviewService.getAverageRatingForCounterparty(counterparty.getId(), counterpartyIsOwner);
        final List<ReviewItemDto> recentReviewItems = reviewService.getRecentCommentReviewsForCounterparty(
                counterparty.getId(),
                counterpartyIsOwner,
                presentationLimitsPolicy.getCounterpartyRecentReviewsLimit());
        final CounterpartyHeaderDto headerDto = new CounterpartyHeaderDto(
                counterparty.getForename() + " " + counterparty.getSurname(),
                counterparty.getForename(),
                counterparty.getSurname(),
                null,
                averageRating,
                0L,
                counterparty.getAbout().map(String::trim).orElse(null),
                counterparty.getMemberSince().orElse(null),
                counterparty.getMemberSince().map(memberSinceFormatter::format).orElse(null),
                counterparty.getProfilePictureId().orElse(null));
        final int ownerListingsPageSize = presentationLimitsPolicy.getCounterpartyOwnerActiveListingsPageSize();
        final Page<CarCard> ownerCarsPage =
                counterpartyIsOwner
                        ? carService.getOwnerCarCards(
                                carService.buildOwnerCarSearchCriteria(
                                        counterparty.getId(),
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        List.of("active"),
                                        null,
                                        null,
                                        0,
                                        CarService.COUNTERPARTY_OTHER_ACTIVE_CARS_SORT,
                                        ownerListingsPageSize,
                                        reservationCarId))
                        : null;
        // Pass raw CarCard rows; the controller converts them to VehicleCardView so
        // the JSP's <ryden:consumerCarCard> tag receives the type it requires.
        final List<CarCard> activeCarCards =
                ownerCarsPage == null ? List.of() : ownerCarsPage.getContent();
        final CounterpartyActiveListingsLoadMore counterpartyActiveListingsLoadMore =
                counterpartyIsOwner && ownerCarsPage != null
                        ? CounterpartyActiveListingsLoadMore.of(
                                ownerCarsPage.isHasNext(),
                                counterparty.getId(),
                                reservationCarId,
                                1,
                                ownerListingsPageSize)
                        : CounterpartyActiveListingsLoadMore.none();
        return Optional.of(
                new CounterpartyProfilePageModel(
                        headerDto.getForename(),
                        headerDto.getSurname(),
                        headerDto.getAbout().orElse(""),
                        headerDto.getProfileImageId().orElse(null),
                        headerDto.getMemberSinceDisplay().orElse(null),
                        headerDto.getAverageRating(),
                        counterparty.isLicenseValidated() || counterparty.getLicenseFileId().isPresent(),
                        counterparty.isIdentityValidated() || counterparty.getIdentityFileId().isPresent(),
                        recentReviewItems,
                        counterpartyIsOwner,
                        activeCarCards,
                        counterpartyActiveListingsLoadMore));
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationCardDisplayRow toReservationCardDisplayRow(final ReservationCard card, final Locale locale) {
        final String pickupDisplay = WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(card.getStartDate(), locale);
        final String returnDisplay = WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(card.getEndDate(), locale);
        final String totalPrice = card.getTotalPrice() != null
                ? ArsMoneyFormat.format(card.getTotalPrice())
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
}
