package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.ListingCard;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.webapp.dto.VehicleCardView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final ListingService listingService;

    @Autowired
    public HomeController(final ListingService listingService) {
        this.listingService = listingService;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView home() {
        final ModelAndView mav = new ModelAndView("home");

        final List<VehicleCardView> cheapestCars = listingService.getCheapestListingCards(8).stream()
                .map(HomeController::toVehicleCardView)
                .collect(Collectors.toList());

        final List<VehicleCardView> mostRecentCars = listingService.getMostRecentListingCards(8).stream()
                .map(HomeController::toVehicleCardView)
                .collect(Collectors.toList());

        mav.addObject("cheapestCars", cheapestCars);
        mav.addObject("mostRecentCars", mostRecentCars);

        return mav;
    }

    private static VehicleCardView toVehicleCardView(final ListingCard card) {
        return new VehicleCardView(
                card.getListingId(),
                card.getBrand(),
                card.getModel(),
                card.getDayPrice(),
                card.getImageId());
    }
}
