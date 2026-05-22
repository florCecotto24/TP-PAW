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
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.ListingCard;
import ar.edu.itba.paw.models.dto.ListingDetail;
import ar.edu.itba.paw.models.dto.ReservationCard;
import ar.edu.itba.paw.models.dto.ReservationCardDisplayRow;
import ar.edu.itba.paw.models.dto.ReservationChatPageModel;
import ar.edu.itba.paw.models.dto.ReservationDetailPageModel;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.profile.CounterpartyActiveListingCardRow;
import ar.edu.itba.paw.models.dto.profile.CounterpartyActiveListingsLoadMore;
import ar.edu.itba.paw.models.dto.profile.CounterpartyHeaderDto;
import ar.edu.itba.paw.models.dto.profile.CounterpartyProfilePageModel;
import ar.edu.itba.paw.models.dto.profile.ReviewItemDto;
import ar.edu.itba.paw.services.policy.PaymentReceiptUploadPolicy;
import ar.edu.itba.paw.services.policy.PresentationLimitsPolicy;

/** Read-only reservation views; domain reads and billable-day math go through {@link ReservationService}. */
@Service
public final class ReservationViewServiceImpl implements ReservationViewService {

    private final ReservationService reservationService;
    private final ListingService listingService;
    private final ListingViewService listingViewService;
    private final UserService userService;
    private final ImageService imageService;
    private final PaymentReceiptUploadPolicy paymentReceiptUploadPolicy;
    private final ReviewService reviewService;
    private final PresentationLimitsPolicy presentationLimitsPolicy;
    private final ReservationMessageService reservationMessageService;

    @Autowired
    public ReservationViewServiceImpl(
            final ReservationService reservationService,
            final ListingService listingService,
            final ListingViewService listingViewService,
            final UserService userService,
            final ImageService imageService,
            final PaymentReceiptUploadPolicy paymentReceiptUploadPolicy,
            @Lazy final ReviewService reviewService,
            final PresentationLimitsPolicy presentationLimitsPolicy,
            final ReservationMessageService reservationMessageService) {
        this.reservationService = reservationService;
        this.listingService = listingService;
        this.listingViewService = listingViewService;
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
        final Optional<ListingDetail> listingDetailOpt = listingService.getListingDetailById(reservation.getListingId());
        if (listingDetailOpt.isEmpty()) {
            return Optional.empty();
        }
        final ListingDetail listingDetail = listingDetailOpt.get();
        final Optional<User> counterpartyOpt =
                viewerIsOwner
                        ? userService.getUserById(reservation.getRiderId())
                        : Optional.of(listingDetail.getOwner());
        if (counterpartyOpt.isEmpty()) {
            return Optional.empty();
        }
        final User counterpartyMinimal = counterpartyOpt.get();
        final User counterparty = userService.getUserById(counterpartyMinimal.getId()).orElse(counterpartyMinimal);
        final Listing listing = listingDetail.getListing();
        final String reservationPickupLocationDisplay = listingViewService.formatPickupForReservationView(
                listing,
                reservation,
                viewerIsOwner);
        final String pickupDisplay = WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(reservation.getStartDate(), locale);
        final String returnDisplay = WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(reservation.getEndDate(), locale);
        final String totalPrice = ArsMoneyFormat.format(reservation.getTotalPrice());
        final long carImageId = listingDetail.getPictures().isEmpty() ? 0L : listingDetail.getPictures().get(0).getImageId();
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
                listing,
                reservationPickupLocationDisplay,
                listingDetail.getCar(),
                listingDetail.getOwner(),
                counterparty,
                counterparty.getProfilePictureId().orElse(null),
                counterparty.getPhoneNumber().orElse(""),
                listingDetail.getOwner().getCbu().orElse(""),
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
                reservation.isPaymentApproved(),
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
        final Optional<Listing> listingOpt = listingService.getListingById(reservation.getListingId());
        if (listingOpt.isEmpty()) {
            return Optional.empty();
        }
        final Listing listing = listingOpt.get();
        final Optional<User> counterpartyOpt = "owner".equals(role)
                ? userService.getUserById(reservation.getRiderId())
                : userService.getListingOwner(reservation.getListingId());
        if (counterpartyOpt.isEmpty()) {
            return Optional.empty();
        }
        final User counterpartyMinimal = counterpartyOpt.get();
        final User counterparty =
                userService.getUserById(counterpartyMinimal.getId()).orElse(counterpartyMinimal);
        final String counterpartyDisplayName = counterparty.getForename() + " " + counterparty.getSurname();
        return Optional.of(new ReservationChatPageModel(
                reservation.getId(),
                listing.getId(),
                listing.getTitle(),
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
        final Optional<User> counterpartyOpt = "owner".equals(role)
                ? userService.getUserById(reservation.getRiderId())
                : userService.getListingOwner(reservation.getListingId());
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
        final Page<ListingCard> ownerListingsPage =
                counterpartyIsOwner
                        ? listingService.getOwnerListingCards(
                                listingService.buildOwnerListingSearchCriteria(
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
                                        ListingService.COUNTERPARTY_OTHER_ACTIVE_LISTINGS_SORT,
                                        ownerListingsPageSize,
                                        reservation.getListingId()))
                        : null;
        final List<CounterpartyActiveListingCardRow> activeRows =
                ownerListingsPage == null
                        ? List.of()
                        : ownerListingsPage.getContent().stream()
                                .map(ReservationViewServiceImpl::toCounterpartyActiveListingCardRow)
                                .toList();
        final CounterpartyActiveListingsLoadMore counterpartyActiveListingsLoadMore =
                counterpartyIsOwner && ownerListingsPage != null
                        ? CounterpartyActiveListingsLoadMore.of(
                                ownerListingsPage.isHasNext(),
                                counterparty.getId(),
                                reservation.getListingId(),
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
                        activeRows,
                        counterpartyActiveListingsLoadMore));
    }

    private static CounterpartyActiveListingCardRow toCounterpartyActiveListingCardRow(final ListingCard card) {
        return new CounterpartyActiveListingCardRow(
                card.getListingId(),
                card.getBrand(),
                card.getModel(),
                card.getDayPrice(),
                card.getImageId(),
                card.getRatingAvg().orElse(null),
                card.getReviewCount());
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationCardDisplayRow toReservationCardDisplayRow(final ReservationCard card, final Locale locale) {
        final String pickupDisplay = WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(card.getStartDate(), locale);
        final String returnDisplay = WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(card.getEndDate(), locale);
        final long days = reservationService.calculateBillableDays(card.getStartDate(), card.getEndDate());
        final String totalPrice = days > 0
                ? ArsMoneyFormat.format(card.getDayPrice().multiply(BigDecimal.valueOf(days)))
                : "-";
        return new ReservationCardDisplayRow(
                card.getReservationId(),
                card.getListingId(),
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
