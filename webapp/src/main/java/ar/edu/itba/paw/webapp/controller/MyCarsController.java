package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.dto.CarCard;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.ReservationCard;
import ar.edu.itba.paw.models.dto.ReservationCardDisplayRow;
import ar.edu.itba.paw.models.pagination.UiPaging;
import ar.edu.itba.paw.models.util.MyHubSortSanitizer;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.OwnerCarDetailPageModel;
import ar.edu.itba.paw.services.CarPictureService;
import ar.edu.itba.paw.services.CarService;
import ar.edu.itba.paw.services.ListingAvailabilityService;
import ar.edu.itba.paw.services.LocationService;
import ar.edu.itba.paw.services.ReservationService;
import ar.edu.itba.paw.services.ReservationViewService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.policy.PaginationPolicy;
import ar.edu.itba.paw.services.policy.ProfileDocumentUploadPolicy;
import ar.edu.itba.paw.services.policy.ReservationTimingPolicy;
import ar.edu.itba.paw.exception.car.AvailabilityRiderLeadViolationException;
import ar.edu.itba.paw.exception.car.CarValidationException;
import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.RydenException;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.util.LocaleMessages;
import ar.edu.itba.paw.webapp.form.CreateListingForm;
import ar.edu.itba.paw.webapp.form.ListingEditForm;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;
import ar.edu.itba.paw.webapp.validation.ListingNeighborhoodFormValidator;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServletServerHttpRequest;
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
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Owner hub: car cards, edit, pause/finish, and per-car reservation analytics and actions. */
@Controller
@RequestMapping("/my-cars")
public final class MyCarsController {

    private final ReservationService reservationService;
    private final ReservationViewService reservationViewService;
    private final ListingNeighborhoodFormValidator listingNeighborhoodFormValidator;
    private final PaginationPolicy paginationPolicy;
    private final LocaleMessages localeMessages;
    private final CarService carService;
    private final CarPictureService carPictureService;
    private final LocationService locationService;
    private final UserService userService;
    private final ListingAvailabilityService listingAvailabilityService;
    private final ReservationTimingPolicy reservationTimingPolicy;
    private final ProfileDocumentUploadPolicy profileDocumentUploadPolicy;
    private final String supportEmail;

    public MyCarsController(
            final ReservationService reservationService,
            final ReservationViewService reservationViewService,
            final ListingNeighborhoodFormValidator listingNeighborhoodFormValidator,
            final PaginationPolicy paginationPolicy,
            final LocaleMessages localeMessages,
            final CarService carService,
            final CarPictureService carPictureService,
            final LocationService locationService,
            final UserService userService,
            final ListingAvailabilityService listingAvailabilityService,
            final ReservationTimingPolicy reservationTimingPolicy,
            final ProfileDocumentUploadPolicy profileDocumentUploadPolicy,
            @Value("${app.support.email}") final String supportEmail) {
        this.reservationService = reservationService;
        this.reservationViewService = reservationViewService;
        this.listingNeighborhoodFormValidator = listingNeighborhoodFormValidator;
        this.paginationPolicy = paginationPolicy;
        this.localeMessages = localeMessages;
        this.carService = carService;
        this.carPictureService = carPictureService;
        this.locationService = locationService;
        this.userService = userService;
        this.listingAvailabilityService = listingAvailabilityService;
        this.reservationTimingPolicy = reservationTimingPolicy;
        this.profileDocumentUploadPolicy = profileDocumentUploadPolicy;
        this.supportEmail = supportEmail;
    }

    @InitBinder("editForm")
    public void initEditFormBinder(final WebDataBinder binder) {
        binder.addValidators(listingNeighborhoodFormValidator);
    }

    @InitBinder("createListingForm")
    public void initCreateListingFormBinder(final WebDataBinder binder) {
        binder.addValidators(listingNeighborhoodFormValidator);
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
        final int safeListingsPage = UiPaging.clampZeroBasedPage(page, resultPage.getTotalItems(), resultPage.getPageSize());
        if (safeListingsPage != page) {
            final RedirectView redirectView = new RedirectView(
                    UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request))
                            .replaceQueryParam("page", safeListingsPage)
                            .build()
                            .toUriString());
            redirectView.setExposeModelAttributes(false);
            return new ModelAndView(redirectView);
        }
        final ModelAndView mav = new ModelAndView("listing/myListings");
        mav.addObject("results", resultPage.getContent());
        mav.addObject("myListingsPage", resultPage);
        mav.addObject("activeTab", "my-cars");
        mav.addObject("listingsCurrentSort", listingsSort);
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
        page = Math.max(0, page);
        final String sort = MyHubSortSanitizer.sanitize(ownerSort, DEFAULT_SORT);
        final var criteria = reservationService.buildReservationSearchCriteria(
                me.getId(), null, ownerCategory, ownerTransmission, ownerPowertrain,
                ownerPriceMin, ownerPriceMax, ownerRating, ownerStatus, page, sort, ownerQ, null);
        final Page<ReservationCard> resultPage = reservationService.getOwnerReservationCards(criteria);
        final int safePage = UiPaging.clampZeroBasedPage(page, resultPage.getTotalItems(), resultPage.getPageSize());
        if (safePage != page) {
            final RedirectView redirectView = new RedirectView(
                    UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request))
                            .replaceQueryParam("page", safePage)
                            .build()
                            .toUriString());
            redirectView.setExposeModelAttributes(false);
            return new ModelAndView(redirectView);
        }
        final Locale locale = LocaleContextHolder.getLocale();
        final List<ReservationCardDisplayRow> reservations = resultPage.getContent().stream()
                .map(card -> reservationViewService.toReservationCardDisplayRow(card, locale))
                .collect(Collectors.toList());
        final ModelAndView mav = new ModelAndView("reservation/ownerReservations");
        mav.addObject("ownerReservations", reservations);
        mav.addObject("ownerReservationsPage", resultPage);
        mav.addObject("selectedCar", null);
        mav.addObject("ownerCurrentSort", sort);
        mav.addObject("activeTab", "owner-reservations");
        return mav;
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
        final Optional<Car> carOpt = carService.getCarById(carId);
        if (carOpt.isEmpty() || carOpt.get().getOwnerId() != me.getId()) {
            return new ModelAndView(redirectTo("/my-cars/reservations"));
        }
        page = Math.max(0, page);
        final String sort = MyHubSortSanitizer.sanitize(ownerSort, DEFAULT_SORT);
        final var criteria = reservationService.buildReservationSearchCriteria(
                me.getId(), null, ownerCategory, ownerTransmission, ownerPowertrain,
                ownerPriceMin, ownerPriceMax, ownerRating, ownerStatus, page, sort, ownerQ, carId);
        final Page<ReservationCard> resultPage = reservationService.getOwnerReservationCards(criteria);
        final int safePage = UiPaging.clampZeroBasedPage(page, resultPage.getTotalItems(), resultPage.getPageSize());
        if (safePage != page) {
            final RedirectView redirectView = new RedirectView(
                    UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request))
                            .replaceQueryParam("page", safePage)
                            .build()
                            .toUriString());
            redirectView.setExposeModelAttributes(false);
            return new ModelAndView(redirectView);
        }
        final Locale locale = LocaleContextHolder.getLocale();
        final List<ReservationCardDisplayRow> reservations = resultPage.getContent().stream()
                .map(card -> reservationViewService.toReservationCardDisplayRow(card, locale))
                .collect(Collectors.toList());
        final ModelAndView mav = new ModelAndView("reservation/ownerReservations");
        mav.addObject("ownerReservations", reservations);
        mav.addObject("ownerReservationsPage", resultPage);
        mav.addObject("selectedCar", carOpt.get());
        mav.addObject("ownerCurrentSort", sort);
        mav.addObject("activeTab", "owner-reservations");
        return mav;
    }

    @PostMapping("/car/{carId}/edit")
    public ModelAndView editCar(
            @CurrentUser final User currentUser,
            @PathVariable("carId") final long carId,
            @Validated(ValidationGroups.OnListingEdit.class) @ModelAttribute("editForm") final ListingEditForm editForm,
            final BindingResult errors) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final Optional<Car> carOpt = carService.getCarById(carId);
        if (carOpt.isEmpty() || carOpt.get().getOwnerId() != me.getId()) {
            return new ModelAndView(redirectTo("/my-cars"));
        }
        if (carOpt.get().getStatus() == Car.Status.DEACTIVATED) {
            return new ModelAndView(redirectTo("/my-cars/car/" + carId));
        }
        if (errors.hasErrors()) {
            final Locale editLocale = LocaleContextHolder.getLocale();
            final Optional<OwnerCarDetailPageModel> pmOpt =
                    carService.buildOwnerCarDetailPageModel(carId, editLocale);
            if (pmOpt.isPresent()) {
                final Page<ReservationCard> previewPage =
                        reservationService.getCarReservationCards(me.getId(), carId, 0, 3, null);
                final List<ReservationCardDisplayRow> previewReservations = previewPage.getContent().stream()
                        .map(card -> reservationViewService.toReservationCardDisplayRow(card, editLocale))
                        .collect(Collectors.toList());
                final ModelAndView mav = new ModelAndView("car/myCarDetail");
                pmOpt.get().populateModel(mav::addObject);
                mav.addObject("editForm", editForm);
                mav.addObject("previewReservations", previewReservations);
                mav.addObject("reservationPreviewTotal", previewPage.getTotalItems());
                mav.addObject("activeTab", "my-cars");
                return mav;
            }
            return new ModelAndView(redirectTo("/my-cars/car/" + carId));
        }
        final List<ListingAvailability> existing = listingAvailabilityService.findByCarId(carId);
        final List<AvailabilityPeriod> newPeriods = editForm.toAvailabilityPeriods();
        final List<BigDecimal> newPeriodPrices = editForm.toPeriodPrices();
        if (newPeriods.isEmpty()) {
            return new ModelAndView(redirectTo("/my-cars/car/" + carId));
        }
        if (existing.isEmpty()) {
            try {
                listingAvailabilityService.createCarAvailabilityPeriods(
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
                final Optional<OwnerCarDetailPageModel> pmOpt =
                        carService.buildOwnerCarDetailPageModel(carId, LocaleContextHolder.getLocale());
                if (pmOpt.isPresent()) {
                    final ModelAndView mav = new ModelAndView("car/myCarDetail");
                    pmOpt.get().populateModel(mav::addObject);
                    mav.addObject("editForm", editForm);
                    mav.addObject("activeTab", "my-cars");
                    return mav;
                }
            }
        } else {
            final ListingAvailability mostRecent = existing.stream()
                    .max(java.util.Comparator.comparing(ListingAvailability::getCreatedAt))
                    .get();
            try {
                for (int i = 0; i < newPeriods.size(); i++) {
                    final AvailabilityPeriod np = newPeriods.get(i);
                    final BigDecimal price = (i < newPeriodPrices.size() && newPeriodPrices.get(i) != null)
                            ? newPeriodPrices.get(i)
                            : editForm.getPricePerDay();
                    listingAvailabilityService.applyOwnerEditByCar(
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
        final Optional<Car> carOpt = carService.getCarById(carId);
        if (carOpt.isEmpty() || carOpt.get().getOwnerId() != me.getId()) {
            return new ModelAndView(redirectTo("/my-cars"));
        }
        page = Math.max(0, page);
        final String statusFilter = reservationViewService.normalizeReservationStatusQueryParam(reservationStatus);
        final Page<ReservationCard> resultPage =
                reservationService.getCarReservationCards(
                        me.getId(), carId, page, paginationPolicy.getDefaultPageSize(), statusFilter);
        final int safeReservationsPage = UiPaging.clampZeroBasedPage(page, resultPage.getTotalItems(), resultPage.getPageSize());
        if (safeReservationsPage != page) {
            final RedirectView redirectView = new RedirectView(
                    UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request))
                            .replaceQueryParam("page", safeReservationsPage)
                            .build()
                            .toUriString());
            redirectView.setExposeModelAttributes(false);
            return new ModelAndView(redirectView);
        }
        final Locale locale = LocaleContextHolder.getLocale();
        final List<ReservationCardDisplayRow> reservations = resultPage.getContent().stream()
                .map(card -> reservationViewService.toReservationCardDisplayRow(card, locale))
                .collect(Collectors.toList());

        final Car car = carOpt.get();
        final ModelAndView mav = new ModelAndView("car/carReservations");
        mav.addObject("car", car);
        mav.addObject("reservations", reservations);
        mav.addObject("carReservationsPage", resultPage);
        mav.addObject("statusFilter", statusFilter);
        mav.addObject("activeTab", "my-cars");
        return mav;
    }

    @GetMapping("/car/{carId}")
    public ModelAndView myCarDetail(
            @CurrentUser final User currentUser,
            @PathVariable final long carId) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final Optional<Car> carOpt = carService.getCarById(carId);
        if (carOpt.isEmpty() || carOpt.get().getOwnerId() != me.getId()) {
            return new ModelAndView(redirectTo("/my-cars"));
        }
        final Locale carDetailLocale = LocaleContextHolder.getLocale();
        final Optional<OwnerCarDetailPageModel> pmOpt =
                carService.buildOwnerCarDetailPageModel(carId, carDetailLocale);
        final ListingEditForm editForm = new ListingEditForm();
        if (pmOpt.isPresent()) {
            applyEditFormDefaultsFromAvailabilities(editForm, pmOpt.get().getAvailabilities());
            final Page<ReservationCard> previewPage =
                    reservationService.getCarReservationCards(me.getId(), carId, 0, 3, null);
            final List<ReservationCardDisplayRow> previewReservations = previewPage.getContent().stream()
                    .map(card -> reservationViewService.toReservationCardDisplayRow(card, carDetailLocale))
                    .collect(Collectors.toList());
            final ModelAndView mav = new ModelAndView("car/myCarDetail");
            pmOpt.get().populateModel(mav::addObject);
            mav.addObject("editForm", editForm);
            mav.addObject("previewReservations", previewReservations);
            mav.addObject("reservationPreviewTotal", previewPage.getTotalItems());
            mav.addObject("activeTab", "my-cars");
            return mav;
        }
        final long carImageId = carPictureService.getCarPicturesByCarId(carOpt.get().getId()).stream()
                .map(p -> p.getImageId())
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(0L);
        final ModelAndView mav = new ModelAndView("car/myCarDetail");
        mav.addObject("car", carOpt.get());
        mav.addObject("carImageId", carImageId);
        mav.addObject("hasPublishedAvailability", false);
        mav.addObject("activeTab", "my-cars");
        return mav;
    }

    @GetMapping("/car/{carId}/create")
    public ModelAndView createListingForm(
            @CurrentUser final User currentUser,
            @PathVariable final long carId) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final Optional<Car> carOpt = carService.getCarById(carId);
        if (carOpt.isEmpty() || carOpt.get().getOwnerId() != me.getId()) {
            return new ModelAndView(redirectTo("/my-cars"));
        }
        if (carService.isModelPendingValidation(carId)) {
            return new ModelAndView(redirectTo("/my-cars/car/" + carId));
        }
        final CreateListingForm form = new CreateListingForm();
        listingAvailabilityService.findMostRecentByCarId(carId)
                .ifPresent(la -> prefillLocationAndTimes(form, la));
        return buildCreateListingView(carOpt.get(), form, me);
    }

    @GetMapping("/car/{carId}/availability/{availabilityId}/edit")
    public ModelAndView editAvailabilityForm(
            @CurrentUser final User currentUser,
            @PathVariable final long carId,
            @PathVariable final long availabilityId) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final Optional<Car> carOpt = carService.getCarById(carId);
        if (carOpt.isEmpty() || carOpt.get().getOwnerId() != me.getId()) {
            return new ModelAndView(redirectTo("/my-cars"));
        }
        final Optional<ListingAvailability> avOpt = listingAvailabilityService.findById(availabilityId);
        if (avOpt.isEmpty() || avOpt.get().getCarId() != carId) {
            return new ModelAndView(redirectTo("/my-cars/car/" + carId));
        }
        return buildEditAvailabilityView(carOpt.get(), availabilityId, buildEditAvailabilityForm(avOpt.get()), me);
    }

    @PostMapping("/car/{carId}/availability/{availabilityId}/edit")
    public ModelAndView editAvailability(
            @CurrentUser final User currentUser,
            @PathVariable final long carId,
            @PathVariable final long availabilityId,
            @Validated(ValidationGroups.OnCreateListing.class) @ModelAttribute("createListingForm") final CreateListingForm form,
            final BindingResult errors) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final Optional<Car> carOpt = carService.getCarById(carId);
        if (carOpt.isEmpty() || carOpt.get().getOwnerId() != me.getId()) {
            return new ModelAndView(redirectTo("/my-cars"));
        }
        final Optional<ListingAvailability> avOpt = listingAvailabilityService.findById(availabilityId);
        if (avOpt.isEmpty() || avOpt.get().getCarId() != carId) {
            return new ModelAndView(redirectTo("/my-cars/car/" + carId));
        }
        final ListingAvailability old = avOpt.get();
        if (!errors.hasErrors()) {
            try {
                listingAvailabilityService.validateEditAvailabilityRiderLead(
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
                listingAvailabilityService.validatePublicationAvailabilityAgainstWallCalendar(form.toAvailabilityPeriods());
            } catch (final CarValidationException e) {
                errors.reject(e.getMessageCode(), e.getMessageArgs(), localeMessages.msg(e));
            }
        }
        if (errors.hasErrors()) {
            return buildEditAvailabilityView(carOpt.get(), availabilityId, form, me);
        }
        final AvailabilityPeriod newPeriod = form.toAvailabilityPeriods().get(0);
        try {
            listingAvailabilityService.applyOwnerEditByCar(
                    carId,
                    old.getStartInclusive(), old.getEndInclusive(),
                    newPeriod.getStartInclusive(), newPeriod.getEndInclusive(),
                    form.getPricePerDay(),
                    form.getStartPointStreet(), form.getStartPointNumber(),
                    form.getNeighborhoodId(),
                    form.getCheckInTime(), form.getCheckOutTime());
        } catch (final CarValidationException e) {
            errors.reject(e.getMessageCode(), e.getMessageArgs(), localeMessages.msg(e));
            return buildEditAvailabilityView(carOpt.get(), availabilityId, form, me);
        }
        return new ModelAndView(redirectTo("/my-cars/car/" + carId));
    }

    @PostMapping("/car/{carId}/create")
    public ModelAndView createListing(
            @CurrentUser final User currentUser,
            @PathVariable final long carId,
            @Validated(ValidationGroups.OnCreateListing.class) @ModelAttribute("createListingForm") final CreateListingForm form,
            final BindingResult errors) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final Optional<Car> carOpt = carService.getCarById(carId);
        if (carOpt.isEmpty() || carOpt.get().getOwnerId() != me.getId()) {
            return new ModelAndView(redirectTo("/my-cars"));
        }
        if (carService.isModelPendingValidation(carId)) {
            return new ModelAndView(redirectTo("/my-cars/car/" + carId));
        }
        if (errors.hasErrors()) {
            return buildCreateListingView(carOpt.get(), form, me);
        }
        final User freshUser = userService.getUserById(me.getId()).orElse(me);
        if (!userService.hasValidCbu(freshUser)) {
            errors.reject(MessageKeys.LISTING_PUBLISH_CBU_REQUIRED, localeMessages.msg(MessageKeys.LISTING_PUBLISH_CBU_REQUIRED));
            return buildCreateListingView(carOpt.get(), form, freshUser);
        }
        if (!errors.hasErrors()) {
            try {
                listingAvailabilityService.validatePublicationAvailabilityRiderLead(
                        form.toAvailabilityPeriods(), form.getCheckInTime(), Instant.now());
            } catch (final AvailabilityRiderLeadViolationException e) {
                errors.rejectValue(
                        "availabilityRows[" + e.getAvailabilityRowIndex() + "].from",
                        e.getMessageCode(), e.getMessageArgs(), localeMessages.msg(e));
            }
        }
        if (!errors.hasErrors()) {
            try {
                listingAvailabilityService.validatePublicationAvailabilityAgainstWallCalendar(form.toAvailabilityPeriods());
            } catch (final CarValidationException e) {
                errors.reject(e.getMessageCode(), e.getMessageArgs(), localeMessages.msg(e));
            }
        }
        if (errors.hasErrors()) {
            return buildCreateListingView(carOpt.get(), form, freshUser);
        }
        try {
            listingAvailabilityService.createCarAvailabilityPeriods(
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
            return buildCreateListingView(carOpt.get(), form, freshUser);
        }
    }

    @GetMapping(value = "/car/{carId}/availability-min-from", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, String> availabilityMinFrom(
            @CurrentUser final User currentUser,
            @PathVariable final long carId,
            @RequestParam(name = "checkIn", required = false) final String checkInRaw) {
        WebAuthUtils.requireUser(currentUser);
        final LocalTime lt = parseCheckInParam(checkInRaw);
        final LocalDate minFrom = listingAvailabilityService.getPublicationMinAvailabilityFirstWallDay(lt, Instant.now());
        return Map.of("minFrom", minFrom.toString());
    }

    @PostMapping(value = "/quick-cbu", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> quickCbu(
            @CurrentUser final User currentUser,
            @RequestParam("cbu") final String cbuRaw) {
        WebAuthUtils.requireUser(currentUser);
        final String trimmed = cbuRaw == null ? "" : cbuRaw.trim();
        if (!userService.isValidCbuFormat(cbuRaw)) {
            return ResponseEntity.badRequest().build();
        }
        userService.updateCbu(currentUser.getId(), trimmed);
        return ResponseEntity.noContent().build();
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
        final Optional<Car> carOpt = carService.getCarById(carId);
        if (carOpt.isEmpty() || carOpt.get().getOwnerId() != me.getId()) {
            return new ModelAndView(redirectTo("/my-cars"));
        }
        if (insuranceFile == null || insuranceFile.isEmpty()) {
            redirectAttributes.addFlashAttribute("carInsuranceErrorMessage",
                    localeMessages.msg(MessageKeys.CAR_INSURANCE_INVALID));
            return new ModelAndView(redirectTo("/my-cars/car/" + carId));
        }
        if (insuranceFile.getSize() > profileDocumentUploadPolicy.getMaxBytes()) {
            final int maxMb = profileDocumentUploadPolicy.getMaxMegabytesRoundedUp();
            redirectAttributes.addFlashAttribute("carInsuranceErrorMessage",
                    localeMessages.msg(MessageKeys.CAR_INSURANCE_TOO_LARGE, maxMb));
            return new ModelAndView(redirectTo("/my-cars/car/" + carId));
        }
        try {
            carService.uploadValidatedCarInsuranceDocument(
                    me.getId(),
                    carId,
                    insuranceFile.getOriginalFilename(),
                    insuranceFile.getContentType(),
                    insuranceFile.getBytes());
        } catch (final RydenException e) {
            redirectAttributes.addFlashAttribute("carInsuranceErrorMessage", localeMessages.msg(e));
        } catch (final IOException e) {
            redirectAttributes.addFlashAttribute("carInsuranceErrorMessage",
                    localeMessages.msg(MessageKeys.PUBLISH_FAILED, supportEmail));
        }
        return new ModelAndView(redirectTo("/my-cars/car/" + carId));
    }

    /**
     * AJAX variant of {@link #uploadInsurance} so the car-detail page can update without a full reload.
     */
    @PostMapping(value = "/car/{carId}/quick-insurance", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> quickInsurance(
            @CurrentUser final User currentUser,
            @PathVariable("carId") final long carId,
            @RequestParam(name = "insuranceFile", required = false) final MultipartFile insuranceFile) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final Optional<Car> carOpt = carService.getCarById(carId);
        if (carOpt.isEmpty() || carOpt.get().getOwnerId() != me.getId()) {
            return ResponseEntity.notFound().build();
        }
        if (insuranceFile == null || insuranceFile.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (insuranceFile.getSize() > profileDocumentUploadPolicy.getMaxBytes()) {
            final int maxMb = profileDocumentUploadPolicy.getMaxMegabytesRoundedUp();
            final HttpHeaders headers = new HttpHeaders();
            headers.add("X-Ryden-Error", localeMessages.msg(MessageKeys.CAR_INSURANCE_TOO_LARGE, maxMb));
            return new ResponseEntity<>(null, headers, HttpStatus.BAD_REQUEST);
        }
        try {
            carService.uploadValidatedCarInsuranceDocument(
                    me.getId(),
                    carId,
                    insuranceFile.getOriginalFilename(),
                    insuranceFile.getContentType(),
                    insuranceFile.getBytes());
            return ResponseEntity.noContent().build();
        } catch (final RydenException e) {
            final HttpHeaders headers = new HttpHeaders();
            headers.add("X-Ryden-Error", localeMessages.msg(e));
            return new ResponseEntity<>(null, headers, HttpStatus.BAD_REQUEST);
        } catch (final IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private static RedirectView redirectTo(final String url) {
        final RedirectView rv = new RedirectView(url, true);
        rv.setExposeModelAttributes(false);
        return rv;
    }

    private static LocalTime parseCheckInParam(final String checkInRaw) {
        if (checkInRaw == null || checkInRaw.isBlank()) {
            return ListingAvailability.DEFAULT_CHECK_IN_TIME;
        }
        try {
            return LocalTime.parse(checkInRaw.trim());
        } catch (final DateTimeParseException ignored) {
            return ListingAvailability.DEFAULT_CHECK_IN_TIME;
        }
    }

    private ModelAndView buildCreateListingView(final Car car, final CreateListingForm form, final User user) {
        final User freshUser = userService.getUserById(user.getId()).orElse(user);
        final boolean userHasCbu = userService.hasValidCbu(freshUser);
        final ModelAndView mav = new ModelAndView("listing/createListing");
        mav.addObject("car", car);
        mav.addObject("createListingForm", form);
        mav.addObject("userHasCbu", userHasCbu);
        mav.addObject("allNeighborhoods", locationService.findAllNeighborhoods());
        final LocalDate minAvail = listingAvailabilityService.getPublicationMinAvailabilityFirstWallDay(
                form.getCheckInTime(), Instant.now());
        mav.addObject("publishMinAvailabilityFrom", minAvail.toString());
        mav.addObject("pickupLeadHours", reservationTimingPolicy.getPickupLeadHours());
        final int forwardDays = listingAvailabilityService.getConfiguredMaxAvailabilityForwardWallDays();
        mav.addObject("maxAvailabilityForwardWallDays", forwardDays);
        final LocalDate wallToday = LocalDate.now(AvailabilityPeriod.WALL_ZONE);
        mav.addObject("publishMaxAvailabilityWallInclusive", wallToday.plusDays(forwardDays).toString());
        mav.addObject("publisherEmail", freshUser.getEmail());
        mav.addObject("activeTab", "my-cars");
        carService.getPriceMarketInsightForCar(car, null)
                .ifPresent(insight -> mav.addObject("priceMarketInsight", insight));
        return mav;
    }

    private static void applyEditFormDefaultsFromAvailabilities(
            final ListingEditForm editForm,
            final List<ListingAvailability> availabilities) {
        if (availabilities == null || availabilities.isEmpty()) {
            return;
        }
        final ListingAvailability mostRecent = availabilities.stream()
                .max(java.util.Comparator.comparing(ListingAvailability::getCreatedAt))
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

    private static void prefillLocationAndTimes(final CreateListingForm form, final ListingAvailability la) {
        form.setStartPointStreet(la.getStartPointStreet());
        form.setStartPointNumber(la.getStartPointNumber().orElse(null));
        form.setNeighborhoodId(la.getNeighborhoodId().orElse(null));
        form.setCheckInTime(la.getCheckInTime());
        form.setCheckOutTime(la.getCheckOutTime());
    }

    private static CreateListingForm buildEditAvailabilityForm(final ListingAvailability av) {
        final CreateListingForm form = new CreateListingForm();
        form.setPricePerDay(av.getDayPriceValue());
        form.setStartPointStreet(av.getStartPointStreet());
        form.setStartPointNumber(av.getStartPointNumber().orElse(null));
        form.setNeighborhoodId(av.getNeighborhoodId().orElse(null));
        form.setCheckInTime(av.getCheckInTime());
        form.setCheckOutTime(av.getCheckOutTime());
        final CreateListingForm.AvailabilityRow row = new CreateListingForm.AvailabilityRow();
        row.setFrom(av.getStartInclusive());
        row.setUntil(av.getEndInclusive());
        form.setAvailabilityRows(List.of(row));
        return form;
    }

    private ModelAndView buildEditAvailabilityView(
            final Car car, final long availabilityId, final CreateListingForm form, final User user) {
        final User freshUser = userService.getUserById(user.getId()).orElse(user);
        final ModelAndView mav = new ModelAndView("listing/editAvailability");
        mav.addObject("car", car);
        mav.addObject("availabilityId", availabilityId);
        mav.addObject("createListingForm", form);
        mav.addObject("allNeighborhoods", locationService.findAllNeighborhoods());
        mav.addObject("activeTab", "my-cars");
        mav.addObject("publisherEmail", freshUser.getEmail());
        final LocalDate minAvail = listingAvailabilityService.getPublicationMinAvailabilityFirstWallDay(
                form.getCheckInTime(), Instant.now());
        mav.addObject("publishMinAvailabilityFrom", minAvail.toString());
        mav.addObject("pickupLeadHours", reservationTimingPolicy.getPickupLeadHours());
        final int forwardDays = listingAvailabilityService.getConfiguredMaxAvailabilityForwardWallDays();
        mav.addObject("maxAvailabilityForwardWallDays", forwardDays);
        final LocalDate wallToday = LocalDate.now(AvailabilityPeriod.WALL_ZONE);
        mav.addObject("publishMaxAvailabilityWallInclusive", wallToday.plusDays(forwardDays).toString());
        carService.getPriceMarketInsightForCar(car, null)
                .ifPresent(insight -> mav.addObject("priceMarketInsight", insight));
        return mav;
    }
}
