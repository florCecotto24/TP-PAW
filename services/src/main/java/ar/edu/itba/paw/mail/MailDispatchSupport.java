package ar.edu.itba.paw.mail;


import java.time.OffsetDateTime;
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
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;

import ar.edu.itba.paw.exception.email.EmailMessagingException;
import ar.edu.itba.paw.models.util.time.WallDateTimeDisplayFormat;

import ar.edu.itba.paw.services.email.EmailServiceImpl;
/**
 * Cross-cutting mail-dispatch primitives shared by every {@code *EmailServiceImpl}.
 *
 * <p>Encapsulates the JavaMail/MimeMessage assembly, exception wrapping, and the small set of
 * locale-aware Thymeleaf helpers (HTML lang attribute, optional-email fallback,
 * wall-time formatting) so that domain-specific email services don't reach into JavaMail or
 * duplicate boilerplate.</p>
 */
@Component
public final class MailDispatchSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailDispatchSupport.class);

    /**
     * Mail templating + JavaMail dispatch lambda. Callers only need to surface
     * {@link EmailMessagingException} (rethrown straight through {@link #runMail(MailAction)}); any
     * other failure inside the lambda is a {@link RuntimeException} (Thymeleaf
     * {@code TemplateProcessingException}, Spring {@code NoSuchMessageException}, etc.) and is
     * wrapped by {@code runMail}.
     */
    @FunctionalInterface
    public interface MailAction {
        void run() throws EmailMessagingException;
    }

    private final Environment environment;
    private final JavaMailSender mailSender;
    private final MessageSource emailMessageSource;
    private final MailPublicUrls mailPublicUrls;

    @Autowired
    public MailDispatchSupport(
            final Environment environment,
            final JavaMailSender mailSender,
            @Qualifier("emailMessageSource") final MessageSource emailMessageSource,
            final MailPublicUrls mailPublicUrls) {
        this.environment = environment;
        this.mailSender = mailSender;
        this.emailMessageSource = emailMessageSource;
        this.mailPublicUrls = mailPublicUrls;
    }

    /**
     * Runs {@code action}; rethrows {@link EmailMessagingException} as-is and wraps any
     * {@link RuntimeException} (Thymeleaf template parse/render errors, Spring
     * {@code NoSuchMessageException}, etc.) into one so the calling {@code *EmailServiceImpl} only
     * has to catch a single typed failure.
     */
    public void runMail(final MailAction action) throws EmailMessagingException {
        try {
            action.run();
        } catch (final EmailMessagingException e) {
            throw e;
        } catch (final RuntimeException e) {
            throw new EmailMessagingException(e);
        }
    }

    /**
     * Sends an HTML email with the embedded {@code logo} CID image. Wraps any underlying
     * {@link MessagingException} into {@link EmailMessagingException} so the caller's
     * {@link #runMail(MailAction)} block can detect the failure (it was previously swallowed
     * here, which made wrong-headers / missing-logo failures invisible at the calling layer).
     *
     * @throws EmailMessagingException when JavaMail cannot assemble or dispatch the message
     */
    public void sendEmail(final String to, final String subject, final String htmlBody) throws EmailMessagingException {
        final MimeMessage message = this.mailSender.createMimeMessage();
        try {
            final MimeMessageHelper helper = new MimeMessageHelper(
                    message, MimeMessageHelper.MULTIPART_MODE_RELATED, "UTF-8");
            helper.setTo(to);
            final String from = environment.getProperty(
                    "mail.from.address",
                    environment.getProperty("mail.server.username", "noreply@localhost"));
            message.setFrom(from);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            final ClassPathResource logo = new ClassPathResource("static/images/logo_transparent.png");
            helper.addInline("logo", logo);
            mailSender.send(message);
        } catch (final MessagingException e) {
            LOGGER.atError().setCause(e).addArgument(to).log("Error sending email to {}");
            throw new EmailMessagingException(e);
        }
    }

    /**
     * Sets the variables every Thymeleaf mail template expects in addition to its own payload:
     * {@code htmlLang} for the {@code <html lang="...">} attribute and {@code homeUrl} for the
     * clickable header logo. Single source of truth so every {@code *EmailServiceImpl} stays in
     * sync without having to remember to set both per template.
     */
    public void setHtmlLangFromLocale(final Context ctx, final Locale mailLocale) {
        final String lang = mailLocale != null
                && mailLocale.getLanguage() != null
                && !mailLocale.getLanguage().isBlank()
                ? mailLocale.getLanguage()
                : "en";
        ctx.setVariable("htmlLang", lang);
        ctx.setVariable("homeUrl", mailPublicUrls.absolutePath("/"));
    }

    /** Avoids printing the literal "null" in Thymeleaf for optional contact fields. */
    public String nonBlankEmailForDisplay(final String email, final Locale mailLocale) {
        if (email != null && !email.isBlank()) {
            return email;
        }
        return emailMessageSource.getMessage("mail.common.notAvailable", null, mailLocale);
    }

    /** Wall-time display string in the supplied locale (falls back to English). */
    public String formatWallDateTime(final OffsetDateTime dateTime, final Locale messageLocale) {
        final Locale locale = messageLocale != null ? messageLocale : Locale.ENGLISH;
        return WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(dateTime, locale);
    }
}
