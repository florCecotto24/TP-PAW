package ar.edu.itba.paw.services.email;

import ar.edu.itba.paw.models.email.admin.AdminInvitationEmailPayload;
import ar.edu.itba.paw.models.email.admin.AdminPromotedEmailPayload;
import ar.edu.itba.paw.models.email.user.EmailVerificationCodeEmailPayload;
import ar.edu.itba.paw.models.email.user.MigratedUserPasswordEmailPayload;
import ar.edu.itba.paw.models.email.user.PasswordResetCodeEmailPayload;

/**
 * Transactional emails for the user identity / auth lifecycle (verification, password reset,
 * migrated-password, admin invitation/promotion). Extracted from {@link EmailService} so
 * that the latter stays focused on reservation/listing notifications.
 *
 * {@link EmailService} keeps these methods declared (back-compat) and delegates here.
 */
public interface UserAccountEmailService {

    /** See {@link EmailService#sendEmailVerificationCode(EmailVerificationCodeEmailPayload)}. */
    void sendEmailVerificationCode(EmailVerificationCodeEmailPayload payload);

    /** See {@link EmailService#sendMigratedUserPassword(MigratedUserPasswordEmailPayload)}. */
    void sendMigratedUserPassword(MigratedUserPasswordEmailPayload payload);

    /** See {@link EmailService#sendAdminInvitation(AdminInvitationEmailPayload)}. */
    void sendAdminInvitation(AdminInvitationEmailPayload payload);

    /** See {@link EmailService#sendAdminPromoted(AdminPromotedEmailPayload)}. */
    void sendAdminPromoted(AdminPromotedEmailPayload payload);

    /** See {@link EmailService#sendPasswordResetCode(PasswordResetCodeEmailPayload)}. */
    void sendPasswordResetCode(PasswordResetCodeEmailPayload payload);
}
