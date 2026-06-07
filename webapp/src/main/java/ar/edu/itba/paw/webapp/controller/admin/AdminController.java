package ar.edu.itba.paw.webapp.controller.admin;


import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;


import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ar.edu.itba.paw.exception.RydenException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarModel;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.reservation.AdminReservationChatPageModel;
import ar.edu.itba.paw.models.dto.reservation.ReservationCard;
import ar.edu.itba.paw.policy.UserValidationPolicy;
import ar.edu.itba.paw.services.user.AdminService;
import ar.edu.itba.paw.webapp.form.admin.CreateAdminUserForm;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.support.UserSessionService;
import ar.edu.itba.paw.webapp.util.LocaleMessages;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;

/** Administration endpoints: user management, car moderation, catalog validation, reservation inspection. */
@Controller
@RequestMapping("/admin")
public final class AdminController {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final AdminService adminService;
    private final UserSessionService userSessionService;
    private final LocaleMessages localeMessages;
    private final UserValidationPolicy userValidationPolicy;

    public AdminController(
            final AdminService adminService,
            final UserSessionService userSessionService,
            final LocaleMessages localeMessages,
            final UserValidationPolicy userValidationPolicy) {
        this.adminService = adminService;
        this.userSessionService = userSessionService;
        this.localeMessages = localeMessages;
        this.userValidationPolicy = userValidationPolicy;
    }

    @ModelAttribute("registrationDisplayNamePartMaxLength")
    public int registrationDisplayNamePartMaxLength() {
        return userValidationPolicy.getDisplayNamePartMaxLength();
    }

    @ModelAttribute("registrationEmailMaxLength")
    public int registrationEmailMaxLength() {
        return userValidationPolicy.getRegistrationEmailMaxLength();
    }

    @ModelAttribute("registrationPasswordMaxLength")
    public int registrationPasswordMaxLength() {
        return userValidationPolicy.getRegistrationPasswordMaxLength();
    }

    /**
     * Runs an admin action and flashes a localized success or error message accordingly. Centralises
     * the {@code try { service; flash success } catch (RydenException e) { flash error }} block that
     * was repeated across every admin POST handler so the controller stays focused on routing.
     */
    private void executeAdminAction(
            final RedirectAttributes redirectAttributes,
            final String successMessageKey,
            final Runnable action) {
        try {
            action.run();
            redirectAttributes.addFlashAttribute("successMessage", localeMessages.msg(successMessageKey));
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("errorMessage", localeMessages.msg(e));
        }
    }

    @GetMapping
    public String adminRoot() {
        return "redirect:/admin/panel";
    }

    @GetMapping("/panel")
    public ModelAndView panel() {
        return new ModelAndView("admin/panel");
    }

    @GetMapping("/users")
    public ModelAndView listUsers(
            @RequestParam(defaultValue = "0") final int page,
            @CurrentUser final User currentAdmin) {
        final User me = WebAuthUtils.requireUser(currentAdmin);
        final Page<User> users = adminService.listUsers(page, DEFAULT_PAGE_SIZE);
        final ModelAndView mav = new ModelAndView("admin/users");
        mav.addObject("users", users);
        mav.addObject("currentAdminId", me.getId());
        mav.addObject("currentAdminGrantorId", me.getRoleAssignedBy().orElse(null));
        return mav;
    }

    @PostMapping("/users/{userId}/promote")
    public String promoteUser(
            @PathVariable final long userId,
            @CurrentUser final User currentAdmin,
            final RedirectAttributes redirectAttributes) {
        final long actingAdminId = WebAuthUtils.requireUser(currentAdmin).getId();
        executeAdminAction(redirectAttributes, "admin.success.promoted",
                () -> adminService.promoteUserToAdmin(userId, actingAdminId));
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{userId}/block")
    public String blockUser(
            @PathVariable final long userId,
            @CurrentUser final User currentAdmin,
            final RedirectAttributes redirectAttributes) {
        final long actingAdminId = WebAuthUtils.requireUser(currentAdmin).getId();
        executeAdminAction(redirectAttributes, "admin.success.blocked", () -> {
            adminService.blockUser(userId, actingAdminId);
            userSessionService.invalidateSessionsForUser(userId);
        });
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{userId}/unblock")
    public String unblockUser(
            @PathVariable final long userId,
            final RedirectAttributes redirectAttributes) {
        executeAdminAction(redirectAttributes, "admin.success.unblocked",
                () -> adminService.unblockUser(userId));
        return "redirect:/admin/users";
    }

    @GetMapping("/users/create")
    public ModelAndView createAdminUserForm() {
        final ModelAndView mav = new ModelAndView("admin/createAdminUser");
        mav.addObject("createAdminUserForm", new CreateAdminUserForm());
        return mav;
    }

    @PostMapping("/users/create")
    public ModelAndView createAdminUser(
            @Validated(ValidationGroups.OnCreateAdminUser.class)
            @ModelAttribute("createAdminUserForm") final CreateAdminUserForm createAdminUserForm,
            final BindingResult bindingResult,
            @CurrentUser final User currentAdmin,
            final Locale locale,
            final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return new ModelAndView("admin/createAdminUser");
        }
        final long actingAdminId = WebAuthUtils.requireUser(currentAdmin).getId();
        executeAdminAction(redirectAttributes, "admin.success.adminCreated", () ->
                adminService.createAdminUser(
                        createAdminUserForm.getEmail(),
                        createAdminUserForm.getForename(),
                        createAdminUserForm.getSurname(),
                        createAdminUserForm.getPassword(),
                        actingAdminId,
                        locale));
        return new ModelAndView("redirect:/admin/users");
    }

    @GetMapping("/cars")
    public ModelAndView listCars(
            @RequestParam(defaultValue = "0") final int page) {
        final Page<Car> cars = adminService.listCars(page, DEFAULT_PAGE_SIZE);
        final ModelAndView mav = new ModelAndView("admin/cars");
        mav.addObject("cars", cars);
        mav.addObject("adminActionForm", new HashMap<>());
        return mav;
    }

    @PostMapping("/cars/{carId}/pause")
    public String pauseCar(
            @PathVariable final long carId,
            @RequestParam(required = false, defaultValue = "false") final boolean fromCarDetail,
            @RequestParam(required = false) final Long carDetailId,
            @RequestParam(required = false) final Integer page,
            @CurrentUser final User currentAdmin,
            final Locale locale,
            final RedirectAttributes redirectAttributes) {
        final long actingAdminId = WebAuthUtils.requireUser(currentAdmin).getId();
        executeAdminAction(redirectAttributes, "admin.success.carPaused",
                () -> adminService.adminPauseCar(carId, actingAdminId, locale));
        return adminCarsRedirect(fromCarDetail, carDetailId, page);
    }

    @PostMapping("/cars/{carId}/resume")
    public String resumeCar(
            @PathVariable final long carId,
            @RequestParam(required = false, defaultValue = "false") final boolean fromCarDetail,
            @RequestParam(required = false) final Long carDetailId,
            @RequestParam(required = false) final Integer page,
            @CurrentUser final User currentAdmin,
            final RedirectAttributes redirectAttributes) {
        final long actingAdminId = WebAuthUtils.requireUser(currentAdmin).getId();
        executeAdminAction(redirectAttributes, "admin.success.carResumed",
                () -> adminService.adminResumeCar(carId, actingAdminId));
        return adminCarsRedirect(fromCarDetail, carDetailId, page);
    }

    private String adminCarsRedirect(final boolean fromCarDetail, final Long carDetailId, final Integer page) {
        if (fromCarDetail && carDetailId != null) {
            return "redirect:/cars/" + carDetailId;
        }
        if (page != null && page >= 0) {
            return "redirect:/admin/cars?page=" + page;
        }
        return "redirect:/admin/cars";
    }

    @GetMapping("/catalog")
    public ModelAndView catalog() {
        final List<CarModel> pendingModels = adminService.findPendingModels();
        final ModelAndView mav = new ModelAndView("admin/catalog");
        mav.addObject("pendingModels", pendingModels);
        mav.addObject("adminActionForm", new HashMap<>());
        return mav;
    }

    @PostMapping("/catalog/entries/{modelId}/validate")
    public String validateCatalogEntry(
            @PathVariable final long modelId,
            final Locale locale,
            final RedirectAttributes redirectAttributes) {
        executeAdminAction(redirectAttributes, "admin.success.entryValidated",
                () -> adminService.validateCatalogEntry(modelId, locale));
        return "redirect:/admin/catalog";
    }

    @PostMapping("/catalog/entries/{modelId}/reject")
    public String rejectCatalogEntry(
            @PathVariable final long modelId,
            final Locale locale,
            final RedirectAttributes redirectAttributes) {
        executeAdminAction(redirectAttributes, "admin.success.entryRejected",
                () -> adminService.rejectCatalogEntry(modelId, locale));
        return "redirect:/admin/catalog";
    }

    @GetMapping("/reservations")
    public ModelAndView listReservations(
            @RequestParam(defaultValue = "0") final int page) {
        final Page<ReservationCard> reservations = adminService.listAllReservations(page, DEFAULT_PAGE_SIZE);
        final ModelAndView mav = new ModelAndView("admin/reservations");
        mav.addObject("reservations", reservations);
        return mav;
    }

    @GetMapping("/reservations/{reservationId}/chat")
    public ModelAndView reservationChat(
            @PathVariable final long reservationId,
            @RequestParam(defaultValue = "0") final int page) {
        final Optional<AdminReservationChatPageModel> pageModelOpt =
                adminService.loadReservationChatPage(reservationId, page, 50);
        if (pageModelOpt.isEmpty()) {
            return new ModelAndView("redirect:/admin/reservations");
        }
        final ModelAndView mav = new ModelAndView("admin/reservationChat");
        pageModelOpt.get().populateModel(mav::addObject);
        return mav;
    }

}
