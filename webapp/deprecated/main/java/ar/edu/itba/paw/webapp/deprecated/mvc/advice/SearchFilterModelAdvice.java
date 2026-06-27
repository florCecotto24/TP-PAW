package ar.edu.itba.paw.webapp.deprecated.mvc.advice;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import ar.edu.itba.paw.services.location.LocationService;
import ar.edu.itba.paw.webapp.deprecated.mvc.controller.HomeController;
import ar.edu.itba.paw.webapp.deprecated.mvc.controller.car.MyCarsController;
import ar.edu.itba.paw.webapp.deprecated.mvc.controller.reservation.MyReservationsController;
import ar.edu.itba.paw.webapp.deprecated.mvc.controller.car.SearchController;
import ar.edu.itba.paw.webapp.util.CarEnumOptions;

/**
 * Supplies search and filter dropdown options on home, search, and “my” car/reservation controllers.
 */
@ControllerAdvice(assignableTypes = {
        HomeController.class, SearchController.class,
        MyCarsController.class, MyReservationsController.class
})
public class SearchFilterModelAdvice {

    private final CarEnumOptions carEnumOptions;
    private final LocationService locationService;

    @Autowired
    public SearchFilterModelAdvice(final CarEnumOptions carEnumOptions, final LocationService locationService) {
        this.carEnumOptions = carEnumOptions;
        this.locationService = locationService;
    }

    @ModelAttribute
    public void addSearchFilterOptions(final Model model) {
        model.addAttribute("categoryFilterOptions", carEnumOptions.carTypeSelectOptions());
        model.addAttribute("transmissionFilterOptions", carEnumOptions.transmissionSelectOptions());
        model.addAttribute("powertrainFilterOptions", carEnumOptions.powertrainSelectOptions());
        model.addAttribute("ratingFilterOptions", carEnumOptions.searchRatingBandOptions());
        model.addAttribute("listingStatusOptions", carEnumOptions.listingStatusSelectOptions());
        model.addAttribute("reservationStatusOptions", carEnumOptions.reservationStatusSelectOptions());
        model.addAttribute("searchAllNeighborhoods", locationService.findAllNeighborhoods());
    }

    @ModelAttribute
    public void addSearchSanitizedNeighborhoodIds(final HttpServletRequest request, final Model model) {
        model.addAttribute(
                "searchSanitizedNeighborhoodIds",
                locationService.resolveSearchNeighborhoodIds(request.getParameterValues("neighborhoodId")));
    }
}
