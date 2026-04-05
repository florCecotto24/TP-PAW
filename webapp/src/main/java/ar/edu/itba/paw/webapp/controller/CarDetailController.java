package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.Car;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.ListingAvailability;
import ar.edu.itba.paw.models.ListingCard;
import ar.edu.itba.paw.models.ListingDetail;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.WallDateTimeParsing;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.webapp.dto.ReservationPeriodOption;
import ar.edu.itba.paw.webapp.dto.VehicleCardView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.time.ZonedDateTime;
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
    public ModelAndView carDetail(@RequestParam(name = "listingId") final long listingId) {
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

        final List<VehicleCardView> similarListings = listingService
                .findSimilarListingCards(listingId, SIMILAR_LISTINGS_LIMIT)
                .stream()
                .map(CarDetailController::listingCardToVehicleCardView)
                .collect(Collectors.toList());

        final List<ListingAvailability> listingAvailabilities = detail.getListingAvailabilities();
        final List<String> availabilityLines = new ArrayList<>(listingAvailabilities.size());
        final List<ReservationPeriodOption> reservationPeriods = new ArrayList<>(listingAvailabilities.size());
        for (final ListingAvailability a : listingAvailabilities) {
            availabilityLines.add(formatAvailabilityLine(a));
            reservationPeriods.add(toReservationPeriodOption(a));
        }

        final ModelAndView mav = new ModelAndView("carDetail");
        mav.addObject("listing", listing);
        mav.addObject("car", car);
        mav.addObject("owner", owner);
        mav.addObject("carGalleryImagePaths", carGalleryImagePaths);
        mav.addObject("listingAvailabilities", listingAvailabilities);
        mav.addObject("availabilityLines", availabilityLines);
        mav.addObject("reservationPeriods", reservationPeriods);
        mav.addObject("reservationFromDefault", "");
        mav.addObject("reservationUntilDefault", "");
        mav.addObject("similarListings", similarListings);
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

    private static ReservationPeriodOption toReservationPeriodOption(final ListingAvailability a) {
        final ZonedDateTime startZ = a.getStartDate().atZoneSameInstant(AvailabilityPeriod.WALL_ZONE);
        ZonedDateTime lastInclusiveZ = a.getEndDate().minusNanos(1).atZoneSameInstant(AvailabilityPeriod.WALL_ZONE);
        if (lastInclusiveZ.isBefore(startZ)) {
            lastInclusiveZ = startZ;
        }
        final String minLocal = startZ.format(WallDateTimeParsing.WALL_INPUT_DATE_TIME);
        final String maxLocal = lastInclusiveZ.format(WallDateTimeParsing.WALL_INPUT_DATE_TIME);
        final String label = WallDateTimeParsing.WALL_DISPLAY_DATE_TIME.format(startZ) + " — "
                + WallDateTimeParsing.WALL_DISPLAY_DATE_TIME.format(lastInclusiveZ);
        return new ReservationPeriodOption(a.getId(), label, minLocal, maxLocal);
    }

    private static String formatAvailabilityLine(final ListingAvailability a) {
        final ZonedDateTime startZ = a.getStartDate().atZoneSameInstant(AvailabilityPeriod.WALL_ZONE);
        ZonedDateTime lastInclusiveZ = a.getEndDate().minusNanos(1).atZoneSameInstant(AvailabilityPeriod.WALL_ZONE);
        if (lastInclusiveZ.isBefore(startZ)) {
            lastInclusiveZ = startZ;
        }
        return WallDateTimeParsing.WALL_DISPLAY_DATE_TIME.format(startZ) + " — "
                + WallDateTimeParsing.WALL_DISPLAY_DATE_TIME.format(lastInclusiveZ);
    }

}
