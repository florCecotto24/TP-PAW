package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.dto.CarCard;
import ar.edu.itba.paw.models.dto.ListingDetail;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.ReservationCard;
import ar.edu.itba.paw.models.dto.ReservationCardDisplayRow;
import ar.edu.itba.paw.models.pagination.UiPaging;
import ar.edu.itba.paw.models.util.MyHubSortSanitizer;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.OwnerListingDetailPageModel;
import ar.edu.itba.paw.services.CarPictureService;
import ar.edu.itba.paw.services.CarService;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.services.ListingViewService;
import ar.edu.itba.paw.services.LocationService;
import ar.edu.itba.paw.services.ReservationService;
import ar.edu.itba.paw.services.ReservationViewService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.policy.PaginationPolicy;
import ar.edu.itba.paw.exception.listing.AvailabilityRiderLeadViolationException;
import ar.edu.itba.paw.exception.listing.ListingValidationException;
import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.util.LocaleMessages;
import ar.edu.itba.paw.webapp.form.CreateListingForm;
import ar.edu.itba.paw.webapp.form.ListingEditForm;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;
import ar.edu.itba.paw.webapp.validation.ListingNeighborhoodFormValidator;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import org.springframework.context.i18n.LocaleContextHolder;
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
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;

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

/** Owner hub: listing cards, edit, pause/finish, and per-listing reservation analytics and actions. */
@Controller
@RequestMapping("/my-cars")
public final class MyListingsController {

    private static final String TAB_LISTINGS = "listings";
    private static final String TAB_RESERVATIONS = "reservations";

    private final ListingService listingService;
    private final ListingViewService listingViewService;
    private final ReservationService reservationService;
    private final ReservationViewService reservationViewService;
    private final ListingNeighborhoodFormValidator listingNeighborhoodFormValidator;
    private final PaginationPolicy paginationPolicy;
    private final LocaleMessages localeMessages;
    private final CarService carService;
    private final CarPictureService carPictureService;
    private final LocationService locationService;
    private final UserService userService;

    public MyListingsController(
            final ListingService listingService,
            final ListingViewService listingViewService,
            final ReservationService reservationService,
            final ReservationViewService reservationViewService,
            final ListingNeighborhoodFormValidator listingNeighborhoodFormValidator,
            final PaginationPolicy paginationPolicy,
            final LocaleMessages localeMessages,
            final CarService carService,
            final CarPictureService carPictureService,
            final LocationService locationService,
            final UserService userService) {
        this.listingService = listingService;
        this.listingViewService = listingViewService;
        this.reservationService = reservationService;
        this.reservationViewService = reservationViewService;
        this.listingNeighborhoodFormValidator = listingNeighborhoodFormValidator;
        this.paginationPolicy = paginationPolicy;
        this.localeMessages = localeMessages;
        this.carService = carService;
        this.carPictureService = carPictureService;
        this.locationService = locationService;
        this.userService = userService;
    }

    @InitBinder("editForm")
    public void initEditFormBinder(final WebDataBinder binder) {
        binder.addValidators(listingNeighborhoodFormValidator);
    }

    @InitBinder("createListingForm")
    public void initCreateListingFormBinder(final WebDataBinder binder) {
        binder.addValidators(listingNeighborhoodFormValidator);
    }

    private static final String DEFAULT_SORT = "date,desc";

    @GetMapping
    public ModelAndView myListings(
            @CurrentUser final User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "0") int ownerPage,
            @RequestParam(required = false) final List<String> listingStatus,
            @RequestParam(required = false) final List<String> ownerStatus,
            @RequestParam(required = false) final String q,
            @RequestParam(required = false) final String tab,
            @RequestParam(required = false) final List<String> category,
            @RequestParam(required = false) final List<String> transmission,
            @RequestParam(required = false) final List<String> powertrain,
            @RequestParam(required = false) final BigDecimal priceMin,
            @RequestParam(required = false) final BigDecimal priceMax,
            @RequestParam(required = false) final List<String> rating,
            @RequestParam(required = false) final String sort,
            @RequestParam(required = false) final List<String> ownerCategory,
            @RequestParam(required = false) final List<String> ownerTransmission,
            @RequestParam(required = false) final List<String> ownerPowertrain,
            @RequestParam(required = false) final BigDecimal ownerPriceMin,
            @RequestParam(required = false) final BigDecimal ownerPriceMax,
            @RequestParam(required = false) final List<String> ownerRating,
            @RequestParam(required = false) final String ownerSort,
            @RequestParam(required = false) final String ownerQ,
            final HttpServletRequest request) {
        final User me = WebAuthUtils.requireUser(currentUser);
        page = Math.max(0, page);
        ownerPage = Math.max(0, ownerPage);

        final String selectedTab = TAB_RESERVATIONS.equals(tab) ? TAB_RESERVATIONS : TAB_LISTINGS;
        final String listingsSort = MyHubSortSanitizer.sanitize(sort, DEFAULT_SORT);
        final String ownerResSort = MyHubSortSanitizer.sanitize(ownerSort, DEFAULT_SORT);
        // Cars tab data
        final var listingsCriteria = listingService.buildOwnerListingSearchCriteria(
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
        final List<CarCard> listings = resultPage.getContent();

        // Reservations tab data
        final var ownerCriteria = reservationService.buildReservationSearchCriteria(
                me.getId(), null, ownerCategory, ownerTransmission, ownerPowertrain, ownerPriceMin, ownerPriceMax,
                ownerRating, ownerStatus, ownerPage, ownerResSort, ownerQ);
        final Page<ReservationCard> ownerResultPage = reservationService.getOwnerReservationCards(ownerCriteria);
        final int safeOwnerPage = UiPaging.clampZeroBasedPage(
                ownerPage, ownerResultPage.getTotalItems(), ownerResultPage.getPageSize());
        if (safeOwnerPage != ownerPage) {
            final RedirectView redirectView = new RedirectView(
                    UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request))
                            .replaceQueryParam("ownerPage", safeOwnerPage)
                            .build()
                            .toUriString());
            redirectView.setExposeModelAttributes(false);
            return new ModelAndView(redirectView);
        }
        final Locale locale = LocaleContextHolder.getLocale();
        final List<ReservationCardDisplayRow> ownerReservations = ownerResultPage.getContent().stream()
                .map(card -> reservationViewService.toReservationCardDisplayRow(card, locale))
                .collect(Collectors.toList());

        final ModelAndView mav = new ModelAndView("myListings");
        mav.addObject("results", listings);
        mav.addObject("myListingsPage", resultPage);
        mav.addObject("ownerReservations", ownerReservations);
        mav.addObject("ownerReservationsPage", ownerResultPage);
        mav.addObject("selectedListingsTab", selectedTab);
        mav.addObject("activeTab", "my-cars");
        mav.addObject("listingsCurrentSort", listingsSort);
        mav.addObject("ownerCurrentSort", ownerResSort);
        return mav;
    }

    @GetMapping("/{listingId}")
    public ModelAndView listingDetail(
            @CurrentUser final User currentUser,
            @PathVariable("listingId") final long listingId) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final Optional<ListingDetail> listingDetailOpt = listingService.getListingDetailById(listingId);
        if (listingDetailOpt.isEmpty() || listingDetailOpt.get().getOwner().getId() != me.getId()) {
            return new ModelAndView(redirectTo("/my-cars"));
        }

        return buildDetailModelAndView(listingDetailOpt.get(), new ListingEditForm());
    }

    @PostMapping("/{listingId}/edit")
    public ModelAndView editListing(
            @CurrentUser final User currentUser,
            @PathVariable("listingId") final long listingId,
            @Validated(ValidationGroups.OnListingEdit.class) @ModelAttribute("editForm") final ListingEditForm editForm,
            final BindingResult errors) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final Optional<ListingDetail> listingDetailOpt = listingService.getListingDetailById(listingId);
        if (listingDetailOpt.isEmpty() || listingDetailOpt.get().getOwner().getId() != me.getId()) {
            return new ModelAndView(redirectTo("/my-cars"));
        }
        if (listingDetailOpt.get().getListing().getStatus() == Listing.Status.FINISHED) {
            return new ModelAndView(redirectTo("/my-cars/" + listingId));
        }
        if (errors.hasErrors()) {
            return buildDetailModelAndView(listingDetailOpt.get(), editForm);
        }

        listingService.updateOwnerListing(
                me.getId(),
                listingId,
                editForm.getPricePerDay(),
                editForm.getStartPointStreet(),
                editForm.getStartPointNumber(),
                editForm.getDescription(),
                editForm.getCheckInTime(),
                editForm.getCheckOutTime(),
                editForm.toAvailabilityPeriods(),
                editForm.toPeriodPrices(),
                editForm.getNeighborhoodId());
        return new ModelAndView(redirectTo("/my-cars/" + listingId));
    }

    @PostMapping("/{listingId}/finish")
    public ModelAndView finishListing(
            @CurrentUser final User currentUser,
            @PathVariable("listingId") final long listingId) {
        final User me = WebAuthUtils.requireUser(currentUser);
        listingService.finishListing(me.getId(), listingId);
        return new ModelAndView(redirectTo("/my-cars/" + listingId));
    }

    @PostMapping("/{listingId}/toggle")
    public ModelAndView toggleListing(
            @CurrentUser final User currentUser,
            @PathVariable("listingId") final long listingId,
            final RedirectAttributes redirectAttributes) {
        final User me = WebAuthUtils.requireUser(currentUser);
        try {
            listingService.toggleListingStatus(me.getId(), listingId);
        } catch (final ListingValidationException e) {
            redirectAttributes.addFlashAttribute("listingToggleErrorMessage", localeMessages.msg(e));
        }
        return new ModelAndView(redirectTo("/my-cars/" + listingId));
    }

    @GetMapping("/{listingId}/reservations")
    public ModelAndView listingReservations(
            @CurrentUser final User currentUser,
            @PathVariable("listingId") final long listingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) final String reservationStatus,
            final HttpServletRequest request) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final Optional<ListingDetail> listingDetailOpt = listingService.getListingDetailById(listingId);
        if (listingDetailOpt.isEmpty() || listingDetailOpt.get().getOwner().getId() != me.getId()) {
            return new ModelAndView(redirectTo("/my-cars"));
        }
        page = Math.max(0, page);
        final String statusFilter = reservationViewService.normalizeReservationStatusQueryParam(reservationStatus);
        final Page<ReservationCard> resultPage =
                reservationService.getListingReservationCards(
                        me.getId(), listingId, page, paginationPolicy.getDefaultPageSize(), statusFilter);
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

        final ListingDetail detail = listingDetailOpt.get();
        final ModelAndView mav = new ModelAndView("listingReservations");
        mav.addObject("listing", detail.getListing());
        mav.addObject("car", detail.getCar());
        mav.addObject("reservations", reservations);
        mav.addObject("listingReservationsPage", resultPage);
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
        final Optional<ListingDetail> detailOpt = listingService.findMostRecentListingByCar(carId)
                .flatMap(l -> listingService.getListingDetailById(l.getId()));
        if (detailOpt.isPresent()) {
            return buildDetailModelAndView(detailOpt.get(), new ListingEditForm());
        }
        final long carImageId = carPictureService.getCarPicturesByCarId(carOpt.get().getId()).stream()
                .findFirst().map(p -> p.getImageId()).orElse(0L);
        final ModelAndView mav = new ModelAndView("myListingDetail");
        mav.addObject("car", carOpt.get());
        mav.addObject("carImageId", carImageId);
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
        return buildCreateListingView(carOpt.get(), new CreateListingForm(), me);
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
                listingService.validatePublicationAvailabilityRiderLead(
                        form.toAvailabilityPeriods(), form.getCheckInTime(), Instant.now());
            } catch (final AvailabilityRiderLeadViolationException e) {
                errors.rejectValue(
                        "availabilityRows[" + e.getAvailabilityRowIndex() + "].from",
                        e.getMessageCode(), e.getMessageArgs(), localeMessages.msg(e));
            }
        }
        if (!errors.hasErrors()) {
            try {
                listingService.validatePublicationAvailabilityAgainstWallCalendar(form.toAvailabilityPeriods());
            } catch (final ListingValidationException e) {
                errors.reject(e.getMessageCode(), e.getMessageArgs(), localeMessages.msg(e));
            }
        }
        if (errors.hasErrors()) {
            return buildCreateListingView(carOpt.get(), form, freshUser);
        }
        try {
            final Listing listing = listingService.createListingForCar(
                    carId,
                    me.getId(),
                    form.getPricePerDay(),
                    form.getStartPointStreet(),
                    form.getStartPointNumber(),
                    form.getDescription(),
                    form.getCheckInTime(),
                    form.getCheckOutTime(),
                    form.toAvailabilityPeriods(),
                    form.toPeriodPrices(),
                    form.getNeighborhoodId());
            return new ModelAndView(redirectTo("/my-cars/" + listing.getId()));
        } catch (final ListingValidationException e) {
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
        final LocalDate minFrom = listingService.getPublicationMinAvailabilityFirstWallDay(lt, Instant.now());
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

    private static RedirectView redirectTo(final String url) {
        final RedirectView rv = new RedirectView(url, true);
        rv.setExposeModelAttributes(false);
        return rv;
    }

    private static LocalTime parseCheckInParam(final String checkInRaw) {
        if (checkInRaw == null || checkInRaw.isBlank()) {
            return Listing.DEFAULT_CHECK_IN_TIME;
        }
        try {
            return LocalTime.parse(checkInRaw.trim());
        } catch (final DateTimeParseException ignored) {
            return Listing.DEFAULT_CHECK_IN_TIME;
        }
    }

    private ModelAndView buildCreateListingView(final Car car, final CreateListingForm form, final User user) {
        final User freshUser = userService.getUserById(user.getId()).orElse(user);
        final boolean userHasCbu = userService.hasValidCbu(freshUser);
        final ModelAndView mav = new ModelAndView("createListing");
        mav.addObject("car", car);
        mav.addObject("createListingForm", form);
        mav.addObject("userHasCbu", userHasCbu);
        mav.addObject("allNeighborhoods", locationService.findAllNeighborhoods());
        final LocalDate minAvail = listingService.getPublicationMinAvailabilityFirstWallDay(
                form.getCheckInTime(), Instant.now());
        mav.addObject("publishMinAvailabilityFrom", minAvail.toString());
        mav.addObject("pickupLeadHours", listingService.getConfiguredPickupLeadHours());
        final int forwardDays = listingService.getConfiguredMaxAvailabilityForwardWallDays();
        mav.addObject("maxAvailabilityForwardWallDays", forwardDays);
        final LocalDate wallToday = LocalDate.now(AvailabilityPeriod.WALL_ZONE);
        mav.addObject("publishMaxAvailabilityWallInclusive", wallToday.plusDays(forwardDays).toString());
        mav.addObject("publisherEmail", freshUser.getEmail());
        mav.addObject("activeTab", "my-cars");
        listingService.getPriceMarketInsightForCar(car, null)
                .ifPresent(insight -> mav.addObject("priceMarketInsight", insight));
        return mav;
    }

    private ModelAndView buildDetailModelAndView(final ListingDetail detail, final ListingEditForm editForm) {
        applyEditFormDefaults(editForm, detail.getListing(), detail.getListingAvailabilities());
        final OwnerListingDetailPageModel pageModel =
                listingViewService.buildOwnerListingDetailPageModel(detail, LocaleContextHolder.getLocale());
        final ModelAndView mav = new ModelAndView("myListingDetail");
        pageModel.populateModel(mav::addObject);
        mav.addObject("editForm", editForm);
        mav.addObject("activeTab", "my-cars");
        return mav;
    }

    private static void applyEditFormDefaults(
            final ListingEditForm editForm,
            final Listing listing,
            final List<ListingAvailability> availabilities) {
        if (editForm.getPricePerDay() == null) {
            editForm.setPricePerDay(listing.getDayPrice());
        }
        if (editForm.getStartPointStreet() == null) {
            editForm.setStartPointStreet(listing.getStartPointStreet());
        }
        if (editForm.getStartPointNumber() == null) {
            editForm.setStartPointNumber(listing.getStartPointNumber().orElse(null));
        }
        if (editForm.getDescription() == null) {
            editForm.setDescription(listing.getDescription());
        }
        if (editForm.getCheckInTime() == null) {
            editForm.setCheckInTime(listing.getCheckInTime());
        }
        if (editForm.getCheckOutTime() == null) {
            editForm.setCheckOutTime(listing.getCheckOutTime());
        }
        if (editForm.getNeighborhoodId() == null) {
            editForm.setNeighborhoodId(listing.getNeighborhoodId().orElse(null));
        }
        editForm.populateDefaultAvailability(availabilities);
    }
}
