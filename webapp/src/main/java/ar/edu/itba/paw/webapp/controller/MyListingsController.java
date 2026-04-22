package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.ListingCard;
import ar.edu.itba.paw.models.ListingDetail;
import ar.edu.itba.paw.models.Page;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.webapp.form.ListingEditForm;
import ar.edu.itba.paw.webapp.dto.VehicleCardView;
import ar.edu.itba.paw.webapp.security.RydenUserDetails;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.validation.Valid;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import ar.edu.itba.paw.models.ListingAvailability;
import ar.edu.itba.paw.models.WallDateTimeDisplayFormat;
import org.springframework.context.i18n.LocaleContextHolder;

@Controller
@RequestMapping("/my-listings")
public class MyListingsController {

    private static final int PAGE_SIZE = 8;

    private final ListingService listingService;

    @Autowired
    public MyListingsController(final ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping
    public ModelAndView myListings(
            final Authentication authentication,
            @RequestParam(defaultValue = "0") int page) {
        final RydenUserDetails details = WebAuthUtils.requireCurrentUser(authentication);
        page = Math.max(0, page);

        final Page<ListingCard> resultPage = listingService.getOwnerListingCards(details.getUserId(), page, PAGE_SIZE);
        final List<VehicleCardView> listings = resultPage.getContent().stream()
                .map(MyListingsController::toVehicleCardView)
                .collect(Collectors.toList());

        final ModelAndView mav = new ModelAndView("myListings");
        mav.addObject("results", listings);
        mav.addObject("myListingsPage", resultPage);
        mav.addObject("activeTab", "my-listings");
        return mav;
    }

    @GetMapping("/{listingId}")
    public ModelAndView listingDetail(
            final Authentication authentication,
            @PathVariable("listingId") final long listingId) {
        final RydenUserDetails details = WebAuthUtils.requireCurrentUser(authentication);
        final Optional<ListingDetail> listingDetailOpt = listingService.getListingDetailById(listingId);
        if (listingDetailOpt.isEmpty() || listingDetailOpt.get().getOwner().getId() != details.getUserId()) {
            return new ModelAndView(new RedirectView("/my-listings", true));
        }

        return buildDetailModelAndView(listingDetailOpt.get(), new ListingEditForm());
    }

    @PostMapping("/{listingId}/edit")
    public ModelAndView editListing(
            final Authentication authentication,
            @PathVariable("listingId") final long listingId,
            @Valid @ModelAttribute("editForm") final ListingEditForm editForm,
            final BindingResult errors) {
        final RydenUserDetails details = WebAuthUtils.requireCurrentUser(authentication);
        final Optional<ListingDetail> listingDetailOpt = listingService.getListingDetailById(listingId);
        if (listingDetailOpt.isEmpty() || listingDetailOpt.get().getOwner().getId() != details.getUserId()) {
            return new ModelAndView(new RedirectView("/my-listings", true));
        }
        if (errors.hasErrors()) {
            return buildDetailModelAndView(listingDetailOpt.get(), editForm);
        }

        listingService.updateOwnerListing(
                details.getUserId(),
                listingId,
                editForm.getPricePerDay(),
                editForm.getStartPoint(),
                editForm.getDescription(),
                editForm.getCheckInTime(),
                editForm.getCheckOutTime(),
                editForm.toAvailabilityPeriods());
        return new ModelAndView(new RedirectView("/my-listings/" + listingId, true));
    }

    @PostMapping("/{listingId}/delete")
    public ModelAndView deleteListing(
            final Authentication authentication,
            @PathVariable("listingId") final long listingId) {
        final RydenUserDetails details = WebAuthUtils.requireCurrentUser(authentication);
        listingService.toggleListingStatus(details.getUserId(), listingId);
        return new ModelAndView(new RedirectView("/my-listings", true));
    }

    private ModelAndView buildDetailModelAndView(final ListingDetail detail, final ListingEditForm editForm) {
        final Listing listing = detail.getListing();
        if (editForm.getPricePerDay() == null) {
            editForm.setPricePerDay(listing.getDayPrice());
        }
        if (editForm.getStartPoint() == null) {
            editForm.setStartPoint(listing.getStartPoint());
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
        if (editForm.getAvailabilityRows().isEmpty() || (editForm.getAvailabilityRows().size() == 1 && editForm.getAvailabilityRows().get(0).getFrom() == null)) {
            final List<ListingEditForm.AvailabilityRow> rows = new ArrayList<>();
            for (final ListingAvailability la : detail.getListingAvailabilities()) {
                final ListingEditForm.AvailabilityRow row = new ListingEditForm.AvailabilityRow();
                row.setFrom(la.getStartInclusive());
                row.setUntil(la.getEndInclusive());
                rows.add(row);
            }
            if (!rows.isEmpty()) {
                editForm.setAvailabilityRows(rows);
            }
        }

        final long carImageId = detail.getPictures().isEmpty() ? 0L : detail.getPictures().get(0).getImageId();
        final ModelAndView mav = new ModelAndView("myListingDetail");
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
