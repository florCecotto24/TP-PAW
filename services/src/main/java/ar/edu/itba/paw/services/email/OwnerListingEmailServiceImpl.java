package ar.edu.itba.paw.services.email;


import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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
import ar.edu.itba.paw.models.email.CarPausedByAdminOwnerEmailPayload;
import ar.edu.itba.paw.models.email.CarPausedMissingCbuOwnerEmailPayload;
import ar.edu.itba.paw.models.email.CarRejectedByAdminOwnerEmailPayload;
import ar.edu.itba.paw.models.email.CarValidatedByAdminOwnerEmailPayload;
import ar.edu.itba.paw.models.email.OwnerBlockedEmailPayload;
import ar.edu.itba.paw.models.email.OwnerPaymentProofReceivedEmailPayload;
import ar.edu.itba.paw.models.email.OwnerRefundProofObligationEmailPayload;
import ar.edu.itba.paw.models.email.ReservationMailPayload;
import ar.edu.itba.paw.models.util.rules.CbuRules;
import ar.edu.itba.paw.mail.MailDispatchSupport;
import ar.edu.itba.paw.mail.MailPublicUrls;
import ar.edu.itba.paw.policy.ReservationTimingPolicy;

/** Owner-facing listing lifecycle + payment-proof / refund-proof / blocked emails. */
@Service
public final class OwnerListingEmailServiceImpl implements OwnerListingEmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OwnerListingEmailServiceImpl.class);

    private static final String LISTING_DELETION_TEMPLATE = "html/car-deleted-owner";
    private static final String RESERVATION_CANCELLATION_USER_TEMPLATE = "html/reservation-cancellation-rider";
    private static final String OWNER_PAYMENT_PROOF_RECEIVED_TEMPLATE = "html/owner-payment-proof-received";
    private static final String OWNER_REFUND_PROOF_OBLIGATION_TEMPLATE = "html/owner-refund-proof-obligation";
    private static final String OWNER_BLOCKED_TEMPLATE = "html/owner-blocked";
    private static final String LISTING_PAUSED_MISSING_CBU_TEMPLATE = "html/car-paused-missing-cbu-owner";
    private static final String LISTING_PAUSED_BY_ADMIN_TEMPLATE = "html/car-paused-by-admin-owner";
    private static final String LISTING_REJECTED_BY_ADMIN_TEMPLATE = "html/car-rejected-by-admin-owner";
    private static final String LISTING_VALIDATED_BY_ADMIN_TEMPLATE = "html/car-validated-by-admin-owner";

    private final MailDispatchSupport mailDispatch;
    private final MailPublicUrls mailPublicUrls;
    private final MessageSource emailMessageSource;
    private final TemplateEngine htmlTemplateEngine;
    private final ReservationTimingPolicy reservationTimingPolicy;

    @Autowired
    public OwnerListingEmailServiceImpl(
            final MailDispatchSupport mailDispatch,
            final MailPublicUrls mailPublicUrls,
            @Qualifier("emailMessageSource") final MessageSource emailMessageSource,
            @Qualifier("emailTemplateEngine") final TemplateEngine htmlTemplateEngine,
            final ReservationTimingPolicy reservationTimingPolicy) {
        this.mailDispatch = mailDispatch;
        this.mailPublicUrls = mailPublicUrls;
        this.emailMessageSource = emailMessageSource;
        this.htmlTemplateEngine = htmlTemplateEngine;
        this.reservationTimingPolicy = reservationTimingPolicy;
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendListingDeletionEmail(final List<ReservationMailPayload> reservationsToCancel) {
        if (reservationsToCancel == null || reservationsToCancel.isEmpty()) {
            LOGGER.atError().log("sendListingDeletionEmail called with null or empty reservationsToCancel");
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
            // We send a minimal rider-only cancellation here (the owner gets the listing-deletion
            // email below). The intro copy mirrors the generic "cancelled" status used by the
            // facade's reservation cancellation flow.
            try {
                final Locale riderLocale = reservationPayload.getMessageLocale();
                final Context riderCtx = new Context(riderLocale);
                mailDispatch.setHtmlLangFromLocale(riderCtx, riderLocale);
                riderCtx.setVariable("reservationTotal", reservationPayload.getReservationTotal());
                riderCtx.setVariable("riderFullName", reservationPayload.getRiderFullName());
                riderCtx.setVariable("vehicleLabel", reservationPayload.getVehicleLabel());
                riderCtx.setVariable("startDateFormatted",
                        mailDispatch.formatWallDateTime(reservationPayload.getStartDate(), riderLocale));
                riderCtx.setVariable("endDateFormatted",
                        mailDispatch.formatWallDateTime(reservationPayload.getEndDate(), riderLocale));
                riderCtx.setVariable("ownerFullName", reservationPayload.getOwnerFullName());
                riderCtx.setVariable("ownerEmail",
                        mailDispatch.nonBlankEmailForDisplay(reservationPayload.getOwnerEmail(), riderLocale));
                riderCtx.setVariable("ownerCbu",
                        reservationPayload.getOwnerCbu() != null ? reservationPayload.getOwnerCbu() : "");
                final String delivery = reservationPayload.getDeliveryLocation();
                final boolean hasDelivery = delivery != null && !delivery.isBlank();
                riderCtx.setVariable("hasDeliveryLocation", hasDelivery);
                riderCtx.setVariable("deliveryLocation", hasDelivery ? delivery : "");
                riderCtx.setVariable("ctaUrl",
                        mailPublicUrls.absolutePath("/my-reservations/" + reservationPayload.getReservationId()));
                riderCtx.setVariable("cancellationIntroRider",
                        emailMessageSource.getMessage(
                                "mail.reservationCancelled.intro.generic", null, riderLocale));
                sendReservationCancellationToClient(reservationPayload, riderCtx,
                        reservationPayload.getRecipientEmail());
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
            mailDispatch.setHtmlLangFromLocale(ownerCtx, ownerLocale);
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

    private void sendListingDeletionToOwner(final ReservationMailPayload payload, final Context ctx)
            throws EmailMessagingException {
        mailDispatch.runMail(() -> {
            ctx.setVariable("ownerEmail",
                    mailDispatch.nonBlankEmailForDisplay(payload.getOwnerEmail(), payload.getOwnerMailLocale()));
            final String htmlContent = htmlTemplateEngine.process(LISTING_DELETION_TEMPLATE, ctx);
            final String subject = emailMessageSource.getMessage(
                    "mail.carDeleted.subject",
                    new Object[] { payload.getVehicleLabel() },
                    payload.getOwnerMailLocale());
            mailDispatch.sendEmail(payload.getOwnerEmail(), subject, htmlContent);
        });
    }

    private void sendReservationCancellationToClient(
            final ReservationMailPayload payload, final Context ctx, final String to)
            throws EmailMessagingException {
        mailDispatch.runMail(() -> {
            final String htmlContent = htmlTemplateEngine.process(RESERVATION_CANCELLATION_USER_TEMPLATE, ctx);
            final String subject = emailMessageSource.getMessage(
                    "mail.reservationCancelled.subject",
                    new Object[] { payload.getVehicleLabel() },
                    payload.getMessageLocale());
            mailDispatch.sendEmail(to, subject, htmlContent);
        });
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
        mailDispatch.setHtmlLangFromLocale(ctx, mailLocale);
        ctx.setVariable("ownerFullName", payload.getOwnerFullName());
        ctx.setVariable("riderFullName", payload.getRiderFullName());
        ctx.setVariable("riderEmail", mailDispatch.nonBlankEmailForDisplay(payload.getRiderEmail(), mailLocale));
        ctx.setVariable("vehicleLabel", payload.getVehicleLabel());
        ctx.setVariable("reservationTotal", payload.getReservationTotal());
        ctx.setVariable("startDateFormatted", mailDispatch.formatWallDateTime(payload.getStartDate(), mailLocale));
        ctx.setVariable("endDateFormatted", mailDispatch.formatWallDateTime(payload.getEndDate(), mailLocale));
        ctx.setVariable("ctaUrl",
                mailPublicUrls.absolutePath("/my-reservations/" + payload.getReservationId() + "?role=owner"));
        final String to = payload.getRecipientEmail();
        try {
            mailDispatch.runMail(() -> {
                final String htmlContent = htmlTemplateEngine.process(OWNER_PAYMENT_PROOF_RECEIVED_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage(
                        "mail.paymentProofSubmittedOwner.subject",
                        new Object[] { payload.getVehicleLabel() }, mailLocale);
                mailDispatch.sendEmail(to, subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(to).addArgument(payload.getReservationId())
                    .log("Owner payment-proof email queued for {} (reservation id={})");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(payload.getReservationId())
                    .log("Failed to queue owner payment-proof email (reservation id={})");
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendOwnerRefundProofObligationEmail(final OwnerRefundProofObligationEmailPayload payload) {
        if (payload == null) {
            LOGGER.atError().log("sendOwnerRefundProofObligationEmail called with null payload");
            return;
        }
        final Locale mailLocale = payload.getMessageLocale();
        final Context ctx = new Context(mailLocale);
        mailDispatch.setHtmlLangFromLocale(ctx, mailLocale);
        ctx.setVariable("ownerFullName", payload.getOwnerFullName());
        ctx.setVariable("vehicleLabel", payload.getVehicleLabel());
        ctx.setVariable("reservationTotal", payload.getReservationTotal());
        ctx.setVariable("startDateFormatted", mailDispatch.formatWallDateTime(payload.getStartDate(), mailLocale));
        ctx.setVariable("endDateFormatted", mailDispatch.formatWallDateTime(payload.getEndDate(), mailLocale));
        ctx.setVariable("refundDeadlineFormatted",
                mailDispatch.formatWallDateTime(payload.getRefundProofDeadlineAt(), mailLocale));
        ctx.setVariable("dueReminder", payload.isDueReminder());
        ctx.setVariable("dueRefundReminderHours", reservationTimingPolicy.getPaymentProofReminderLeadHours());
        ctx.setVariable("ctaUrl",
                mailPublicUrls.absolutePath("/my-reservations/" + payload.getReservationId() + "?role=owner"));
        final String to = payload.getRecipientEmail();
        try {
            mailDispatch.runMail(() -> {
                final String htmlContent = htmlTemplateEngine.process(OWNER_REFUND_PROOF_OBLIGATION_TEMPLATE, ctx);
                final String subjectKey = payload.isDueReminder()
                        ? "mail.ownerRefundProof.subjectReminder"
                        : "mail.ownerRefundProof.subjectInitial";
                final String subject = emailMessageSource.getMessage(
                        subjectKey, new Object[] { payload.getVehicleLabel() }, mailLocale);
                mailDispatch.sendEmail(to, subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(to).addArgument(payload.getReservationId())
                    .log("Owner refund-proof email queued for {} (reservation id={})");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(payload.getReservationId())
                    .log("Failed to queue owner refund-proof email (reservation id={})");
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendOwnerBlockedEmail(final OwnerBlockedEmailPayload payload) {
        if (payload == null) {
            LOGGER.atError().log("sendOwnerBlockedEmail called with null payload");
            return;
        }
        final Locale mailLocale = payload.getMessageLocale();
        final Context ctx = new Context(mailLocale);
        mailDispatch.setHtmlLangFromLocale(ctx, mailLocale);
        ctx.setVariable("ownerFullName", payload.getOwnerFullName());
        // Adapt domain rows into a view-only structure with pre-formatted deadlines so the template stays free of formatting logic.
        final List<OwnerBlockedTemplateRow> rows = payload.getOverdueReservations().stream()
                .map(r -> new OwnerBlockedTemplateRow(
                        r.getReservationId(),
                        r.getVehicleLabel(),
                        mailDispatch.formatWallDateTime(r.getRefundProofDeadlineAt(), mailLocale),
                        r.getReservationTotal()))
                .collect(Collectors.toList());
        ctx.setVariable("overdueReservations", rows);
        ctx.setVariable("ctaUrl", mailPublicUrls.absolutePath("/my-reservations?role=owner"));
        final String to = payload.getRecipientEmail();
        try {
            mailDispatch.runMail(() -> {
                final String htmlContent = htmlTemplateEngine.process(OWNER_BLOCKED_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage(
                        "mail.ownerBlocked.subject", new Object[0], mailLocale);
                mailDispatch.sendEmail(to, subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(to).addArgument(rows.size())
                    .log("Owner-blocked email queued for {} ({} overdue refund proofs)");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(to).log("Failed to queue owner-blocked email for {}");
        }
    }

    /** View-only DTO for the Thymeleaf {@code overdueReservations} list; deadline is pre-formatted in the recipient's locale. */
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
    @Async("mailTaskExecutor")
    public void sendListingPausedDueToMissingCbu(final CarPausedMissingCbuOwnerEmailPayload payload) {
        final String ownerEmail = payload.getOwnerEmail();
        if (ownerEmail == null || ownerEmail.isBlank()) {
            LOGGER.atWarn().log("Skipping listing-paused-CBU email: missing recipient");
            return;
        }
        final Locale locale = payload.getMessageLocale();
        final String vehicleLabel = payload.getVehicleLabel();
        final long carId = payload.getCarId();
        final Context ctx = new Context(locale);
        mailDispatch.setHtmlLangFromLocale(ctx, locale);
        ctx.setVariable("ownerFullName", payload.getOwnerFullName());
        ctx.setVariable("vehicleLabel", vehicleLabel);
        ctx.setVariable("cbuRequiredDigits", CbuRules.REQUIRED_DIGIT_LENGTH);
        ctx.setVariable("ctaUrl", mailPublicUrls.absolutePath("/my-cars/car/" + carId));
        ctx.setVariable("profileUrl", mailPublicUrls.absolutePath("/profile"));
        try {
            mailDispatch.runMail(() -> {
                final String htmlContent = htmlTemplateEngine.process(LISTING_PAUSED_MISSING_CBU_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage(
                        "mail.carPausedMissingCbu.subject", new Object[] { vehicleLabel }, locale);
                mailDispatch.sendEmail(ownerEmail, subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(ownerEmail).addArgument(carId)
                    .log("Car paused (missing CBU) email sent to {} (car id={})");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(carId)
                    .log("Failed to send car paused (missing CBU) email (car id={})");
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendCarPausedByAdmin(final CarPausedByAdminOwnerEmailPayload payload) {
        final String ownerEmail = payload.getOwnerEmail();
        if (ownerEmail == null || ownerEmail.isBlank()) {
            LOGGER.atWarn().log("Skipping listing-paused-by-admin email: missing recipient");
            return;
        }
        final Locale locale = payload.getMessageLocale();
        final String vehicleLabel = payload.getVehicleLabel();
        final long carId = payload.getCarId();
        final Context ctx = new Context(locale);
        mailDispatch.setHtmlLangFromLocale(ctx, locale);
        ctx.setVariable("ownerFullName", payload.getOwnerFullName());
        ctx.setVariable("vehicleLabel", vehicleLabel);
        ctx.setVariable("ctaUrl", mailPublicUrls.absolutePath("/my-cars/car/" + carId));
        try {
            mailDispatch.runMail(() -> {
                final String htmlContent = htmlTemplateEngine.process(LISTING_PAUSED_BY_ADMIN_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage(
                        "mail.carPausedByAdmin.subject", new Object[] { vehicleLabel }, locale);
                mailDispatch.sendEmail(ownerEmail, subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(ownerEmail).addArgument(carId)
                    .log("Car paused by admin email sent to {} (car id={})");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(carId)
                    .log("Failed to send car paused by admin email (car id={})");
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendCarRejectedByAdmin(final CarRejectedByAdminOwnerEmailPayload payload) {
        if (payload == null) {
            LOGGER.atError().log("sendCarRejectedByAdmin called with null payload");
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
        mailDispatch.setHtmlLangFromLocale(ctx, locale);
        ctx.setVariable("ownerFullName", payload.getOwnerFullName());
        ctx.setVariable("vehicleLabel", vehicleLabel);
        ctx.setVariable("brandName", payload.getBrandName() != null ? payload.getBrandName() : "");
        ctx.setVariable("modelName", payload.getModelName() != null ? payload.getModelName() : "");
        ctx.setVariable("brandRejected", payload.isBrandRejected());
        ctx.setVariable("modelRejected", payload.isModelRejected());
        ctx.setVariable("ctaUrl", mailPublicUrls.absolutePath("/my-cars"));
        try {
            mailDispatch.runMail(() -> {
                final String htmlContent = htmlTemplateEngine.process(LISTING_REJECTED_BY_ADMIN_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage(
                        "mail.carRejectedByAdmin.subject", new Object[] { vehicleLabel }, locale);
                mailDispatch.sendEmail(ownerEmail, subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(ownerEmail).addArgument(carId)
                    .log("Catalog entry rejected by admin email sent to {} (car id={})");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(carId)
                    .log("Failed to send catalog entry rejected by admin email (car id={})");
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendCarValidatedByAdmin(final CarValidatedByAdminOwnerEmailPayload payload) {
        if (payload == null) {
            LOGGER.atError().log("sendCarValidatedByAdmin called with null payload");
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
        mailDispatch.setHtmlLangFromLocale(ctx, locale);
        ctx.setVariable("ownerFullName", payload.getOwnerFullName());
        ctx.setVariable("vehicleLabel", vehicleLabel);
        ctx.setVariable("brandName", payload.getBrandName() != null ? payload.getBrandName() : "");
        ctx.setVariable("modelName", payload.getModelName() != null ? payload.getModelName() : "");
        ctx.setVariable("brandValidated", payload.isBrandValidated());
        ctx.setVariable("ctaUrl", mailPublicUrls.absolutePath("/my-cars"));
        try {
            mailDispatch.runMail(() -> {
                final String htmlContent = htmlTemplateEngine.process(LISTING_VALIDATED_BY_ADMIN_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage(
                        "mail.carValidatedByAdmin.subject", new Object[] { vehicleLabel }, locale);
                mailDispatch.sendEmail(ownerEmail, subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(ownerEmail).addArgument(carId)
                    .log("Catalog entry validated by admin email sent to {} (car id={})");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(carId)
                    .log("Failed to send catalog entry validated by admin email (car id={})");
        }
    }
}
