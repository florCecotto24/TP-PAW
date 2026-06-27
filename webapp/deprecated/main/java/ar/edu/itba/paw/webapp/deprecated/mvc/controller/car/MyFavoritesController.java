package ar.edu.itba.paw.webapp.deprecated.mvc.controller.car;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Min;

import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import ar.edu.itba.paw.exception.car.FavoriteValidationException;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.models.pagination.UiPaging;
import ar.edu.itba.paw.services.car.FavCarService;
import ar.edu.itba.paw.webapp.config.properties.AppPaginationProperties;
import ar.edu.itba.paw.webapp.dto.VehicleCardView;
import ar.edu.itba.paw.webapp.support.ConsumerVehicleCardViewFactory;
import ar.edu.itba.paw.webapp.deprecated.mvc.support.CurrentUser;
import ar.edu.itba.paw.webapp.support.SafeRefererResolver;
import ar.edu.itba.paw.webapp.util.LocaleMessages;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;

/**
 * "Mis favoritos" hub and favorite-toggle endpoint. All business rules (own-car check,
 * allowed-status filter, ordering) live in {@link FavCarService}; this controller only
 * maps request parameters and view data.
 */
@Controller
@RequestMapping("/my-favorites")
@Validated
public class MyFavoritesController {

    private final FavCarService favCarService;
    private final AppPaginationProperties appPaginationProperties;
    private final ConsumerVehicleCardViewFactory consumerVehicleCardViewFactory;
    private final LocaleMessages localeMessages;

    public MyFavoritesController(
            final FavCarService favCarService,
            final AppPaginationProperties appPaginationProperties,
            final ConsumerVehicleCardViewFactory consumerVehicleCardViewFactory,
            final LocaleMessages localeMessages) {
        this.favCarService = favCarService;
        this.appPaginationProperties = appPaginationProperties;
        this.consumerVehicleCardViewFactory = consumerVehicleCardViewFactory;
        this.localeMessages = localeMessages;
    }

    @GetMapping
    public ModelAndView myFavorites(
            @CurrentUser final User currentUser,
            @RequestParam(defaultValue = "0") @Min(0) final int page) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final int pageSize = appPaginationProperties.getDefaultPageSize();
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
            final HttpServletRequest request,
            final RedirectAttributes redirectAttributes) {
        final User me = WebAuthUtils.requireUser(currentUser);
        try {
            favCarService.toggleFavorite(carId, me.getId());
        } catch (final FavoriteValidationException e) {
            redirectAttributes.addFlashAttribute("favoriteToggleErrorMessage", localeMessages.msg(e));
        }
        // Same-app validated; defaults to the favorites hub when the Referer is missing or unsafe.
        final String target = SafeRefererResolver.sameAppRelativePathOrDefault(request, referer, "/my-favorites");
        return new ModelAndView(new RedirectView(target, true));
    }
}
