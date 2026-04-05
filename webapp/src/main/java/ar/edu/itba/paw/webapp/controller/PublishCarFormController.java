package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.dto.ImageUpload;
import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.RydenException;
import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.Car;
import ar.edu.itba.paw.services.CarService;
import ar.edu.itba.paw.webapp.form.PublishCarForm;
import ar.edu.itba.paw.webapp.support.LocaleMessages;
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
import java.util.ArrayList;
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

    private final CarService carService;
    private final LocaleMessages localeMessages;

    @Autowired
    public PublishCarFormController(final CarService carService, final LocaleMessages localeMessages) {
        this.carService = carService;
        this.localeMessages = localeMessages;
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
        if (errors.hasErrors()) {
            return index(form);
        }

        final List<ImageUpload> uploads;
        try {
            uploads = toImageUploads(form.getPictures());
        } catch (final IOException e) {
            errors.reject(
                    MessageKeys.PUBLISH_IMAGES_READ,
                    localeMessages.msg(MessageKeys.PUBLISH_IMAGES_READ));
            return index(form);
        }

        try {
            final List<AvailabilityPeriod> periods = form.toAvailabilityPeriods();
            final var result = carService.publish(
                    form.getOwnerEmail(),
                    form.getOwnerName(),
                    form.getOwnerSurname(),
                    form.getPlate(),
                    form.getBrand(),
                    form.getModel(),
                    form.getType(),
                    form.getPowertrain(),
                    form.getTransmission(),
                    form.getPricePerDay(),
                    form.getStartPoint(),
                    form.getDescription(),
                    periods,
                    uploads);

            final ModelAndView mav = new ModelAndView("publishCarConfirmation");
            mav.addObject("car", result.getCar());
            mav.addObject("listing", result.getListing());
            mav.addObject("publisher", result.getPublisher());
            return mav;
        } catch (final RydenException e) {
            final String detail = localeMessages.msg(e);
            errors.reject(
                    MessageKeys.PUBLISH_FAILED,
                    localeMessages.msg(MessageKeys.PUBLISH_FAILED, detail));
            return index(form);
        }
    }

    private static List<ImageUpload> toImageUploads(final MultipartFile[] pictures) throws IOException {
        final List<ImageUpload> out = new ArrayList<>();
        if (pictures == null) {
            return out;
        }
        for (final MultipartFile picture : pictures) {
            if (picture == null || picture.isEmpty()) {
                continue;
            }
            out.add(new ImageUpload(
                    picture.getOriginalFilename(),
                    picture.getContentType(),
                    picture.getBytes()));
        }
        return out;
    }
}
