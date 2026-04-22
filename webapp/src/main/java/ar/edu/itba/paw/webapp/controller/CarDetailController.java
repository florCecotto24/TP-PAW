package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Car;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.ListingCard;
import ar.edu.itba.paw.models.ListingDetail;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.webapp.dto.VehicleCardView;
import ar.edu.itba.paw.webapp.util.BookableWallRangesJson;
import ar.edu.itba.paw.webapp.util.BookableWallRangesJson.LocalDateSegment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class CarDetailController {

    private static final int SIMILAR_LISTINGS_LIMIT = 4;

    private final ListingService listingService;

    @Autowired
    public CarDetailController(final ListingService listingService) {
        this.listingService = listingService;
    }

    @RequestMapping(value = "/car-detail", method = RequestMethod.GET)
    public ModelAndView carDetail(
            @RequestParam(name = "listingId") final long listingId,
            @ModelAttribute(name = LoggedUserAdvice.CURRENT_USER_MODEL_KEY, binding = false) final User currentUser) {
        final Optional<ListingDetail> detailOpt = listingService.getListingDetailById(listingId);
        if (detailOpt.isEmpty()) {
            return new ModelAndView(new RedirectView("/search", true));
        }
        final ListingDetail detail = detailOpt.get();
        final Listing listing = detail.getListing();
        final Car car = detail.getCar();

        final User owner = detail.getOwner();

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
        final List<LocalDateSegment> bookableSegments = BookableWallRangesJson.mergeAdjacentSegments(rawSegments);
        final String bookableWallRangesJson = BookableWallRangesJson.toJsonArray(bookableSegments);
        final boolean hasBookableDays = !bookableSegments.isEmpty();

        final ModelAndView mav = new ModelAndView("carDetail");
        mav.addObject("listing", listing);
        mav.addObject("car", car);
        mav.addObject("owner", owner);
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

    private static VehicleCardView listingCardToVehicleCardView(final ListingCard card) {
        return new VehicleCardView(
                card.getListingId(),
                card.getBrand(),
                card.getModel(),
                card.getDayPrice(),
                card.getImageId());
    }

}
