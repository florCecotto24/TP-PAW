package ar.edu.itba.paw.webapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class CarDetailController {

    @RequestMapping(value = "/car-detail", method = RequestMethod.GET)
    public ModelAndView carDetail() {
        final ModelAndView mav = new ModelAndView("carDetail");
        return mav;
    }
}
