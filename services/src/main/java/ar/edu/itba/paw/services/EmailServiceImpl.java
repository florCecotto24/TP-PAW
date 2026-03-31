package ar.edu.itba.paw.services;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

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

    private static final String RESERVATION_CONFIRMATION_TEMPLATE = "html/reservation-confirmation";

    private static final Locale MAIL_LOCALE = Locale.ENGLISH;

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
        try {
            sendReservationConfirmationEmailSync(payload);
            LOG.info("Reservation confirmation email sent to " + payload.getRecipientEmail()
                    + " (reservation id=" + payload.getReservationId() + ")");
        } catch (final Exception e) {
            LOG.error("Failed to send reservation confirmation email to " + payload.getRecipientEmail()
                    + " (reservation id=" + payload.getReservationId() + ")", e);
        }
    }

    private void sendReservationConfirmationEmailSync(final ReservationConfirmationPayload payload) throws EmailMessagingException {
        runMail(() -> {
            final DateTimeFormatter dateTimeFormatter =
                    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(MAIL_LOCALE);
            final Context ctx = new Context(MAIL_LOCALE);
            ctx.setVariable("riderFullName", payload.getRiderFullName());
            ctx.setVariable("reservationId", payload.getReservationId());
            ctx.setVariable("vehicleLabel", payload.getVehicleLabel());
            ctx.setVariable("startDateFormatted", dateTimeFormatter.format(payload.getStartDate()));
            ctx.setVariable("endDateFormatted", dateTimeFormatter.format(payload.getEndDate()));
            final String delivery = payload.getDeliveryLocation();
            final boolean hasDelivery = delivery != null && !delivery.isBlank();
            ctx.setVariable("hasDeliveryLocation", hasDelivery);
            ctx.setVariable("deliveryLocation", hasDelivery ? delivery : "");
            final String baseUrl = environment.getProperty("mail.app.public.base.url", "http://localhost:8080").replaceAll("/+$", "");
            final String ctaUrl = baseUrl + "/car-detail?listingId=" + payload.getListingId();
            ctx.setVariable("ctaUrl", ctaUrl);

            final String htmlContent = this.htmlTemplateEngine.process(RESERVATION_CONFIRMATION_TEMPLATE, ctx);
            final MimeMessage mimeMessage = this.mailSender.createMimeMessage();
            final MimeMessageHelper message = new MimeMessageHelper(mimeMessage, "UTF-8");
            final String subject = emailMessageSource.getMessage(
                    "mail.reservation.subject",
                    new Object[]{payload.getReservationId()},
                    MAIL_LOCALE);
            message.setSubject(subject);
            final String from = environment.getProperty("mail.from.address", environment.getProperty("mail.server.username", "noreply@localhost"));
            message.setFrom(from);
            message.setTo(payload.getRecipientEmail());
            message.setText(htmlContent, true);

            this.mailSender.send(mimeMessage);
        });
    }
}
