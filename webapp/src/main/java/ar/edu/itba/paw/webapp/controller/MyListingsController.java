package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.ListingCard;
import ar.edu.itba.paw.models.Page;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.webapp.dto.VehicleCardView;
import ar.edu.itba.paw.webapp.security.RydenUserDetails;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class MyListingsController {

    private static final int PAGE_SIZE = 8;

    private final ListingService listingService;

    @Autowired
    public MyListingsController(final ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping("/my-listings")
    public ModelAndView myListings(
            final Authentication authentication,
            @RequestParam(defaultValue = "0") int page) {
        final RydenUserDetails details = WebAuthUtils.requireCurrentUser(authentication);
        page = Math.max(0, page);

        final Page<ListingCard> resultPage = listingService.getOwnerListingCards(details.getUserId(), page, PAGE_SIZE);
        final List<VehicleCardView> listings = resultPage.getContent().stream()
                .map(MyListingsController::toVehicleCardView)
                .collect(Collectors.toList());

        final ModelAndView mav = new ModelAndView("myListings");
        mav.addObject("results", listings);
        mav.addObject("myListingsPage", resultPage);
        mav.addObject("activeTab", "my-listings");
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

