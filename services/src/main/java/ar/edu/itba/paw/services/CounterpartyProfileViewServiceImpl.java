package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.profile.CounterpartyActiveListingsFragment;
import ar.edu.itba.paw.models.dto.profile.CounterpartyActiveCarsLoadMore;
import ar.edu.itba.paw.models.dto.profile.CounterpartyProfilePageModel;
import ar.edu.itba.paw.models.dto.profile.ReviewItemDto;
import ar.edu.itba.paw.services.policy.PresentationLimitsPolicy;

/**
 * Builds the {@link CounterpartyProfilePageModel} used by both the public counterparty profile
 * (linked from a car-detail page) and the reservation-participant counterparty profile (linked
 * from "My reservations"). Centralizes the bundle "user + recent reviews + other active
 * listings + load-more state" plus the {@link CarService#buildOwnerCarSearchCriteria} call,
 * which used to be hand-rolled with the same hard-coded {@code ("active", "rating,desc")}
 * arguments in three different controller handlers.
 */
@Service
public final class CounterpartyProfileViewServiceImpl implements CounterpartyProfileViewService {

    private final UserService userService;
    private final CarService carService;
    private final ReviewService reviewService;
    private final ReservationService reservationService;
    private final PresentationLimitsPolicy presentationLimitsPolicy;

    @Autowired
    public CounterpartyProfileViewServiceImpl(
            final UserService userService,
            final CarService carService,
            @Lazy final ReviewService reviewService,
            final ReservationService reservationService,
            final PresentationLimitsPolicy presentationLimitsPolicy) {
        this.userService = userService;
        this.carService = carService;
        this.reviewService = reviewService;
        this.reservationService = reservationService;
        this.presentationLimitsPolicy = presentationLimitsPolicy;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CounterpartyProfilePageModel> loadPublicCounterpartyProfile(
            final long counterpartyUserId, final Long currentCarId, final Locale locale) {
        final Optional<User> counterpartyOpt = userService.getUserById(counterpartyUserId);
        if (counterpartyOpt.isEmpty()) {
            return Optional.empty();
        }
        // Public profile linked from a car-detail page is always shown for an owner; ratings and
        // recent reviews use the owner-side aggregation.
        return Optional.of(buildPageModel(counterpartyOpt.get(), currentCarId, true, locale));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CounterpartyProfilePageModel> loadCounterpartyProfileForReservationParticipant(
            final long viewerUserId, final long reservationId, final String role, final Locale locale) {
        final boolean viewerIsOwner = "owner".equals(role);
        final Optional<Reservation> reservationOpt = viewerIsOwner
                ? reservationService.getOwnerReservationById(viewerUserId, reservationId)
                : reservationService.getRiderReservationById(viewerUserId, reservationId);
        if (reservationOpt.isEmpty()) {
            return Optional.empty();
        }
        final Reservation reservation = reservationOpt.get();
        final long reservationCarId = reservation.getCarId();
        // Counterparty is whichever participant the viewer is NOT.
        final Optional<User> counterpartyOpt = viewerIsOwner
                ? userService.getUserById(reservation.getRiderId())
                : carService.getCarById(reservationCarId).map(Car::getOwner);
        if (counterpartyOpt.isEmpty()) {
            return Optional.empty();
        }
        // When the viewer is the rider, the counterparty is the owner; conversely when the
        // viewer is the owner, the counterparty is a rider — and riders do not own listings.
        final boolean counterpartyIsOwner = !viewerIsOwner;
        return Optional.of(buildPageModel(counterpartyOpt.get(), reservationCarId, counterpartyIsOwner, locale));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CounterpartyActiveListingsFragment> loadCounterpartyActiveListingsPage(
            final long counterpartyUserId, final Long excludeCarId, final int page) {
        if (page < 1) {
            return Optional.empty();
        }
        if (userService.getUserById(counterpartyUserId).isEmpty()) {
            return Optional.empty();
        }
        final int pageSize = presentationLimitsPolicy.getCounterpartyOwnerActiveListingsPageSize();
        final Page<CarCard> listingPage = carService.getOwnerCarCards(
                carService.buildOwnerCarSearchCriteria(
                        counterpartyUserId,
                        null, null, null, null, null,
                        List.of("active"),
                        null, null,
                        page,
                        CarService.COUNTERPARTY_OTHER_ACTIVE_CARS_SORT,
                        pageSize,
                        excludeCarId));
        return Optional.of(CounterpartyActiveListingsFragment.of(
                listingPage.getContent(),
                listingPage.isHasNext(),
                listingPage.isHasNext() ? page + 1 : page));
    }

    private CounterpartyProfilePageModel buildPageModel(
            final User counterparty,
            final Long excludeCarId,
            final boolean counterpartyIsOwner,
            final Locale locale) {
        final DateTimeFormatter memberSinceFormatter = DateTimeFormatter.ofPattern("LLLL uuuu").withLocale(locale);
        final BigDecimal averageRating =
                reviewService.getAverageRatingForCounterparty(counterparty.getId(), counterpartyIsOwner);
        final List<ReviewItemDto> recentReviewItems = reviewService.getRecentCommentReviewsForCounterparty(
                counterparty.getId(),
                counterpartyIsOwner,
                presentationLimitsPolicy.getCounterpartyRecentReviewsLimit());
        final int ownerListingsPageSize = presentationLimitsPolicy.getCounterpartyOwnerActiveListingsPageSize();
        // Riders do not own listings; only owners get the "other active listings" grid.
        final Page<CarCard> ownerCarsPage = counterpartyIsOwner
                ? carService.getOwnerCarCards(
                        carService.buildOwnerCarSearchCriteria(
                                counterparty.getId(),
                                null, null, null, null, null,
                                List.of("active"),
                                null, null,
                                0,
                                CarService.COUNTERPARTY_OTHER_ACTIVE_CARS_SORT,
                                ownerListingsPageSize,
                                excludeCarId))
                : null;
        final List<CarCard> activeCarCards =
                ownerCarsPage == null ? List.of() : ownerCarsPage.getContent();
        final CounterpartyActiveCarsLoadMore loadMore = counterpartyIsOwner && ownerCarsPage != null
                ? CounterpartyActiveCarsLoadMore.of(
                        ownerCarsPage.isHasNext(),
                        counterparty.getId(),
                        excludeCarId,
                        1,
                        ownerListingsPageSize)
                : CounterpartyActiveCarsLoadMore.none();
        return new CounterpartyProfilePageModel(
                counterparty.getForename(),
                counterparty.getSurname(),
                counterparty.getAbout().map(String::trim).orElse(""),
                counterparty.getProfilePictureId().orElse(null),
                counterparty.getMemberSince().map(memberSinceFormatter::format).orElse(null),
                averageRating,
                counterparty.isLicenseValidated() || counterparty.getLicenseFileId().isPresent(),
                counterparty.isIdentityValidated() || counterparty.getIdentityFileId().isPresent(),
                recentReviewItems,
                counterpartyIsOwner,
                activeCarCards,
                loadMore);
    }
}
