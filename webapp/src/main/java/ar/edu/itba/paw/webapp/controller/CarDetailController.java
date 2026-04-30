package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.dto.ListingCard;
import ar.edu.itba.paw.models.dto.ListingDetail;
import ar.edu.itba.paw.models.dto.ListingPublicReview;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.profile.CounterpartyHeaderDto;
import ar.edu.itba.paw.models.dto.profile.ReviewItemDto;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.util.OwnerListingSearchCriteria;
import ar.edu.itba.paw.models.util.WallDateTimeDisplayFormat;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.services.ReservationService;
import ar.edu.itba.paw.services.ReviewService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.dto.ListingReviewRowView;
import ar.edu.itba.paw.webapp.dto.VehicleCardView;
import ar.edu.itba.paw.webapp.util.BookableWallRangesJson;
import ar.edu.itba.paw.webapp.util.BookableWallRangesJson.LocalDateSegment;
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
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class CarDetailController {

    private static final int SIMILAR_LISTINGS_LIMIT = 4;
    private static final int LISTING_REVIEWS_PAGE_SIZE = 5;
    private static final int COUNTERPARTY_RECENT_REVIEWS_LIMIT = 3;
    private static final int COUNTERPARTY_ACTIVE_LISTINGS_PAGE_SIZE = 8;

    private final ListingService listingService;
    private final ReservationService reservationService;
    private final ReviewService reviewService;
    private final UserService userService;

    @Autowired
    public CarDetailController(
            final ListingService listingService,
            final ReservationService reservationService,
            final ReviewService reviewService,
            final UserService userService) {
        this.listingService = listingService;
        this.reservationService = reservationService;
        this.reviewService = reviewService;
        this.userService = userService;
    }

    @RequestMapping(value = "/car-detail", method = RequestMethod.GET)
    public ModelAndView carDetail(
            @RequestParam(name = "listingId") final long listingId,
            @RequestParam(name = "reviewPage", defaultValue = "0") final int reviewPage,
            @CurrentUser final User currentUser,
            final HttpServletRequest request) {
        final Optional<ListingDetail> detailOpt = listingService.getListingDetailById(listingId);
        if (detailOpt.isEmpty()) {
            return new ModelAndView(new RedirectView("/search", true));
        }
        final ListingDetail detail = detailOpt.get();
        final Listing listing = detail.getListing();
        final Car car = detail.getCar();

        final User owner = detail.getOwner();
        final Long ownerProfileImageId = userService.getUserById(owner.getId())
                .flatMap(User::getProfilePictureId)
                .orElse(null);

        final List<String> carGalleryImagePaths = detail.getPictures().stream()
                .map(cp -> "/image/" + cp.getImageId())
                .collect(Collectors.toList());

        final User viewer = currentUser;
        final List<VehicleCardView> similarListings = listingService
                .findSimilarListingCards(listingId, SIMILAR_LISTINGS_LIMIT, viewer)
                .stream()
                .map(CarDetailController::listingCardToVehicleCardView)
                .collect(Collectors.toList());

        final List<AvailabilityPeriod> bookable = listingService.getBookableWallAvailabilityPeriods(listingId);
        final List<LocalDateSegment> rawSegments = new ArrayList<>(bookable.size());
        for (final AvailabilityPeriod p : bookable) {
            rawSegments.add(new LocalDateSegment(p.getStartInclusive(), p.getEndInclusive()));
        }
        List<LocalDateSegment> bookableSegments = BookableWallRangesJson.mergeAdjacentSegments(rawSegments);
        final LocalTime pickupWall = Optional.ofNullable(listing.getCheckInTime()).orElse(LocalTime.of(10, 0));
        final int pickupLeadHours = reservationService.getConfiguredPickupLeadHours();
        final Instant minPickupExclusive = Instant.now().plus(pickupLeadHours, ChronoUnit.HOURS);
        bookableSegments = BookableWallRangesJson.clipSegmentsToMinPickupInstant(
                bookableSegments, pickupWall, AvailabilityPeriod.WALL_ZONE, minPickupExclusive);
        final String bookableWallRangesJson = BookableWallRangesJson.toJsonArray(bookableSegments);
        final boolean hasBookableDays = !bookableSegments.isEmpty();

        final String listingPublicLocation = listingService.formatPublicPickupLocation(listing);

        final Locale locale = RequestContextUtils.getLocale(request);
        final long reviewTotal = reviewService.countReviewsForListing(listingId);
        final Page<ListingPublicReview> reviewSource =
                reviewService.getListingPublicReviews(listingId, reviewPage, LISTING_REVIEWS_PAGE_SIZE);
        final List<ListingReviewRowView> reviewRows = reviewSource.getContent().stream()
                .map(r -> new ListingReviewRowView(
                        r.getReviewerForename(),
                        r.getReviewerSurname(),
                        WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(r.getCreatedAt(), locale),
                        r.getRating(),
                        r.getComment().orElse("")))
                .collect(Collectors.toList());
        final Page<ListingReviewRowView> listingReviewPage = new Page<>(
                reviewRows,
                reviewSource.getCurrentPage(),
                reviewSource.getPageSize(),
                reviewSource.getTotalItems());

        final String listingReviewCountLabel = reviewTotal > 0 ? Long.toString(reviewTotal) : null;
        final String listingRatingLabel =
                reviewTotal > 0 && listing.getRatingAvg().isPresent()
                        ? formatOneDecimal(listing.getRatingAvg().get(), locale)
                        : null;

        final ModelAndView mav = new ModelAndView("carDetail");
        mav.addObject("maxReservationBillableDays", reservationService.getConfiguredMaxReservationBillableDays());
        mav.addObject("listing", listing);
        mav.addObject("listingPublicLocation", listingPublicLocation);
        mav.addObject("listingRatingLabel", listingRatingLabel);
        mav.addObject("listingReviewCountLabel", listingReviewCountLabel);
        mav.addObject("listingReviewPage", listingReviewPage);
        mav.addObject("car", car);
        mav.addObject("owner", owner);
        mav.addObject("ownerProfileImageId", ownerProfileImageId);
        mav.addObject("carGalleryImagePaths", carGalleryImagePaths);
        mav.addObject("hasBookableDays", hasBookableDays);
        mav.addObject("bookableWallRangesJson", bookableWallRangesJson);
        mav.addObject("reservationFromDefault", "");
        mav.addObject("reservationUntilDefault", "");
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
            @RequestParam(name = "listingId", required = false) final Long currentListingId) {
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
                COUNTERPARTY_RECENT_REVIEWS_LIMIT);
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
        final List<VehicleCardView> counterpartyActiveListings = listingService.getOwnerListingCards(
                        listingService.buildOwnerListingSearchCriteria(
                                counterparty.getId(), null, null, null, null, List.of("active"), null, 0, null))
                .getContent()
                .stream()
                .filter(card -> currentListingId == null || card.getListingId() != currentListingId)
                .map(CarDetailController::listingCardToVehicleCardView)
                .collect(Collectors.toList());

        final ModelAndView mav = new ModelAndView("counterpartyProfile");
        mav.addObject("activeTab", "explore");
        mav.addObject("counterpartyForename", headerDto.getForename());
        mav.addObject("counterpartySurname", headerDto.getSurname());
        mav.addObject("counterpartyAbout", headerDto.getAbout().orElse(""));
        mav.addObject("counterpartyProfileImageId", headerDto.getProfileImageId().orElse(null));
        mav.addObject("counterpartyMemberSinceDisplay", headerDto.getMemberSinceDisplay().orElse(null));
        mav.addObject("counterpartyAverageRating", headerDto.getAverageRating());
        mav.addObject(
                "counterpartyLicenseValidated",
                counterparty.isLicenseValidated() || counterparty.getLicenseFileId().isPresent());
        mav.addObject(
                "counterpartyInsuranceValidated",
                counterparty.isInsuranceValidated() || counterparty.getInsuranceFileId().isPresent());
        mav.addObject(
                "counterpartyIdentityValidated",
                counterparty.isIdentityValidated() || counterparty.getIdentityFileId().isPresent());
        mav.addObject(
                "recentReviewComments",
                recentReviewItems.stream()
                        .map(item -> item.getComment().orElse("").trim())
                        .filter(comment -> !comment.isEmpty())
                        .collect(Collectors.toList()));
        mav.addObject("showCounterpartyActiveListings", true);
        mav.addObject("counterpartyActiveListings", counterpartyActiveListings);
        return mav;
    }

    private static String formatOneDecimal(final BigDecimal value, final Locale locale) {
        final NumberFormat nf = NumberFormat.getNumberInstance(locale != null ? locale : Locale.ENGLISH);
        nf.setMinimumFractionDigits(1);
        nf.setMaximumFractionDigits(1);
        return nf.format(value);
    }

    private static VehicleCardView listingCardToVehicleCardView(final ListingCard card) {
        return new VehicleCardView(
                card.getListingId(),
                card.getBrand(),
                card.getModel(),
                card.getDayPrice(),
                card.getImageId(),
                null,
                card.getRatingAvg().orElse(null));
    }
}
