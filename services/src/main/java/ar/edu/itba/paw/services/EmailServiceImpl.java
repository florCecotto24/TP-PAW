package ar.edu.itba.paw.services;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.TimeZone;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.ReservationConfirmationPayload;

@Service
public class EmailServiceImpl implements EmailService {

    @FunctionalInterface
    private interface MailAction {
        void run() throws Exception;
    }

    private static void runMail(final MailAction action) throws EmailMessagingException {
        try {
            action.run();
        } catch (final EmailMessagingException e) {
            throw e;
        } catch (final MessagingException e) {
            throw new EmailMessagingException(e);
        } catch (final MailException e) {
            throw new EmailMessagingException(e);
        } catch (final Exception e) {
            throw new EmailMessagingException(e);
        }
    }

    private static final Log LOG = LogFactory.getLog(EmailServiceImpl.class);

    private static final String RESERVATION_CONFIRMATION_USER_TEMPLATE = "html/reservation-confirmation-rider";
    private static final String RESERVATION_CONFIRMATION_OWNER_TEMPLATE = "html/reservation-confirmation-owner";

    private static final Locale MAIL_LOCALE = Locale.ENGLISH;
    private static final DateTimeFormatter MAIL_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(MAIL_LOCALE);

    private static String formatWallDateTime(final java.time.OffsetDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        TimeZone timeZone = LocaleContextHolder.getTimeZone();
        ZoneId zoneId = timeZone.toZoneId();
        return MAIL_DATE_TIME_FORMATTER.format(
                dateTime.toInstant().atZone(zoneId).toLocalDateTime());
    }

    @Autowired
    private Environment environment;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    @Qualifier("emailMessageSource")
    private MessageSource emailMessageSource;

    @Autowired
    @Qualifier("emailTemplateEngine")
    private TemplateEngine htmlTemplateEngine;

    @Override
    @Async("mailTaskExecutor")
    public void sendReservationConfirmationEmail(final ReservationConfirmationPayload payload) {
        if (payload == null) {
            LOG.error("sendReservationConfirmationEmail called with null payload");
            return;
        }

        final Context ctx = new Context(MAIL_LOCALE);
        ctx.setVariable("riderFullName", payload.getRiderFullName());
        ctx.setVariable("reservationId", payload.getReservationId());
        ctx.setVariable("vehicleLabel", payload.getVehicleLabel());
        ctx.setVariable("startDateFormatted", formatWallDateTime(payload.getStartDate()));
        ctx.setVariable("endDateFormatted", formatWallDateTime(payload.getEndDate()));
        final String delivery = payload.getDeliveryLocation();
        final boolean hasDelivery = delivery != null && !delivery.isBlank();
        ctx.setVariable("hasDeliveryLocation", hasDelivery);
        ctx.setVariable("deliveryLocation", hasDelivery ? delivery : "");
        ctx.setVariable("ownerFullName", payload.getOwnerFullName());
        final String baseUrl = environment.getProperty("mail.app.public.base.url", "http://localhost:8080").replaceAll("/+$", "");
        final String ctaUrl = baseUrl + "/car-detail?listingId=" + payload.getListingId();
        ctx.setVariable("ctaUrl", ctaUrl);

        try {
            sendReservationConfirmationToClient(payload, ctx);
            sendReservationConfirmationToOwner(payload, ctx);
            LOG.info("Reservation confirmation email sent to " + payload.getRecipientEmail()
                    + " (reservation id=" + payload.getReservationId() + ")");
            LOG.info("Reservation confirmation email sent to " + payload.getOwnerEmail()
                    + " (reservation id=" + payload.getReservationId() + ")");
        } catch (final Exception e) {
            LOG.error("Failed to send reservation confirmation email (reservation id=" + payload.getReservationId() + ")", e);
        }
    }

    private void sendReservationConfirmationToClient(final ReservationConfirmationPayload payload, Context ctx) throws EmailMessagingException {
        runMail(() -> {

            ctx.setVariable("ownerEmail", payload.getOwnerEmail());
            final String htmlContent = this.htmlTemplateEngine.process(RESERVATION_CONFIRMATION_USER_TEMPLATE, ctx);

            final String subject = emailMessageSource.getMessage(
                    "mail.reservation.subject",
                    new Object[]{payload.getVehicleLabel()},
                    MAIL_LOCALE);
            sendEmail(payload.getRecipientEmail(), subject, htmlContent);
        });
    }

    private void sendReservationConfirmationToOwner(final ReservationConfirmationPayload payload, Context ctx) throws EmailMessagingException {
        runMail(() -> {
            ctx.setVariable("riderEmail", payload.getRecipientEmail());

            final String htmlContent = this.htmlTemplateEngine.process(RESERVATION_CONFIRMATION_OWNER_TEMPLATE, ctx);

            final String subject = emailMessageSource.getMessage(
                    "mail.reservation.subject",
                    new Object[]{payload.getVehicleLabel()},
                    MAIL_LOCALE);
            sendEmail(payload.getOwnerEmail(), subject, htmlContent);
        });
    }

    private void sendEmail(String to, String subject, String htmlBody) {
        final MimeMessage message = this.mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            final String from = environment.getProperty("mail.from.address", environment.getProperty("mail.server.username", "noreply@localhost"));
            message.setFrom(from);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            LOG.error("Error enviando mail a "+ to + ":" + e.getMessage());
        }
    }

}
