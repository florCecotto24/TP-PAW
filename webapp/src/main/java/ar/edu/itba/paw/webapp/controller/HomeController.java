package ar.edu.itba.paw.webapp.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.ListingCard;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.services.policy.PaginationPolicy;
import ar.edu.itba.paw.webapp.dto.VehicleCardView;
import ar.edu.itba.paw.webapp.support.CurrentUser;

@Controller
public final class HomeController {

    private final ListingService listingService;
    private final PaginationPolicy paginationPolicy;

    public HomeController(final ListingService listingService, final PaginationPolicy paginationPolicy) {
        this.listingService = listingService;
        this.paginationPolicy = paginationPolicy;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView home(
            @RequestParam(defaultValue = "0") int cheapestPage,
            @RequestParam(defaultValue = "0") int recentPage,
            @CurrentUser final User currentUser) {
        final ModelAndView mav = new ModelAndView("home");

        cheapestPage = Math.max(0, cheapestPage);
        recentPage   = Math.max(0, recentPage);

        final User viewer = currentUser;

        final int uiPageSize = paginationPolicy.getUiPageSize();
        final Page<ListingCard> cheapestRaw =
                listingService.getCheapestListingCards(cheapestPage, uiPageSize, viewer);
        final Page<ListingCard> recentRaw =
                listingService.getMostRecentListingCards(recentPage, uiPageSize, viewer);

        final Page<VehicleCardView> cheapestCarsPage = mapPage(cheapestRaw);
        final Page<VehicleCardView> recentCarsPage   = mapPage(recentRaw);

        mav.addObject("cheapestCarsPage", cheapestCarsPage);
        mav.addObject("recentCarsPage", recentCarsPage);
        mav.addObject("activeTab", "home");

        return mav;
    }

    private static Page<VehicleCardView> mapPage(final Page<ListingCard> source) {
        final List<VehicleCardView> views = source.getContent().stream()
                .map(VehicleCardView::fromListingCard)
                .collect(Collectors.toList());
        return new Page<>(views, source.getCurrentPage(), source.getPageSize(), source.getTotalItems());
    }
}
