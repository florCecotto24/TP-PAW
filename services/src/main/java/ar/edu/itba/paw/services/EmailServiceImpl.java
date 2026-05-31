package ar.edu.itba.paw.services;

import java.util.List;
import java.util.Locale;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import ar.edu.itba.paw.exception.email.EmailMessagingException;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.email.EmailVerificationCodeEmailPayload;
import ar.edu.itba.paw.models.email.MigratedUserPasswordEmailPayload;
import ar.edu.itba.paw.models.email.PasswordResetCodeEmailPayload;
import ar.edu.itba.paw.models.email.ReservationCancellationEmailPayload;
import ar.edu.itba.paw.models.email.ReservationMailPayload;
import ar.edu.itba.paw.services.mail.MailPublicUrls;
import ar.edu.itba.paw.services.policy.ReservationTimingPolicy;
import ar.edu.itba.paw.models.email.OwnerPaymentProofReceivedEmailPayload;
import ar.edu.itba.paw.models.email.OwnerBlockedEmailPayload;
import ar.edu.itba.paw.models.email.OwnerRefundProofObligationEmailPayload;
import ar.edu.itba.paw.models.email.RiderRefundProofReceivedEmailPayload;
import ar.edu.itba.paw.models.email.RiderCarReturnEmailPayload;
import ar.edu.itba.paw.models.email.ListingPausedMissingCbuOwnerEmailPayload;
import ar.edu.itba.paw.models.email.RiderReviewInviteEmailPayload;
import ar.edu.itba.paw.models.email.ReservationChatDigestEmailPayload;
import ar.edu.itba.paw.models.email.ListingValidatedByAdminOwnerEmailPayload;
import ar.edu.itba.paw.models.util.rules.CbuRules;
import ar.edu.itba.paw.models.util.time.WallDateTimeDisplayFormat;

/** Thymeleaf + JavaMail; {@code @Async} on send methods; no JDBC DAOs. */
@Service
public final class EmailServiceImpl implements EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailServiceImpl.class);

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
    private static final String RIDER_DUE_PAYMENT_PROOF_TEMPLATE = "html/reservation-due-payment-reminder-rider";
    private static final String OWNER_REFUND_PROOF_OBLIGATION_TEMPLATE = "html/owner-refund-proof-obligation";
    private static final String OWNER_BLOCKED_TEMPLATE = "html/owner-blocked";
    private static final String RIDER_REFUND_PROOF_RECEIVED_TEMPLATE = "html/rider-refund-proof-received";
    private static final String LISTING_PAUSED_MISSING_CBU_TEMPLATE = "html/listing-paused-missing-cbu-owner";
    private static final String LISTING_PAUSED_BY_ADMIN_TEMPLATE = "html/listing-paused-by-admin-owner";
    private static final String LISTING_REJECTED_BY_ADMIN_TEMPLATE = "html/listing-rejected-by-admin-owner";
    private static final String LISTING_VALIDATED_BY_ADMIN_TEMPLATE = "html/listing-validated-by-admin-owner";
    private static final String RESERVATION_CHAT_DIGEST_TEMPLATE = "html/reservation-chat-digest";

    private static String formatWallDateTime(final java.time.OffsetDateTime dateTime, final Locale messageLocale) {
        final Locale locale = messageLocale != null ? messageLocale : Locale.ENGLISH;
        return WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(dateTime, locale);
    }

    /** Avoids the literal "null" in Thymeleaf for optional contact fields. */
    private String nonBlankEmailForDisplay(final String email, final Locale mailLocale) {
        if (email != null && !email.isBlank()) {
            return email;
        }
        return emailMessageSource.getMessage("mail.common.notAvailable", null, mailLocale);
    }

    /** BCP 47 language tag for the HTML root only (no technical identifiers in the body). */
    private static void setHtmlLangFromLocale(final Context ctx, final Locale mailLocale) {
        final String lang = mailLocale != null && mailLocale.getLanguage() != null && !mailLocale.getLanguage().isBlank()
                ? mailLocale.getLanguage()
                : "en";
        ctx.setVariable("htmlLang", lang);
    }

    /** Shared reservation fields; CTA variables are added by the caller (templates differ). */
    private Context buildReservationNotificationContext(final ReservationMailPayload payload, final Locale mailLocale) {
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
        final Locale loc = mailLocale != null ? mailLocale : Locale.ENGLISH;
        ctx.setVariable("ownerEmail", nonBlankEmailForDisplay(payload.getOwnerEmail(), loc));
        ctx.setVariable("ownerCbu", payload.getOwnerCbu() != null ? payload.getOwnerCbu() : "");
        return ctx;
    }

    private Context buildReservationConfirmationMailContext(final ReservationMailPayload payload, final Locale mailLocale) {
        final Context ctx = buildReservationNotificationContext(payload, mailLocale);
        ctx.setVariable("ctaUrlRider", mailPublicUrls.absolutePath("/my-reservations/" + payload.getReservationId()));
        ctx.setVariable("ctaUrlOwner", mailPublicUrls.absolutePath("/my-cars/car/" + payload.getCarId()));
        return ctx;
    }

    /** Rider-facing templates that deep-link into {@code GET /my-reservations/{id}} (default role is rider). */
    private Context buildRiderReservationDetailMailContext(final ReservationMailPayload payload, final Locale mailLocale) {
        final Context ctx = buildReservationNotificationContext(payload, mailLocale);
        ctx.setVariable("ctaUrl", mailPublicUrls.absolutePath("/my-reservations/" + payload.getReservationId()));
        return ctx;
    }

    private Context buildReservationCancellationRiderMailContext(
            final ReservationMailPayload payload,
            final Locale mailLocale,
            final Reservation.Status cancellationStatus) {
        final Context ctx = buildRiderReservationDetailMailContext(payload, mailLocale);
        ctx.setVariable("cancellationIntroRider", riderCancellationIntro(cancellationStatus, mailLocale));
        return ctx;
    }

    /** Owner cancellation template uses {@code ctaUrl} (host cars). */
    private Context buildReservationCancellationOwnerMailContext(
            final ReservationMailPayload payload,
            final Locale mailLocale,
            final Reservation.Status cancellationStatus) {
        final Context ctx = buildReservationNotificationContext(payload, mailLocale);
        ctx.setVariable("ctaUrl", mailPublicUrls.absolutePath("/my-cars/car/" + payload.getCarId()));
        ctx.setVariable("cancellationIntroOwner", ownerCancellationIntro(cancellationStatus, mailLocale));
        return ctx;
    }

    private String riderCancellationIntro(final Reservation.Status cancellationStatus, final Locale mailLocale) {
        final String key = switch (cancellationStatus) {
            case CANCELLED_BY_RIDER -> "mail.reservationCancelled.introRider.byRider";
            case CANCELLED_BY_OWNER -> "mail.reservationCancelled.introRider.byOwner";
            case CANCELLED_DUE_TO_MISSING_PAYMENT_PROOF -> "mail.reservationCancelled.introRider.missingProof";
            default -> "mail.reservationCancelled.intro.generic";
        };
        return emailMessageSource.getMessage(key, null, mailLocale);
    }

    private String ownerCancellationIntro(final Reservation.Status cancellationStatus, final Locale mailLocale) {
        final String key = switch (cancellationStatus) {
            case CANCELLED_BY_RIDER -> "mail.reservationCancelled.introOwner.byRider";
            case CANCELLED_BY_OWNER -> "mail.reservationCancelled.introOwner.byOwner";
            case CANCELLED_DUE_TO_MISSING_PAYMENT_PROOF -> "mail.reservationCancelled.introOwner.missingProof";
            default -> "mail.reservationCancelled.intro.generic";
        };
        return emailMessageSource.getMessage(key, null, mailLocale);
    }

    private final Environment environment;
    private final MailPublicUrls mailPublicUrls;
    private final JavaMailSender mailSender;
    private final MessageSource emailMessageSource;
    private final TemplateEngine htmlTemplateEngine;
    private final ReservationTimingPolicy reservationTimingPolicy;

    public EmailServiceImpl(
            final Environment environment,
            final MailPublicUrls mailPublicUrls,
            final JavaMailSender mailSender,
            @Qualifier("emailMessageSource") final MessageSource emailMessageSource,
            @Qualifier("emailTemplateEngine") final TemplateEngine htmlTemplateEngine,
            final ReservationTimingPolicy reservationTimingPolicy) {
        this.environment = environment;
        this.mailPublicUrls = mailPublicUrls;
        this.mailSender = mailSender;
        this.emailMessageSource = emailMessageSource;
        this.htmlTemplateEngine = htmlTemplateEngine;
        this.reservationTimingPolicy = reservationTimingPolicy;
    }

    /** Invalid call site: log once and do not send mail. */
    private static boolean skipBecauseNullPayload(final Object payload, final String method) {
        if (payload != null) {
            return false;
        }
        LOGGER.atError().log("{} called with null payload", method);
        return true;
    }

    /** @return true when listing-deletion mail should be skipped. */
    private static boolean skipBecauseNullOrEmptyReservations(final List<ReservationMailPayload> list) {
        if (list != null && !list.isEmpty()) {
            return false;
        }
        LOGGER.atError().log("sendListingDeletionEmail called with null or empty reservationsToCancel");
        return true;
    }

    @Override
    @Transactional
    @Async("mailTaskExecutor")
    public void sendReservationConfirmationEmail(final ReservationMailPayload payload) {
        if (skipBecauseNullPayload(payload, "sendReservationConfirmationEmail")) {
            return;
        }

        final Context riderCtx = buildReservationConfirmationMailContext(payload, payload.getMessageLocale());
        final Context ownerCtx = buildReservationConfirmationMailContext(payload, payload.getOwnerMailLocale());

        try {
            sendReservationConfirmationToClient(payload, riderCtx);
            sendReservationConfirmationToOwner(payload, ownerCtx);
            LOGGER.atInfo().addArgument(payload.getRecipientEmail()).addArgument(payload.getReservationId()).log("Reservation confirmation email sent to {} (reservation id={})");
            LOGGER.atInfo().addArgument(payload.getOwnerEmail()).addArgument(payload.getReservationId()).log("Reservation confirmation email sent to {} (reservation id={})");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(payload.getReservationId())
                    .log("Failed to send reservation confirmation email (reservation id={})");
        }
    }

    @Override
    @Transactional
    @Async("mailTaskExecutor")
    public void sendRiderReservationConfirmedAfterPaymentProof(final ReservationMailPayload payload) {
        if (skipBecauseNullPayload(payload, "sendRiderReservationConfirmedAfterPaymentProof")) {
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
            LOGGER.atInfo().addArgument(payload.getRecipientEmail()).addArgument(payload.getReservationId()).log("Rider reservation confirmed-after-proof email sent to {} (reservation id={})");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(payload.getReservationId())
                    .log("Failed to send rider confirmed-after-proof email (reservation id={})");
        }
    }

    @Override
    @Transactional
    @Async("mailTaskExecutor")
    public void sendEmailVerificationCode(final EmailVerificationCodeEmailPayload payload) {
        final String to = payload.getRecipientEmail();
        final String code = payload.getCode();
        if (to == null || to.isBlank() || code == null) {
            LOGGER.atError().log("sendEmailVerificationCode: missing to or code");
            return;
        }
        final Locale mailLocale = payload.getMessageLocale();
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
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(to).log("Failed to send email verification code to {}");
        }
    }

    @Override
    @Transactional
    @Async("mailTaskExecutor")
    public void sendMigratedUserPassword(final MigratedUserPasswordEmailPayload payload) {
        final String to = payload.getRecipientEmail();
        final String plainPassword = payload.getPlainPassword();
        if (to == null || to.isBlank() || plainPassword == null || plainPassword.isBlank()) {
            LOGGER.atError().log("sendMigratedUserPassword: missing to or password");
            return;
        }
        final Locale mailLocale = payload.getMessageLocale();
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
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(to).log("Failed to send migrated password email to {}");
        }
    }

    @Override
    @Transactional
    @Async("mailTaskExecutor")
    public void sendPasswordResetCode(final PasswordResetCodeEmailPayload payload) {
        final String to = payload.getRecipientEmail();
        final String code = payload.getCode();
        if (to == null || to.isBlank() || code == null) {
            LOGGER.atError().log("sendPasswordResetCode: missing to or code");
            return;
        }
        final Locale mailLocale = payload.getMessageLocale();
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
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(to).log("Failed to send password reset code to {}");
        }
    }

    @Override
    @Transactional
    @Async("mailTaskExecutor")
    public void sendReservationReminderEmail(final ReservationMailPayload payload) {
        if (skipBecauseNullPayload(payload, "sendReservationReminderEmail")) {
            return;
        }

        final Locale mailLocale = payload.getMessageLocale();
        final Context ctx = buildRiderReservationDetailMailContext(payload, mailLocale);

        final String to = payload.getRecipientEmail();

        try {
            runMail(() -> {
                final String htmlContent = this.htmlTemplateEngine.process(RESERVATION_REMINDER_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage("mail.reservationReminder.subject", null, mailLocale);
                sendEmail(to, subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(to).log("Reminder sent to {}");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(to).log("Failed to send reservation reminder email to {}");
        }
    }

    @Override
    @Transactional
    @Async("mailTaskExecutor")
    public void sendRiderDuePaymentProofEmail(final ReservationMailPayload payload) {
        if (skipBecauseNullPayload(payload, "sendRiderDuePaymentProofEmail")) {
            return;
        }

        final Locale mailLocale = payload.getMessageLocale();
        final Context ctx = buildReservationConfirmationMailContext(payload, mailLocale);
        ctx.setVariable("duePaymentReminderHours", reservationTimingPolicy.getPaymentProofReminderLeadHours());

        final String to = payload.getRecipientEmail();

        try {
            runMail(() -> {
                final String htmlContent = this.htmlTemplateEngine.process(RIDER_DUE_PAYMENT_PROOF_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage(
                        "mail.reservationDuePaymentProof.subject",
                        new Object[] { payload.getVehicleLabel() },
                        mailLocale);
                sendEmail(to, subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(to).log("Due payment proof reminder email queued for {}");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(to).log("Failed to queue due payment proof reminder email for {}");
        }
    }

    private void sendReservationConfirmationToClient(final ReservationMailPayload payload, Context ctx) throws EmailMessagingException {
        runMail(() -> {

            final String htmlContent = this.htmlTemplateEngine.process(RESERVATION_CONFIRMATION_USER_TEMPLATE, ctx);

            final String subject = emailMessageSource.getMessage(
                    "mail.reservationRequestSent.subject",
                    new Object[] { payload.getVehicleLabel() },
                    payload.getMessageLocale());
            sendEmail(payload.getRecipientEmail(), subject, htmlContent);
        });
    }

    private void sendReservationConfirmationToOwner(final ReservationMailPayload payload, Context ctx) throws EmailMessagingException {
        runMail(() -> {
            ctx.setVariable(
                    "riderEmail",
                    nonBlankEmailForDisplay(payload.getRecipientEmail(), payload.getOwnerMailLocale()));

            final String htmlContent = this.htmlTemplateEngine.process(RESERVATION_CONFIRMATION_OWNER_TEMPLATE, ctx);

            final String subject = emailMessageSource.getMessage(
                    "mail.reservationRequestReceived.subject",
                    new Object[] { payload.getVehicleLabel() },
                    payload.getOwnerMailLocale());
            sendEmail(payload.getOwnerEmail(), subject, htmlContent);
        });
    }

    @Override
    @Transactional
    @Async("mailTaskExecutor")
    public void sendReservationCancellationEmail(final ReservationCancellationEmailPayload payload) {
        if (skipBecauseNullPayload(payload, "sendReservationCancellationEmail")) {
            return;
        }
        final ReservationMailPayload mail = payload.getMail();
        if (skipBecauseNullPayload(mail, "sendReservationCancellationEmail")) {
            return;
        }
        final Reservation.Status cancellationStatus = payload.getCancellationStatus();

        final Context riderCtx = buildReservationCancellationRiderMailContext(
                mail, mail.getMessageLocale(), cancellationStatus);
        final Context ownerCtx = buildReservationCancellationOwnerMailContext(
                mail, mail.getOwnerMailLocale(), cancellationStatus);

        try {
            sendReservationCancellationToClient(mail, riderCtx, mail.getRecipientEmail());
            sendReservationCancellationToOwner(mail, ownerCtx);
            LOGGER.atInfo().addArgument(mail.getRecipientEmail()).addArgument(mail.getReservationId()).log("Reservation cancellation email sent to {} (reservation id={})");
            LOGGER.atInfo().addArgument(mail.getOwnerEmail()).addArgument(mail.getReservationId()).log("Reservation cancellation email sent to {} (reservation id={})");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(mail.getReservationId())
                    .log("Failed to send reservation cancellation email (reservation id={})");
        }
    }

    @Override
    @Transactional
    @Async("mailTaskExecutor")
    public void sendRiderReturnReminderEmail(final RiderCarReturnEmailPayload payload) {
        if (skipBecauseNullPayload(payload, "sendRiderReturnReminderEmail")) {
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
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(to).log("Failed to queue return reminder email for {}");
        }
    }

    @Override
    @Transactional
    @Async("mailTaskExecutor")
    public void sendRiderReturnCheckoutEmail(final RiderCarReturnEmailPayload payload) {
        if (skipBecauseNullPayload(payload, "sendRiderReturnCheckoutEmail")) {
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
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(to).log("Failed to queue return checkout email for {}");
        }
    }

    @Override
    @Transactional
    @Async("mailTaskExecutor")
    public void sendOwnerPaymentProofReceivedEmail(final OwnerPaymentProofReceivedEmailPayload payload) {
        if (skipBecauseNullPayload(payload, "sendOwnerPaymentProofReceivedEmail")) {
            return;
        }
        final Locale mailLocale = payload.getMessageLocale();
        final Context ctx = new Context(mailLocale);
        setHtmlLangFromLocale(ctx, mailLocale);
        ctx.setVariable("ownerFullName", payload.getOwnerFullName());
        ctx.setVariable("riderFullName", payload.getRiderFullName());
        ctx.setVariable("riderEmail", nonBlankEmailForDisplay(payload.getRiderEmail(), mailLocale));
        ctx.setVariable("vehicleLabel", payload.getVehicleLabel());
        ctx.setVariable("reservationTotal", payload.getReservationTotal());
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
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(payload.getReservationId())
                    .log("Failed to queue owner payment-proof email (reservation id={})");
        }
    }

    @Override
    @Transactional
    @Async("mailTaskExecutor")
    public void sendOwnerRefundProofObligationEmail(final OwnerRefundProofObligationEmailPayload payload) {
        if (skipBecauseNullPayload(payload, "sendOwnerRefundProofObligationEmail")) {
            return;
        }
        final Locale mailLocale = payload.getMessageLocale();
        final Context ctx = new Context(mailLocale);
        setHtmlLangFromLocale(ctx, mailLocale);
        ctx.setVariable("ownerFullName", payload.getOwnerFullName());
        ctx.setVariable("vehicleLabel", payload.getVehicleLabel());
        ctx.setVariable("reservationTotal", payload.getReservationTotal());
        ctx.setVariable("startDateFormatted", formatWallDateTime(payload.getStartDate(), mailLocale));
        ctx.setVariable("endDateFormatted", formatWallDateTime(payload.getEndDate(), mailLocale));
        ctx.setVariable("refundDeadlineFormatted", formatWallDateTime(payload.getRefundProofDeadlineAt(), mailLocale));
        ctx.setVariable("dueReminder", payload.isDueReminder());
        ctx.setVariable("dueRefundReminderHours", reservationTimingPolicy.getPaymentProofReminderLeadHours());
        ctx.setVariable("ctaUrl", mailPublicUrls.absolutePath("/my-reservations/" + payload.getReservationId() + "?role=owner"));
        final String to = payload.getRecipientEmail();
        try {
            runMail(() -> {
                final String htmlContent = this.htmlTemplateEngine.process(OWNER_REFUND_PROOF_OBLIGATION_TEMPLATE, ctx);
                final String subjectKey = payload.isDueReminder()
                        ? "mail.ownerRefundProof.subjectReminder"
                        : "mail.ownerRefundProof.subjectInitial";
                final String subject = emailMessageSource.getMessage(
                        subjectKey,
                        new Object[] { payload.getVehicleLabel() },
                        mailLocale);
                sendEmail(to, subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(to).addArgument(payload.getReservationId()).log("Owner refund-proof email queued for {} (reservation id={})");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(payload.getReservationId())
                    .log("Failed to queue owner refund-proof email (reservation id={})");
        }
    }

    @Override
    @Transactional
    @Async("mailTaskExecutor")
    public void sendOwnerBlockedEmail(final OwnerBlockedEmailPayload payload) {
        if (skipBecauseNullPayload(payload, "sendOwnerBlockedEmail")) {
            return;
        }
        final Locale mailLocale = payload.getMessageLocale();
        final Context ctx = new Context(mailLocale);
        setHtmlLangFromLocale(ctx, mailLocale);
        ctx.setVariable("ownerFullName", payload.getOwnerFullName());
        // Adapt domain rows into a view-only structure with pre-formatted deadlines so the template stays free of formatting logic.
        final List<OwnerBlockedTemplateRow> rows = payload.getOverdueReservations().stream()
                .map(r -> new OwnerBlockedTemplateRow(
                        r.getReservationId(),
                        r.getVehicleLabel(),
                        formatWallDateTime(r.getRefundProofDeadlineAt(), mailLocale),
                        r.getReservationTotal()))
                .collect(java.util.stream.Collectors.toList());
        ctx.setVariable("overdueReservations", rows);
        ctx.setVariable("ctaUrl", mailPublicUrls.absolutePath("/my-reservations?role=owner"));
        final String to = payload.getRecipientEmail();
        try {
            runMail(() -> {
                final String htmlContent = this.htmlTemplateEngine.process(OWNER_BLOCKED_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage(
                        "mail.ownerBlocked.subject", new Object[0], mailLocale);
                sendEmail(to, subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(to).addArgument(rows.size()).log("Owner-blocked email queued for {} ({} overdue refund proofs)");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(to).log("Failed to queue owner-blocked email for {}");
        }
    }

    /** View-only DTO for the Thymeleaf {@code overdueReservations} list; deadline is pre-formatted in the request's locale. */
    public static final class OwnerBlockedTemplateRow {
        private final long reservationId;
        private final String vehicleLabel;
        private final String refundProofDeadlineFormatted;
        private final String reservationTotal;

        public OwnerBlockedTemplateRow(final long reservationId, final String vehicleLabel,
                                       final String refundProofDeadlineFormatted, final String reservationTotal) {
            this.reservationId = reservationId;
            this.vehicleLabel = vehicleLabel;
            this.refundProofDeadlineFormatted = refundProofDeadlineFormatted;
            this.reservationTotal = reservationTotal;
        }

        public long getReservationId() { return reservationId; }
        public String getVehicleLabel() { return vehicleLabel; }
        public String getRefundProofDeadlineFormatted() { return refundProofDeadlineFormatted; }
        public String getReservationTotal() { return reservationTotal; }
    }

    @Override
    @Transactional
    @Async("mailTaskExecutor")
    public void sendRiderRefundProofReceivedEmail(final RiderRefundProofReceivedEmailPayload payload) {
        if (skipBecauseNullPayload(payload, "sendRiderRefundProofReceivedEmail")) {
            return;
        }
        final Locale mailLocale = payload.getMessageLocale();
        final Context ctx = new Context(mailLocale);
        setHtmlLangFromLocale(ctx, mailLocale);
        ctx.setVariable("riderFullName", payload.getRiderFullName());
        ctx.setVariable("ownerFullName", payload.getOwnerFullName());
        ctx.setVariable("ownerEmail", nonBlankEmailForDisplay(payload.getOwnerEmail(), mailLocale));
        ctx.setVariable("vehicleLabel", payload.getVehicleLabel());
        ctx.setVariable("reservationTotal", payload.getReservationTotal());
        ctx.setVariable("startDateFormatted", formatWallDateTime(payload.getStartDate(), mailLocale));
        ctx.setVariable("endDateFormatted", formatWallDateTime(payload.getEndDate(), mailLocale));
        ctx.setVariable("ctaUrl", mailPublicUrls.absolutePath("/my-reservations/" + payload.getReservationId() + "?role=rider"));
        final String to = payload.getRecipientEmail();
        try {
            runMail(() -> {
                final String htmlContent = this.htmlTemplateEngine.process(RIDER_REFUND_PROOF_RECEIVED_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage(
                        "mail.riderRefundProofReceived.subject",
                        new Object[] { payload.getVehicleLabel() },
                        mailLocale);
                sendEmail(to, subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(to).addArgument(payload.getReservationId()).log("Rider refund-proof email queued for {} (reservation id={})");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(payload.getReservationId())
                    .log("Failed to queue rider refund-proof email (reservation id={})");
        }
    }

    @Override
    @Transactional
    @Async("mailTaskExecutor")
    public void sendRiderReviewInviteEmail(final RiderReviewInviteEmailPayload payload) {
        if (skipBecauseNullPayload(payload, "sendRiderReviewInviteEmail")) {
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
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(to).log("Failed to queue rider review invite email for {}");
        }
    }

    private Context buildRiderCarReturnContext(final RiderCarReturnEmailPayload payload, final Locale mailLocale) {
        final Context ctx = new Context(mailLocale);
        setHtmlLangFromLocale(ctx, mailLocale);
        ctx.setVariable("riderFullName", payload.getRiderFullName());
        ctx.setVariable("vehicleLabel", payload.getVehicleLabel());
        ctx.setVariable("ownerEmail", nonBlankEmailForDisplay(payload.getOwnerEmail(), mailLocale));
        ctx.setVariable("checkoutFormatted", payload.getCheckoutFormatted());
        ctx.setVariable("returnLocationLine", payload.getReturnLocationLine());
        ctx.setVariable("ctaUrl", mailPublicUrls.absolutePath(payload.getReservationDetailPath()));
        return ctx;
    }

    @Override
    @Transactional
    @Async("mailTaskExecutor")
    public void sendListingDeletionEmail(final List<ReservationMailPayload> reservationsToCancel) {
        if (skipBecauseNullOrEmptyReservations(reservationsToCancel)) {
            return;
        }

        final ReservationMailPayload ownerPayload = reservationsToCancel.getFirst();
        if (ownerPayload == null) {
            LOGGER.atError().log("sendListingDeletionEmail called with a null payload entry");
            return;
        }

        for (final ReservationMailPayload reservationPayload : reservationsToCancel) {
            if (reservationPayload == null || reservationPayload.getReservationId() <= 0) {
                continue;
            }

            final Context cancellationCtx = buildReservationCancellationRiderMailContext(
                    reservationPayload,
                    reservationPayload.getMessageLocale(),
                    Reservation.Status.CANCELLED);

            try {
                sendReservationCancellationToClient(reservationPayload, cancellationCtx, reservationPayload.getRecipientEmail());
                LOGGER.atInfo().addArgument(reservationPayload.getRecipientEmail())
                        .addArgument(reservationPayload.getCarId())
                        .log("Reservation cancellation email sent to {} (car id={})");
            } catch (final EmailMessagingException | RuntimeException e) {
                LOGGER.atError().setCause(e).addArgument(reservationPayload.getCarId())
                        .log("Failed to send reservation cancellation email (car id={})");
            }
        }

        try {
            final Locale ownerLocale = ownerPayload.getOwnerMailLocale();
            final Context ownerCtx = new Context(ownerLocale);
            setHtmlLangFromLocale(ownerCtx, ownerLocale);
            ownerCtx.setVariable("vehicleLabel", ownerPayload.getVehicleLabel());
            ownerCtx.setVariable("ownerFullName", ownerPayload.getOwnerFullName());
            ownerCtx.setVariable("pricePerDay", ownerPayload.getReservationTotal());
            ownerCtx.setVariable("ctaUrl", mailPublicUrls.absolutePath("/my-cars/car/" + ownerPayload.getCarId()));
            sendListingDeletionToOwner(ownerPayload, ownerCtx);
            LOGGER.atInfo().addArgument(ownerPayload.getOwnerEmail()).addArgument(ownerPayload.getCarId())
                    .log("Listing deletion owner email sent to {} (car id={})");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(ownerPayload.getCarId())
                    .log("Failed to send listing deletion owner email (car id={})");
        }
    }

    private void sendListingDeletionToOwner(final ReservationMailPayload payload, Context ctx) throws EmailMessagingException {
        runMail(() -> {

            ctx.setVariable("ownerEmail", nonBlankEmailForDisplay(payload.getOwnerEmail(), payload.getOwnerMailLocale()));
            final String htmlContent = this.htmlTemplateEngine.process(LISTING_DELETION_TEMPLATE, ctx);

            final String subject = emailMessageSource.getMessage(
                    "mail.listingDeleted.subject",
                    new Object[]{payload.getVehicleLabel()},
                    payload.getOwnerMailLocale());
            sendEmail(payload.getOwnerEmail(), subject, htmlContent);
        });
    }

    private void sendReservationCancellationToClient(final ReservationMailPayload payload, Context ctx,
                                                     String to) throws EmailMessagingException {
        runMail(() -> {

            final String htmlContent = this.htmlTemplateEngine.process(RESERVATION_CANCELLATION_USER_TEMPLATE, ctx);

            final String subject = emailMessageSource.getMessage(
                    "mail.reservationCancelled.subject",
                    new Object[]{payload.getVehicleLabel()},
                    payload.getMessageLocale());
            sendEmail(to, subject, htmlContent);
        });
    }

    private void sendReservationCancellationToOwner(final ReservationMailPayload payload, Context ctx) throws EmailMessagingException {
        runMail(() -> {
            ctx.setVariable(
                    "riderEmail",
                    nonBlankEmailForDisplay(payload.getRecipientEmail(), payload.getOwnerMailLocale()));

            final String htmlContent = this.htmlTemplateEngine.process(RESERVATION_CANCELLATION_OWNER_TEMPLATE, ctx);

            final String subject = emailMessageSource.getMessage(
                    "mail.reservationCancelled.subject",
                    new Object[]{payload.getVehicleLabel()},
                    payload.getOwnerMailLocale());
            sendEmail(payload.getOwnerEmail(), subject, htmlContent);
        });
    }

    @Override
    @Transactional
    @Async("mailTaskExecutor")
    public void sendListingPausedDueToMissingCbu(final ListingPausedMissingCbuOwnerEmailPayload payload) {
        final String ownerEmail = payload.getOwnerEmail();
        if (ownerEmail == null || ownerEmail.isBlank()) {
            LOGGER.atWarn().log("Skipping listing-paused-CBU email: missing recipient");
            return;
        }
        final Locale locale = payload.getMessageLocale();
        final String vehicleLabel = payload.getVehicleLabel();
        final long carId = payload.getCarId();
        final Context ctx = new Context(locale);
        setHtmlLangFromLocale(ctx, locale);
        ctx.setVariable("ownerFullName", payload.getOwnerFullName());
        ctx.setVariable("vehicleLabel", vehicleLabel);
        ctx.setVariable("cbuRequiredDigits", CbuRules.REQUIRED_DIGIT_LENGTH);
        ctx.setVariable("ctaUrl", mailPublicUrls.absolutePath("/my-cars/car/" + carId));
        ctx.setVariable("profileUrl", mailPublicUrls.absolutePath("/profile"));
        try {
            runMail(() -> {
                final String htmlContent = this.htmlTemplateEngine.process(LISTING_PAUSED_MISSING_CBU_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage(
                        "mail.listingPausedMissingCbu.subject",
                        new Object[] { vehicleLabel },
                        locale);
                sendEmail(ownerEmail, subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(ownerEmail).addArgument(carId).log("Car paused (missing CBU) email sent to {} (car id={})");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(carId)
                    .log("Failed to send car paused (missing CBU) email (car id={})");
        }
    }

    @Override
    @Transactional
    @Async("mailTaskExecutor")
    public void sendListingPausedByAdmin(final ar.edu.itba.paw.models.email.ListingPausedByAdminOwnerEmailPayload payload) {
        final String ownerEmail = payload.getOwnerEmail();
        if (ownerEmail == null || ownerEmail.isBlank()) {
            LOGGER.atWarn().log("Skipping listing-paused-by-admin email: missing recipient");
            return;
        }
        final Locale locale = payload.getMessageLocale();
        final String vehicleLabel = payload.getVehicleLabel();
        final long carId = payload.getCarId();
        final Context ctx = new Context(locale);
        setHtmlLangFromLocale(ctx, locale);
        ctx.setVariable("ownerFullName", payload.getOwnerFullName());
        ctx.setVariable("vehicleLabel", vehicleLabel);
        ctx.setVariable("ctaUrl", mailPublicUrls.absolutePath("/my-cars/car/" + carId));
        try {
            runMail(() -> {
                final String htmlContent = this.htmlTemplateEngine.process(LISTING_PAUSED_BY_ADMIN_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage(
                        "mail.listingPausedByAdmin.subject",
                        new Object[] { vehicleLabel },
                        locale);
                sendEmail(ownerEmail, subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(ownerEmail).addArgument(carId).log("Car paused by admin email sent to {} (car id={})");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(carId)
                    .log("Failed to send car paused by admin email (car id={})");
        }
    }

    @Override
    @Transactional
    @Async("mailTaskExecutor")
    public void sendListingRejectedByAdmin(final ar.edu.itba.paw.models.email.ListingRejectedByAdminOwnerEmailPayload payload) {
        if (skipBecauseNullPayload(payload, "sendListingRejectedByAdmin")) {
            return;
        }
        final String ownerEmail = payload.getOwnerEmail();
        if (ownerEmail == null || ownerEmail.isBlank()) {
            LOGGER.atWarn().log("Skipping listing-rejected-by-admin email: missing recipient");
            return;
        }
        final Locale locale = payload.getMessageLocale();
        final String vehicleLabel = payload.getVehicleLabel();
        final long carId = payload.getCarId();
        final Context ctx = new Context(locale);
        setHtmlLangFromLocale(ctx, locale);
        ctx.setVariable("ownerFullName", payload.getOwnerFullName());
        ctx.setVariable("vehicleLabel", vehicleLabel);
        ctx.setVariable("brandName", payload.getBrandName() != null ? payload.getBrandName() : "");
        ctx.setVariable("modelName", payload.getModelName() != null ? payload.getModelName() : "");
        ctx.setVariable("brandRejected", payload.isBrandRejected());
        ctx.setVariable("modelRejected", payload.isModelRejected());
        ctx.setVariable("ctaUrl", mailPublicUrls.absolutePath("/my-cars"));
        try {
            runMail(() -> {
                final String htmlContent = this.htmlTemplateEngine.process(LISTING_REJECTED_BY_ADMIN_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage(
                        "mail.listingRejectedByAdmin.subject",
                        new Object[] { vehicleLabel },
                        locale);
                sendEmail(ownerEmail, subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(ownerEmail).addArgument(carId).log("Catalog entry rejected by admin email sent to {} (car id={})");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(carId)
                    .log("Failed to send catalog entry rejected by admin email (car id={})");
        }
    }

    @Override
    @Transactional
    @Async("mailTaskExecutor")
    public void sendListingValidatedByAdmin(final ListingValidatedByAdminOwnerEmailPayload payload) {
        if (skipBecauseNullPayload(payload, "sendListingValidatedByAdmin")) {
            return;
        }
        final String ownerEmail = payload.getOwnerEmail();
        if (ownerEmail == null || ownerEmail.isBlank()) {
            LOGGER.atWarn().log("Skipping listing-validated-by-admin email: missing recipient");
            return;
        }
        final Locale locale = payload.getMessageLocale();
        final String vehicleLabel = payload.getVehicleLabel();
        final long carId = payload.getCarId();
        final Context ctx = new Context(locale);
        setHtmlLangFromLocale(ctx, locale);
        ctx.setVariable("ownerFullName", payload.getOwnerFullName());
        ctx.setVariable("vehicleLabel", vehicleLabel);
        ctx.setVariable("brandName", payload.getBrandName() != null ? payload.getBrandName() : "");
        ctx.setVariable("modelName", payload.getModelName() != null ? payload.getModelName() : "");
        ctx.setVariable("brandValidated", payload.isBrandValidated());
        ctx.setVariable("ctaUrl", mailPublicUrls.absolutePath("/my-cars"));
        try {
            runMail(() -> {
                final String htmlContent = this.htmlTemplateEngine.process(LISTING_VALIDATED_BY_ADMIN_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage(
                        "mail.listingValidatedByAdmin.subject",
                        new Object[] { vehicleLabel },
                        locale);
                sendEmail(ownerEmail, subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(ownerEmail).addArgument(carId).log("Catalog entry validated by admin email sent to {} (car id={})");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(carId)
                    .log("Failed to send catalog entry validated by admin email (car id={})");
        }
    }

    @Override
    @Transactional
    @Async("mailTaskExecutor")
    public void sendReservationChatDigestEmail(final ReservationChatDigestEmailPayload payload) {
        if (skipBecauseNullPayload(payload, "sendReservationChatDigestEmail")) {
            return;
        }
        final String to = payload.getRecipientEmail();
        if (to == null || to.isBlank()) {
            LOGGER.atError().log("sendReservationChatDigestEmail: missing recipient email");
            return;
        }
        if (payload.getConversations().isEmpty()) {
            return;
        }
        final Locale mailLocale = payload.getMessageLocale();
        final Context ctx = new Context(mailLocale);
        setHtmlLangFromLocale(ctx, mailLocale);
        ctx.setVariable("recipientFullName", payload.getRecipientFullName());
        ctx.setVariable("totalMessageCount", payload.getTotalMessageCount());
        ctx.setVariable("conversationCount", payload.getConversations().size());
        ctx.setVariable("conversations", payload.getConversations());
        try {
            runMail(() -> {
                final String htmlContent = this.htmlTemplateEngine.process(RESERVATION_CHAT_DIGEST_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage(
                        "mail.reservationChatDigest.subject",
                        new Object[] { payload.getTotalMessageCount() },
                        mailLocale);
                sendEmail(to, subject, htmlContent);
            });
            LOGGER.atInfo()
                    .addArgument(to)
                    .addArgument(payload.getTotalMessageCount())
                    .log("Reservation chat digest email sent to {} ({} message(s))");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError()
                    .setCause(e)
                    .addArgument(to)
                    .log("Failed to send reservation chat digest email to {}");
        }
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
            LOGGER.atError().setCause(e).addArgument(to).log("Error sending email to {}");
        }
    }

}