package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.car.BookableSegmentProjection;
import ar.edu.itba.paw.models.dto.car.CarGalleryMediaItem;
import ar.edu.itba.paw.models.dto.car.CarPublicReview;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.car.detail.CarDetailPageModel;
import ar.edu.itba.paw.models.dto.car.detail.CarReviewRow;
import ar.edu.itba.paw.models.util.time.BookableWallRangesJson;
import ar.edu.itba.paw.models.util.media.CarGalleryMediaItems;
import ar.edu.itba.paw.models.util.time.WallDateTimeDisplayFormat;
import ar.edu.itba.paw.services.policy.PaginationPolicy;
import ar.edu.itba.paw.services.policy.PresentationLimitsPolicy;

/**
 * Owns the orchestration the {@code GET /cars/{carId}} handler used to do by hand: car lookup,
 * owner visibility, gallery, bookable-day JSON, paged reviews with localized dates, similar
 * listings, and viewer-specific flags (admin / owner / favorited). The controller is left
 * only with the parts that genuinely belong to it: parsing query params, applying the
 * redirect-on-empty rule, and converting raw {@code CarCard}s to view-layer {@code
 * VehicleCardView}s for the JSP tag.
 */
@Service
public final class CarDetailViewServiceImpl implements CarDetailViewService {

    private static final String REVIEWS_VIEW_LIST = "list";
    private static final String REVIEWS_VIEW_CAROUSEL = "carousel";

    private final CarService carService;
    private final CarPictureService carPictureService;
    private final CarAvailabilityService carAvailabilityService;
    private final ReservationService reservationService;
    private final ReviewService reviewService;
    private final UserService userService;
    private final FavCarService favCarService;
    private final PaginationPolicy paginationPolicy;
    private final PresentationLimitsPolicy presentationLimitsPolicy;

    @Autowired
    public CarDetailViewServiceImpl(
            final CarService carService,
            final CarPictureService carPictureService,
            final CarAvailabilityService carAvailabilityService,
            final ReservationService reservationService,
            final ReviewService reviewService,
            final UserService userService,
            final FavCarService favCarService,
            final PaginationPolicy paginationPolicy,
            final PresentationLimitsPolicy presentationLimitsPolicy) {
        this.carService = carService;
        this.carPictureService = carPictureService;
        this.carAvailabilityService = carAvailabilityService;
        this.reservationService = reservationService;
        this.reviewService = reviewService;
        this.userService = userService;
        this.favCarService = favCarService;
        this.paginationPolicy = paginationPolicy;
        this.presentationLimitsPolicy = presentationLimitsPolicy;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CarDetailPageModel> loadCarDetailPage(
            final long carId,
            final User viewer,
            final boolean viewerIsAdmin,
            final int reviewPageParam,
            final String reviewsViewParam,
            final Locale locale) {
        final Optional<Car> carOpt = carService.getCarById(carId);
        if (carOpt.isEmpty()) {
            return Optional.empty();
        }
        final Car car = carOpt.get();
        final User owner = car.getOwner();
        final boolean isOwnerRequesting = viewer != null && viewer.getId() == owner.getId();
        // Cars owned by a blocked user are hidden from the public catalog. The owner themselves
        // and admins still get the page so they can take corrective action.
        if (owner.isBlocked() && !isOwnerRequesting && !viewerIsAdmin) {
            return Optional.empty();
        }

        final Long ownerProfileImageId = userService.getUserById(owner.getId())
                .flatMap(User::getProfilePictureId)
                .orElse(null);

        final List<CarGalleryMediaItem> carGalleryMedia = CarGalleryMediaItems.fromPictures(
                carPictureService.getCarPicturesByCarId(carId));

        final List<BookableSegmentProjection> bookableSegments =
                carAvailabilityService.getBookableSegmentsForRiderDatePickerByCar(carId, Instant.now());
        final String bookableWallRangesJson = BookableWallRangesJson.toJsonArray(bookableSegments);
        final boolean hasBookableDays = !bookableSegments.isEmpty();

        final BigDecimal minEffectiveDayPrice =
                carAvailabilityService.resolveMinEffectiveDayPriceByCar(carId, null);
        final boolean carPriceIsVariable = minEffectiveDayPrice != null
                && carAvailabilityService.isCarPriceVariableByCar(carId, minEffectiveDayPrice);

        final long reviewTotal = reviewService.countReviewsForCar(carId);
        // The carousel always renders page 0 (it is a "teaser"). Only the list view honours
        // ?reviewPage; we clamp it to >= 0 here so the controller doesn't have to.
        final String reviewsView =
                REVIEWS_VIEW_LIST.equalsIgnoreCase(reviewsViewParam) ? REVIEWS_VIEW_LIST : REVIEWS_VIEW_CAROUSEL;
        final int effectiveReviewPage = REVIEWS_VIEW_LIST.equals(reviewsView) ? Math.max(0, reviewPageParam) : 0;
        final Page<CarPublicReview> reviewSource = reviewService.getCarPublicReviews(
                carId, effectiveReviewPage, paginationPolicy.getCarPublicReviewsPageSize());
        final List<CarReviewRow> reviewRows = reviewSource.getContent().stream()
                .map(r -> new CarReviewRow(
                        r.getReviewerForename(),
                        r.getReviewerSurname(),
                        WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(r.getCreatedAt(), locale),
                        r.getRating(),
                        r.getComment().orElse(""),
                        r.getImageId().orElse(null)))
                .collect(Collectors.toList());
        final Page<CarReviewRow> carReviewPage = new Page<>(
                reviewRows,
                reviewSource.getCurrentPage(),
                reviewSource.getPageSize(),
                reviewSource.getTotalItems());

        final String reviewCountLabel = reviewTotal > 0 ? Long.toString(reviewTotal) : null;
        final String ratingLabel =
                reviewTotal > 0 && car.getRatingAvg().isPresent()
                        ? formatOneDecimal(car.getRatingAvg().get(), locale)
                        : null;

        final String carTitle = buildCarTitle(car);
        final String similarSearchUrl = "/search?category=" + car.getType().name()
                + "&transmission=" + car.getTransmission().name()
                + "&powertrain=" + car.getPowertrain().name();

        final boolean carIsFavoritable = viewer != null && !isOwnerRequesting;
        // Skip the favorite lookup entirely when the badge wouldn't be rendered anyway.
        final boolean carIsFavorited = carIsFavoritable && favCarService.isFavorited(car.getId(), viewer.getId());

        return Optional.of(CarDetailPageModel.builder()
                .car(car)
                .carTitle(carTitle)
                .carMinEffectiveDayPrice(minEffectiveDayPrice)
                .carPriceIsVariable(carPriceIsVariable)
                .carRatingLabel(ratingLabel)
                .carReviewCountLabel(reviewCountLabel)
                .carReviewPage(carReviewPage)
                .reviewsView(reviewsView)
                .owner(owner)
                .ownerProfileImageId(ownerProfileImageId)
                .carGalleryMedia(carGalleryMedia)
                .hasBookableDays(hasBookableDays)
                .bookableWallRangesJson(bookableWallRangesJson)
                .similarListings(carService.findSimilarCarCards(
                        carId, presentationLimitsPolicy.getCarDetailSimilarListingsLimit(), viewer))
                .similarSearchUrl(similarSearchUrl)
                .maxReservationBillableDays(reservationService.getConfiguredMaxReservationBillableDays())
                .isOwnerRequesting(isOwnerRequesting)
                .currentUserIsAdmin(viewerIsAdmin)
                .carIsFavoritable(carIsFavoritable)
                .carIsFavorited(carIsFavorited)
                .build());
    }

    private static String buildCarTitle(final Car car) {
        final String brand = car.getBrand() != null ? car.getBrand() : "";
        final String model = car.getModel() != null ? car.getModel() : "";
        final String sep = !brand.isEmpty() && !model.isEmpty() ? " " : "";
        return brand + sep + model;
    }

    private static String formatOneDecimal(final BigDecimal value, final Locale locale) {
        final NumberFormat nf = NumberFormat.getNumberInstance(locale != null ? locale : Locale.ENGLISH);
        nf.setMinimumFractionDigits(1);
        nf.setMaximumFractionDigits(1);
        return nf.format(value);
    }
}
