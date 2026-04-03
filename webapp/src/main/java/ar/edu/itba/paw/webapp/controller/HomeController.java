package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Car;
import ar.edu.itba.paw.services.CarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@Controller
public class HomeController {

    private final CarService carService;

    @Autowired
    public HomeController(CarService carService) {
        this.carService = carService;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView home(){
        final ModelAndView mav = new ModelAndView("home");

        final List<Car> featuredMock = List.of(
                new Car(1L, 1L, "ABC123", "BMW", "Series 5", Car.Type.SEDAN, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL),
                new Car(2L, 1L, "DEF456", "Audi", "A6 S-Line", Car.Type.SEDAN, Car.Powertrain.GASOLINE, Car.Transmission.AUTOMATIC),
                new Car(3L, 1L, "GHI789", "Volvo", "S90", Car.Type.SEDAN, Car.Powertrain.HYBRID, Car.Transmission.AUTOMATIC),
                new Car(4L, 1L, "JKL012", "Honda", "Civic", Car.Type.SEDAN, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL),
                new Car(5L, 1L, "MNO345", "Mercedes", "C-Class", Car.Type.SEDAN, Car.Powertrain.GASOLINE, Car.Transmission.AUTOMATIC)
        );

//        mav.addObject("cheapestCars", featuredMock);
//        mav.addObject("mostRecentCars", featuredMock);

        mav.addObject("cheapestCars", carService.getCheapestCars());
        mav.addObject("mostRecentCars", carService.getMostRecentCars());

        return mav;
    }

}
