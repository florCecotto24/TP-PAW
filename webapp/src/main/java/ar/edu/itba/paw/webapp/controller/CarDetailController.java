package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.dto.BookableSegmentProjection;
import ar.edu.itba.paw.models.dto.CarCard;
import ar.edu.itba.paw.models.dto.ListingPublicReview;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.profile.CounterpartyActiveListingsLoadMore;
import ar.edu.itba.paw.models.dto.profile.CounterpartyHeaderDto;
import ar.edu.itba.paw.models.dto.profile.ReviewItemDto;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.util.WallDateTimeDisplayFormat;
import ar.edu.itba.paw.models.util.OwnerCarSearchCriteria;
import ar.edu.itba.paw.services.CarService;
import ar.edu.itba.paw.services.ListingAvailabilityService;
import ar.edu.itba.paw.services.ReservationService;
import ar.edu.itba.paw.services.ReviewService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.policy.PaginationPolicy;
import ar.edu.itba.paw.services.policy.PresentationLimitsPolicy;
import ar.edu.itba.paw.webapp.support.ConsumerVehicleCardViewFactory;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.dto.ListingReviewRowView;
import ar.edu.itba.paw.webapp.dto.VehicleCardView;
import ar.edu.itba.paw.webapp.util.BookableWallRangesJson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/** Public car detail, reviews pagination, similar cars, and bookable availability JSON for pickers. */
@Controller
public class CarDetailController {

    private final CarService carService;
    private final ListingAvailabilityService listingAvailabilityService;
    private final ReservationService reservationService;
    private final ReviewService reviewService;
    private final UserService userService;
    private final PaginationPolicy paginationPolicy;
    private final PresentationLimitsPolicy presentationLimitsPolicy;
    private final ConsumerVehicleCardViewFactory consumerVehicleCardViewFactory;

    @Autowired
    public CarDetailController(
            final CarService carService,
            final ListingAvailabilityService listingAvailabilityService,
            final ReservationService reservationService,
            final ReviewService reviewService,
            final UserService userService,
            final PaginationPolicy paginationPolicy,
            final PresentationLimitsPolicy presentationLimitsPolicy,
            final ConsumerVehicleCardViewFactory consumerVehicleCardViewFactory) {
        this.carService = carService;
        this.listingAvailabilityService = listingAvailabilityService;
        this.reservationService = reservationService;
        this.reviewService = reviewService;
        this.userService = userService;
        this.paginationPolicy = paginationPolicy;
        this.presentationLimitsPolicy = presentationLimitsPolicy;
        this.consumerVehicleCardViewFactory = consumerVehicleCardViewFactory;
    }

    @RequestMapping(value = "/car-detail", method = RequestMethod.GET)
    public ModelAndView carDetail(
            @RequestParam(name = "carId", required = false) final Long carIdParam,
            @RequestParam(name = "reviewPage", defaultValue = "0") final int reviewPage,
            @RequestParam(name = "from", required = false) final String fromDateParam,
            @RequestParam(name = "until", required = false) final String untilDateParam,
            @RequestParam(name = "searchNbId", required = false) final List<Long> searchNeighborhoodIds,
            @CurrentUser final User currentUser,
            final HttpServletRequest request) {

        if (carIdParam == null) {
            return new ModelAndView(new RedirectView("/search", true));
        }
        final Optional<Car> carOpt = carService.getCarById(carIdParam);
        if (carOpt.isEmpty()) {
            return new ModelAndView(new RedirectView("/search", true));
        }
        final Car car = carOpt.get();
        final long carId = car.getId();

        final User owner = car.getOwner();
        final boolean isOwnerRequesting = currentUser != null && currentUser.getId() == owner.getId();
        final Long ownerProfileImageId = userService.getUserById(owner.getId())
                .flatMap(User::getProfilePictureId)
                .orElse(null);

        final List<String> carGalleryImagePaths = car.getPictures().stream()
                .map(cp -> "/image/" + cp.getImageId())
                .collect(Collectors.toList());

        final List<VehicleCardView> similarListings = consumerVehicleCardViewFactory.toConsumerVehicleCardViews(
                carService.findSimilarCarCards(
                        carId, presentationLimitsPolicy.getCarDetailSimilarListingsLimit(), currentUser));

        final List<BookableSegmentProjection> bookableSegments =
                listingAvailabilityService.getBookableSegmentsForRiderDatePickerByCar(carId, Instant.now());
        final String bookableWallRangesJson = BookableWallRangesJson.toJsonArray(bookableSegments);
        final boolean hasBookableDays = !bookableSegments.isEmpty();

        final BigDecimal minEffectiveDayPrice = listingAvailabilityService.resolveMinEffectiveDayPriceByCar(carId, null);
        final boolean priceIsVariable = minEffectiveDayPrice != null
                && listingAvailabilityService.isCarPriceVariableByCar(carId, minEffectiveDayPrice);

        final Locale locale = RequestContextUtils.getLocale(request);
        final long reviewTotal = reviewService.countReviewsForCar(carId);
        final Page<ListingPublicReview> reviewSource =
                reviewService.getCarPublicReviews(
                        carId, reviewPage, paginationPolicy.getListingPublicReviewsPageSize());
        final List<ListingReviewRowView> reviewRows = reviewSource.getContent().stream()
                .map(r -> new ListingReviewRowView(
                        r.getReviewerForename(),
                        r.getReviewerSurname(),
                        WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(r.getCreatedAt(), locale),
                        r.getRating(),
                        r.getComment().orElse("")))
                .collect(Collectors.toList());
        final Page<ListingReviewRowView> carReviewPage = new Page<>(
                reviewRows,
                reviewSource.getCurrentPage(),
                reviewSource.getPageSize(),
                reviewSource.getTotalItems());

        final String reviewCountLabel = reviewTotal > 0 ? Long.toString(reviewTotal) : null;
        final String ratingLabel =
                reviewTotal > 0 && car.getRatingAvg().isPresent()
                        ? formatOneDecimal(car.getRatingAvg().get(), locale)
                        : null;

        final String carTitle = (car.getBrand() != null ? car.getBrand() : "")
                + (car.getBrand() != null && car.getModel() != null ? " " : "")
                + (car.getModel() != null ? car.getModel() : "");

        final ModelAndView mav = new ModelAndView("carDetail");
        mav.addObject("isOwnerRequesting", isOwnerRequesting);
        mav.addObject("maxReservationBillableDays", reservationService.getConfiguredMaxReservationBillableDays());
        mav.addObject("car", car);
        mav.addObject("carTitle", carTitle);
        mav.addObject("carMinEffectiveDayPrice", minEffectiveDayPrice);
        mav.addObject("carPriceIsVariable", priceIsVariable);
        mav.addObject("carRatingLabel", ratingLabel);
        mav.addObject("carReviewCountLabel", reviewCountLabel);
        mav.addObject("carReviewPage", carReviewPage);
        mav.addObject("owner", owner);
        mav.addObject("ownerProfileImageId", ownerProfileImageId);
        mav.addObject("carGalleryImagePaths", carGalleryImagePaths);
        mav.addObject("hasBookableDays", hasBookableDays);
        mav.addObject("bookableWallRangesJson", bookableWallRangesJson);
        mav.addObject("reservationFromDefault", fromDateParam != null ? fromDateParam : "");
        mav.addObject("reservationUntilDefault", untilDateParam != null ? untilDateParam : "");
        mav.addObject("searchNeighborhoodIds", searchNeighborhoodIds != null ? searchNeighborhoodIds : List.of());
        mav.addObject("similarListings", similarListings);
        final String similarSearchUrl = "/search?category=" + car.getType().name()
                + "&transmission=" + car.getTransmission().name()
                + "&powertrain=" + car.getPowertrain().name();
        mav.addObject("similarSearchUrl", similarSearchUrl);
        return mav;
    }

    @GetMapping("/counterparty-profile")
    public ModelAndView ownerProfile(
            @RequestParam("userId") final long userId,
            @RequestParam(name = "carId", required = false) final Long currentCarId) {
        final Optional<User> counterpartyOpt = userService.getUserById(userId);
        if (counterpartyOpt.isEmpty()) {
            return new ModelAndView(new RedirectView("/search", true));
        }

        final User counterparty = counterpartyOpt.get();
        final Locale locale = LocaleContextHolder.getLocale();
        final DateTimeFormatter memberSinceFormatter = DateTimeFormatter.ofPattern("LLLL uuuu").withLocale(locale);
        final BigDecimal averageRating = reviewService.getAverageRatingForCounterparty(counterparty.getId(), true);
        final List<ReviewItemDto> recentReviewItems = reviewService.getRecentCommentReviewsForCounterparty(
                counterparty.getId(),
                true,
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
        final int counterpartyListingsPageSize = presentationLimitsPolicy.getCounterpartyOwnerActiveListingsPageSize();
        final OwnerCarSearchCriteria counterpartyCriteria = new OwnerCarSearchCriteria(
                counterparty.getId(), 0, counterpartyListingsPageSize,
                List.of("active"), null, null, null, null, null, null, null,
                "rating", "desc", currentCarId);
        final Page<CarCard> ownerActiveListingsPage = carService.getOwnerCarCards(counterpartyCriteria);
        final List<VehicleCardView> counterpartyActiveListings =
                consumerVehicleCardViewFactory.toConsumerVehicleCardViews(ownerActiveListingsPage.getContent());
        final CounterpartyActiveListingsLoadMore counterpartyActiveListingsLoadMore =
                CounterpartyActiveListingsLoadMore.of(
                        ownerActiveListingsPage.isHasNext(),
                        counterparty.getId(),
                        currentCarId,
                        1,
                        counterpartyListingsPageSize);

        final ModelAndView mav = new ModelAndView("counterpartyProfile");
        mav.addObject("activeTab", "explore");
        mav.addObject("counterpartyForename", headerDto.getForename());
        mav.addObject("counterpartySurname", headerDto.getSurname());
        mav.addObject("counterpartyAbout", headerDto.getAbout().orElse(""));
        mav.addObject("counterpartyProfileImageId", headerDto.getProfileImageId().orElse(null));
        mav.addObject("counterpartyMemberSinceDisplay", headerDto.getMemberSinceDisplay().orElse(null));
        final BigDecimal avgRating = headerDto.getAverageRating();
        mav.addObject("counterpartyAverageRating", avgRating);
        mav.addObject(
                "counterpartyRatingFloor",
                avgRating != null ? avgRating.longValue() : 0L);
        mav.addObject(
                "counterpartyLicenseValidated",
                counterparty.isLicenseValidated() || counterparty.getLicenseFileId().isPresent());
        mav.addObject(
                "counterpartyIdentityValidated",
                counterparty.isIdentityValidated() || counterparty.getIdentityFileId().isPresent());
        mav.addObject(
                "recentReviewComments",
                recentReviewItems);
        mav.addObject("showCounterpartyActiveListings", true);
        mav.addObject("counterpartyActiveListings", counterpartyActiveListings);
        mav.addObject("counterpartyActiveListingsLoadMore", counterpartyActiveListingsLoadMore);
        return mav;
    }

    /**
     * HTML fragment: next page of active listings for the counterparty profile grid (see {@code counterparty-profile.js}).
     */
    @GetMapping("/counterparty-profile/active-listings-page")
    public ModelAndView counterpartyActiveListingsPage(
            @RequestParam("userId") final long userId,
            @RequestParam(name = "carId", required = false) final Long excludeCarId,
            @RequestParam("page") final int page,
            final HttpServletResponse response) {
        if (page < 1) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return emptyCounterpartyListingsFragment();
        }
        if (userService.getUserById(userId).isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return emptyCounterpartyListingsFragment();
        }

        final int pageSize = presentationLimitsPolicy.getCounterpartyOwnerActiveListingsPageSize();
        final OwnerCarSearchCriteria pageCriteria = new OwnerCarSearchCriteria(
                userId, page, pageSize,
                List.of("active"), null, null, null, null, null, null, null,
                "rating", "desc", excludeCarId);
        final Page<CarCard> listingPage = carService.getOwnerCarCards(pageCriteria);
        final List<VehicleCardView> cards =
                consumerVehicleCardViewFactory.toConsumerVehicleCardViews(listingPage.getContent());
        final ModelAndView mav = new ModelAndView("counterpartyActiveListingCols");
        mav.addObject("counterpartyActiveListings", cards);
        mav.addObject("fragmentHasMore", listingPage.isHasNext());
        mav.addObject("fragmentNextPage", listingPage.isHasNext() ? page + 1 : page);
        return mav;
    }

    private static ModelAndView emptyCounterpartyListingsFragment() {
        final ModelAndView mav = new ModelAndView("counterpartyActiveListingCols");
        mav.addObject("counterpartyActiveListings", List.of());
        mav.addObject("fragmentHasMore", false);
        mav.addObject("fragmentNextPage", 0);
        return mav;
    }

    private static String formatOneDecimal(final BigDecimal value, final Locale locale) {
        final NumberFormat nf = NumberFormat.getNumberInstance(locale != null ? locale : Locale.ENGLISH);
        nf.setMinimumFractionDigits(1);
        nf.setMaximumFractionDigits(1);
        return nf.format(value);
    }

}
