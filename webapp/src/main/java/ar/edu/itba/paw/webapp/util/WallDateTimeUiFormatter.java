package ar.edu.itba.paw.webapp.util;

import java.util.Locale;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

import ar.edu.itba.paw.models.WallDateTimeDisplayFormat;
import ar.edu.itba.paw.webapp.form.ReservationForm;

@Component
public class WallDateTimeUiFormatter {

    public String formatReservationDateTimeInput(final String raw) {
        final Locale locale = LocaleContextHolder.getLocale();
        return WallDateTimeDisplayFormat.formatClientWallDateTimeInputOrRaw(raw, locale);
    }

    public void addReservationFormDateDisplays(final ModelAndView mav, final ReservationForm form) {
        if (form == null) {
            return;
        }
        mav.addObject("fromDateTimeDisplay", formatReservationDateTimeInput(form.getFromDateTime()));
        mav.addObject("untilDateTimeDisplay", formatReservationDateTimeInput(form.getUntilDateTime()));
    }
}
