package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.ListingCard;
import ar.edu.itba.paw.models.ListingDetail;
import ar.edu.itba.paw.models.Page;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.services.LocationService;
import ar.edu.itba.paw.webapp.form.ListingEditForm;
import ar.edu.itba.paw.webapp.dto.VehicleCardView;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;
import ar.edu.itba.paw.webapp.validation.ListingNeighborhoodFormValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import ar.edu.itba.paw.models.WallDateTimeDisplayFormat;
import org.springframework.context.i18n.LocaleContextHolder;

@Controller
@RequestMapping("/my-listings")
public class MyListingsController {

    private static final int PAGE_SIZE = 8;
    private static final Set<String> OWNER_LISTING_STATUS_WHITELIST = Set.of("active", "paused", "finished");

    private final ListingService listingService;
    private final LocationService locationService;
    private final ListingNeighborhoodFormValidator listingNeighborhoodFormValidator;

    @Autowired
    public MyListingsController(
            final ListingService listingService,
            final LocationService locationService,
            final ListingNeighborhoodFormValidator listingNeighborhoodFormValidator) {
        this.listingService = listingService;
        this.locationService = locationService;
        this.listingNeighborhoodFormValidator = listingNeighborhoodFormValidator;
    }

    @InitBinder("editForm")
    public void initEditFormBinder(final WebDataBinder binder) {
        binder.addValidators(listingNeighborhoodFormValidator);
    }

    @GetMapping
    public ModelAndView myListings(
            @ModelAttribute(name = LoggedUserAdvice.CURRENT_USER_MODEL_KEY, binding = false) final User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) final String listingStatus,
            @RequestParam(required = false) final String q,
            final HttpServletRequest request) {
        final User me = WebAuthUtils.requireUser(currentUser);
        page = Math.max(0, page);

        final String listingStatusFilter = normalizeOwnerListingStatusParam(listingStatus);
        final Page<ListingCard> resultPage =
                listingService.getOwnerListingCards(me.getId(), page, PAGE_SIZE, listingStatusFilter, q);

        final int lastPage = resultPage.getTotalPages() - 1;
        if (page > lastPage) {
            final RedirectView redirectView = new RedirectView(
                    UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request))
                            .replaceQueryParam("page", lastPage)
                            .build()
                            .toUriString());
            redirectView.setExposeModelAttributes(false);
            return new ModelAndView(redirectView);
        }
        final List<VehicleCardView> listings = resultPage.getContent().stream()
                .map(card -> {
                    final String statusKey = listingService.getListingById(card.getListingId())
                            .map(l -> l.getStatus().name())
                            .orElse(null);
                    return new VehicleCardView(
                            card.getListingId(),
                            card.getBrand(),
                            card.getModel(),
                            card.getDayPrice(),
                            card.getImageId(),
                            statusKey);
                })
                .collect(Collectors.toList());

        final ModelAndView mav = new ModelAndView("myListings");
        mav.addObject("results", listings);
        mav.addObject("myListingsPage", resultPage);
        mav.addObject("activeTab", "my-listings");
        mav.addObject("ownerListingStatusFilter", listingStatusFilter != null ? listingStatusFilter : "");
        return mav;
    }

    private static String normalizeOwnerListingStatusParam(final String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        final String t = raw.trim().toLowerCase();
        return OWNER_LISTING_STATUS_WHITELIST.contains(t) ? t : null;
    }

    @GetMapping("/{listingId}")
    public ModelAndView listingDetail(
            @ModelAttribute(name = LoggedUserAdvice.CURRENT_USER_MODEL_KEY, binding = false) final User currentUser,
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
            @ModelAttribute(name = LoggedUserAdvice.CURRENT_USER_MODEL_KEY, binding = false) final User currentUser,
            @PathVariable("listingId") final long listingId,
            @Valid @ModelAttribute("editForm") final ListingEditForm editForm,
            final BindingResult errors) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final Optional<ListingDetail> listingDetailOpt = listingService.getListingDetailById(listingId);
        if (listingDetailOpt.isEmpty() || listingDetailOpt.get().getOwner().getId() != me.getId()) {
            return new ModelAndView(new RedirectView("/my-listings", true));
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
                null,
                editForm.getNeighborhoodId());
        return new ModelAndView(new RedirectView("/my-listings/" + listingId, true));
    }

    @PostMapping("/{listingId}/toggle")
    public ModelAndView toggleListing(
            @ModelAttribute(name = LoggedUserAdvice.CURRENT_USER_MODEL_KEY, binding = false) final User currentUser,
            @PathVariable("listingId") final long listingId) {
        final User me = WebAuthUtils.requireUser(currentUser);
        listingService.toggleListingStatus(me.getId(), listingId);
        return new ModelAndView(new RedirectView("/my-listings/" + listingId, true));
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

        final long carImageId = detail.getPictures().isEmpty() ? 0L : detail.getPictures().get(0).getImageId();
        final ModelAndView mav = new ModelAndView("myListingDetail");
        mav.addObject("allNeighborhoods", locationService.findAllNeighborhoods());
        mav.addObject("listing", listing);
        mav.addObject("listingCreatedAtDisplay", WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(listing.getCreatedAt(), LocaleContextHolder.getLocale()));
        mav.addObject("car", detail.getCar());
        mav.addObject("owner", detail.getOwner());
        mav.addObject("availabilities", detail.getListingAvailabilities());
        mav.addObject("carImageId", carImageId);
        mav.addObject("statusKey", listing.getStatus().name());
        mav.addObject("editForm", editForm);
        mav.addObject("activeTab", "my-listings");
        return mav;
    }

    private static VehicleCardView toVehicleCardView(final ListingCard card) {
        return new VehicleCardView(
                card.getListingId(),
                card.getBrand(),
                card.getModel(),
                card.getDayPrice(),
                card.getImageId());
    }
}
