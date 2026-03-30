package ar.edu.itba.paw.webapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ReservationController {

    @RequestMapping(value = "/reservation/new", method = RequestMethod.GET)
    public ModelAndView newReservation(@RequestParam(value = "listingId", required = false) final Long listingId,
                                       @RequestParam(value = "availabilityId", required = false) final Long availabilityId,
                                       @RequestParam(value = "carName", required = false) final String carName,
                                       @RequestParam(value = "fromDateTime", required = false) final String fromDateTime,
                                       @RequestParam(value = "untilDateTime", required = false) final String untilDateTime,
                                       @RequestParam(value = "deliveryLocation", required = false) final String deliveryLocation) {
        final ModelAndView mav = new ModelAndView("reservationForm");
        mav.addObject("listingId", listingId);
        mav.addObject("availabilityId", availabilityId);
        mav.addObject("carName", carName == null || carName.isBlank() ? "Mercedes-Benz E-Class 300" : carName);
        mav.addObject("fromDateTime", fromDateTime);
        mav.addObject("untilDateTime", untilDateTime);
        mav.addObject("deliveryLocation", deliveryLocation);
        return mav;
    }

    @RequestMapping(value = "/reservation", method = RequestMethod.POST)
    public ModelAndView createReservation(@RequestParam("email") final String email,
                                          @RequestParam("name") final String name,
                                          @RequestParam("surname") final String surname,
                                          @RequestParam(value = "listingId", required = false) final Long listingId,
                                          @RequestParam(value = "availabilityId", required = false) final Long availabilityId,
                                          @RequestParam(value = "carName", required = false) final String carName,
                                          @RequestParam(value = "fromDateTime", required = false) final String fromDateTime,
                                          @RequestParam(value = "untilDateTime", required = false) final String untilDateTime,
                                          @RequestParam(value = "deliveryLocation", required = false) final String deliveryLocation) {
        final ModelAndView mav = new ModelAndView("reservationConfirmation");
        mav.addObject("email", email);
        mav.addObject("name", name);
        mav.addObject("surname", surname);
        mav.addObject("listingId", listingId);
        mav.addObject("availabilityId", availabilityId);
        mav.addObject("carName", carName == null || carName.isBlank() ? "Mercedes-Benz E-Class 300" : carName);
        mav.addObject("fromDateTime", fromDateTime);
        mav.addObject("untilDateTime", untilDateTime);
        mav.addObject("deliveryLocation", deliveryLocation);
        return mav;
    }
}

