package ar.edu.itba.paw.webapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class PublishController {

    @GetMapping("/publish-car")
    public ModelAndView publishCar() {
        ModelAndView mav = new ModelAndView("publishCar");
        mav.addObject("activeTab", "publish-car");

        return mav;
    }
}
