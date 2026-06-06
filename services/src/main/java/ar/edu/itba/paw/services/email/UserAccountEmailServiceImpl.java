package ar.edu.itba.paw.services.email;


import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import ar.edu.itba.paw.exception.email.EmailMessagingException;
import ar.edu.itba.paw.models.email.AdminInvitationEmailPayload;
import ar.edu.itba.paw.models.email.AdminPromotedEmailPayload;
import ar.edu.itba.paw.models.email.EmailVerificationCodeEmailPayload;
import ar.edu.itba.paw.models.email.MigratedUserPasswordEmailPayload;
import ar.edu.itba.paw.models.email.PasswordResetCodeEmailPayload;
import ar.edu.itba.paw.mail.MailDispatchSupport;
import ar.edu.itba.paw.mail.MailPublicUrls;

/** Identity / auth lifecycle emails. Extracted from {@code EmailServiceImpl}. */
@Service
public final class UserAccountEmailServiceImpl implements UserAccountEmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserAccountEmailServiceImpl.class);

    private static final String EMAIL_VERIFICATION_TEMPLATE = "html/email-verification-code";
    private static final String MIGRATED_PASSWORD_TEMPLATE = "html/migrated-password";
    private static final String PASSWORD_RESET_TEMPLATE = "html/password-reset-code";
    private static final String ADMIN_INVITATION_TEMPLATE = "html/admin-invitation";
    private static final String ADMIN_PROMOTED_TEMPLATE = "html/admin-promoted";

    private final MailDispatchSupport mailDispatch;
    private final MailPublicUrls mailPublicUrls;
    private final MessageSource emailMessageSource;
    private final TemplateEngine htmlTemplateEngine;

    @Autowired
    public UserAccountEmailServiceImpl(
            final MailDispatchSupport mailDispatch,
            final MailPublicUrls mailPublicUrls,
            @Qualifier("emailMessageSource") final MessageSource emailMessageSource,
            @Qualifier("emailTemplateEngine") final TemplateEngine htmlTemplateEngine) {
        this.mailDispatch = mailDispatch;
        this.mailPublicUrls = mailPublicUrls;
        this.emailMessageSource = emailMessageSource;
        this.htmlTemplateEngine = htmlTemplateEngine;
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
        mailDispatch.setHtmlLangFromLocale(ctx, mailLocale);
        ctx.setVariable("code", code);
        ctx.setVariable("verifyUrl", mailPublicUrls.absolutePath("/verify-email"));

        try {
            mailDispatch.runMail(() -> {
                final String htmlContent = htmlTemplateEngine.process(EMAIL_VERIFICATION_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage(
                        "mail.emailVerification.subject", null, mailLocale);
                mailDispatch.sendEmail(to, subject, htmlContent);
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
        mailDispatch.setHtmlLangFromLocale(ctx, mailLocale);
        ctx.setVariable("plainPassword", plainPassword);
        ctx.setVariable("loginUrl", mailPublicUrls.absolutePath("/login"));
        try {
            mailDispatch.runMail(() -> {
                final String htmlContent = htmlTemplateEngine.process(MIGRATED_PASSWORD_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage(
                        "mail.migratedPassword.subject", null, mailLocale);
                mailDispatch.sendEmail(to, subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(to).log("Migrated user password email sent to {}");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(to).log("Failed to send migrated password email to {}");
        }
    }

    @Override
    @Transactional
    @Async("mailTaskExecutor")
    public void sendAdminInvitation(final AdminInvitationEmailPayload payload) {
        final String to = payload.getRecipientEmail();
        final String plainPassword = payload.getPlainPassword();
        if (to == null || to.isBlank() || plainPassword == null || plainPassword.isBlank()) {
            LOGGER.atError().log("sendAdminInvitation: missing to or password");
            return;
        }
        final Locale mailLocale = payload.getMessageLocale();
        final Context ctx = new Context(mailLocale);
        mailDispatch.setHtmlLangFromLocale(ctx, mailLocale);
        ctx.setVariable("recipientFullName", payload.getRecipientFullName());
        ctx.setVariable("plainPassword", plainPassword);
        ctx.setVariable("loginUrl", mailPublicUrls.absolutePath("/login"));
        try {
            mailDispatch.runMail(() -> {
                final String htmlContent = htmlTemplateEngine.process(ADMIN_INVITATION_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage(
                        "mail.adminInvitation.subject", null, mailLocale);
                mailDispatch.sendEmail(to, subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(to).log("Admin invitation email sent to {}");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(to).log("Failed to send admin invitation email to {}");
        }
    }

    @Override
    @Transactional
    @Async("mailTaskExecutor")
    public void sendAdminPromoted(final AdminPromotedEmailPayload payload) {
        if (payload == null) {
            LOGGER.atError().log("sendAdminPromoted called with null payload");
            return;
        }
        final String to = payload.getRecipientEmail();
        if (to == null || to.isBlank()) {
            LOGGER.atWarn().log("Skipping admin-promoted email: missing recipient");
            return;
        }
        final Locale mailLocale = payload.getMessageLocale();
        final Context ctx = new Context(mailLocale);
        mailDispatch.setHtmlLangFromLocale(ctx, mailLocale);
        ctx.setVariable("recipientFullName", payload.getRecipientFullName());
        ctx.setVariable("grantedByFullName", payload.getGrantedByFullName());
        ctx.setVariable("ctaUrl", mailPublicUrls.absolutePath("/admin/panel"));
        try {
            mailDispatch.runMail(() -> {
                final String htmlContent = htmlTemplateEngine.process(ADMIN_PROMOTED_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage(
                        "mail.adminPromoted.subject", null, mailLocale);
                mailDispatch.sendEmail(to, subject, htmlContent);
            });
            LOGGER.atInfo()
                    .addArgument(to)
                    .addArgument(payload.getTargetUserId())
                    .log("Admin promoted email sent to {} (target user id={})");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(to).log("Failed to send admin promoted email to {}");
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
        mailDispatch.setHtmlLangFromLocale(ctx, mailLocale);
        ctx.setVariable("code", code);
        ctx.setVariable("resetUrl", mailPublicUrls.absolutePath("/forgot-password/reset"));
        try {
            mailDispatch.runMail(() -> {
                final String htmlContent = htmlTemplateEngine.process(PASSWORD_RESET_TEMPLATE, ctx);
                final String subject = emailMessageSource.getMessage(
                        "mail.passwordReset.subject", null, mailLocale);
                mailDispatch.sendEmail(to, subject, htmlContent);
            });
            LOGGER.atInfo().addArgument(to).log("Password reset code sent to {}");
        } catch (final EmailMessagingException | RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(to).log("Failed to send password reset code to {}");
        }
    }
}
