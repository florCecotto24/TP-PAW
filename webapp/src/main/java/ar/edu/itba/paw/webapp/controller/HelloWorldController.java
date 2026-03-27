package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ar.edu.itba.paw.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Controller
public class HelloWorldController {

    private final UserService userService;

    @Autowired
    public HelloWorldController(UserService userService) {
        this.userService = userService;
    }
    
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView helloWorld(){
        final ModelAndView mav = new ModelAndView("home");
        mav.addObject("message", "Hello World from Controller");
        return mav;
    }

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public ModelAndView createUser(@RequestParam("email") final String email,
                                   @RequestParam("name") final String name){
        final ModelAndView mav = new ModelAndView("home");
        User user = userService.createUser(email, name);
        mav.addObject("message", "Hello World " + user.getName());

        final List<User> mockResults = List.of(
                new User(1L, "facu@gmail.com", "facu"),
                new User(2L, "flor@gmail.com", "flor"),
                new User(3L, "caro@gmail.com", "caro")
        );
        mav.addObject("results", mockResults);

        return mav;
    }
}
