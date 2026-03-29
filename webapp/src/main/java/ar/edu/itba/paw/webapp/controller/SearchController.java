package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.CarPicture;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.services.CarPictureService;
import ar.edu.itba.paw.services.CarService;
import ar.edu.itba.paw.services.ListingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
public class SearchController {

    private final CarService carService;
    private final ListingService listingService;
    private final CarPictureService carPictureService;

    @Autowired
    public SearchController(final CarService carService, final ListingService listingService, final CarPictureService carPictureService) {
        this.carService = carService;
        this.listingService = listingService;
        this.carPictureService = carPictureService;
    }

    //    Esto está un toqusín mal, pero es para probar los botones... @Flor
    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public ModelAndView search(@RequestParam(required = false) String query,
                         @RequestParam(required = false) List<String> category,
                         @RequestParam(required = false) List<String> price) {
        final ModelAndView mav = new ModelAndView("search");

        // pass filter options to JSP
        mav.addObject("categories", List.of("Manual", "Hybrid", "Automatic"));
        mav.addObject("prices", List.of("Free", "Paid"));
        mav.addObject("ratings", List.of("1", "2", "3", "4", "5"));

        final List<SearchResultView> results = listingService.getAllListings().stream()
                .map(listing -> toSearchResult(listing).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        mav.addObject("results", results);
        mav.addObject("activeTab", "search");

        return mav;
    }

    //Esto funciona. No sé si es la mejor manera de hacerlo... Veremos en clase qué onda.
    private java.util.Optional<SearchResultView> toSearchResult(final Listing listing) {
        return carService.getCarById(listing.getCarId())
                .map(car -> {
                    // Get the first image for this car (if any)
                    final long imageId = carPictureService.getCarPicturesByCarId(car.getId())
                            .stream()
                            .findFirst()
                            .map(CarPicture::getImageId)
                            .orElse(0L);
                    
                    return new SearchResultView(
                            listing.getId(),
                            car.getBrand(),
                            car.getModel(),
                            listing.getDayPrice(),
                            imageId
                    );
                });
    }

    public static class SearchResultView {
        private final long listingId;
        private final String brand;
        private final String model;
        private final BigDecimal price;
        private final long imageId;

        public SearchResultView(final long listingId, final String brand, final String model, final BigDecimal price, final long imageId) {
            this.listingId = listingId;
            this.brand = brand;
            this.model = model;
            this.price = price;
            this.imageId = imageId;
        }

        public long getListingId() {
            return listingId;
        }

        public String getBrand() {
            return brand;
        }

        public String getModel() {
            return model;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public long getImageId() {
            return imageId;
        }
    }
}

