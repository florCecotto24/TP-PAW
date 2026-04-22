package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.ListingCard;
import ar.edu.itba.paw.models.Page;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.webapp.dto.VehicleCardView;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private static final int CAROUSEL_PAGE_SIZE = 8;

    private final ListingService listingService;

    @Autowired
    public HomeController(final ListingService listingService) {
        this.listingService = listingService;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView home(
            @RequestParam(defaultValue = "0") int cheapestPage,
            @RequestParam(defaultValue = "0") int recentPage,
            final Authentication authentication) {
        final ModelAndView mav = new ModelAndView("home");

        cheapestPage = Math.max(0, cheapestPage);
        recentPage   = Math.max(0, recentPage);

        final User viewer = WebAuthUtils.viewerUser(authentication).orElse(null);

        final Page<ListingCard> cheapestRaw =
                listingService.getCheapestListingCards(cheapestPage, CAROUSEL_PAGE_SIZE, viewer);
        final Page<ListingCard> recentRaw =
                listingService.getMostRecentListingCards(recentPage, CAROUSEL_PAGE_SIZE, viewer);

        final Page<VehicleCardView> cheapestCarsPage = mapPage(cheapestRaw);
        final Page<VehicleCardView> recentCarsPage   = mapPage(recentRaw);

        mav.addObject("cheapestCarsPage", cheapestCarsPage);
        mav.addObject("recentCarsPage", recentCarsPage);

        return mav;
    }

    private static Page<VehicleCardView> mapPage(final Page<ListingCard> source) {
        final List<VehicleCardView> views = source.getContent().stream()
                .map(HomeController::toVehicleCardView)
                .collect(Collectors.toList());
        return new Page<>(views, source.getCurrentPage(), source.getPageSize(), source.getTotalItems());
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
