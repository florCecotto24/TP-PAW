package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Car;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.CarService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ar.edu.itba.paw.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

//@Controller
public class HelloWorldController {

//    private final UserService userService;
//    private final CarService carService;
//
//    @Autowired
//    public HelloWorldController(UserService userService, CarService carService) {
//        this.userService = userService;
//        this.carService = carService;
//    }
//    @RequestMapping(value = "/", method = RequestMethod.GET)
//    public ModelAndView helloWorld(){
//        final ModelAndView mav = new ModelAndView("home");
//        final List<Car> featuredMock = List.of(
//                new Car(1L, 1L, "ABC123", "BMW", "Series 5", Car.Type.SEDAN, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL),
//                new Car(2L, 1L, "DEF456", "Audi", "A6 S-Line", Car.Type.SEDAN, Car.Powertrain.GASOLINE, Car.Transmission.AUTOMATIC),
//                new Car(3L, 1L, "GHI789", "Volvo", "S90", Car.Type.SEDAN, Car.Powertrain.HYBRID, Car.Transmission.AUTOMATIC),
//                new Car(4L, 1L, "JKL012", "Honda", "Civic", Car.Type.SEDAN, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL),
//                new Car(5L, 1L, "MNO345", "Mercedes", "C-Class", Car.Type.SEDAN, Car.Powertrain.GASOLINE, Car.Transmission.AUTOMATIC)
//        );
//
//        mav.addObject("featuredCars", featuredMock);
//        mav.addObject("mostSearchedCars", featuredMock);
//
//        return mav;
//    }
//
//    @RequestMapping(value = "/", method = RequestMethod.POST)
//    public ModelAndView createUser(@RequestParam("email") final String email,
//                                   @RequestParam("name") final String name){
//        final ModelAndView mav = new ModelAndView("home");
//        User user = userService.createUser(email, name);
//        mav.addObject("message", "Hello World " + user.getName());
//
//        final List<User> mockResults = List.of(
//                new User(1L, "facu@gmail.com", "facu"),
//                new User(2L, "flor@gmail.com", "flor"),
//                new User(3L, "caro@gmail.com", "caro")
//        );
//        mav.addObject("results", mockResults);
//
//        return mav;
//    }
}
