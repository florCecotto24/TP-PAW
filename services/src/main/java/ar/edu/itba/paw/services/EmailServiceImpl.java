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
    private static final String EMAIL_VERIFICATION_TEMPLATE = "html/email-verification-code";
    private static final String MIGRATED_PASSWORD_TEMPLATE = "html/migrated-password";
    private static final String PASSWORD_RESET_TEMPLATE = "html/password-reset-code";

    private static String formatWallDateTime(final java.time.OffsetDateTime dateTime, final Locale messageLocale) {
        if (dateTime == null) {
            return "";
        }
        final Locale locale = messageLocale != null ? messageLocale : Locale.ENGLISH;
        final DateTimeFormatter formatter =
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale);
        final TimeZone timeZone = LocaleContextHolder.getTimeZone();
        final ZoneId zoneId = timeZone.toZoneId();
        return formatter.format(dateTime.toInstant().atZone(zoneId).toLocalDateTime());
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

        final Locale mailLocale = payload.getMessageLocale();
        final Context ctx = new Context(mailLocale);
        ctx.setVariable("riderFullName", payload.getRiderFullName());
        ctx.setVariable("reservationId", payload.getReservationId());
        ctx.setVariable("vehicleLabel", payload.getVehicleLabel());
        ctx.setVariable("startDateFormatted", formatWallDateTime(payload.getStartDate(), mailLocale));
        ctx.setVariable("endDateFormatted", formatWallDateTime(payload.getEndDate(), mailLocale));
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

    @Override
    @Async("mailTaskExecutor")
    public void sendEmailVerificationCode(final String to, final String code, final Locale locale) {
        if (to == null || to.isBlank() || code == null) {
            LOG.error("sendEmailVerificationCode: missing to or code");
            return;
        }
        final Locale mailLocale = locale != null ? locale : Locale.ENGLISH;
        final Context ctx = new Context(mailLocale);
        ctx.setVariable("code", code);
        final String baseUrl = environment.getProperty("mail.app.public.base.url", "http://localhost:8080").replaceAll("/+$", "");
        final String contextPath = environment.getProperty("mail.app.context.path", "").replaceAll("/+$", "");
        ctx.setVariable("verifyUrl", baseUrl + contextPath + "/verify-email");

        try {
            runMail(() -> {
                final String htmlContent = this.htmlTemplateEngine.process(EMAIL_VERIFICATION_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage("mail.emailVerification.subject", null, mailLocale);
                sendEmail(to, subject, htmlContent);
            });
            LOG.info("Email verification code sent to " + to);
        } catch (final Exception e) {
            LOG.error("Failed to send email verification code to " + to, e);
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendMigratedUserPassword(final String to, final String plainPassword, final Locale locale) {
        if (to == null || to.isBlank() || plainPassword == null || plainPassword.isBlank()) {
            LOG.error("sendMigratedUserPassword: missing to or password");
            return;
        }
        final Locale mailLocale = locale != null ? locale : Locale.ENGLISH;
        final Context ctx = new Context(mailLocale);
        ctx.setVariable("plainPassword", plainPassword);
        try {
            runMail(() -> {
                final String htmlContent = this.htmlTemplateEngine.process(MIGRATED_PASSWORD_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage("mail.migratedPassword.subject", null, mailLocale);
                sendEmail(to, subject, htmlContent);
            });
            LOG.info("Migrated user password email sent to " + to);
        } catch (final Exception e) {
            LOG.error("Failed to send migrated password email to " + to, e);
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendPasswordResetCode(final String to, final String code, final Locale locale) {
        if (to == null || to.isBlank() || code == null) {
            LOG.error("sendPasswordResetCode: missing to or code");
            return;
        }
        final Locale mailLocale = locale != null ? locale : Locale.ENGLISH;
        final Context ctx = new Context(mailLocale);
        ctx.setVariable("code", code);
        final String baseUrl = environment.getProperty("mail.app.public.base.url", "http://localhost:8080").replaceAll("/+$", "");
        final String contextPath = environment.getProperty("mail.app.context.path", "").replaceAll("/+$", "");
        ctx.setVariable("resetUrl", baseUrl + contextPath + "/forgot-password/reset");
        try {
            runMail(() -> {
                final String htmlContent = this.htmlTemplateEngine.process(PASSWORD_RESET_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage("mail.passwordReset.subject", null, mailLocale);
                sendEmail(to, subject, htmlContent);
            });
            LOG.info("Password reset code sent to " + to);
        } catch (final Exception e) {
            LOG.error("Failed to send password reset code to " + to, e);
        }
    }

    private void sendReservationConfirmationToClient(final ReservationConfirmationPayload payload, Context ctx) throws EmailMessagingException {
        runMail(() -> {

            ctx.setVariable("ownerEmail", payload.getOwnerEmail());
            final String htmlContent = this.htmlTemplateEngine.process(RESERVATION_CONFIRMATION_USER_TEMPLATE, ctx);

            final String subject = emailMessageSource.getMessage(
                    "mail.reservationConfirmation.subject",
                    new Object[]{payload.getVehicleLabel()},
                    payload.getMessageLocale());
            sendEmail(payload.getRecipientEmail(), subject, htmlContent);
        });
    }

    private void sendReservationConfirmationToOwner(final ReservationConfirmationPayload payload, Context ctx) throws EmailMessagingException {
        runMail(() -> {
            ctx.setVariable("riderEmail", payload.getRecipientEmail());

            final String htmlContent = this.htmlTemplateEngine.process(RESERVATION_CONFIRMATION_OWNER_TEMPLATE, ctx);

            final String subject = emailMessageSource.getMessage(
                    "mail.reservationConfirmation.subject",
                    new Object[]{payload.getVehicleLabel()},
                    payload.getMessageLocale());
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
