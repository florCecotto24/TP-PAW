package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.util.CarEnumOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(assignableTypes = {HomeController.class, SearchController.class})
public class SearchFilterModelAdvice {

    private final CarEnumOptions carEnumOptions;

    @Autowired
    public SearchFilterModelAdvice(final CarEnumOptions carEnumOptions) {
        this.carEnumOptions = carEnumOptions;
    }

    @ModelAttribute
    public void addSearchFilterOptions(final Model model) {
        model.addAttribute("categoryFilterOptions", carEnumOptions.carTypeSelectOptions());
        model.addAttribute("transmissionFilterOptions", carEnumOptions.transmissionSelectOptions());
        model.addAttribute("powertrainFilterOptions", carEnumOptions.powertrainSelectOptions());
        model.addAttribute("priceFilterOptions", carEnumOptions.searchPriceBandOptions());
    }
}

