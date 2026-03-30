package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.Car;
import ar.edu.itba.paw.models.Image;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.CarPictureService;
import ar.edu.itba.paw.services.CarService;
import ar.edu.itba.paw.services.ImageService;
import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.PublishCarForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/publish-car")
public class PublishCarFormController {

    @ModelAttribute("carTypes")
    public Car.Type[] carTypes() {
        return Car.Type.values();
    }

    @ModelAttribute("powertrains")
    public Car.Powertrain[] powertrains() {
        return Car.Powertrain.values();
    }

    @ModelAttribute("transmissions")
    public Car.Transmission[] transmissions() {
        return Car.Transmission.values();
    }

    private final ListingService listingService;
    private final CarService carService;
    private final ImageService imageService;
    private final CarPictureService carPictureService;
    private final UserService userService;

    @Autowired
    public PublishCarFormController(
            final ListingService listingService,
            final CarService carService,
            final ImageService imageService,
            final CarPictureService carPictureService,
            final UserService userService) {
        this.listingService = listingService;
        this.carService = carService;
        this.imageService = imageService;
        this.carPictureService = carPictureService;
        this.userService = userService;
    }

    @GetMapping
    public ModelAndView index(@ModelAttribute("publishCarForm") final PublishCarForm form) {
        final ModelAndView mav = new ModelAndView("publishCarForm");
        mav.addObject("activeTab", "publish-car");
        return mav;
    }

    @PostMapping
    public ModelAndView formSubmit(
            @Valid @ModelAttribute("publishCarForm") final PublishCarForm form,
            final BindingResult errors) {
        final List<AvailabilityPeriod> periods = form.toAvailabilityPeriods();
        if (periods.isEmpty()) {
            errors.reject("availability.required", "Add at least one availability period.");
        }
        for (final AvailabilityPeriod period : periods) {
            if (!period.isValidOrder()) {
                errors.reject("availability.order", "In each period, the end date must be equal or after the start date.");
                break;
            }
        }

        if (errors.hasErrors()) {
            return index(form);
        }

        final User publisher = userService.findOrCreatePublisher(form.getOwnerEmail(), form.getOwnerName(), form.getOwnerSurname());
        final Car car = carService.createCar(
                publisher.getId(),
                form.getPlate(),
                form.getBrand(),
                form.getModel(),
                form.getType(),
                form.getPowertrain(),
                form.getTransmission());
        final Listing listing = listingService.createListing(
                car.getId(),
                Listing.Status.ACTIVE,
                form.getPricePerDay(),
                form.getStartPoint(),
                form.getDescription(),
                periods);

        processPictures(car.getId(), form.getPictures());

        ModelAndView mav = new ModelAndView("publishCarConfirmation");
        mav.addObject("car", car);
        mav.addObject("listing", listing);
        mav.addObject("publisher", publisher);

        System.out.println(listing);
        return mav;
    }

    private void processPictures(final long carId, final MultipartFile[] pictures) {
        if (pictures == null || pictures.length == 0) {
            return;
        }

        int displayOrder = 1;
        for (final MultipartFile picture : pictures) {
            if (picture.isEmpty()) {
                continue;
            }

            try {
                final String filename = picture.getOriginalFilename();
                final String contentType = picture.getContentType();
                final byte[] data = picture.getBytes();
                if (data == null || data.length == 0) {
                    continue;
                }

                final Image image = imageService.createImage(filename, contentType, data);
                carPictureService.createCarPicture(carId, image.getId(), displayOrder);

                displayOrder++;
            } catch (final IOException e) {
                System.err.println("Error processing image: " + e.getMessage());
            }
        }
    }
}