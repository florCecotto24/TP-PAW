package ar.edu.itba.paw.webapp.advice;

import javax.servlet.http.HttpServletRequest;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import ar.edu.itba.paw.services.LocationService;
import ar.edu.itba.paw.webapp.controller.HomeController;
import ar.edu.itba.paw.webapp.controller.SearchController;
import ar.edu.itba.paw.webapp.util.CarEnumOptions;

@ControllerAdvice(assignableTypes = {HomeController.class, SearchController.class})
public final class SearchFilterModelAdvice {

    private final CarEnumOptions carEnumOptions;
    private final LocationService locationService;

    public SearchFilterModelAdvice(final CarEnumOptions carEnumOptions, final LocationService locationService) {
        this.carEnumOptions = carEnumOptions;
        this.locationService = locationService;
    }

    @ModelAttribute
    public void addSearchFilterOptions(final Model model) {
        model.addAttribute("categoryFilterOptions", carEnumOptions.carTypeSelectOptions());
        model.addAttribute("transmissionFilterOptions", carEnumOptions.transmissionSelectOptions());
        model.addAttribute("powertrainFilterOptions", carEnumOptions.powertrainSelectOptions());
        model.addAttribute("priceFilterOptions", carEnumOptions.searchPriceBandOptions());
        model.addAttribute("searchAllNeighborhoods", locationService.findAllNeighborhoods());
    }

    @ModelAttribute
    public void addSearchSanitizedNeighborhoodIds(final HttpServletRequest request, final Model model) {
        model.addAttribute(
                "searchSanitizedNeighborhoodIds",
                locationService.resolveSearchNeighborhoodIds(request.getParameterValues("neighborhoodId")));
    }
}
