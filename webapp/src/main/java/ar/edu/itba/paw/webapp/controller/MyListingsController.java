package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.domain.Neighborhood;
import ar.edu.itba.paw.models.dto.ListingCard;
import ar.edu.itba.paw.models.dto.ListingDetail;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.ReservationCard;
import ar.edu.itba.paw.models.pagination.UiPaging;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.util.WallDateTimeDisplayFormat;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.services.LocationService;
import ar.edu.itba.paw.services.ReservationService;
import ar.edu.itba.paw.services.policy.PaginationPolicy;
import ar.edu.itba.paw.exception.listing.ListingValidationException;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.util.LocaleMessages;
import ar.edu.itba.paw.webapp.dto.ReservationCardView;
import ar.edu.itba.paw.webapp.form.ListingEditForm;
import ar.edu.itba.paw.webapp.dto.VehicleCardView;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;
import ar.edu.itba.paw.webapp.validation.ListingNeighborhoodFormValidator;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import org.springframework.context.i18n.LocaleContextHolder;
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
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/my-listings")
public final class MyListingsController {

    private static final String TAB_LISTINGS = "listings";
    private static final String TAB_RESERVATIONS = "reservations";

    private final ListingService listingService;
    private final LocationService locationService;
    private final ReservationService reservationService;
    private final ListingNeighborhoodFormValidator listingNeighborhoodFormValidator;
    private final PaginationPolicy paginationPolicy;
    private final LocaleMessages localeMessages;

    public MyListingsController(
            final ListingService listingService,
            final LocationService locationService,
            final ReservationService reservationService,
            final ListingNeighborhoodFormValidator listingNeighborhoodFormValidator,
            final PaginationPolicy paginationPolicy,
            final LocaleMessages localeMessages) {
        this.listingService = listingService;
        this.locationService = locationService;
        this.reservationService = reservationService;
        this.listingNeighborhoodFormValidator = listingNeighborhoodFormValidator;
        this.paginationPolicy = paginationPolicy;
        this.localeMessages = localeMessages;
    }

    @InitBinder("editForm")
    public void initEditFormBinder(final WebDataBinder binder) {
        binder.addValidators(listingNeighborhoodFormValidator);
    }

    private static final String DEFAULT_SORT = "date,desc";
    private static final Set<String> VALID_SORTS = Set.of(
            "date,desc", "date,asc", "price,asc", "price,desc", "rating,asc", "rating,desc");

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
            final HttpServletRequest request) {
        final User me = WebAuthUtils.requireUser(currentUser);
        page = Math.max(0, page);
        ownerPage = Math.max(0, ownerPage);

        final String selectedTab = TAB_RESERVATIONS.equals(tab) ? TAB_RESERVATIONS : TAB_LISTINGS;
        // Listings tab data
        final var listingsCriteria = listingService.buildOwnerListingSearchCriteria(
                me.getId(), category, transmission, powertrain, priceMin, priceMax,
                listingStatus, rating, q, page, sort);
        final Page<ListingCard> resultPage = listingService.getOwnerListingCards(listingsCriteria);
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
        final List<VehicleCardView> listings = resultPage.getContent().stream()
                .map(VehicleCardView::fromOwnerListingCard)
                .collect(Collectors.toList());

        // Reservations tab data
        final var ownerCriteria = reservationService.buildReservationSearchCriteria(
                me.getId(), null, ownerCategory, ownerTransmission, ownerPowertrain, ownerPriceMin, ownerPriceMax,
                ownerRating, ownerStatus, ownerPage, ownerSort);
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
        final List<ReservationCardView> ownerReservations = ownerResultPage.getContent().stream()
                .map(card -> toReservationCardView(card, locale))
                .collect(Collectors.toList());

        final ModelAndView mav = new ModelAndView("myListings");
        mav.addObject("results", listings);
        mav.addObject("myListingsPage", resultPage);
        mav.addObject("ownerReservations", ownerReservations);
        mav.addObject("ownerReservationsPage", ownerResultPage);
        mav.addObject("selectedListingsTab", selectedTab);
        mav.addObject("activeTab", "my-listings");
        mav.addObject("listingsCurrentSort", sort != null && VALID_SORTS.contains(sort) ? sort : DEFAULT_SORT);
        mav.addObject("ownerCurrentSort", ownerSort != null && VALID_SORTS.contains(ownerSort) ? ownerSort : DEFAULT_SORT);
        return mav;
    }

    private static final Set<String> RESERVATION_STATUS_WHITELIST =
            Set.of("pending", "accepted", "started", "cancelled", "finished");

    private static String normalizeReservationStatusParam(final String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        final String t = raw.trim().toLowerCase();
        return RESERVATION_STATUS_WHITELIST.contains(t) ? t : null;
    }

    private ReservationCardView toReservationCardView(final ReservationCard card, final Locale locale) {
        final String pickupDisplay = WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(card.getStartDate(), locale);
        final String returnDisplay = WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(card.getEndDate(), locale);
        final long days = reservationService.calculateBillableDays(card.getStartDate(), card.getEndDate());
        final String totalPrice = days > 0
                ? formatMoney(card.getDayPrice().multiply(BigDecimal.valueOf(days)))
                : "-";
        return new ReservationCardView(
                card.getReservationId(),
                card.getListingId(),
                card.getImageId(),
                card.getBrand(),
                card.getModel(),
                pickupDisplay,
                returnDisplay,
                card.getStatus().name().toLowerCase(Locale.ROOT),
                totalPrice);
    }

    private static String formatMoney(final BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }

    @GetMapping("/{listingId}")
    public ModelAndView listingDetail(
            @CurrentUser final User currentUser,
            @PathVariable("listingId") final long listingId) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final Optional<ListingDetail> listingDetailOpt = listingService.getListingDetailById(listingId);
        if (listingDetailOpt.isEmpty() || listingDetailOpt.get().getOwner().getId() != me.getId()) {
            return new ModelAndView(new RedirectView("/my-listings", true));
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
            return new ModelAndView(new RedirectView("/my-listings", true));
        }
        if (listingDetailOpt.get().getListing().getStatus() == Listing.Status.FINISHED) {
            return new ModelAndView(new RedirectView("/my-listings/" + listingId, true));
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
                editForm.getNeighborhoodId());
        return new ModelAndView(new RedirectView("/my-listings/" + listingId, true));
    }

    @PostMapping("/{listingId}/finish")
    public ModelAndView finishListing(
            @CurrentUser final User currentUser,
            @PathVariable("listingId") final long listingId) {
        final User me = WebAuthUtils.requireUser(currentUser);
        listingService.finishListing(me.getId(), listingId);
        return new ModelAndView(new RedirectView("/my-listings/" + listingId, true));
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
        return new ModelAndView(new RedirectView("/my-listings/" + listingId, true));
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
            return new ModelAndView(new RedirectView("/my-listings", true));
        }
        page = Math.max(0, page);
        final String statusFilter = normalizeReservationStatusParam(reservationStatus);
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
        final List<ReservationCardView> reservations = resultPage.getContent().stream()
                .map(card -> toReservationCardView(card, locale))
                .collect(Collectors.toList());

        final ListingDetail detail = listingDetailOpt.get();
        final ModelAndView mav = new ModelAndView("listingReservations");
        mav.addObject("listing", detail.getListing());
        mav.addObject("car", detail.getCar());
        mav.addObject("reservations", reservations);
        mav.addObject("listingReservationsPage", resultPage);
        mav.addObject("statusFilter", statusFilter);
        mav.addObject("activeTab", "my-listings");
        return mav;
    }

    private ModelAndView buildDetailModelAndView(final ListingDetail detail, final ListingEditForm editForm) {
        final Listing listing = detail.getListing();
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
        editForm.populateDefaultAvailability(detail.getListingAvailabilities());

        final long carImageId = detail.getPictures().isEmpty() ? 0L : detail.getPictures().get(0).getImageId();
        final long ownerId = detail.getOwner().getId();
        final long listingId = listing.getId();
        final Locale locale = LocaleContextHolder.getLocale();

        final Map<String, Long> reservationStatusCounts =
                reservationService.countListingReservationsByStatus(ownerId, listingId);
        final long reservationTotal = reservationStatusCounts.values().stream().mapToLong(Long::longValue).sum();

        final String totalEarnings = formatMoney(reservationService.getListingTotalEarnings(ownerId, listingId));
        final String pendingEarnings = formatMoney(reservationService.getListingPendingEarnings(ownerId, listingId));
        final long totalDaysRented = reservationService.getListingTotalDaysRented(ownerId, listingId);
        final long reservationsThisMonth = reservationService.getListingReservationsThisMonth(ownerId, listingId);

        final long cancelled = reservationStatusCounts.getOrDefault("cancelled", 0L);
        final String cancellationRateDisplay = reservationTotal > 0
                ? String.format("%.1f%%", (double) cancelled / reservationTotal * 100)
                : "0.0%";

        final String nextReservationDisplay = reservationService.getListingNextReservationDate(ownerId, listingId)
                .map(dt -> WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(dt, locale))
                .orElse(null);

        final List<Neighborhood> allNeighborhoods = locationService.findAllNeighborhoods();
        final Long listingNbId = listing.getNeighborhoodId().orElse(null);
        final String listingNeighborhoodName = listingNbId == null ? null : allNeighborhoods.stream()
                .filter(nb -> nb.getId() == listingNbId)
                .map(Neighborhood::getName)
                .findFirst().orElse(null);

        final ModelAndView mav = new ModelAndView("myListingDetail");
        mav.addObject("allNeighborhoods", allNeighborhoods);
        mav.addObject("listingNeighborhoodName", listingNeighborhoodName);
        mav.addObject("listingStreetNumber", listing.getStartPointNumber().orElse(null));
        mav.addObject("listing", listing);
        mav.addObject("listingCreatedAtDisplay", WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(listing.getCreatedAt(), locale));
        mav.addObject("car", detail.getCar());
        mav.addObject("owner", detail.getOwner());
        mav.addObject("availabilities", detail.getListingAvailabilities());
        mav.addObject("carImageId", carImageId);
        mav.addObject("statusKey", listing.getStatus().name());
        mav.addObject("editForm", editForm);
        mav.addObject("activeTab", "my-listings");
        mav.addObject("reservationStatusCounts", reservationStatusCounts);
        mav.addObject("reservationTotal", reservationTotal);
        mav.addObject("listingTotalEarnings", totalEarnings);
        mav.addObject("listingPendingEarnings", pendingEarnings);
        mav.addObject("listingTotalDaysRented", totalDaysRented);
        mav.addObject("listingReservationsThisMonth", reservationsThisMonth);
        mav.addObject("listingCancellationRate", cancellationRateDisplay);
        mav.addObject("listingNextReservationDisplay", nextReservationDisplay);
        final int forwardDays = listingService.getConfiguredMaxAvailabilityForwardWallDays();
        final java.time.LocalDate wallToday = java.time.LocalDate.now(AvailabilityPeriod.WALL_ZONE);
        final List<ListingAvailability> editPastAvailabilities = detail.getListingAvailabilities().stream()
                .filter(la -> la.getEndInclusive().isBefore(wallToday))
                .collect(Collectors.toList());
        mav.addObject("editPastAvailabilities", editPastAvailabilities);
        mav.addObject("editAvailMaxYmd", wallToday.plusDays(forwardDays).toString());
        mav.addObject("editAvailWallToday", wallToday);
        return mav;
    }
}
