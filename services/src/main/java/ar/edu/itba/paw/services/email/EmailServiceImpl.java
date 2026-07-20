package ar.edu.itba.paw.services.email;


import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import ar.edu.itba.paw.exception.email.EmailMessagingException;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.util.rules.SupportedLocales;
import ar.edu.itba.paw.models.email.admin.AdminInvitationEmailPayload;
import ar.edu.itba.paw.models.email.admin.AdminPromotedEmailPayload;
import ar.edu.itba.paw.models.email.listing.CarPausedByAdminOwnerEmailPayload;
import ar.edu.itba.paw.models.email.listing.CarPausedMissingCbuOwnerEmailPayload;
import ar.edu.itba.paw.models.email.listing.CarPausedMissingIdentityOwnerEmailPayload;
import ar.edu.itba.paw.models.email.listing.CarRejectedByAdminOwnerEmailPayload;
import ar.edu.itba.paw.models.email.listing.CarValidatedByAdminOwnerEmailPayload;
import ar.edu.itba.paw.models.email.user.EmailVerificationCodeEmailPayload;
import ar.edu.itba.paw.models.email.user.MigratedUserPasswordEmailPayload;
import ar.edu.itba.paw.models.email.reservation.OwnerBlockedEmailPayload;
import ar.edu.itba.paw.models.email.reservation.OwnerPaymentProofReceivedEmailPayload;
import ar.edu.itba.paw.models.email.reservation.OwnerRefundProofObligationEmailPayload;
import ar.edu.itba.paw.models.email.user.PasswordResetCodeEmailPayload;
import ar.edu.itba.paw.models.email.reservation.ReservationCancellationEmailPayload;
import ar.edu.itba.paw.models.email.reservation.chat.ReservationChatDigestEmailPayload;
import ar.edu.itba.paw.models.email.reservation.ReservationMailPayload;
import ar.edu.itba.paw.models.email.reservation.RiderCarReturnEmailPayload;
import ar.edu.itba.paw.models.email.reservation.RiderRefundProofReceivedEmailPayload;
import ar.edu.itba.paw.models.email.reservation.RiderReviewInviteEmailPayload;
import ar.edu.itba.paw.mail.MailDispatchSupport;
import ar.edu.itba.paw.mail.MailPublicUrls;
import ar.edu.itba.paw.policy.ReservationTimingPolicy;


/**
 * Reservation-centric email service (rider/owner reservation lifecycle, return reminders, chat
 * digest, etc.). Other email domains live in dedicated services that this class delegates to so
 * the public {@link EmailService} contract stays unchanged:
 *
 * 
 *  - User identity / auth emails → {@link UserAccountEmailService}
 *  - Owner-facing listing-lifecycle emails → {@link OwnerListingEmailService}
 *
 * JavaMail/MimeMessage assembly and other low-level concerns are centralized in
 * {@link MailDispatchSupport}.
 *
 * Transactionality: every public method here is either an {@code @Async} mail sender
 * (Thymeleaf render + {@link MailDispatchSupport#runMail}) or a pure synchronous pass-through
 * to one of the delegated mail services. Neither branch touches JPA on this thread — the
 * {@code @Async} dispatch even hops threads, so a caller-side tx wouldn't propagate anyway.
 * {@code @Transactional} is intentionally omitted throughout the class — AGENTS.md line 229
 * exempts "mail-only {@code @Async} senders" from the transactionality rule, and line 240
 * permits public service methods without {@code @Transactional} under documented justification.
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailServiceImpl.class);

    private static final String RESERVATION_CONFIRMATION_USER_TEMPLATE = "html/reservation-confirmation-rider";
    private static final String RESERVATION_CONFIRMATION_OWNER_TEMPLATE = "html/reservation-confirmation-owner";
    private static final String RIDER_RESERVATION_CONFIRMED_AFTER_PROOF_TEMPLATE = "html/rider-reservation-confirmed-after-proof";
    private static final String RESERVATION_CANCELLATION_USER_TEMPLATE = "html/reservation-cancellation-rider";
    private static final String RESERVATION_CANCELLATION_OWNER_TEMPLATE = "html/reservation-cancellation-owner";
    private static final String RESERVATION_REMINDER_TEMPLATE = "html/reservation-reminder-rider";
    private static final String RIDER_RETURN_REMINDER_TEMPLATE = "html/rider-return-reminder";
    private static final String RIDER_RETURN_CHECKOUT_TEMPLATE = "html/rider-return-checkout";
    private static final String RIDER_REVIEW_INVITE_TEMPLATE = "html/rider-review-invite";
    private static final String RIDER_DUE_PAYMENT_PROOF_TEMPLATE = "html/reservation-due-payment-reminder-rider";
    private static final String RIDER_REFUND_PROOF_RECEIVED_TEMPLATE = "html/rider-refund-proof-received";
    private static final String RESERVATION_CHAT_DIGEST_TEMPLATE = "html/reservation-chat-digest";

    private final MailDispatchSupport mailDispatch;
    private final MailPublicUrls mailPublicUrls;
    private final MessageSource emailMessageSource;
    private final TemplateEngine htmlTemplateEngine;
    private final ReservationTimingPolicy reservationTimingPolicy;
    private final UserAccountEmailService userAccountEmailService;
    private final OwnerListingEmailService ownerListingEmailService;

    @Autowired
    public EmailServiceImpl(
            final MailDispatchSupport mailDispatch,
            final MailPublicUrls mailPublicUrls,
            @Qualifier("emailMessageSource") final MessageSource emailMessageSource,
            @Qualifier("emailTemplateEngine") final TemplateEngine htmlTemplateEngine,
            final ReservationTimingPolicy reservationTimingPolicy,
            final UserAccountEmailService userAccountEmailService,
            final OwnerListingEmailService ownerListingEmailService) {
        this.mailDispatch = mailDispatch;
        this.mailPublicUrls = mailPublicUrls;
        this.emailMessageSource = emailMessageSource;
        this.htmlTemplateEngine = htmlTemplateEngine;
        this.reservationTimingPolicy = reservationTimingPolicy;
        this.userAccountEmailService = userAccountEmailService;
        this.ownerListingEmailService = ownerListingEmailService;
    }

    // -------- Shared reservation-context helpers (rider/owner reservation templates) --------

    private static boolean skipBecauseNullPayload(final Object payload, final String method) {
        if (payload != null) {
            return false;
        }
        LOGGER.atError().log("{} called with null payload", method);
        return true;
    }

    /** Shared reservation fields; CTA variables are added by the caller (templates differ). */
    private Context buildReservationNotificationContext(final ReservationMailPayload payload, final Locale mailLocale) {
        final Context ctx = new Context(mailLocale);
        mailDispatch.setHtmlLangFromLocale(ctx, mailLocale);
        ctx.setVariable("reservationTotal", payload.getReservationTotal());
        ctx.setVariable("riderFullName", payload.getRiderFullName());
        ctx.setVariable("vehicleLabel", payload.getVehicleLabel());
        ctx.setVariable("startDateFormatted", mailDispatch.formatWallDateTime(payload.getStartDate(), mailLocale));
        ctx.setVariable("endDateFormatted", mailDispatch.formatWallDateTime(payload.getEndDate(), mailLocale));
        final String delivery = payload.getDeliveryLocation();
        final boolean hasDelivery = delivery != null && !delivery.isBlank();
        ctx.setVariable("hasDeliveryLocation", hasDelivery);
        ctx.setVariable("deliveryLocation", hasDelivery ? delivery : "");
        ctx.setVariable("ownerFullName", payload.getOwnerFullName());
        final Locale loc = mailLocale != null ? mailLocale : SupportedLocales.DEFAULT;
        ctx.setVariable("ownerEmail", mailDispatch.nonBlankEmailForDisplay(payload.getOwnerEmail(), loc));
        ctx.setVariable("ownerCbu", payload.getOwnerCbu() != null ? payload.getOwnerCbu() : "");
        return ctx;
    }

    private Context buildReservationConfirmationMailContext(final ReservationMailPayload payload, final Locale mailLocale) {
        final Context ctx = buildReservationNotificationContext(payload, mailLocale);
        ctx.setVariable("ctaUrlRider", mailPublicUrls.absolutePathWithSelf(
                "/my-reservations/" + payload.getReservationId(), "reservations", payload.getReservationId()));
        ctx.setVariable("ctaUrlOwner", mailPublicUrls.absolutePathWithSelf(
                "/my-cars/car/" + payload.getCarId(), "cars", payload.getCarId()));
        return ctx;
    }

    /** Rider-facing templates that deep-link into {@code GET /my-reservations/{id}}. */
    private Context buildRiderReservationDetailMailContext(final ReservationMailPayload payload, final Locale mailLocale) {
        final Context ctx = buildReservationNotificationContext(payload, mailLocale);
        ctx.setVariable("ctaUrl", mailPublicUrls.absolutePathWithSelf(
                "/my-reservations/" + payload.getReservationId(), "reservations", payload.getReservationId()));
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

    private Context buildReservationCancellationOwnerMailContext(
            final ReservationMailPayload payload,
            final Locale mailLocale,
            final Reservation.Status cancellationStatus) {
        final Context ctx = buildReservationNotificationContext(payload, mailLocale);
        ctx.setVariable("ctaUrl", mailPublicUrls.absolutePathWithSelf(
                "/my-cars/car/" + payload.getCarId(), "cars", payload.getCarId()));
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

    private Context buildRiderCarReturnContext(final RiderCarReturnEmailPayload payload, final Locale mailLocale) {
        final Context ctx = new Context(mailLocale);
        mailDispatch.setHtmlLangFromLocale(ctx, mailLocale);
        ctx.setVariable("riderFullName", payload.getRiderFullName());
        ctx.setVariable("vehicleLabel", payload.getVehicleLabel());
        ctx.setVariable("ownerEmail", mailDispatch.nonBlankEmailForDisplay(payload.getOwnerEmail(), mailLocale));
        ctx.setVariable("checkoutFormatted", payload.getCheckoutFormatted());
        ctx.setVariable("returnLocationLine", payload.getReturnLocationLine());
        ctx.setVariable("ctaUrl", mailPublicUrls.absolutePath(payload.getReservationDetailPath()));
        return ctx;
    }

    // -------- Reservation lifecycle emails (rider+owner; remain owned by this service) --------

    @Override
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
            LOGGER.atInfo().addArgument(payload.getRecipientEmail()).addArgument(payload.getReservationId())
                    .log("Reservation confirmation email sent to {} (reservation id={})");
            LOGGER.atInfo().addArgument(payload.getOwnerEmail()).addArgument(payload.getReservationId())
                    .log("Reservation confirmation email sent to {} (reservation id={})");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(payload.getReservationId())
                    .log("Failed to send reservation confirmation email (reservation id={})");
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendRiderReservationConfirmedAfterPaymentProof(final ReservationMailPayload payload) {
        if (skipBecauseNullPayload(payload, "sendRiderReservationConfirmedAfterPaymentProof")) {
            return;
        }
        final Context ctx = buildReservationConfirmationMailContext(payload, payload.getMessageLocale());
        try {
            mailDispatch.runMail(() -> {
                final String htmlContent = htmlTemplateEngine.process(RIDER_RESERVATION_CONFIRMED_AFTER_PROOF_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage(
                        "mail.reservationConfirmedAfterProof.subject",
                        new Object[] { payload.getVehicleLabel() },
                        payload.getMessageLocale());
                mailDispatch.sendEmail(payload.getRecipientEmail(), subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(payload.getRecipientEmail()).addArgument(payload.getReservationId())
                    .log("Rider reservation confirmed-after-proof email sent to {} (reservation id={})");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(payload.getReservationId())
                    .log("Failed to send rider confirmed-after-proof email (reservation id={})");
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendReservationReminderEmail(final ReservationMailPayload payload) {
        if (skipBecauseNullPayload(payload, "sendReservationReminderEmail")) {
            return;
        }
        final Locale mailLocale = payload.getMessageLocale();
        final Context ctx = buildRiderReservationDetailMailContext(payload, mailLocale);
        final String to = payload.getRecipientEmail();
        try {
            mailDispatch.runMail(() -> {
                final String htmlContent = htmlTemplateEngine.process(RESERVATION_REMINDER_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage("mail.reservationReminder.subject", null, mailLocale);
                mailDispatch.sendEmail(to, subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(to).log("Reminder sent to {}");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(to).log("Failed to send reservation reminder email to {}");
        }
    }

    @Override
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
            mailDispatch.runMail(() -> {
                final String htmlContent = htmlTemplateEngine.process(RIDER_DUE_PAYMENT_PROOF_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage(
                        "mail.reservationDuePaymentProof.subject",
                        new Object[] { payload.getVehicleLabel() },
                        mailLocale);
                mailDispatch.sendEmail(to, subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(to).log("Due payment proof reminder email queued for {}");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(to).log("Failed to queue due payment proof reminder email for {}");
        }
    }

    @Override
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
            LOGGER.atInfo().addArgument(mail.getRecipientEmail()).addArgument(mail.getReservationId())
                    .log("Reservation cancellation email sent to {} (reservation id={})");
            LOGGER.atInfo().addArgument(mail.getOwnerEmail()).addArgument(mail.getReservationId())
                    .log("Reservation cancellation email sent to {} (reservation id={})");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(mail.getReservationId())
                    .log("Failed to send reservation cancellation email (reservation id={})");
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendRiderReturnReminderEmail(final RiderCarReturnEmailPayload payload) {
        if (skipBecauseNullPayload(payload, "sendRiderReturnReminderEmail")) {
            return;
        }
        final Locale mailLocale = payload.getMessageLocale();
        final Context ctx = buildRiderCarReturnContext(payload, mailLocale);
        final String to = payload.getRecipientEmail();
        try {
            mailDispatch.runMail(() -> {
                final String htmlContent = htmlTemplateEngine.process(RIDER_RETURN_REMINDER_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage("mail.riderReturnReminder.subject", null, mailLocale);
                mailDispatch.sendEmail(to, subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(to).log("Return reminder email queued for {}");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(to).log("Failed to queue return reminder email for {}");
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendRiderReturnCheckoutEmail(final RiderCarReturnEmailPayload payload) {
        if (skipBecauseNullPayload(payload, "sendRiderReturnCheckoutEmail")) {
            return;
        }
        final Locale mailLocale = payload.getMessageLocale();
        final Context ctx = buildRiderCarReturnContext(payload, mailLocale);
        final String to = payload.getRecipientEmail();
        try {
            mailDispatch.runMail(() -> {
                final String htmlContent = htmlTemplateEngine.process(RIDER_RETURN_CHECKOUT_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage("mail.riderReturnCheckout.subject", null, mailLocale);
                mailDispatch.sendEmail(to, subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(to).log("Return checkout email queued for {}");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(to).log("Failed to queue return checkout email for {}");
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendRiderReviewInviteEmail(final RiderReviewInviteEmailPayload payload) {
        if (skipBecauseNullPayload(payload, "sendRiderReviewInviteEmail")) {
            return;
        }
        final Locale mailLocale = payload.getMessageLocale();
        final Context ctx = new Context(mailLocale);
        mailDispatch.setHtmlLangFromLocale(ctx, mailLocale);
        ctx.setVariable("riderFullName", payload.getRiderFullName());
        ctx.setVariable("vehicleLabel", payload.getVehicleLabel());
        ctx.setVariable("ctaUrl", mailPublicUrls.absolutePath(payload.getReviewSectionPath()));
        final String to = payload.getRecipientEmail();
        try {
            mailDispatch.runMail(() -> {
                final String htmlContent = htmlTemplateEngine.process(RIDER_REVIEW_INVITE_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage("mail.riderReviewInvite.subject", null, mailLocale);
                mailDispatch.sendEmail(to, subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(to).log("Rider review invite email queued for {}");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(to).log("Failed to queue rider review invite email for {}");
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendRiderRefundProofReceivedEmail(final RiderRefundProofReceivedEmailPayload payload) {
        if (skipBecauseNullPayload(payload, "sendRiderRefundProofReceivedEmail")) {
            return;
        }
        final Locale mailLocale = payload.getMessageLocale();
        final Context ctx = new Context(mailLocale);
        mailDispatch.setHtmlLangFromLocale(ctx, mailLocale);
        ctx.setVariable("riderFullName", payload.getRiderFullName());
        ctx.setVariable("ownerFullName", payload.getOwnerFullName());
        ctx.setVariable("ownerEmail", mailDispatch.nonBlankEmailForDisplay(payload.getOwnerEmail(), mailLocale));
        ctx.setVariable("vehicleLabel", payload.getVehicleLabel());
        ctx.setVariable("reservationTotal", payload.getReservationTotal());
        ctx.setVariable("startDateFormatted", mailDispatch.formatWallDateTime(payload.getStartDate(), mailLocale));
        ctx.setVariable("endDateFormatted", mailDispatch.formatWallDateTime(payload.getEndDate(), mailLocale));
        ctx.setVariable("ctaUrl",
                mailPublicUrls.absolutePathWithSelf(
                        "/my-reservations/" + payload.getReservationId() + "?role=rider",
                        "reservations",
                        payload.getReservationId()));
        final String to = payload.getRecipientEmail();
        try {
            mailDispatch.runMail(() -> {
                final String htmlContent = htmlTemplateEngine.process(RIDER_REFUND_PROOF_RECEIVED_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage(
                        "mail.riderRefundProofReceived.subject",
                        new Object[] { payload.getVehicleLabel() }, mailLocale);
                mailDispatch.sendEmail(to, subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(to).addArgument(payload.getReservationId())
                    .log("Rider refund-proof email queued for {} (reservation id={})");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(payload.getReservationId())
                    .log("Failed to queue rider refund-proof email (reservation id={})");
        }
    }

    @Override
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
        mailDispatch.setHtmlLangFromLocale(ctx, mailLocale);
        ctx.setVariable("recipientFullName", payload.getRecipientFullName());
        ctx.setVariable("totalMessageCount", payload.getTotalMessageCount());
        ctx.setVariable("conversationCount", payload.getConversations().size());
        ctx.setVariable("conversations", payload.getConversations());
        try {
            mailDispatch.runMail(() -> {
                final String htmlContent = htmlTemplateEngine.process(RESERVATION_CHAT_DIGEST_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage(
                        "mail.reservationChatDigest.subject",
                        new Object[] { payload.getTotalMessageCount() }, mailLocale);
                mailDispatch.sendEmail(to, subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(to).addArgument(payload.getTotalMessageCount())
                    .log("Reservation chat digest email sent to {} ({} message(s))");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(to)
                    .log("Failed to send reservation chat digest email to {}");
        }
    }

    // -------- Reservation send helpers --------

    private void sendReservationConfirmationToClient(final ReservationMailPayload payload, final Context ctx)
            throws EmailMessagingException {
        mailDispatch.runMail(() -> {
            final String htmlContent = htmlTemplateEngine.process(RESERVATION_CONFIRMATION_USER_TEMPLATE, ctx);
            final String subject = emailMessageSource.getMessage(
                    "mail.reservationRequestSent.subject",
                    new Object[] { payload.getVehicleLabel() },
                    payload.getMessageLocale());
            mailDispatch.sendEmail(payload.getRecipientEmail(), subject, htmlContent);
        });
    }

    private void sendReservationConfirmationToOwner(final ReservationMailPayload payload, final Context ctx)
            throws EmailMessagingException {
        mailDispatch.runMail(() -> {
            ctx.setVariable("riderEmail",
                    mailDispatch.nonBlankEmailForDisplay(payload.getRecipientEmail(), payload.getOwnerMailLocale()));
            final String htmlContent = htmlTemplateEngine.process(RESERVATION_CONFIRMATION_OWNER_TEMPLATE, ctx);
            final String subject = emailMessageSource.getMessage(
                    "mail.reservationRequestReceived.subject",
                    new Object[] { payload.getVehicleLabel() },
                    payload.getOwnerMailLocale());
            mailDispatch.sendEmail(payload.getOwnerEmail(), subject, htmlContent);
        });
    }

    private void sendReservationCancellationToClient(final ReservationMailPayload payload, final Context ctx,
                                                     final String to) throws EmailMessagingException {
        mailDispatch.runMail(() -> {
            final String htmlContent = htmlTemplateEngine.process(RESERVATION_CANCELLATION_USER_TEMPLATE, ctx);
            final String subject = emailMessageSource.getMessage(
                    "mail.reservationCancelled.subject",
                    new Object[] { payload.getVehicleLabel() },
                    payload.getMessageLocale());
            mailDispatch.sendEmail(to, subject, htmlContent);
        });
    }

    private void sendReservationCancellationToOwner(final ReservationMailPayload payload, final Context ctx)
            throws EmailMessagingException {
        mailDispatch.runMail(() -> {
            ctx.setVariable("riderEmail",
                    mailDispatch.nonBlankEmailForDisplay(payload.getRecipientEmail(), payload.getOwnerMailLocale()));
            final String htmlContent = htmlTemplateEngine.process(RESERVATION_CANCELLATION_OWNER_TEMPLATE, ctx);
            final String subject = emailMessageSource.getMessage(
                    "mail.reservationCancelled.subject",
                    new Object[] { payload.getVehicleLabel() },
                    payload.getOwnerMailLocale());
            mailDispatch.sendEmail(payload.getOwnerEmail(), subject, htmlContent);
        });
    }

    // -------- Mail-only delegates to UserAccountEmailService / OwnerListingEmailService --------
    // (See class-level Javadoc for the transactionality rationale.)

    @Override
    public void sendEmailVerificationCode(final EmailVerificationCodeEmailPayload payload) {
        userAccountEmailService.sendEmailVerificationCode(payload);
    }

    @Override
    public void sendMigratedUserPassword(final MigratedUserPasswordEmailPayload payload) {
        userAccountEmailService.sendMigratedUserPassword(payload);
    }

    @Override
    public void sendAdminInvitation(final AdminInvitationEmailPayload payload) {
        userAccountEmailService.sendAdminInvitation(payload);
    }

    @Override
    public void sendAdminPromoted(final AdminPromotedEmailPayload payload) {
        userAccountEmailService.sendAdminPromoted(payload);
    }

    @Override
    public void sendPasswordResetCode(final PasswordResetCodeEmailPayload payload) {
        userAccountEmailService.sendPasswordResetCode(payload);
    }

    @Override
    public void sendListingDeletionEmail(final List<ReservationMailPayload> reservationsToCancel) {
        ownerListingEmailService.sendListingDeletionEmail(reservationsToCancel);
    }

    @Override
    public void sendOwnerPaymentProofReceivedEmail(final OwnerPaymentProofReceivedEmailPayload payload) {
        ownerListingEmailService.sendOwnerPaymentProofReceivedEmail(payload);
    }

    @Override
    public void sendOwnerRefundProofObligationEmail(final OwnerRefundProofObligationEmailPayload payload) {
        ownerListingEmailService.sendOwnerRefundProofObligationEmail(payload);
    }

    @Override
    public void sendOwnerBlockedEmail(final OwnerBlockedEmailPayload payload) {
        ownerListingEmailService.sendOwnerBlockedEmail(payload);
    }

    @Override
    public void sendListingPausedDueToMissingCbu(final CarPausedMissingCbuOwnerEmailPayload payload) {
        ownerListingEmailService.sendListingPausedDueToMissingCbu(payload);
    }

    @Override
    public void sendListingPausedDueToMissingIdentity(final CarPausedMissingIdentityOwnerEmailPayload payload) {
        ownerListingEmailService.sendListingPausedDueToMissingIdentity(payload);
    }

    @Override
    public void sendCarPausedByAdmin(final CarPausedByAdminOwnerEmailPayload payload) {
        ownerListingEmailService.sendCarPausedByAdmin(payload);
    }

    @Override
    public void sendCarRejectedByAdmin(final CarRejectedByAdminOwnerEmailPayload payload) {
        ownerListingEmailService.sendCarRejectedByAdmin(payload);
    }

    @Override
    public void sendCarValidatedByAdmin(final CarValidatedByAdminOwnerEmailPayload payload) {
        ownerListingEmailService.sendCarValidatedByAdmin(payload);
    }
}
