package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Car;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.services.CarService;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.webapp.form.PublishCarForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;

@Controller
@RequestMapping("/publish-car")
public class PublishCarFormController {

    @ModelAttribute("carTypes")
    public Car.Type[] carTypes() {
        return Car.Type.values();
    }

    @ModelAttribute("powertrains")
    public Car.Powertrain[] powertrains() {
        return Car.Powertrain.values();
    }

    @ModelAttribute("transmissions")
    public Car.Transmission[] transmissions() {
        return Car.Transmission.values();
    }

    private final ListingService listingService;
    private final CarService carService;

    @Autowired
    public PublishCarFormController(ListingService listingService, CarService carService) {
        this.listingService = listingService;
        this.carService = carService;
    }

    @GetMapping
    public ModelAndView index(@ModelAttribute("publishCarForm") final PublishCarForm form) {
        return new ModelAndView("publishCarForm");
    }

    @PostMapping
    public ModelAndView formSubmit(@Valid @ModelAttribute("publishCarForm") final PublishCarForm form, final
                             BindingResult errors) {
        if (errors.hasErrors()) {
            return index(form);
        }

        final Car car = carService.createCar(22, form.getPlate(), form.getBrand(), form.getModel(), form.getType(),
                form.getPowertrain(), form.getTransmission());
        final Listing listing = listingService.createListing(car.getId(), Listing.Status.ACTIVE, form.getPricePerDay(), form.getStartPoint(), form.getDescription());

        //@TODO crear una pestaña de éxito con la info de la publicación y redirigir a esa página
        return new ModelAndView("redirect:/search");
    }
}