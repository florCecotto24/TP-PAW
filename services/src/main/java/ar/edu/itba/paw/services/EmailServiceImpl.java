package ar.edu.itba.paw.services;

import java.util.Locale;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import ar.edu.itba.paw.models.ReservationConfirmationPayload;
import ar.edu.itba.paw.models.WallDateTimeDisplayFormat;

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
        } catch (final Exception e) {
            throw new EmailMessagingException(e);
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailServiceImpl.class);


    private static final String RESERVATION_CONFIRMATION_USER_TEMPLATE = "html/reservation-confirmation-rider";
    private static final String RESERVATION_CONFIRMATION_OWNER_TEMPLATE = "html/reservation-confirmation-owner";
    private static final String RESERVATION_CANCELLATION_USER_TEMPLATE = "html/reservation-cancellation-rider";
    private static final String RESERVATION_CANCELLATION_OWNER_TEMPLATE = "html/reservation-cancellation-owner";
    private static final String EMAIL_VERIFICATION_TEMPLATE = "html/email-verification-code";
    private static final String MIGRATED_PASSWORD_TEMPLATE = "html/migrated-password";
    private static final String PASSWORD_RESET_TEMPLATE = "html/password-reset-code";
    private static final String RESERVATION_REMINDER_TEMPLATE = "html/reservation-reminder-rider";

    private static String formatWallDateTime(final java.time.OffsetDateTime dateTime, final Locale messageLocale) {
        final Locale locale = messageLocale != null ? messageLocale : Locale.ENGLISH;
        return WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(dateTime, locale);
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
            LOGGER.atError().log("sendReservationConfirmationEmail called with null payload");
            return;
        }

        final Locale mailLocale = payload.getMessageLocale();
        final Context ctx = new Context(mailLocale);
        ctx.setVariable("reservationTotal", payload.getReservationTotal());
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
        final String ctaUrlRider = baseUrl + "/my-reservations/" + payload.getReservationId();
        ctx.setVariable("ctaUrlRider", ctaUrlRider);
        final String ctaUrlOwner = baseUrl + "/my-listings/" + payload.getListingId();
        ctx.setVariable("ctaUrlOwner", ctaUrlOwner);

        try {
            sendReservationConfirmationToClient(payload, ctx);
            sendReservationConfirmationToOwner(payload, ctx);
            LOGGER.atInfo().log("Reservation confirmation email sent to " + payload.getRecipientEmail()
                    + " (reservation id=" + payload.getReservationId() + ")");
            LOGGER.atInfo().log("Reservation confirmation email sent to " + payload.getOwnerEmail()
                    + " (reservation id=" + payload.getReservationId() + ")");
        } catch (final Exception e) {
            LOGGER.atError().addArgument(payload.getReservationId()).log("Failed to send reservation confirmation email (reservation id={})");
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendEmailVerificationCode(final String to, final String code, final Locale locale) {
        if (to == null || to.isBlank() || code == null) {
            LOGGER.atError().log("sendEmailVerificationCode: missing to or code");
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
            LOGGER.atInfo().addArgument(to).log("Email verification code sent to {}");
        } catch (final Exception e) {
            LOGGER.atError().addArgument(to).log("Failed to send email verification code to {}");
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendMigratedUserPassword(final String to, final String plainPassword, final Locale locale) {
        if (to == null || to.isBlank() || plainPassword == null || plainPassword.isBlank()) {
            LOGGER.atError().log("sendMigratedUserPassword: missing to or password");
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
            LOGGER.atInfo().addArgument(to).log("Migrated user password email sent to {}");
        } catch (final Exception e) {
            LOGGER.atError().addArgument(to).log("Failed to send migrated password email to {}");
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendPasswordResetCode(final String to, final String code, final Locale locale) {
        if (to == null || to.isBlank() || code == null) {
            LOGGER.atError().log("sendPasswordResetCode: missing to or code");
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
            LOGGER.atInfo().addArgument(to).log("Password reset code sent to {}");
        } catch (final Exception e) {
            LOGGER.atError().addArgument(to).log("Failed to send password reset code to {}");
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendReservationReminderEmail(final ReservationConfirmationPayload payload) {
        if (payload == null) {
            LOGGER.atError().log("sendReservationConfirmationEmail called with null payload");
            return;
        }

        final Locale mailLocale = payload.getMessageLocale();
        final Context ctx = new Context(mailLocale);
        ctx.setVariable("reservationTotal", payload.getReservationTotal());
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
        final String ctaUrl = baseUrl + "/my-reservations/" + payload.getReservationId();
        ctx.setVariable("ctaUrl", ctaUrl);

        final String to = payload.getRecipientEmail();

        try {
            runMail(() -> {
                final String htmlContent = this.htmlTemplateEngine.process(RESERVATION_REMINDER_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage("mail.reservationReminder.subject", null, mailLocale);
                sendEmail(to, subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(to).log("Reminder sent to {}");
        } catch (final Exception e) {
            LOGGER.atError().addArgument(to).log("Failed to reminder to {}");
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

    @Override
    @Async("mailTaskExecutor")
    public void sendReservationCancellationEmail(final ReservationConfirmationPayload payload) {
        if (payload == null) {
            LOGGER.atError().log("sendReservationCancellationEmail called with null payload");
            return;
        }

        final Locale mailLocale = payload.getMessageLocale();
        final Context ctx = new Context(mailLocale);
        ctx.setVariable("reservationTotal", payload.getReservationTotal());
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
        final String ctaUrlRider = baseUrl + "/my-reservations/" + payload.getReservationId();
        ctx.setVariable("ctaUrlRider", ctaUrlRider);
        final String ctaUrlOwner = baseUrl + "/my-listings/" + payload.getListingId();
        ctx.setVariable("ctaUrlOwner", ctaUrlOwner);

        try {
            sendReservationCancellationToClient(payload, ctx);
            sendReservationCancellationToOwner(payload, ctx);
            LOGGER.atInfo().log("Reservation cancellation email sent to " + payload.getRecipientEmail()
                    + " (reservation id=" + payload.getReservationId() + ")");
            LOGGER.atInfo().log("Reservation cancellation email sent to " + payload.getOwnerEmail()
                    + " (reservation id=" + payload.getReservationId() + ")");
        } catch (final Exception e) {
            LOGGER.atError().addArgument(payload.getReservationId()).log("Failed to send reservation cancellation email (reservation id={})");
        }
    }

    private void sendReservationCancellationToClient(final ReservationConfirmationPayload payload, Context ctx) throws EmailMessagingException {
        runMail(() -> {

            ctx.setVariable("ownerEmail", payload.getOwnerEmail());
            final String htmlContent = this.htmlTemplateEngine.process(RESERVATION_CANCELLATION_USER_TEMPLATE, ctx);

            final String subject = emailMessageSource.getMessage(
                    "mail.reservationCancelled.subject",
                    new Object[]{payload.getVehicleLabel()},
                    payload.getMessageLocale());
            sendEmail(payload.getRecipientEmail(), subject, htmlContent);
        });
    }

    private void sendReservationCancellationToOwner(final ReservationConfirmationPayload payload, Context ctx) throws EmailMessagingException {
        runMail(() -> {
            ctx.setVariable("riderEmail", payload.getRecipientEmail());

            final String htmlContent = this.htmlTemplateEngine.process(RESERVATION_CANCELLATION_OWNER_TEMPLATE, ctx);

            final String subject = emailMessageSource.getMessage(
                    "mail.reservationCancelled.subject",
                    new Object[]{payload.getVehicleLabel()},
                    payload.getMessageLocale());
            sendEmail(payload.getOwnerEmail(), subject, htmlContent);
        });
    }

    private void sendEmail(String to, String subject, String htmlBody) {
        final MimeMessage message = this.mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_RELATED, "UTF-8");
            helper.setTo(to);
            final String from = environment.getProperty("mail.from.address", environment.getProperty("mail.server.username", "noreply@localhost"));
            message.setFrom(from);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            ClassPathResource logo = new ClassPathResource("static/images/logo_transparent.png");
            helper.addInline("logo", logo);
            mailSender.send(message);
        } catch (MessagingException e) {
            LOGGER.atError().log("Error enviando mail a "+ to + ":" + e.getMessage());
        }
    }

}
