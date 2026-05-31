package ar.edu.itba.paw.webapp.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ar.edu.itba.paw.models.dto.CarCard;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.services.CarService;
import ar.edu.itba.paw.services.policy.PaginationPolicy;
import ar.edu.itba.paw.webapp.dto.VehicleCardView;
import ar.edu.itba.paw.webapp.support.ConsumerVehicleCardViewFactory;

/** Home page: cheapest and most-recent car cards across the whole catalog (including the viewer's own). */
@Controller
public final class HomeController {

    private final CarService carService;
    private final PaginationPolicy paginationPolicy;
    private final ConsumerVehicleCardViewFactory consumerVehicleCardViewFactory;

    public HomeController(
            final CarService carService,
            final PaginationPolicy paginationPolicy,
            final ConsumerVehicleCardViewFactory consumerVehicleCardViewFactory) {
        this.carService = carService;
        this.paginationPolicy = paginationPolicy;
        this.consumerVehicleCardViewFactory = consumerVehicleCardViewFactory;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView home(
            @RequestParam(defaultValue = "0") int cheapestPage,
            @RequestParam(defaultValue = "0") int recentPage) {
        final ModelAndView mav = new ModelAndView("home");

        cheapestPage = Math.max(0, cheapestPage);
        recentPage   = Math.max(0, recentPage);

        final int uiPageSize = paginationPolicy.getUiPageSize();
        final Page<CarCard> cheapestRaw =
                carService.getCheapestCarCards(cheapestPage, uiPageSize);
        final Page<CarCard> recentRaw =
                carService.getMostRecentCarCards(recentPage, uiPageSize);

        final Page<VehicleCardView> cheapestCarsPage = mapPage(cheapestRaw);
        final Page<VehicleCardView> recentCarsPage   = mapPage(recentRaw);

        mav.addObject("cheapestCarsPage", cheapestCarsPage);
        mav.addObject("recentCarsPage", recentCarsPage);
        mav.addObject("activeTab", "home");

        return mav;
    }

    private Page<VehicleCardView> mapPage(final Page<CarCard> source) {
        final List<VehicleCardView> views =
                consumerVehicleCardViewFactory.toConsumerVehicleCardViews(source.getContent());
        return new Page<>(views, source.getCurrentPage(), source.getPageSize(), source.getTotalItems());
    }
}
