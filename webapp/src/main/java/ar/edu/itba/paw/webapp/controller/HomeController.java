package ar.edu.itba.paw.webapp.controller;

import javax.validation.constraints.Min;

import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
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
@Validated
public class HomeController {

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
            @RequestParam(defaultValue = "0") @Min(0) final int cheapestPage,
            @RequestParam(defaultValue = "0") @Min(0) final int recentPage,
            @CurrentUser final User currentUser) {
        final int uiPageSize = appPaginationProperties.getUiPageSize();
        final Long viewerUserId = currentUser == null ? null : currentUser.getId();

        final Page<VehicleCardView> cheapestCarsPage = mapPage(
                carService.getCheapestCarCards(cheapestPage, uiPageSize), viewerUserId);
        final Page<VehicleCardView> recentCarsPage = mapPage(
                carService.getMostRecentCarCards(recentPage, uiPageSize), viewerUserId);

        final ModelAndView mav = new ModelAndView("home");
        mav.addObject("cheapestCarsPage", cheapestCarsPage);
        mav.addObject("recentCarsPage", recentCarsPage);
        mav.addObject("activeTab", "home");
        return mav;
    }

    private Page<VehicleCardView> mapPage(final Page<CarCard> source, final Long viewerUserId) {
        return new Page<>(
                consumerVehicleCardViewFactory.toConsumerVehicleCardViews(source.getContent(), viewerUserId),
                source.getCurrentPage(),
                source.getPageSize(),
                source.getTotalItems());
    }
}
