package ar.edu.itba.paw.webapp.controller;

import java.net.URI;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import ar.edu.itba.paw.exception.car.FavoriteValidationException;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.pagination.UiPaging;
import ar.edu.itba.paw.services.FavCarService;
import ar.edu.itba.paw.services.policy.PaginationPolicy;
import ar.edu.itba.paw.webapp.util.LocaleMessages;
import ar.edu.itba.paw.webapp.dto.VehicleCardView;
import ar.edu.itba.paw.webapp.support.ConsumerVehicleCardViewFactory;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;

/**
 * "Mis favoritos" hub and favorite-toggle endpoint. All business rules (own-car check,
 * allowed-status filter, ordering) live in {@link FavCarService}; this controller only
 * maps request parameters and view data.
 */
@Controller
@RequestMapping("/my-favorites")
public final class MyFavoritesController {

    private final FavCarService favCarService;
    private final PaginationPolicy paginationPolicy;
    private final ConsumerVehicleCardViewFactory consumerVehicleCardViewFactory;
    private final LocaleMessages localeMessages;

    public MyFavoritesController(
            final FavCarService favCarService,
            final PaginationPolicy paginationPolicy,
            final ConsumerVehicleCardViewFactory consumerVehicleCardViewFactory,
            final LocaleMessages localeMessages) {
        this.favCarService = favCarService;
        this.paginationPolicy = paginationPolicy;
        this.consumerVehicleCardViewFactory = consumerVehicleCardViewFactory;
        this.localeMessages = localeMessages;
    }

    @GetMapping
    public ModelAndView myFavorites(
            @CurrentUser final User currentUser,
            @RequestParam(defaultValue = "0") int page) {
        final User me = WebAuthUtils.requireUser(currentUser);
        page = Math.max(0, page);
        final int pageSize = paginationPolicy.getDefaultPageSize();
        final Page<CarCard> favoritesPage = favCarService.findMyFavorites(me.getId(), page, pageSize);
        final int safePage = UiPaging.clampZeroBasedPage(page, favoritesPage.getTotalItems(), favoritesPage.getPageSize());
        if (safePage != page) {
            return new ModelAndView(new RedirectView("/my-favorites?page=" + safePage, true));
        }
        final List<VehicleCardView> cards =
                consumerVehicleCardViewFactory.toConsumerVehicleCardViews(
                        favoritesPage.getContent(), me.getId());
        final Page<VehicleCardView> resultsPage = new Page<>(
                cards,
                favoritesPage.getCurrentPage(),
                favoritesPage.getPageSize(),
                favoritesPage.getTotalItems());
        final ModelAndView mav = new ModelAndView("favorites/myFavorites");
        mav.addObject("favoritesPage", resultsPage);
        mav.addObject("activeTab", "my-favorites");
        return mav;
    }

    @PostMapping("/toggle")
    public ModelAndView toggleFavorite(
            @CurrentUser final User currentUser,
            @RequestParam("carId") final long carId,
            @RequestHeader(name = "Referer", required = false) final String referer,
            final RedirectAttributes redirectAttributes) {
        final User me = WebAuthUtils.requireUser(currentUser);
        try {
            favCarService.toggleFavorite(carId, me.getId());
        } catch (final FavoriteValidationException e) {
            redirectAttributes.addFlashAttribute("favoriteToggleErrorMessage", localeMessages.msg(e));
        }
        return new ModelAndView(new RedirectView(resolveRedirectTarget(referer), false));
    }

    /** Bounce the user back to where they came from; default to the favorites hub when unknown. */
    private static String resolveRedirectTarget(final String referer) {
        if (referer == null || referer.isBlank()) {
            return "/my-favorites";
        }
        try {
            return URI.create(referer).toString();
        } catch (final IllegalArgumentException e) {
            return "/my-favorites";
        }
    }
}
