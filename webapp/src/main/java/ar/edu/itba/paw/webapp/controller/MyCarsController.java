package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.CarAvailability;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.util.search.MyHubSortSanitizer;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.reservation.CarReservationsListPageModel;
import ar.edu.itba.paw.models.dto.car.CarAvailabilityEditorPageModel;
import ar.edu.itba.paw.models.dto.car.MyCarDetailPageModel;
import ar.edu.itba.paw.models.dto.car.OwnerCarDetailPageModel;
import ar.edu.itba.paw.models.dto.reservation.OwnerReservationsListPageModel;
import ar.edu.itba.paw.services.CarService;
import ar.edu.itba.paw.services.CarAvailabilityService;
import ar.edu.itba.paw.services.CarAvailabilityEditorViewService;
import ar.edu.itba.paw.services.MyCarDetailViewService;
import ar.edu.itba.paw.services.ReservationService;
import ar.edu.itba.paw.services.ReservationViewService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.policy.PaginationPolicy;
import ar.edu.itba.paw.services.policy.ProfileDocumentUploadPolicy;
import ar.edu.itba.paw.exception.car.AvailabilityRiderLeadViolationException;
import ar.edu.itba.paw.exception.car.CarValidationException;
import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.webapp.support.facade.CarInsuranceUploadFacade;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.support.OwnerCarLookup;
import ar.edu.itba.paw.webapp.support.PageClampRedirect;
import ar.edu.itba.paw.webapp.util.LocaleMessages;
import ar.edu.itba.paw.webapp.form.CreateCarAvailabilityForm;
import ar.edu.itba.paw.webapp.form.CarAvailabilityEditForm;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;
import ar.edu.itba.paw.webapp.validation.CarAvailabilityNeighborhoodFormValidator;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Owner hub: car cards, edit, pause/finish, and per-car reservation analytics and actions. */
@Controller
@RequestMapping("/my-cars")
public final class MyCarsController {

    private final ReservationService reservationService;
    private final ReservationViewService reservationViewService;
    private final CarAvailabilityNeighborhoodFormValidator carAvailabilityNeighborhoodFormValidator;
    private final PaginationPolicy paginationPolicy;
    private final LocaleMessages localeMessages;
    private final CarService carService;
    private final UserService userService;
    private final CarAvailabilityService carAvailabilityService;
    private final CarAvailabilityEditorViewService carAvailabilityEditorViewService;
    private final MyCarDetailViewService myCarDetailViewService;
    private final ProfileDocumentUploadPolicy profileDocumentUploadPolicy;
    private final OwnerCarLookup ownerCarLookup;
    private final CarInsuranceUploadFacade carInsuranceUploadFacade;

    public MyCarsController(
            final ReservationService reservationService,
            final ReservationViewService reservationViewService,
            final CarAvailabilityNeighborhoodFormValidator carAvailabilityNeighborhoodFormValidator,
            final PaginationPolicy paginationPolicy,
            final LocaleMessages localeMessages,
            final CarService carService,
            final UserService userService,
            final CarAvailabilityService carAvailabilityService,
            final CarAvailabilityEditorViewService carAvailabilityEditorViewService,
            final MyCarDetailViewService myCarDetailViewService,
            final ProfileDocumentUploadPolicy profileDocumentUploadPolicy,
            final OwnerCarLookup ownerCarLookup,
            final CarInsuranceUploadFacade carInsuranceUploadFacade) {
        this.reservationService = reservationService;
        this.reservationViewService = reservationViewService;
        this.carAvailabilityNeighborhoodFormValidator = carAvailabilityNeighborhoodFormValidator;
        this.paginationPolicy = paginationPolicy;
        this.localeMessages = localeMessages;
        this.carService = carService;
        this.userService = userService;
        this.carAvailabilityService = carAvailabilityService;
        this.carAvailabilityEditorViewService = carAvailabilityEditorViewService;
        this.myCarDetailViewService = myCarDetailViewService;
        this.profileDocumentUploadPolicy = profileDocumentUploadPolicy;
        this.ownerCarLookup = ownerCarLookup;
        this.carInsuranceUploadFacade = carInsuranceUploadFacade;
    }

    @InitBinder("editForm")
    public void initEditFormBinder(final WebDataBinder binder) {
        binder.addValidators(carAvailabilityNeighborhoodFormValidator);
    }

    @InitBinder("createCarAvailabilityForm")
    public void initCreateCarAvailabilityFormBinder(final WebDataBinder binder) {
        binder.addValidators(carAvailabilityNeighborhoodFormValidator);
    }

    @ModelAttribute("uploadMaxProfileDocumentMegabytes")
    public int uploadMaxProfileDocumentMegabytes() {
        return profileDocumentUploadPolicy.getMaxMegabytesRoundedUp();
    }

    private static final String DEFAULT_SORT = "date,desc";

    @GetMapping
    public ModelAndView myCars(
            @CurrentUser final User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) final List<String> listingStatus,
            @RequestParam(required = false) final String q,
            @RequestParam(required = false) final List<String> category,
            @RequestParam(required = false) final List<String> transmission,
            @RequestParam(required = false) final List<String> powertrain,
            @RequestParam(required = false) final BigDecimal priceMin,
            @RequestParam(required = false) final BigDecimal priceMax,
            @RequestParam(required = false) final List<String> rating,
            @RequestParam(required = false) final String sort,
            final HttpServletRequest request) {
        final User me = WebAuthUtils.requireUser(currentUser);
        page = Math.max(0, page);
        final String listingsSort = MyHubSortSanitizer.sanitize(sort, DEFAULT_SORT);
        final var listingsCriteria = carService.buildOwnerCarSearchCriteria(
                me.getId(), category, transmission, powertrain, priceMin, priceMax,
                listingStatus, rating, q, page, listingsSort);
        final Page<CarCard> resultPage = carService.getOwnerCarCards(listingsCriteria);
        final Optional<ModelAndView> clamp = PageClampRedirect.ifOutOfRange(page, resultPage, "page", request);
        if (clamp.isPresent()) {
            return clamp.get();
        }
        final ModelAndView mav = new ModelAndView("car/myCars");
        mav.addObject("results", resultPage.getContent());
        mav.addObject("myListingsPage", resultPage);
        mav.addObject("activeTab", "my-cars");
        mav.addObject("listingsCurrentSort", listingsSort);
        mav.addObject("currentUserBlocked", me.isBlocked());
        // Per-car badge "you have a reservation requiring a refund receipt": independent of whether the
        // owner is already blocked, so it surfaces the obligation even before the deadline expires.
        mav.addObject("pendingRefundCarIds",
                reservationService.findOwnerCarIdsWithReservationRequiringRefundProof(me.getId()));
        return mav;
    }

    @GetMapping("/reservations")
    public ModelAndView ownerReservations(
            @CurrentUser final User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) final List<String> ownerStatus,
            @RequestParam(required = false) final String ownerQ,
            @RequestParam(required = false) final List<String> ownerCategory,
            @RequestParam(required = false) final List<String> ownerTransmission,
            @RequestParam(required = false) final List<String> ownerPowertrain,
            @RequestParam(required = false) final BigDecimal ownerPriceMin,
            @RequestParam(required = false) final BigDecimal ownerPriceMax,
            @RequestParam(required = false) final List<String> ownerRating,
            @RequestParam(required = false) final String ownerSort,
            final HttpServletRequest request) {
        final User me = WebAuthUtils.requireUser(currentUser);
        return renderOwnerReservations(
                me, /* selectedCar */ null, /* carIdFilter */ null,
                page, ownerStatus, ownerQ, ownerCategory, ownerTransmission, ownerPowertrain,
                ownerPriceMin, ownerPriceMax, ownerRating, ownerSort, request);
    }

    @GetMapping("/reservations/{carId}")
    public ModelAndView ownerReservationsForCar(
            @CurrentUser final User currentUser,
            @PathVariable final long carId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) final List<String> ownerStatus,
            @RequestParam(required = false) final String ownerQ,
            @RequestParam(required = false) final List<String> ownerCategory,
            @RequestParam(required = false) final List<String> ownerTransmission,
            @RequestParam(required = false) final List<String> ownerPowertrain,
            @RequestParam(required = false) final BigDecimal ownerPriceMin,
            @RequestParam(required = false) final BigDecimal ownerPriceMax,
            @RequestParam(required = false) final List<String> ownerRating,
            @RequestParam(required = false) final String ownerSort,
            final HttpServletRequest request) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final OwnerCarLookup.Result lookup = ownerCarLookup.loadOwnedCar(carId, "/my-cars/reservations");
        if (lookup.redirect().isPresent()) {
            return lookup.redirect().get();
        }
        return renderOwnerReservations(
                me, lookup.car().orElseThrow(), carId,
                page, ownerStatus, ownerQ, ownerCategory, ownerTransmission, ownerPowertrain,
                ownerPriceMin, ownerPriceMax, ownerRating, ownerSort, request);
    }

    private ModelAndView renderOwnerReservations(
            final User me,
            final Car selectedCarOrNull,
            final Long carIdFilterOrNull,
            int page,
            final List<String> ownerStatus,
            final String ownerQ,
            final List<String> ownerCategory,
            final List<String> ownerTransmission,
            final List<String> ownerPowertrain,
            final BigDecimal ownerPriceMin,
            final BigDecimal ownerPriceMax,
            final List<String> ownerRating,
            final String ownerSort,
            final HttpServletRequest request) {
        page = Math.max(0, page);
        final String sort = MyHubSortSanitizer.sanitize(ownerSort, DEFAULT_SORT);
        final var criteria = reservationService.buildReservationSearchCriteria(
                me.getId(), null, ownerCategory, ownerTransmission, ownerPowertrain,
                ownerPriceMin, ownerPriceMax, ownerRating, ownerStatus, page, sort, ownerQ, carIdFilterOrNull);
        final OwnerReservationsListPageModel pageModel = reservationViewService.loadOwnerReservationsListPage(
                criteria, selectedCarOrNull, sort, LocaleContextHolder.getLocale());
        final Optional<ModelAndView> clamp = PageClampRedirect.ifOutOfRange(
                page, pageModel.getResultPage(), "page", request);
        if (clamp.isPresent()) {
            return clamp.get();
        }
        final ModelAndView mav = new ModelAndView("reservation/ownerReservations");
        pageModel.populateModel(mav::addObject);
        mav.addObject("activeTab", "owner-reservations");
        // Per-reservation badge "you must upload a refund receipt for this reservation": same condition
        // as the per-car badge in /my-cars but indexed by reservation id for the cards on this page.
        mav.addObject("pendingRefundReservationIds",
                reservationService.findOwnerReservationIdsRequiringRefundProof(me.getId()));
        return mav;
    }

    @PostMapping("/car/{carId}/edit")
    public ModelAndView editCar(
            @CurrentUser final User currentUser,
            @PathVariable("carId") final long carId,
            @Validated(ValidationGroups.OnListingEdit.class) @ModelAttribute("editForm") final CarAvailabilityEditForm editForm,
            final BindingResult errors) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final OwnerCarLookup.Result lookup = ownerCarLookup.loadOwnedCar(carId, "/my-cars");
        if (lookup.redirect().isPresent()) {
            return lookup.redirect().get();
        }
        final Car car = lookup.car().orElseThrow();
        if (car.getStatus() == Car.Status.DEACTIVATED) {
            return new ModelAndView(redirectTo("/my-cars/car/" + carId));
        }
        if (errors.hasErrors()) {
            return renderMyCarDetailOrRedirect(me, car, editForm, /* withPreview */ true);
        }
        final List<CarAvailability> existing = carAvailabilityService.findByCarId(carId);
        final List<AvailabilityPeriod> newPeriods = editForm.toAvailabilityPeriods();
        final List<BigDecimal> newPeriodPrices = editForm.toPeriodPrices();
        if (newPeriods.isEmpty()) {
            return new ModelAndView(redirectTo("/my-cars/car/" + carId));
        }
        if (existing.isEmpty()) {
            try {
                carAvailabilityService.createCarAvailabilityPeriods(
                        carId,
                        editForm.getPricePerDay(),
                        editForm.getStartPointStreet(),
                        editForm.getStartPointNumber(),
                        editForm.getNeighborhoodId(),
                        editForm.getCheckInTime(),
                        editForm.getCheckOutTime(),
                        newPeriods,
                        newPeriodPrices,
                        1);
            } catch (final CarValidationException e) {
                errors.reject(e.getMessageCode(), e.getMessageArgs(), localeMessages.msg(e));
                return renderMyCarDetailOrRedirect(me, car, editForm, /* withPreview */ false);
            }
        } else {
            final CarAvailability mostRecent = existing.stream()
                    .max(java.util.Comparator.comparing(CarAvailability::getCreatedAt))
                    .get();
            try {
                for (int i = 0; i < newPeriods.size(); i++) {
                    final AvailabilityPeriod np = newPeriods.get(i);
                    final BigDecimal price = (i < newPeriodPrices.size() && newPeriodPrices.get(i) != null)
                            ? newPeriodPrices.get(i)
                            : editForm.getPricePerDay();
                    carAvailabilityService.applyOwnerEditByCar(
                            carId,
                            mostRecent.getStartInclusive(),
                            mostRecent.getEndInclusive(),
                            np.getStartInclusive(),
                            np.getEndInclusive(),
                            price,
                            editForm.getStartPointStreet(),
                            editForm.getStartPointNumber(),
                            editForm.getNeighborhoodId(),
                            editForm.getCheckInTime(),
                            editForm.getCheckOutTime());
                }
            } catch (final CarValidationException e) {
                errors.reject(e.getMessageCode(), e.getMessageArgs(), localeMessages.msg(e));
            }
        }
        return new ModelAndView(redirectTo("/my-cars/car/" + carId));
    }

    /**
     * Used by both {@code editCar} branches (binding errors + post-create CarValidationException)
     * to re-render the same car-detail page with the bound edit form on top. When the rich page
     * model is not available, falls back to the {@code /my-cars/car/{carId}} redirect — matching
     * the original handler's pre-refactor behaviour.
     */
    private ModelAndView renderMyCarDetailOrRedirect(
            final User me, final Car car, final CarAvailabilityEditForm editForm, final boolean withPreview) {
        final MyCarDetailPageModel pm = myCarDetailViewService.loadMyCarDetailPage(
                me.getId(), car, LocaleContextHolder.getLocale(), 3);
        if (pm.getVariant() != MyCarDetailPageModel.Variant.RICH) {
            return new ModelAndView(redirectTo("/my-cars/car/" + car.getId()));
        }
        final ModelAndView mav = new ModelAndView("car/myCarDetail");
        pm.getOwnerPageModel().orElseThrow().populateModel(mav::addObject);
        mav.addObject("editForm", editForm);
        if (withPreview) {
            mav.addObject("previewReservations", pm.getPreviewReservations());
            mav.addObject("reservationPreviewTotal", pm.getReservationPreviewTotal());
        }
        mav.addObject("activeTab", "my-cars");
        return mav;
    }

    @PostMapping("/car/{carId}/deactivate")
    public ModelAndView deactivateCar(
            @CurrentUser final User currentUser,
            @PathVariable("carId") final long carId) {
        final User me = WebAuthUtils.requireUser(currentUser);
        carService.deactivateCar(me.getId(), carId);
        return new ModelAndView(redirectTo("/my-cars/car/" + carId));
    }

    @PostMapping("/car/{carId}/toggle")
    public ModelAndView toggleCar(
            @CurrentUser final User currentUser,
            @PathVariable("carId") final long carId,
            final RedirectAttributes redirectAttributes) {
        final User me = WebAuthUtils.requireUser(currentUser);
        try {
            carService.toggleCarStatus(me.getId(), carId);
        } catch (final CarValidationException e) {
            redirectAttributes.addFlashAttribute("carToggleErrorMessage", localeMessages.msg(e));
        }
        return new ModelAndView(redirectTo("/my-cars/car/" + carId));
    }

    @GetMapping("/car/{carId}/reservations")
    public ModelAndView carReservations(
            @CurrentUser final User currentUser,
            @PathVariable("carId") final long carId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) final String reservationStatus,
            final HttpServletRequest request) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final OwnerCarLookup.Result lookup = ownerCarLookup.loadOwnedCar(carId, "/my-cars");
        if (lookup.redirect().isPresent()) {
            return lookup.redirect().get();
        }
        page = Math.max(0, page);
        final String statusFilter = reservationViewService.normalizeReservationStatusQueryParam(reservationStatus);
        final CarReservationsListPageModel pageModel = reservationViewService.loadCarReservationsListPage(
                me.getId(), lookup.car().orElseThrow(), page, paginationPolicy.getDefaultPageSize(),
                statusFilter, LocaleContextHolder.getLocale());
        final Optional<ModelAndView> clamp = PageClampRedirect.ifOutOfRange(
                page, pageModel.getResultPage(), "page", request);
        if (clamp.isPresent()) {
            return clamp.get();
        }
        final ModelAndView mav = new ModelAndView("car/carReservations");
        pageModel.populateModel(mav::addObject);
        mav.addObject("activeTab", "my-cars");
        return mav;
    }

    @GetMapping("/car/{carId}")
    public ModelAndView myCarDetail(
            @CurrentUser final User currentUser,
            @PathVariable final long carId) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final OwnerCarLookup.Result lookup = ownerCarLookup.loadOwnedCar(carId, "/my-cars");
        if (lookup.redirect().isPresent()) {
            return lookup.redirect().get();
        }
        final Car car = lookup.car().orElseThrow();
        final MyCarDetailPageModel pm = myCarDetailViewService.loadMyCarDetailPage(
                me.getId(), car, LocaleContextHolder.getLocale(), 3);
        if (pm.getVariant() == MyCarDetailPageModel.Variant.RICH) {
            final OwnerCarDetailPageModel owner = pm.getOwnerPageModel().orElseThrow();
            final CarAvailabilityEditForm editForm = new CarAvailabilityEditForm();
            applyEditFormDefaultsFromAvailabilities(editForm, owner.getAvailabilities());
            final ModelAndView mav = new ModelAndView("car/myCarDetail");
            owner.populateModel(mav::addObject);
            mav.addObject("editForm", editForm);
            mav.addObject("previewReservations", pm.getPreviewReservations());
            mav.addObject("reservationPreviewTotal", pm.getReservationPreviewTotal());
            mav.addObject("activeTab", "my-cars");
            return mav;
        }
        final ModelAndView mav = new ModelAndView("car/myCarDetail");
        mav.addObject("car", car);
        mav.addObject("carImageId", pm.getFallbackCarImageId());
        mav.addObject("hasPublishedAvailability", false);
        mav.addObject("activeTab", "my-cars");
        return mav;
    }

    @GetMapping("/car/{carId}/create")
    public ModelAndView createCarAvailabilityForm(
            @CurrentUser final User currentUser,
            @PathVariable final long carId) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final OwnerCarLookup.Result lookup = ownerCarLookup.loadOwnedCar(carId, "/my-cars");
        if (lookup.redirect().isPresent()) {
            return lookup.redirect().get();
        }
        if (carService.isModelPendingValidation(carId)) {
            return new ModelAndView(redirectTo("/my-cars/car/" + carId));
        }
        final CreateCarAvailabilityForm form = new CreateCarAvailabilityForm();
        carAvailabilityService.findMostRecentByCarId(carId)
                .ifPresent(la -> prefillLocationAndTimes(form, la));
        return buildCreateListingView(lookup.car().orElseThrow(), form, me);
    }

    @GetMapping("/car/{carId}/availability/{availabilityId}/edit")
    public ModelAndView editAvailabilityForm(
            @CurrentUser final User currentUser,
            @PathVariable final long carId,
            @PathVariable final long availabilityId) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final OwnerCarLookup.Result lookup = ownerCarLookup.loadOwnedCar(carId, "/my-cars");
        if (lookup.redirect().isPresent()) {
            return lookup.redirect().get();
        }
        final Optional<CarAvailability> avOpt = carAvailabilityService.findById(availabilityId);
        if (avOpt.isEmpty() || avOpt.get().getCarId() != carId) {
            return new ModelAndView(redirectTo("/my-cars/car/" + carId));
        }
        return buildEditAvailabilityView(
                lookup.car().orElseThrow(), availabilityId, buildEditAvailabilityForm(avOpt.get()), me);
    }

    @PostMapping("/car/{carId}/availability/{availabilityId}/edit")
    public ModelAndView editAvailability(
            @CurrentUser final User currentUser,
            @PathVariable final long carId,
            @PathVariable final long availabilityId,
            @Validated(ValidationGroups.OnCreateListing.class) @ModelAttribute("createCarAvailabilityForm") final CreateCarAvailabilityForm form,
            final BindingResult errors) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final OwnerCarLookup.Result lookup = ownerCarLookup.loadOwnedCar(carId, "/my-cars");
        if (lookup.redirect().isPresent()) {
            return lookup.redirect().get();
        }
        final Car car = lookup.car().orElseThrow();
        final Optional<CarAvailability> avOpt = carAvailabilityService.findById(availabilityId);
        if (avOpt.isEmpty() || avOpt.get().getCarId() != carId) {
            return new ModelAndView(redirectTo("/my-cars/car/" + carId));
        }
        final CarAvailability old = avOpt.get();
        if (!errors.hasErrors()) {
            try {
                carAvailabilityService.validateEditAvailabilityRiderLead(
                        form.toAvailabilityPeriods(), form.getCheckInTime(), Instant.now(),
                        old.getStartInclusive());
            } catch (final AvailabilityRiderLeadViolationException e) {
                errors.rejectValue(
                        "availabilityRows[" + e.getAvailabilityRowIndex() + "].from",
                        e.getMessageCode(), e.getMessageArgs(), localeMessages.msg(e));
            }
        }
        if (!errors.hasErrors()) {
            try {
                carAvailabilityService.validatePublicationAvailabilityAgainstWallCalendar(form.toAvailabilityPeriods());
            } catch (final CarValidationException e) {
                errors.reject(e.getMessageCode(), e.getMessageArgs(), localeMessages.msg(e));
            }
        }
        if (errors.hasErrors()) {
            return buildEditAvailabilityView(car, availabilityId, form, me);
        }
        final AvailabilityPeriod newPeriod = form.toAvailabilityPeriods().get(0);
        try {
            carAvailabilityService.applyOwnerEditByCar(
                    carId,
                    old.getStartInclusive(), old.getEndInclusive(),
                    newPeriod.getStartInclusive(), newPeriod.getEndInclusive(),
                    form.getPricePerDay(),
                    form.getStartPointStreet(), form.getStartPointNumber(),
                    form.getNeighborhoodId(),
                    form.getCheckInTime(), form.getCheckOutTime());
        } catch (final CarValidationException e) {
            errors.reject(e.getMessageCode(), e.getMessageArgs(), localeMessages.msg(e));
            return buildEditAvailabilityView(car, availabilityId, form, me);
        }
        return new ModelAndView(redirectTo("/my-cars/car/" + carId));
    }

    @PostMapping("/car/{carId}/create")
    public ModelAndView createListing(
            @CurrentUser final User currentUser,
            @PathVariable final long carId,
            @Validated(ValidationGroups.OnCreateListing.class) @ModelAttribute("createCarAvailabilityForm") final CreateCarAvailabilityForm form,
            final BindingResult errors) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final OwnerCarLookup.Result lookup = ownerCarLookup.loadOwnedCar(carId, "/my-cars");
        if (lookup.redirect().isPresent()) {
            return lookup.redirect().get();
        }
        final Car car = lookup.car().orElseThrow();
        if (carService.isModelPendingValidation(carId)) {
            return new ModelAndView(redirectTo("/my-cars/car/" + carId));
        }
        if (errors.hasErrors()) {
            return buildCreateListingView(car, form, me);
        }
        final User freshUser = userService.getUserById(me.getId()).orElse(me);
        if (!userService.hasValidCbu(freshUser)) {
            errors.reject(MessageKeys.CAR_PUBLISH_CBU_REQUIRED, localeMessages.msg(MessageKeys.CAR_PUBLISH_CBU_REQUIRED));
            return buildCreateListingView(car, form, freshUser);
        }
        if (!errors.hasErrors()) {
            try {
                carAvailabilityService.validatePublicationAvailabilityRiderLead(
                        form.toAvailabilityPeriods(), form.getCheckInTime(), Instant.now());
            } catch (final AvailabilityRiderLeadViolationException e) {
                errors.rejectValue(
                        "availabilityRows[" + e.getAvailabilityRowIndex() + "].from",
                        e.getMessageCode(), e.getMessageArgs(), localeMessages.msg(e));
            }
        }
        if (!errors.hasErrors()) {
            try {
                carAvailabilityService.validatePublicationAvailabilityAgainstWallCalendar(form.toAvailabilityPeriods());
            } catch (final CarValidationException e) {
                errors.reject(e.getMessageCode(), e.getMessageArgs(), localeMessages.msg(e));
            }
        }
        if (errors.hasErrors()) {
            return buildCreateListingView(car, form, freshUser);
        }
        try {
            carAvailabilityService.createCarAvailabilityPeriods(
                    carId,
                    form.getPricePerDay(),
                    form.getStartPointStreet(),
                    form.getStartPointNumber(),
                    form.getNeighborhoodId(),
                    form.getCheckInTime(),
                    form.getCheckOutTime(),
                    form.toAvailabilityPeriods(),
                    form.toPeriodPrices(),
                    form.getMinimumRentalDays());
            return new ModelAndView(redirectTo("/my-cars/car/" + carId));
        } catch (final CarValidationException e) {
            errors.reject(e.getMessageCode(), e.getMessageArgs(), localeMessages.msg(e));
            return buildCreateListingView(car, form, freshUser);
        }
    }

    @GetMapping(value = "/car/{carId}/availability-min-from", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, String> availabilityMinFrom(
            @CurrentUser final User currentUser,
            @PathVariable final long carId,
            @RequestParam(name = "checkIn", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) final LocalTime checkIn) {
        WebAuthUtils.requireUser(currentUser);
        final LocalTime lt = checkIn != null ? checkIn : CarAvailability.DEFAULT_CHECK_IN_TIME;
        final LocalDate minFrom = carAvailabilityService.getPublicationMinAvailabilityFirstWallDay(lt, Instant.now());
        return Map.of("minFrom", minFrom.toString());
    }

    /**
     * Uploads (or replaces) the insurance document for the car. Same content-type/size policy as
     * profile documents. Used by the inline form in {@code myCarDetail.jsp}.
     */
    @PostMapping(value = "/car/{carId}/insurance", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ModelAndView uploadInsurance(
            @CurrentUser final User currentUser,
            @PathVariable("carId") final long carId,
            @RequestParam(name = "insuranceFile", required = false) final MultipartFile insuranceFile,
            final RedirectAttributes redirectAttributes) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final OwnerCarLookup.Result lookup = ownerCarLookup.loadOwnedCar(carId, "/my-cars");
        if (lookup.redirect().isPresent()) {
            return lookup.redirect().get();
        }
        final var outcome = carInsuranceUploadFacade.attemptUpload(me.getId(), carId, insuranceFile);
        if (!outcome.isOk()) {
            outcome.getLocalizedMessage().ifPresent(
                    msg -> redirectAttributes.addFlashAttribute("carInsuranceErrorMessage", msg));
        }
        return new ModelAndView(redirectTo("/my-cars/car/" + carId));
    }

    /**
     * AJAX variant of {@link #uploadInsurance} so the car-detail page can update without a full reload.
     * Ownership is enforced by {@code CarOwnerWebAuthorization} on {@code /my-cars/car/{carId}/**}
     * in the Spring Security filter chain, so this handler does not re-check it.
     */
    @PostMapping(value = "/car/{carId}/quick-insurance", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> quickInsurance(
            @CurrentUser final User currentUser,
            @PathVariable("carId") final long carId,
            @RequestParam(name = "insuranceFile", required = false) final MultipartFile insuranceFile) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final var outcome = carInsuranceUploadFacade.attemptUpload(me.getId(), carId, insuranceFile);
        switch (outcome.getStatus()) {
            case OK:
                return ResponseEntity.noContent().build();
            case TOO_LARGE:
            case BUSINESS_ERROR:
                final HttpHeaders headers = new HttpHeaders();
                outcome.getLocalizedMessage().ifPresent(msg -> headers.add("X-Ryden-Error", msg));
                return new ResponseEntity<>(null, headers, HttpStatus.BAD_REQUEST);
            case MISSING_FILE:
            case READ_ERROR:
            default:
                return ResponseEntity.badRequest().build();
        }
    }

    private static RedirectView redirectTo(final String url) {
        final RedirectView rv = new RedirectView(url, true);
        rv.setExposeModelAttributes(false);
        return rv;
    }

    private ModelAndView buildCreateListingView(final Car car, final CreateCarAvailabilityForm form, final User user) {
        final CarAvailabilityEditorPageModel context = carAvailabilityEditorViewService.loadEditorContext(
                car, user.getId(), form.getCheckInTime());
        final ModelAndView mav = new ModelAndView("car/createCarAvailability");
        context.populateModel(mav::addObject);
        mav.addObject("createCarAvailabilityForm", form);
        mav.addObject("userHasCbu", context.isUserHasCbu());
        mav.addObject("activeTab", "my-cars");
        return mav;
    }

    private static void applyEditFormDefaultsFromAvailabilities(
            final CarAvailabilityEditForm editForm,
            final List<CarAvailability> availabilities) {
        if (availabilities == null || availabilities.isEmpty()) {
            return;
        }
        final CarAvailability mostRecent = availabilities.stream()
                .max(java.util.Comparator.comparing(CarAvailability::getCreatedAt))
                .get();
        if (editForm.getPricePerDay() == null) {
            editForm.setPricePerDay(mostRecent.getDayPriceValue());
        }
        if (editForm.getStartPointStreet() == null) {
            editForm.setStartPointStreet(mostRecent.getStartPointStreet());
        }
        if (editForm.getStartPointNumber() == null) {
            editForm.setStartPointNumber(mostRecent.getStartPointNumber().orElse(null));
        }
        if (editForm.getCheckInTime() == null) {
            editForm.setCheckInTime(mostRecent.getCheckInTime());
        }
        if (editForm.getCheckOutTime() == null) {
            editForm.setCheckOutTime(mostRecent.getCheckOutTime());
        }
        if (editForm.getNeighborhoodId() == null) {
            editForm.setNeighborhoodId(mostRecent.getNeighborhoodId().orElse(null));
        }
        editForm.populateDefaultAvailability(availabilities);
    }

    private static void prefillLocationAndTimes(final CreateCarAvailabilityForm form, final CarAvailability la) {
        form.setStartPointStreet(la.getStartPointStreet());
        form.setStartPointNumber(la.getStartPointNumber().orElse(null));
        form.setNeighborhoodId(la.getNeighborhoodId().orElse(null));
        form.setCheckInTime(la.getCheckInTime());
        form.setCheckOutTime(la.getCheckOutTime());
    }

    private static CreateCarAvailabilityForm buildEditAvailabilityForm(final CarAvailability av) {
        final CreateCarAvailabilityForm form = new CreateCarAvailabilityForm();
        form.setPricePerDay(av.getDayPriceValue());
        form.setStartPointStreet(av.getStartPointStreet());
        form.setStartPointNumber(av.getStartPointNumber().orElse(null));
        form.setNeighborhoodId(av.getNeighborhoodId().orElse(null));
        form.setCheckInTime(av.getCheckInTime());
        form.setCheckOutTime(av.getCheckOutTime());
        final CreateCarAvailabilityForm.AvailabilityRow row = new CreateCarAvailabilityForm.AvailabilityRow();
        row.setFrom(av.getStartInclusive());
        row.setUntil(av.getEndInclusive());
        form.setAvailabilityRows(List.of(row));
        return form;
    }

    private ModelAndView buildEditAvailabilityView(
            final Car car, final long availabilityId, final CreateCarAvailabilityForm form, final User user) {
        final CarAvailabilityEditorPageModel context = carAvailabilityEditorViewService.loadEditorContext(
                car, user.getId(), form.getCheckInTime());
        final ModelAndView mav = new ModelAndView("car/editCarAvailability");
        context.populateModel(mav::addObject);
        mav.addObject("createCarAvailabilityForm", form);
        mav.addObject("availabilityId", availabilityId);
        mav.addObject("activeTab", "my-cars");
        return mav;
    }
}
