package ar.edu.itba.paw.webapp.controller.car;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.car.AvailabilityRiderLeadViolationException;
import ar.edu.itba.paw.exception.car.CarValidationException;
import ar.edu.itba.paw.exception.reservation.ReservationConflictException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarAvailability;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.dto.car.AvailabilityCreateInput;
import ar.edu.itba.paw.models.dto.car.CarAvailabilityEditorPageModel;
import ar.edu.itba.paw.models.dto.car.ManageCarPeriodsPageModel;
import ar.edu.itba.paw.models.dto.car.MyCarDetailPageModel;
import ar.edu.itba.paw.models.dto.car.MyCarsListPageModel;
import ar.edu.itba.paw.models.dto.car.OwnerCarDetailPageModel;
import ar.edu.itba.paw.models.dto.reservation.CarReservationsListPageModel;
import ar.edu.itba.paw.models.dto.reservation.OwnerReservationsListPageModel;
import ar.edu.itba.paw.models.util.search.MyHubSortSanitizer;
import ar.edu.itba.paw.models.util.time.AppTimezone;
import ar.edu.itba.paw.policy.ProfileDocumentUploadPolicy;
import ar.edu.itba.paw.services.car.CarAvailabilityService;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.car.view.CarAvailabilityEditorViewService;
import ar.edu.itba.paw.services.car.view.ManageCarPeriodsViewService;
import ar.edu.itba.paw.services.car.view.MyCarDetailViewService;
import ar.edu.itba.paw.services.car.view.MyCarsListViewService;
import ar.edu.itba.paw.services.reservation.ReservationService;
import ar.edu.itba.paw.services.reservation.view.ReservationViewService;
import ar.edu.itba.paw.webapp.config.properties.AppPaginationProperties;
import ar.edu.itba.paw.webapp.form.car.CarAvailabilityEditForm;
import ar.edu.itba.paw.webapp.form.car.CreateCarAvailabilityForm;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.support.OwnerCarLookup;
import ar.edu.itba.paw.webapp.support.PageClampRedirect;
import ar.edu.itba.paw.webapp.support.facade.CarInsuranceUploadFacade;
import ar.edu.itba.paw.webapp.util.LocaleMessages;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import ar.edu.itba.paw.webapp.validation.car.CarAvailabilityNeighborhoodFormValidator;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Owner hub: car cards, edit, pause/finish, and per-car reservation analytics and actions. */
@Controller
@RequestMapping("/my-cars")
@Validated
public class MyCarsController {

    private final ReservationService reservationService;
    private final ReservationViewService reservationViewService;
    private final CarAvailabilityNeighborhoodFormValidator carAvailabilityNeighborhoodFormValidator;
    private final AppPaginationProperties appPaginationProperties;
    private final LocaleMessages localeMessages;
    private final CarService carService;
    private final CarAvailabilityService carAvailabilityService;
    private final CarAvailabilityEditorViewService carAvailabilityEditorViewService;
    private final MyCarDetailViewService myCarDetailViewService;
    private final MyCarsListViewService myCarsListViewService;
    private final ManageCarPeriodsViewService manageCarPeriodsViewService;
    private final ProfileDocumentUploadPolicy profileDocumentUploadPolicy;
    private final OwnerCarLookup ownerCarLookup;
    private final CarInsuranceUploadFacade carInsuranceUploadFacade;

    public MyCarsController(
            final ReservationService reservationService,
            final ReservationViewService reservationViewService,
            final CarAvailabilityNeighborhoodFormValidator carAvailabilityNeighborhoodFormValidator,
            final AppPaginationProperties appPaginationProperties,
            final LocaleMessages localeMessages,
            final CarService carService,
            final CarAvailabilityService carAvailabilityService,
            final CarAvailabilityEditorViewService carAvailabilityEditorViewService,
            final MyCarDetailViewService myCarDetailViewService,
            final MyCarsListViewService myCarsListViewService,
            final ManageCarPeriodsViewService manageCarPeriodsViewService,
            final ProfileDocumentUploadPolicy profileDocumentUploadPolicy,
            final OwnerCarLookup ownerCarLookup,
            final CarInsuranceUploadFacade carInsuranceUploadFacade) {
        this.reservationService = reservationService;
        this.reservationViewService = reservationViewService;
        this.carAvailabilityNeighborhoodFormValidator = carAvailabilityNeighborhoodFormValidator;
        this.appPaginationProperties = appPaginationProperties;
        this.localeMessages = localeMessages;
        this.carService = carService;
        this.carAvailabilityService = carAvailabilityService;
        this.carAvailabilityEditorViewService = carAvailabilityEditorViewService;
        this.myCarDetailViewService = myCarDetailViewService;
        this.myCarsListViewService = myCarsListViewService;
        this.manageCarPeriodsViewService = manageCarPeriodsViewService;
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
            @RequestParam(defaultValue = "0") @Min(0) final int page,
            @RequestParam(required = false) final List<Car.Status> listingStatus,
            @RequestParam(required = false) final String q,
            @RequestParam(required = false) final List<Car.Type> category,
            @RequestParam(required = false) final List<Car.Transmission> transmission,
            @RequestParam(required = false) final List<Car.Powertrain> powertrain,
            @RequestParam(required = false) final BigDecimal priceMin,
            @RequestParam(required = false) final BigDecimal priceMax,
            @RequestParam(required = false) final List<String> rating,
            @RequestParam(required = false) final String sort,
            final HttpServletRequest request) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final String listingsSort = MyHubSortSanitizer.sanitize(sort, DEFAULT_SORT);
        final var listingsCriteria = carService.buildOwnerCarSearchCriteria(
                me.getId(), category, transmission, powertrain, priceMin, priceMax,
                listingStatus, rating, q, page, appPaginationProperties.getDefaultPageSize(), listingsSort);
        final MyCarsListPageModel pageModel = myCarsListViewService.loadMyCarsListPage(
                listingsCriteria, listingsSort, me.isBlocked());
        final Optional<ModelAndView> clamp =
                PageClampRedirect.ifOutOfRange(page, pageModel.getResultPage(), "page", request);
        if (clamp.isPresent()) {
            return clamp.get();
        }
        final ModelAndView mav = new ModelAndView("car/myCars");
        pageModel.populateModel(mav::addObject);
        mav.addObject("activeTab", "my-cars");
        return mav;
    }

    @GetMapping("/reservations")
    public ModelAndView ownerReservations(
            @CurrentUser final User currentUser,
            @RequestParam(defaultValue = "0") @Min(0) final int page,
            @RequestParam(required = false) final List<Reservation.Status> ownerStatus,
            @RequestParam(required = false) final String ownerQ,
            @RequestParam(required = false) final List<Car.Type> ownerCategory,
            @RequestParam(required = false) final List<Car.Transmission> ownerTransmission,
            @RequestParam(required = false) final List<Car.Powertrain> ownerPowertrain,
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
            @RequestParam(defaultValue = "0") @Min(0) final int page,
            @RequestParam(required = false) final List<Reservation.Status> ownerStatus,
            @RequestParam(required = false) final String ownerQ,
            @RequestParam(required = false) final List<Car.Type> ownerCategory,
            @RequestParam(required = false) final List<Car.Transmission> ownerTransmission,
            @RequestParam(required = false) final List<Car.Powertrain> ownerPowertrain,
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
            final int page,
            final List<Reservation.Status> ownerStatus,
            final String ownerQ,
            final List<Car.Type> ownerCategory,
            final List<Car.Transmission> ownerTransmission,
            final List<Car.Powertrain> ownerPowertrain,
            final BigDecimal ownerPriceMin,
            final BigDecimal ownerPriceMax,
            final List<String> ownerRating,
            final String ownerSort,
            final HttpServletRequest request) {
        final String sort = MyHubSortSanitizer.sanitize(ownerSort, DEFAULT_SORT);
        final var criteria = reservationService.buildReservationSearchCriteria(
                me.getId(), null, ownerCategory, ownerTransmission, ownerPowertrain,
                ownerPriceMin, ownerPriceMax, ownerRating, ownerStatus, page,
                appPaginationProperties.getDefaultPageSize(), sort, ownerQ, carIdFilterOrNull);
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
        if (editForm.toAvailabilityPeriods().isEmpty()) {
            return new ModelAndView(redirectTo("/my-cars/car/" + carId));
        }
        try {
            carAvailabilityService.applyOwnerBulkEdit(
                    me.getId(), carId, toBulkEditInput(editForm), Instant.now());
        } catch (final CarValidationException e) {
            errors.reject(e.getMessageCode(), e.getMessageArgs(), localeMessages.msg(e));
            return renderMyCarDetailOrRedirect(me, car, editForm, /* withPreview */ false);
        }
        return new ModelAndView(redirectTo("/my-cars/car/" + carId));
    }

    private static AvailabilityCreateInput toBulkEditInput(final CarAvailabilityEditForm editForm) {
        // minimumRentalDays is set to 1 to match the legacy create-fallback branch when the car has
        // no existing availability rows; bulk-edit on an existing car does not change the configured
        // minimum (that is what /min-rental-days exists for).
        return new AvailabilityCreateInput(
                editForm.getPricePerDay(),
                editForm.getStartPointStreet(),
                editForm.getStartPointNumber(),
                editForm.getNeighborhoodId(),
                editForm.getCheckInTime(),
                editForm.getCheckOutTime(),
                editForm.toAvailabilityPeriods(),
                editForm.toPeriodPrices(),
                1);
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
            @RequestParam(defaultValue = "0") @Min(0) final int page,
            @RequestParam(required = false) final String reservationStatus,
            final HttpServletRequest request) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final OwnerCarLookup.Result lookup = ownerCarLookup.loadOwnedCar(carId, "/my-cars");
        if (lookup.redirect().isPresent()) {
            return lookup.redirect().get();
        }
        final String statusFilter = reservationViewService.normalizeReservationStatusQueryParam(reservationStatus);
        final CarReservationsListPageModel pageModel = reservationViewService.loadCarReservationsListPage(
                me.getId(), lookup.car().orElseThrow(), page, appPaginationProperties.getDefaultPageSize(),
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
            editForm.populateDefaultsFromExistingAvailabilities(owner.getAvailabilities());
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

    @GetMapping("/car/{carId}/periods")
    public ModelAndView manageCarPeriods(
            @CurrentUser final User currentUser,
            @PathVariable final long carId,
            @RequestParam(required = false) final String month,
            @RequestParam(defaultValue = "0") @Min(0) final int page) {
        WebAuthUtils.requireUser(currentUser);
        final OwnerCarLookup.Result lookup = ownerCarLookup.loadOwnedCar(carId, "/my-cars");
        if (lookup.redirect().isPresent()) {
            return lookup.redirect().get();
        }
        final Car car = lookup.car().orElseThrow();
        final YearMonth activeMonth = parseYearMonthOrDefault(month);
        final ManageCarPeriodsPageModel pm = manageCarPeriodsViewService.loadManageCarPeriodsPage(
                car, activeMonth, LocaleContextHolder.getLocale(),
                page, appPaginationProperties.getManagePeriodsPageSize());
        if (pm.getTotalPages() > 0 && page >= pm.getTotalPages()) {
            final int clampedPage = Math.max(0, pm.getTotalPages() - 1);
            return new ModelAndView(redirectTo(
                    "/my-cars/car/" + carId + "/periods?month=" + activeMonth + "&page=" + clampedPage));
        }
        final ModelAndView mav = new ModelAndView("car/manageCarPeriods");
        pm.populateModel(mav::addObject);
        final CreateCarAvailabilityForm createForm = new CreateCarAvailabilityForm();
        pm.getMostRecentAvailability().ifPresent(la -> prefillLocationAndTimes(createForm, la));
        mav.addObject("createCarAvailabilityForm", createForm);
        mav.addObject("activeTab", "my-cars");
        return mav;
    }

    private static YearMonth parseYearMonthOrDefault(final String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now(AppTimezone.WALL_ZONE);
        }
        try {
            return YearMonth.parse(month);
        } catch (final DateTimeParseException e) {
            return YearMonth.now(AppTimezone.WALL_ZONE);
        }
    }

    @PostMapping("/car/{carId}/min-rental-days")
    public ModelAndView updateMinRentalDays(
            @CurrentUser final User currentUser,
            @PathVariable final long carId,
            @RequestParam final int minimumRentalDays,
            @RequestParam(required = false) final String month,
            final RedirectAttributes redirectAttributes) {
        WebAuthUtils.requireUser(currentUser);
        final OwnerCarLookup.Result lookup = ownerCarLookup.loadOwnedCar(carId, "/my-cars");
        if (lookup.redirect().isPresent()) {
            return lookup.redirect().get();
        }
        try {
            carAvailabilityService.updateMinimumRentalDays(carId, minimumRentalDays);
        } catch (final CarValidationException e) {
            redirectAttributes.addFlashAttribute("minRentalDaysError", localeMessages.msg(e));
        }
        return new ModelAndView(redirectTo("/my-cars/car/" + carId + "/periods"
                + (month != null && !month.isBlank() ? "?month=" + month : "")));
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
        final Optional<CarAvailability> avOpt = carAvailabilityService.findByIdForCar(carId, availabilityId);
        if (avOpt.isEmpty()) {
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
            @RequestParam(required = false) final String month,
            @Validated(ValidationGroups.OnCreateListing.class) @ModelAttribute("createCarAvailabilityForm") final CreateCarAvailabilityForm form,
            final BindingResult errors) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final OwnerCarLookup.Result lookup = ownerCarLookup.loadOwnedCar(carId, "/my-cars");
        if (lookup.redirect().isPresent()) {
            return lookup.redirect().get();
        }
        final Car car = lookup.car().orElseThrow();
        if (errors.hasErrors()) {
            return buildManagePeriodsViewWithError(car, month, String.valueOf(availabilityId), form, errors, 0);
        }
        try {
            carAvailabilityService.editAvailability(
                    me.getId(), carId, availabilityId, toAvailabilityInput(form), Instant.now());
        } catch (final AvailabilityRiderLeadViolationException e) {
            errors.rejectValue(
                    "availabilityRows[" + e.getAvailabilityRowIndex() + "].from",
                    e.getMessageCode(), e.getMessageArgs(), localeMessages.msg(e));
            return buildManagePeriodsViewWithError(car, month, String.valueOf(availabilityId), form, errors, 0);
        } catch (final ReservationConflictException e) {
            // Owner tried to shrink/move the period so that already-booked reservations
            // would fall outside the new window. Mirror createListing: surface as a form
            // error instead of letting it propagate to the global 400 page.
            errors.reject(e.getMessageCode(), e.getMessageArgs(), localeMessages.msg(e));
            return buildManagePeriodsViewWithError(car, month, String.valueOf(availabilityId), form, errors, 0);
        } catch (final CarValidationException e) {
            if (MessageKeys.CAR_AVAILABILITY_NOT_FOUND.equals(e.getMessageCode())
                    || MessageKeys.CAR_AVAILABILITY_NOT_OWNED.equals(e.getMessageCode())) {
                return new ModelAndView(redirectTo("/my-cars/car/" + carId));
            }
            errors.reject(e.getMessageCode(), e.getMessageArgs(), localeMessages.msg(e));
            return buildManagePeriodsViewWithError(car, month, String.valueOf(availabilityId), form, errors, 0);
        }
        return new ModelAndView(redirectTo("/my-cars/car/" + carId + "/periods"
                + (month != null && !month.isBlank() ? "?month=" + month : "")));
    }

    @PostMapping("/car/{carId}/create")
    public ModelAndView createListing(
            @CurrentUser final User currentUser,
            @PathVariable final long carId,
            @RequestParam(required = false) final String month,
            @Validated(ValidationGroups.OnCreateListing.class) @ModelAttribute("createCarAvailabilityForm") final CreateCarAvailabilityForm form,
            final BindingResult errors) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final OwnerCarLookup.Result lookup = ownerCarLookup.loadOwnedCar(carId, "/my-cars");
        if (lookup.redirect().isPresent()) {
            return lookup.redirect().get();
        }
        final Car car = lookup.car().orElseThrow();
        if (errors.hasErrors()) {
            return buildManagePeriodsViewWithError(car, month, "create", form, errors, 0);
        }
        try {
            carAvailabilityService.createListing(
                    me.getId(), carId, toAvailabilityInput(form), Instant.now());
            return new ModelAndView(redirectTo("/my-cars/car/" + carId + "/periods"
                    + (month != null && !month.isBlank() ? "?month=" + month : "")));
        } catch (final AvailabilityRiderLeadViolationException e) {
            errors.rejectValue(
                    "availabilityRows[" + e.getAvailabilityRowIndex() + "].from",
                    e.getMessageCode(), e.getMessageArgs(), localeMessages.msg(e));
            return buildManagePeriodsViewWithError(car, month, "create", form, errors, 0);
        } catch (final ReservationConflictException e) {
            errors.reject(e.getMessageCode(), e.getMessageArgs(), localeMessages.msg(e));
            return buildManagePeriodsViewWithError(car, month, "create", form, errors, 0);
        } catch (final CarValidationException e) {
            if (MessageKeys.CAR_CREATE_MODEL_PENDING.equals(e.getMessageCode())) {
                return new ModelAndView(redirectTo("/my-cars/car/" + carId));
            }
            errors.reject(e.getMessageCode(), e.getMessageArgs(), localeMessages.msg(e));
            return buildManagePeriodsViewWithError(car, month, "create", form, errors, 0);
        }
    }

    private static AvailabilityCreateInput toAvailabilityInput(final CreateCarAvailabilityForm form) {
        return new AvailabilityCreateInput(
                form.getPricePerDay(),
                form.getStartPointStreet(),
                form.getStartPointNumber(),
                form.getNeighborhoodId(),
                form.getCheckInTime(),
                form.getCheckOutTime(),
                form.toAvailabilityPeriods(),
                form.toPeriodPrices(),
                form.getMinimumRentalDays());
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

    private ModelAndView buildManagePeriodsViewWithError(
            final Car car, final String month, final String inlineFormOpen,
            final CreateCarAvailabilityForm form, final BindingResult errors,
            final int page) {
        final ManageCarPeriodsPageModel pm = manageCarPeriodsViewService.loadManageCarPeriodsPage(
                car, parseYearMonthOrDefault(month), LocaleContextHolder.getLocale(),
                page, appPaginationProperties.getManagePeriodsPageSize());
        final ModelAndView mav = new ModelAndView("car/manageCarPeriods");
        pm.populateModel(mav::addObject);
        mav.addObject("inlineFormOpen", inlineFormOpen);
        mav.addObject("createCarAvailabilityForm", form);
        mav.addObject(BindingResult.MODEL_KEY_PREFIX + "createCarAvailabilityForm", errors);
        mav.addObject("activeTab", "my-cars");
        return mav;
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
