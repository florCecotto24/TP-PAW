package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.services.LocationService;
import ar.edu.itba.paw.webapp.util.CarEnumOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice(assignableTypes = {HomeController.class, SearchController.class})
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

