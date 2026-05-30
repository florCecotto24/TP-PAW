package ar.edu.itba.paw.webapp.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
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
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.CarModel;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.ReservationMessage;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.ReservationCard;
import ar.edu.itba.paw.services.AdminService;
import ar.edu.itba.paw.services.policy.UserValidationPolicy;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.form.CreateAdminUserForm;
import ar.edu.itba.paw.webapp.util.LocaleMessages;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;

import org.springframework.security.core.Authentication;

/** Administration endpoints: user management, car moderation, catalog validation, reservation inspection. */
@Controller
@RequestMapping("/admin")
public final class AdminController {

    private static final Logger LOG = LoggerFactory.getLogger(AdminController.class);

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final AdminService adminService;
    private final SessionRegistry sessionRegistry;
    private final LocaleMessages localeMessages;
    private final UserValidationPolicy userValidationPolicy;

    public AdminController(
            final AdminService adminService,
            final SessionRegistry sessionRegistry,
            final LocaleMessages localeMessages,
            final UserValidationPolicy userValidationPolicy) {
        this.adminService = adminService;
        this.sessionRegistry = sessionRegistry;
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
            final Authentication authentication) {
        final long currentAdminId = requireAdminId(authentication);
        final Long currentAdminGrantorId = requireAdminGrantorId(authentication).orElse(null);
        final Page<User> users = adminService.listUsers(page, DEFAULT_PAGE_SIZE);
        final ModelAndView mav = new ModelAndView("admin/users");
        mav.addObject("users", users);
        mav.addObject("currentAdminId", currentAdminId);
        mav.addObject("currentAdminGrantorId", currentAdminGrantorId);
        return mav;
    }

    @PostMapping("/users/{userId}/promote")
    public String promoteUser(
            @PathVariable final long userId,
            final Authentication authentication,
            final RedirectAttributes redirectAttributes) {
        final long actingAdminId = requireAdminId(authentication);
        try {
            adminService.promoteUserToAdmin(userId, actingAdminId);
            redirectAttributes.addFlashAttribute("successMessage", localeMessages.msg("admin.success.promoted"));
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("errorMessage", localeMessages.msg(e));
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{userId}/block")
    public String blockUser(
            @PathVariable final long userId,
            final Authentication authentication,
            final RedirectAttributes redirectAttributes) {
        final long actingAdminId = requireAdminId(authentication);
        try {
            adminService.blockUser(userId, actingAdminId);
            invalidateUserSessions(userId);
            redirectAttributes.addFlashAttribute("successMessage", localeMessages.msg("admin.success.blocked"));
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("errorMessage", localeMessages.msg(e));
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{userId}/unblock")
    public String unblockUser(
            @PathVariable final long userId,
            final RedirectAttributes redirectAttributes) {
        try {
            adminService.unblockUser(userId);
            redirectAttributes.addFlashAttribute("successMessage", localeMessages.msg("admin.success.unblocked"));
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("errorMessage", localeMessages.msg(e));
        }
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
            final Authentication authentication,
            final Locale locale,
            final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return new ModelAndView("admin/createAdminUser");
        }
        final long actingAdminId = requireAdminId(authentication);
        try {
            adminService.createAdminUser(
                    createAdminUserForm.getEmail(),
                    createAdminUserForm.getForename(),
                    createAdminUserForm.getSurname(),
                    createAdminUserForm.getPassword(),
                    actingAdminId,
                    locale);
            redirectAttributes.addFlashAttribute("successMessage", localeMessages.msg("admin.success.adminCreated"));
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("errorMessage", localeMessages.msg(e));
        }
        return new ModelAndView("redirect:/admin/users");
    }

    @GetMapping("/cars")
    public ModelAndView listAdminPausedCars() {
        final List<Car> cars = adminService.findAdminPausedCars();
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
            final Authentication authentication,
            final Locale locale,
            final RedirectAttributes redirectAttributes) {
        final long actingAdminId = requireAdminId(authentication);
        try {
            adminService.adminPauseCar(carId, actingAdminId, locale);
            redirectAttributes.addFlashAttribute("successMessage", localeMessages.msg("admin.success.carPaused"));
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("errorMessage", localeMessages.msg(e));
        }
        if (fromCarDetail && carDetailId != null) {
            return "redirect:/car-detail?carId=" + carDetailId;
        }
        return "redirect:/admin/cars";
    }

    @PostMapping("/cars/{carId}/resume")
    public String resumeCar(
            @PathVariable final long carId,
            @RequestParam(required = false, defaultValue = "false") final boolean fromCarDetail,
            @RequestParam(required = false) final Long carDetailId,
            final Authentication authentication,
            final RedirectAttributes redirectAttributes) {
        final long actingAdminId = requireAdminId(authentication);
        try {
            adminService.adminResumeCar(carId, actingAdminId);
            redirectAttributes.addFlashAttribute("successMessage", localeMessages.msg("admin.success.carResumed"));
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("errorMessage", localeMessages.msg(e));
        }
        if (fromCarDetail && carDetailId != null) {
            return "redirect:/car-detail?carId=" + carDetailId;
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
        try {
            adminService.validateCatalogEntry(modelId, locale);
            redirectAttributes.addFlashAttribute("successMessage", localeMessages.msg("admin.success.entryValidated"));
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("errorMessage", localeMessages.msg(e));
        }
        return "redirect:/admin/catalog";
    }

    @PostMapping("/catalog/entries/{modelId}/reject")
    public String rejectCatalogEntry(
            @PathVariable final long modelId,
            final Locale locale,
            final RedirectAttributes redirectAttributes) {
        try {
            adminService.rejectCatalogEntry(modelId, locale);
            redirectAttributes.addFlashAttribute("successMessage", localeMessages.msg("admin.success.entryRejected"));
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("errorMessage", localeMessages.msg(e));
        }
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
        final Reservation reservation = adminService.getReservationById(reservationId).orElse(null);
        if (reservation == null) {
            return new ModelAndView("redirect:/admin/reservations");
        }
        final int pageSize = 50;
        final List<ReservationMessage> messages =
                adminService.getAdminChatMessages(reservationId, page * pageSize, pageSize);
        final long totalMessages = adminService.countReservationMessages(reservationId);
        final ModelAndView mav = new ModelAndView("admin/reservationChat");
        mav.addObject("reservation", reservation);
        mav.addObject("messages", messages);
        mav.addObject("totalMessages", totalMessages);
        mav.addObject("currentPage", page);
        mav.addObject("pageSize", pageSize);
        return mav;
    }

    private long requireAdminId(final Authentication authentication) {
        return WebAuthUtils.requireCurrentUser(authentication).getUserId();
    }

    private Optional<Long> requireAdminGrantorId(final Authentication authentication) {
        return WebAuthUtils.requireCurrentUser(authentication).getRoleAssignedBy();
    }

    private void invalidateUserSessions(final long userId) {
        for (final Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal instanceof RydenUserDetails details && details.getUserId() == userId) {
                for (final SessionInformation session : sessionRegistry.getAllSessions(principal, false)) {
                    session.expireNow();
                }
            }
        }
    }
}
