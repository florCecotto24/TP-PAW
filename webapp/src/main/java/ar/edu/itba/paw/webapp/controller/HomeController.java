package ar.edu.itba.paw.webapp.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.webapp.config.properties.AppPaginationProperties;
import ar.edu.itba.paw.webapp.dto.VehicleCardView;
import ar.edu.itba.paw.webapp.support.ConsumerVehicleCardViewFactory;
import ar.edu.itba.paw.webapp.support.CurrentUser;

/** Home page: cheapest and most-recent car cards across the whole catalog (including the viewer's own). */
@Controller
public final class HomeController {

    private final CarService carService;
    private final AppPaginationProperties appPaginationProperties;
    private final ConsumerVehicleCardViewFactory consumerVehicleCardViewFactory;

    public HomeController(
            final CarService carService,
            final AppPaginationProperties appPaginationProperties,
            final ConsumerVehicleCardViewFactory consumerVehicleCardViewFactory) {
        this.carService = carService;
        this.appPaginationProperties = appPaginationProperties;
        this.consumerVehicleCardViewFactory = consumerVehicleCardViewFactory;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView home(
            @RequestParam(defaultValue = "0") int cheapestPage,
            @RequestParam(defaultValue = "0") int recentPage,
            @CurrentUser final User currentUser) {
        final ModelAndView mav = new ModelAndView("home");

        cheapestPage = Math.max(0, cheapestPage);
        recentPage   = Math.max(0, recentPage);

        final int uiPageSize = appPaginationProperties.getUiPageSize();
        final Page<CarCard> cheapestRaw =
                carService.getCheapestCarCards(cheapestPage, uiPageSize);
        final Page<CarCard> recentRaw =
                carService.getMostRecentCarCards(recentPage, uiPageSize);

        final Long viewerUserId = currentUser == null ? null : currentUser.getId();
        final Page<VehicleCardView> cheapestCarsPage = mapPage(cheapestRaw, viewerUserId);
        final Page<VehicleCardView> recentCarsPage   = mapPage(recentRaw, viewerUserId);

        mav.addObject("cheapestCarsPage", cheapestCarsPage);
        mav.addObject("recentCarsPage", recentCarsPage);
        mav.addObject("activeTab", "home");

        return mav;
    }

    private Page<VehicleCardView> mapPage(final Page<CarCard> source, final Long viewerUserId) {
        final List<VehicleCardView> views =
                consumerVehicleCardViewFactory.toConsumerVehicleCardViews(source.getContent(), viewerUserId);
        return new Page<>(views, source.getCurrentPage(), source.getPageSize(), source.getTotalItems());
    }
}
