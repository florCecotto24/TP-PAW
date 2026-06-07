package ar.edu.itba.paw.webapp.validation.file;

import java.util.Locale;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import ar.edu.itba.paw.webapp.validation.constraint.file.MaxFileSize;
import ar.edu.itba.paw.webapp.validation.support.FileSizeLimitProvider;

/**
 * Bean Validation engine for {@link MaxFileSize}. Resolves the upper bound either from the
 * annotation's compile-time {@code maxBytes()} or from a {@link FileSizeLimitProvider} Spring bean
 * referenced by {@code sizeProvider()}; in the latter case the violation message is also rebuilt to
 * carry the runtime {@code maxMb} as its first argument so the message bundle key (default
 * {@code validation.file.tooLarge}) can be {@code "... at most {0} MB"}.
 */
@Component
public final class MaxFileSizeValidator implements ConstraintValidator<MaxFileSize, MultipartFile> {

    private final ApplicationContext applicationContext;
    private final MessageSource messageSource;

    private long staticMaxBytes;
    private Class<? extends FileSizeLimitProvider> providerClass;
    private String rawMessage;

    @Autowired
    public MaxFileSizeValidator(final ApplicationContext applicationContext, final MessageSource messageSource) {
        this.applicationContext = applicationContext;
        this.messageSource = messageSource;
    }

    @Override
    public void initialize(final MaxFileSize constraint) {
        this.staticMaxBytes = constraint.maxBytes();
        this.providerClass = constraint.sizeProvider();
        this.rawMessage = constraint.message();
    }

    @Override
    public boolean isValid(final MultipartFile value, final ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        final long actualSize = value.getSize();
        if (actualSize < 0L) {
            return true;
        }
        final FileSizeLimitProvider provider = resolveProvider();
        final long maxBytes = provider != null ? provider.getMaxBytes() : staticMaxBytes;
        if (maxBytes <= 0L || actualSize <= maxBytes) {
            return true;
        }
        final int maxMb = provider != null
                ? provider.getMaxMegabytesRoundedUp()
                : (int) Math.max(1L, (maxBytes + 1_048_575L) / 1_048_576L);
        emitMessage(context, maxMb);
        return false;
    }

    private FileSizeLimitProvider resolveProvider() {
        if (providerClass == null || providerClass == FileSizeLimitProvider.None.class) {
            return null;
        }
        return applicationContext.getBean(providerClass);
    }

    private void emitMessage(final ConstraintValidatorContext context, final int maxMb) {
        context.disableDefaultConstraintViolation();
        final String template = resolveTemplate(maxMb);
        context.buildConstraintViolationWithTemplate(template).addConstraintViolation();
    }

    private String resolveTemplate(final int maxMb) {
        // Bean Validation message templates are wrapped in '{key}' to mean "resolve via bundle". We
        // honour that pattern so callers can use either a literal message or a bundle key.
        if (rawMessage != null && rawMessage.startsWith("{") && rawMessage.endsWith("}")) {
            final String key = rawMessage.substring(1, rawMessage.length() - 1);
            final Locale locale = LocaleContextHolder.getLocale();
            return messageSource.getMessage(key, new Object[] { maxMb }, key, locale);
        }
        return rawMessage != null ? rawMessage.replace("{0}", Integer.toString(maxMb)) : "";
    }
}
