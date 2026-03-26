package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Car;
import ar.edu.itba.paw.services.CarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@Controller
public class SearchController {

    private final CarService carService;

    @Autowired
    public SearchController(final CarService carService) {
        this.carService = carService;
    }

    //    Esto está un toqusín mal, pero es para probar los botones... @Flor
    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public ModelAndView search(@RequestParam(required = false) String query,
                         @RequestParam(required = false) List<String> category,
                         @RequestParam(required = false) List<String> price) {
        final ModelAndView mav = new ModelAndView("search");

        // pass filter options to JSP
        mav.addObject("categories", List.of("Manual", "Hybrid", "Automatic"));
        mav.addObject("prices", List.of("Free", "Paid"));
        mav.addObject("ratings", List.of("1", "2", "3", "4", "5"));

        // Son mocks para no insertar cars en la BD. Hay que hacerlo desde publish!
        final List<Car> mockResults = List.of(
                new Car(1L, 1L, "ABC123", "Toyotus", "Corolla", Car.Type.SEDAN, Car.Powertrain.GASOLINE,
                        Car.Trasnmission.MANUAL),
                new Car(2L, 1L, "DEF456", "Toyota", "Corolla Hybrid", Car.Type.SEDAN, Car.Powertrain.HYBRID, Car.Trasnmission.AUTOMATIC),
                new Car(3L, 1L, "GHI789", "Toyota", "bZ4X", Car.Type.SUV, Car.Powertrain.ELECTRIC, Car.Trasnmission.AUTOMATIC)
        );
        mav.addObject("results", mockResults);
        return mav;
    }
}
