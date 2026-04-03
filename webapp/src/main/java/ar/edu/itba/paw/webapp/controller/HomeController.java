package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.CarPicture;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.services.CarPictureService;
import ar.edu.itba.paw.services.CarService;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.webapp.dto.VehicleCardView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final CarService carService;
    private final ListingService listingService;
    private final CarPictureService carPictureService;

    @Autowired
    public HomeController(final CarService carService, final ListingService listingService, final CarPictureService carPictureService) {
        this.carService = carService;
        this.listingService = listingService;
        this.carPictureService = carPictureService;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView home(){
        final ModelAndView mav = new ModelAndView("home");

        final List<VehicleCardView> cheapestCars = listingService.getCheapestListings(8).stream()
                .map(listing -> toSearchResult(listing).orElse(null))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        final List<VehicleCardView> mostRecentCars = listingService.getMostRecentListings(8).stream()
                .map(listing -> toSearchResult(listing).orElse(null))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        mav.addObject("cheapestCars", cheapestCars);
        mav.addObject("mostRecentCars", mostRecentCars);

        return mav;
    }

    private Optional<VehicleCardView> toSearchResult(final Listing listing) {
        return carService.getCarById(listing.getCarId())
                .map(car -> {
                    // Get the first image for this car (if any)
                    final long imageId = carPictureService.getCarPicturesByCarId(car.getId())
                            .stream()
                            .findFirst()
                            .map(CarPicture::getImageId)
                            .orElse(0L);
                    
                    return new VehicleCardView(
                            listing.getId(),
                            car.getBrand(),
                            car.getModel(),
                            listing.getDayPrice(),
                            imageId
                    );
                });
    }

}
