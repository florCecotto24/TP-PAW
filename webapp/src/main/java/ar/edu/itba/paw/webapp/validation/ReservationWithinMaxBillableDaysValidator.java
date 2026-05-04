package ar.edu.itba.paw.webapp.validation;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.services.ReservationService;
import ar.edu.itba.paw.webapp.form.ReservationForm;
import ar.edu.itba.paw.webapp.validation.constraint.ReservationWithinMaxBillableDays;

/** Bean Validation engine for {@link ReservationWithinMaxBillableDays} using {@link ReservationService} billable-day rules. */
@Component
public final class ReservationWithinMaxBillableDaysValidator
        implements ConstraintValidator<ReservationWithinMaxBillableDays, ReservationForm> {

    private static final Logger LOG = LoggerFactory.getLogger(ReservationWithinMaxBillableDaysValidator.class);

    private final MessageSource messageSource;
    private final ReservationService reservationService;

    public ReservationWithinMaxBillableDaysValidator(
            final MessageSource messageSource,
            final ReservationService reservationService) {
        this.messageSource = messageSource;
        this.reservationService = reservationService;
    }

    @Override
    public boolean isValid(final ReservationForm form, final ConstraintValidatorContext context) {
        if (form == null || form.getFromDateTime() == null || form.getUntilDateTime() == null) {
            return true;
        }
        if (form.getFromDateTime().isBlank() || form.getUntilDateTime().isBlank()) {
            return true;
        }
        final OffsetDateTime start;
        final OffsetDateTime end;
        try {
            start = AvailabilityPeriod.parseWallLocalDateTimeToUtc(form.getFromDateTime());
            end = AvailabilityPeriod.parseWallLocalDateTimeToUtc(form.getUntilDateTime());
        } catch (final DateTimeParseException e) {
            LOG.atDebug()
                    .setMessage("Reservation max-billable-days check skipped: unparseable wall datetimes from=[{}] until=[{}]")
                    .addArgument(form.getFromDateTime())
                    .addArgument(form.getUntilDateTime())
                    .setCause(e)
                    .log();
            return true;
        }
        if (!end.isAfter(start)) {
            return true;
        }
        final long billable = reservationService.calculateBillableDays(start, end);
        final int max = reservationService.getConfiguredMaxReservationBillableDays();
        if (billable <= max) {
            return true;
        }
        final Locale locale = LocaleContextHolder.getLocale();
        final String msg = messageSource.getMessage("validation.reservationForm.maxBillableDays", new Object[] { max }, locale);
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(msg)
                .addPropertyNode("fromDateTime")
                .addConstraintViolation();
        return false;
    }
}
