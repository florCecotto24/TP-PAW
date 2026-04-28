package ar.edu.itba.paw.services;

import java.util.List;
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

import ar.edu.itba.paw.models.email.ReservationConfirmationPayload;
import ar.edu.itba.paw.services.mail.MailPublicUrls;
import ar.edu.itba.paw.models.email.OwnerPaymentProofReceivedEmailPayload;
import ar.edu.itba.paw.models.email.RiderCarReturnEmailPayload;
import ar.edu.itba.paw.models.email.RiderReviewInviteEmailPayload;
import ar.edu.itba.paw.models.util.WallDateTimeDisplayFormat;

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
    private static final String RIDER_RESERVATION_CONFIRMED_AFTER_PROOF_TEMPLATE = "html/rider-reservation-confirmed-after-proof";
    private static final String RESERVATION_CANCELLATION_USER_TEMPLATE = "html/reservation-cancellation-rider";
    private static final String RESERVATION_CANCELLATION_OWNER_TEMPLATE = "html/reservation-cancellation-owner";
    private static final String EMAIL_VERIFICATION_TEMPLATE = "html/email-verification-code";
    private static final String MIGRATED_PASSWORD_TEMPLATE = "html/migrated-password";
    private static final String PASSWORD_RESET_TEMPLATE = "html/password-reset-code";
    private static final String RESERVATION_REMINDER_TEMPLATE = "html/reservation-reminder-rider";
    private static final String LISTING_DELETION_TEMPLATE = "html/listing-deleted-owner";
    private static final String RIDER_RETURN_REMINDER_TEMPLATE = "html/rider-return-reminder";
    private static final String RIDER_RETURN_CHECKOUT_TEMPLATE = "html/rider-return-checkout";
    private static final String RIDER_REVIEW_INVITE_TEMPLATE = "html/rider-review-invite";
    private static final String OWNER_PAYMENT_PROOF_RECEIVED_TEMPLATE = "html/owner-payment-proof-received";

    private static String formatWallDateTime(final java.time.OffsetDateTime dateTime, final Locale messageLocale) {
        final Locale locale = messageLocale != null ? messageLocale : Locale.ENGLISH;
        return WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(dateTime, locale);
    }

    /** BCP 47 language tag for the HTML root only (no technical identifiers in the body). */
    private static void setHtmlLangFromLocale(final Context ctx, final Locale mailLocale) {
        final String lang = mailLocale != null && mailLocale.getLanguage() != null && !mailLocale.getLanguage().isBlank()
                ? mailLocale.getLanguage()
                : "en";
        ctx.setVariable("htmlLang", lang);
    }

    /** Shared reservation fields; CTA variables are added by the caller (templates differ). */
    private Context buildReservationNotificationContext(final ReservationConfirmationPayload payload, final Locale mailLocale) {
        final Context ctx = new Context(mailLocale);
        setHtmlLangFromLocale(ctx, mailLocale);
        ctx.setVariable("reservationTotal", payload.getReservationTotal());
        ctx.setVariable("riderFullName", payload.getRiderFullName());
        ctx.setVariable("vehicleLabel", payload.getVehicleLabel());
        ctx.setVariable("startDateFormatted", formatWallDateTime(payload.getStartDate(), mailLocale));
        ctx.setVariable("endDateFormatted", formatWallDateTime(payload.getEndDate(), mailLocale));
        final String delivery = payload.getDeliveryLocation();
        final boolean hasDelivery = delivery != null && !delivery.isBlank();
        ctx.setVariable("hasDeliveryLocation", hasDelivery);
        ctx.setVariable("deliveryLocation", hasDelivery ? delivery : "");
        ctx.setVariable("ownerFullName", payload.getOwnerFullName());
        return ctx;
    }

    private Context buildReservationConfirmationMailContext(final ReservationConfirmationPayload payload, final Locale mailLocale) {
        final Context ctx = buildReservationNotificationContext(payload, mailLocale);
        ctx.setVariable("ctaUrlRider", mailPublicUrls.absolutePath("/my-reservations/" + payload.getReservationId()));
        ctx.setVariable("ctaUrlOwner", mailPublicUrls.absolutePath("/my-listings/" + payload.getListingId()));
        return ctx;
    }

    /** Cancellation / reminder rider templates use {@code ctaUrl} (reservation detail). */
    private Context buildReservationCancellationRiderMailContext(final ReservationConfirmationPayload payload, final Locale mailLocale) {
        final Context ctx = buildReservationNotificationContext(payload, mailLocale);
        ctx.setVariable("ctaUrl", mailPublicUrls.absolutePath("/my-reservations/" + payload.getReservationId()));
        return ctx;
    }

    /** Cancellation owner template uses {@code ctaUrl} (host listings). */
    private Context buildReservationCancellationOwnerMailContext(final ReservationConfirmationPayload payload, final Locale mailLocale) {
        final Context ctx = buildReservationNotificationContext(payload, mailLocale);
        ctx.setVariable("ctaUrl", mailPublicUrls.absolutePath("/my-listings/" + payload.getListingId()));
        return ctx;
    }

    @Autowired
    private Environment environment;

    @Autowired
    private MailPublicUrls mailPublicUrls;

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

        final Context riderCtx = buildReservationConfirmationMailContext(payload, payload.getMessageLocale());
        final Context ownerCtx = buildReservationConfirmationMailContext(payload, payload.getOwnerMailLocale());

        try {
            sendReservationConfirmationToClient(payload, riderCtx);
            sendReservationConfirmationToOwner(payload, ownerCtx);
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
    public void sendRiderReservationConfirmedAfterPaymentProof(final ReservationConfirmationPayload payload) {
        if (payload == null) {
            LOGGER.atError().log("sendRiderReservationConfirmedAfterPaymentProof called with null payload");
            return;
        }
        final Context ctx = buildReservationConfirmationMailContext(payload, payload.getMessageLocale());
        try {
            runMail(() -> {
                final String htmlContent = this.htmlTemplateEngine.process(RIDER_RESERVATION_CONFIRMED_AFTER_PROOF_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage(
                        "mail.reservationConfirmedAfterProof.subject",
                        new Object[] { payload.getVehicleLabel() },
                        payload.getMessageLocale());
                sendEmail(payload.getRecipientEmail(), subject, htmlContent);
            });
            LOGGER.atInfo().log("Rider reservation confirmed-after-proof email sent to " + payload.getRecipientEmail()
                    + " (reservation id=" + payload.getReservationId() + ")");
        } catch (final Exception e) {
            LOGGER.atError().addArgument(payload.getReservationId()).log(
                    "Failed to send rider confirmed-after-proof email (reservation id={})");
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
        setHtmlLangFromLocale(ctx, mailLocale);
        ctx.setVariable("code", code);
        ctx.setVariable("verifyUrl", mailPublicUrls.absolutePath("/verify-email"));

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
        setHtmlLangFromLocale(ctx, mailLocale);
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
        setHtmlLangFromLocale(ctx, mailLocale);
        ctx.setVariable("code", code);
        ctx.setVariable("resetUrl", mailPublicUrls.absolutePath("/forgot-password/reset"));
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
        final Context ctx = buildReservationCancellationRiderMailContext(payload, mailLocale);

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
                    "mail.reservationRequestSent.subject",
                    new Object[] { payload.getVehicleLabel() },
                    payload.getMessageLocale());
            sendEmail(payload.getRecipientEmail(), subject, htmlContent);
        });
    }

    private void sendReservationConfirmationToOwner(final ReservationConfirmationPayload payload, Context ctx) throws EmailMessagingException {
        runMail(() -> {
            ctx.setVariable("riderEmail", payload.getRecipientEmail());

            final String htmlContent = this.htmlTemplateEngine.process(RESERVATION_CONFIRMATION_OWNER_TEMPLATE, ctx);

            final String subject = emailMessageSource.getMessage(
                    "mail.reservationRequestSent.subject",
                    new Object[] { payload.getVehicleLabel() },
                    payload.getOwnerMailLocale());
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

        final Context riderCtx = buildReservationCancellationRiderMailContext(payload, payload.getMessageLocale());
        final Context ownerCtx = buildReservationCancellationOwnerMailContext(payload, payload.getOwnerMailLocale());

        try {
            sendReservationCancellationToClient(payload, riderCtx, payload.getRecipientEmail());
            sendReservationCancellationToOwner(payload, ownerCtx);
            LOGGER.atInfo().log("Reservation cancellation email sent to " + payload.getRecipientEmail()
                    + " (reservation id=" + payload.getReservationId() + ")");
            LOGGER.atInfo().log("Reservation cancellation email sent to " + payload.getOwnerEmail()
                    + " (reservation id=" + payload.getReservationId() + ")");
        } catch (final Exception e) {
            LOGGER.atError().addArgument(payload.getReservationId()).log("Failed to send reservation cancellation email (reservation id={})");
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendRiderReturnReminderEmail(final RiderCarReturnEmailPayload payload) {
        if (payload == null) {
            LOGGER.atError().log("sendRiderReturnReminderEmail called with null payload");
            return;
        }
        final Locale mailLocale = payload.getMessageLocale();
        final Context ctx = buildRiderCarReturnContext(payload, mailLocale);
        final String to = payload.getRecipientEmail();
        try {
            runMail(() -> {
                final String htmlContent = this.htmlTemplateEngine.process(RIDER_RETURN_REMINDER_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage("mail.riderReturnReminder.subject", null, mailLocale);
                sendEmail(to, subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(to).log("Return reminder email queued for {}");
        } catch (final Exception e) {
            LOGGER.atError().addArgument(to).log("Failed to queue return reminder email for {}");
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendRiderReturnCheckoutEmail(final RiderCarReturnEmailPayload payload) {
        if (payload == null) {
            LOGGER.atError().log("sendRiderReturnCheckoutEmail called with null payload");
            return;
        }
        final Locale mailLocale = payload.getMessageLocale();
        final Context ctx = buildRiderCarReturnContext(payload, mailLocale);
        final String to = payload.getRecipientEmail();
        try {
            runMail(() -> {
                final String htmlContent = this.htmlTemplateEngine.process(RIDER_RETURN_CHECKOUT_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage("mail.riderReturnCheckout.subject", null, mailLocale);
                sendEmail(to, subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(to).log("Return checkout email queued for {}");
        } catch (final Exception e) {
            LOGGER.atError().addArgument(to).log("Failed to queue return checkout email for {}");
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendOwnerPaymentProofReceivedEmail(final OwnerPaymentProofReceivedEmailPayload payload) {
        if (payload == null) {
            LOGGER.atError().log("sendOwnerPaymentProofReceivedEmail called with null payload");
            return;
        }
        final Locale mailLocale = payload.getMessageLocale();
        final Context ctx = new Context(mailLocale);
        setHtmlLangFromLocale(ctx, mailLocale);
        ctx.setVariable("ownerFullName", payload.getOwnerFullName());
        ctx.setVariable("riderFullName", payload.getRiderFullName());
        ctx.setVariable("vehicleLabel", payload.getVehicleLabel());
        ctx.setVariable("startDateFormatted", formatWallDateTime(payload.getStartDate(), mailLocale));
        ctx.setVariable("endDateFormatted", formatWallDateTime(payload.getEndDate(), mailLocale));
        ctx.setVariable("ctaUrl", mailPublicUrls.absolutePath("/my-reservations/" + payload.getReservationId() + "?role=owner"));
        final String to = payload.getRecipientEmail();
        try {
            runMail(() -> {
                final String htmlContent = this.htmlTemplateEngine.process(OWNER_PAYMENT_PROOF_RECEIVED_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage(
                        "mail.paymentProofSubmittedOwner.subject",
                        new Object[] { payload.getVehicleLabel() },
                        mailLocale);
                sendEmail(to, subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(to).addArgument(payload.getReservationId()).log("Owner payment-proof email queued for {} (reservation id={})");
        } catch (final Exception e) {
            LOGGER.atError().addArgument(payload.getReservationId()).log("Failed to queue owner payment-proof email (reservation id={})");
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendRiderReviewInviteEmail(final RiderReviewInviteEmailPayload payload) {
        if (payload == null) {
            LOGGER.atError().log("sendRiderReviewInviteEmail called with null payload");
            return;
        }
        final Locale mailLocale = payload.getMessageLocale();
        final Context ctx = new Context(mailLocale);
        setHtmlLangFromLocale(ctx, mailLocale);
        ctx.setVariable("riderFullName", payload.getRiderFullName());
        ctx.setVariable("vehicleLabel", payload.getVehicleLabel());
        ctx.setVariable("ctaUrl", mailPublicUrls.absolutePath(payload.getReviewSectionPath()));
        final String to = payload.getRecipientEmail();
        try {
            runMail(() -> {
                final String htmlContent = this.htmlTemplateEngine.process(RIDER_REVIEW_INVITE_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage("mail.riderReviewInvite.subject", null, mailLocale);
                sendEmail(to, subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(to).log("Rider review invite email queued for {}");
        } catch (final Exception e) {
            LOGGER.atError().addArgument(to).log("Failed to queue rider review invite email for {}");
        }
    }

    private Context buildRiderCarReturnContext(final RiderCarReturnEmailPayload payload, final Locale mailLocale) {
        final Context ctx = new Context(mailLocale);
        setHtmlLangFromLocale(ctx, mailLocale);
        ctx.setVariable("riderFullName", payload.getRiderFullName());
        ctx.setVariable("vehicleLabel", payload.getVehicleLabel());
        ctx.setVariable("ownerEmail", payload.getOwnerEmail());
        ctx.setVariable("checkoutFormatted", payload.getCheckoutFormatted());
        ctx.setVariable("returnLocationLine", payload.getReturnLocationLine());
        ctx.setVariable("ctaUrl", mailPublicUrls.absolutePath(payload.getReservationDetailPath()));
        return ctx;
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendListingDeletionEmail(final List<ReservationConfirmationPayload> reservationsToCancel) {
        if (reservationsToCancel == null || reservationsToCancel.isEmpty()) {
            LOGGER.atError().log("sendListingDeletionEmail called with null or empty reservationsToCancel");
            return;
        }

        final ReservationConfirmationPayload ownerPayload = reservationsToCancel.getFirst();
        if (ownerPayload == null) {
            LOGGER.atError().log("sendListingDeletionEmail called with a null payload entry");
            return;
        }

        for (final ReservationConfirmationPayload reservationPayload : reservationsToCancel) {
            if (reservationPayload == null || reservationPayload.getReservationId() <= 0) {
                continue;
            }

            final Context cancellationCtx = buildReservationCancellationRiderMailContext(
                    reservationPayload,
                    reservationPayload.getMessageLocale());

            try {
                sendReservationCancellationToClient(reservationPayload, cancellationCtx, reservationPayload.getRecipientEmail());
                LOGGER.atInfo().addArgument(reservationPayload.getRecipientEmail())
                        .addArgument(reservationPayload.getListingId())
                        .log("Reservation cancellation email sent to {} (listing id={})");
            } catch (final Exception e) {
                LOGGER.atError().addArgument(reservationPayload.getListingId())
                        .log("Failed to send reservation cancellation email (listing id={})");
            }
        }

        try {
            final Locale ownerLocale = ownerPayload.getOwnerMailLocale();
            final Context ownerCtx = new Context(ownerLocale);
            setHtmlLangFromLocale(ownerCtx, ownerLocale);
            ownerCtx.setVariable("vehicleLabel", ownerPayload.getVehicleLabel());
            ownerCtx.setVariable("ownerFullName", ownerPayload.getOwnerFullName());
            ownerCtx.setVariable("pricePerDay", ownerPayload.getReservationTotal());
            ownerCtx.setVariable("ctaUrl", mailPublicUrls.absolutePath("/my-listings/" + ownerPayload.getListingId()));
            sendListingDeletionToOwner(ownerPayload, ownerCtx);
            LOGGER.atInfo().addArgument(ownerPayload.getOwnerEmail()).addArgument(ownerPayload.getListingId())
                    .log("Listing deletion owner email sent to {} (listing id={})");
        } catch (final Exception e) {
            LOGGER.atError().addArgument(ownerPayload.getListingId())
                    .log("Failed to send listing deletion owner email (listing id={})");
        }
    }

    private void sendListingDeletionToOwner(final ReservationConfirmationPayload payload, Context ctx) throws EmailMessagingException {
        runMail(() -> {

            ctx.setVariable("ownerEmail", payload.getOwnerEmail());
            final String htmlContent = this.htmlTemplateEngine.process(LISTING_DELETION_TEMPLATE, ctx);

            final String subject = emailMessageSource.getMessage(
                    "mail.listingDeleted.subject",
                    new Object[]{payload.getVehicleLabel()},
                    payload.getOwnerMailLocale());
            sendEmail(payload.getOwnerEmail(), subject, htmlContent);
        });
    }

    private void sendReservationCancellationToClient(final ReservationConfirmationPayload payload, Context ctx,
                                                     String to) throws EmailMessagingException {
        runMail(() -> {

            ctx.setVariable("ownerEmail", payload.getOwnerEmail());
            final String htmlContent = this.htmlTemplateEngine.process(RESERVATION_CANCELLATION_USER_TEMPLATE, ctx);

            final String subject = emailMessageSource.getMessage(
                    "mail.reservationCancelled.subject",
                    new Object[]{payload.getVehicleLabel()},
                    payload.getMessageLocale());
            sendEmail(to, subject, htmlContent);
        });
    }

    private void sendReservationCancellationToOwner(final ReservationConfirmationPayload payload, Context ctx) throws EmailMessagingException {
        runMail(() -> {
            ctx.setVariable("riderEmail", payload.getRecipientEmail());

            final String htmlContent = this.htmlTemplateEngine.process(RESERVATION_CANCELLATION_OWNER_TEMPLATE, ctx);

            final String subject = emailMessageSource.getMessage(
                    "mail.reservationCancelled.subject",
                    new Object[]{payload.getVehicleLabel()},
                    payload.getOwnerMailLocale());
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