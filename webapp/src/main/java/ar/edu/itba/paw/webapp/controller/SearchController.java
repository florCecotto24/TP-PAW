package ar.edu.itba.paw.webapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;
import java.util.List;

@Controller
public class SearchController {

//    Esto está un toqusín mal, pero es para probar los botones... @Flor
    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public ModelAndView search(@RequestParam(required = false) String query,
                         @RequestParam(required = false) List<String> category,
                         @RequestParam(required = false) List<String> price) {
        final ModelAndView mav = new ModelAndView("/WEB-INF/jsp/search.jsp");

        // pass filter options to JSP
        mav.addObject("categories", List.of("Manual", "Hybrid", "Automatic"));
        mav.addObject("prices", List.of("Free", "Paid"));
        mav.addObject("ratings", List.of("1", "2", "3", "4", "5"));

        // use selected filters
        // List<Result> results = service.search(query, category, price);
        // model.addAttribute("results", results);
        return mav;
    }
}
