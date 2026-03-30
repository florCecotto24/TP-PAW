package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.Car;
import ar.edu.itba.paw.models.CarPicture;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.ListingAvailability;
import ar.edu.itba.paw.services.CarPictureService;
import ar.edu.itba.paw.services.CarService;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.webapp.dto.ReservationPeriodOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class CarDetailController {

    private static final DateTimeFormatter DT_LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final DateTimeFormatter LABEL_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ListingService listingService;
    private final CarService carService;
    private final CarPictureService carPictureService;

    @Autowired
    public CarDetailController(
            final ListingService listingService,
            final CarService carService,
            final CarPictureService carPictureService) {
        this.listingService = listingService;
        this.carService = carService;
        this.carPictureService = carPictureService;
    }

    @RequestMapping(value = "/car-detail", method = RequestMethod.GET)
    public ModelAndView carDetail(@RequestParam(name = "listingId") final long listingId) {
        final Optional<Listing> listingOpt = listingService.getListingById(listingId);
        if (listingOpt.isEmpty()) {
            return new ModelAndView(new RedirectView("/search", true));
        }
        final Listing listing = listingOpt.get();
        final Car car = carService.getCarById(listing.getCarId()).orElse(null);
        if (car == null) {
            return new ModelAndView(new RedirectView("/search", true));
        }

        final List<String> carGalleryImagePaths = carPictureService.getCarPicturesByCarId(car.getId()).stream()
                .sorted(Comparator.comparingInt(CarPicture::getDisplayOrder))
                .map(cp -> "/image/" + cp.getImageId())
                .collect(Collectors.toList());

        final List<ListingAvailability> listingAvailabilities = listingService.findAvailabilityByListingId(listingId);
        final List<String> availabilityLines = new ArrayList<>(listingAvailabilities.size());
        final List<ReservationPeriodOption> reservationPeriods = new ArrayList<>(listingAvailabilities.size());
        for (final ListingAvailability a : listingAvailabilities) {
            availabilityLines.add(formatAvailabilityLine(a));
            reservationPeriods.add(toReservationPeriodOption(a));
        }
        String reservationFromDefault = "";
        String reservationUntilDefault = "";
        if (reservationPeriods.size() == 1) {
            final ReservationPeriodOption only = reservationPeriods.get(0);
            reservationFromDefault = only.getMinDateTimeLocal();
            reservationUntilDefault = only.getMaxDateTimeLocal();
        }

        final ModelAndView mav = new ModelAndView("carDetail");
        mav.addObject("listing", listing);
        mav.addObject("car", car);
        mav.addObject("carGalleryImagePaths", carGalleryImagePaths);
        mav.addObject("listingAvailabilities", listingAvailabilities);
        mav.addObject("availabilityLines", availabilityLines);
        mav.addObject("reservationPeriods", reservationPeriods);
        mav.addObject("reservationFromDefault", reservationFromDefault);
        mav.addObject("reservationUntilDefault", reservationUntilDefault);
        return mav;
    }

    private static ReservationPeriodOption toReservationPeriodOption(final ListingAvailability a) {
        final ZonedDateTime startZ = a.getStartDate().atZoneSameInstant(AvailabilityPeriod.WALL_ZONE);
        ZonedDateTime lastInclusiveZ = a.getEndDate().minusNanos(1).atZoneSameInstant(AvailabilityPeriod.WALL_ZONE);
        if (lastInclusiveZ.isBefore(startZ)) {
            lastInclusiveZ = startZ;
        }
        final String minLocal = startZ.format(DT_LOCAL);
        final String maxLocal = lastInclusiveZ.format(DT_LOCAL);
        final String label = LABEL_FMT.format(startZ) + " — " + LABEL_FMT.format(lastInclusiveZ);
        return new ReservationPeriodOption(a.getId(), label, minLocal, maxLocal);
    }

    private static String formatAvailabilityLine(final ListingAvailability a) {
        final ZonedDateTime startZ = a.getStartDate().atZoneSameInstant(AvailabilityPeriod.WALL_ZONE);
        ZonedDateTime lastInclusiveZ = a.getEndDate().minusNanos(1).atZoneSameInstant(AvailabilityPeriod.WALL_ZONE);
        if (lastInclusiveZ.isBefore(startZ)) {
            lastInclusiveZ = startZ;
        }
        return LABEL_FMT.format(startZ) + " — " + LABEL_FMT.format(lastInclusiveZ);
    }
}
