package ar.edu.itba.paw.webapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ar.edu.itba.paw.services.AdminService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.CarService;
import ar.edu.itba.paw.services.ReservationService;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;
    private final CarService carService;
    private final ReservationService reservationService;

    @Autowired
    public AdminController(AdminService adminService, UserService userService, CarService carService, ReservationService reservationService) {
        this.adminService = adminService;
        this.userService = userService;
        this.carService = carService;
        this.reservationService = reservationService;
    }

    @GetMapping
    public ModelAndView panel() {
        return new ModelAndView("admin/panel");
    }

    @GetMapping("/searchUser")
    public ModelAndView searchUser(@RequestParam("email") String email) {
        ModelAndView mav = new ModelAndView("admin/panel");
        mav.addObject("tab", "users");
        userService.findByEmail(email).ifPresentOrElse(
            user -> mav.addObject("userSearchResult", user),
            () -> mav.addObject("error", "User not found")
        );
        return mav;
    }

    @PostMapping("/promoteUser")
    public ModelAndView promoteUser(@AuthenticationPrincipal RydenUserDetails admin, @RequestParam("targetUserId") long targetUserId) {
        try {
            adminService.promoteToAdmin(admin.getUserId(), targetUserId);
            ModelAndView mav = new ModelAndView("redirect:/admin?tab=users");
            return mav;
        } catch (Exception e) {
            ModelAndView mav = new ModelAndView("admin/panel");
            mav.addObject("tab", "users");
            mav.addObject("error", e.getMessage());
            return mav;
        }
    }

    @PostMapping("/blockUser")
    public ModelAndView blockUser(@AuthenticationPrincipal RydenUserDetails admin, @RequestParam("targetUserId") long targetUserId) {
        try {
            adminService.blockUser(admin.getUserId(), targetUserId);
            ModelAndView mav = new ModelAndView("redirect:/admin?tab=users");
            return mav;
        } catch (Exception e) {
            ModelAndView mav = new ModelAndView("admin/panel");
            mav.addObject("tab", "users");
            mav.addObject("error", e.getMessage());
            return mav;
        }
    }

    @GetMapping("/searchCar")
    public ModelAndView searchCar(@RequestParam("carId") long carId) {
        ModelAndView mav = new ModelAndView("admin/panel");
        mav.addObject("tab", "cars");
        carService.getCarById(carId).ifPresentOrElse(
            car -> mav.addObject("carSearchResult", car),
            () -> mav.addObject("error", "Car not found")
        );
        return mav;
    }

    @PostMapping("/pauseCar")
    public ModelAndView pauseCar(@AuthenticationPrincipal RydenUserDetails admin, @RequestParam("carId") long carId) {
        adminService.pauseCar(admin.getUserId(), carId);
        return new ModelAndView("redirect:/admin?tab=cars");
    }

    @PostMapping("/resumeCar")
    public ModelAndView resumeCar(@AuthenticationPrincipal RydenUserDetails admin, @RequestParam("carId") long carId) {
        adminService.resumeCar(admin.getUserId(), carId);
        return new ModelAndView("redirect:/admin?tab=cars");
    }

    @GetMapping("/searchReservation")
    public ModelAndView searchReservation(@RequestParam("reservationId") long reservationId) {
        ModelAndView mav = new ModelAndView("admin/panel");
        mav.addObject("tab", "reservations");
        reservationService.getReservationById(reservationId).ifPresentOrElse(
            res -> mav.addObject("resSearchResult", res),
            () -> mav.addObject("error", "Reservation not found")
        );
        return mav;
    }

    @GetMapping("/reservation/{id}/chat")
    public ModelAndView viewChat(@PathVariable("id") long id) {
        ModelAndView mav = new ModelAndView("admin/chat");
        reservationService.getReservationById(id).ifPresentOrElse(
            res -> mav.addObject("reservation", res),
            () -> mav.addObject("error", "Reservation not found")
        );
        return mav;
    }
}
