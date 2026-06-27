package ar.edu.itba.paw.webapp.config;

import java.util.Locale;

import javax.validation.MessageInterpolator;
import javax.validation.Validation;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.validation.ValidationConfig;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.hibernate.validator.resourceloading.PlatformResourceBundleLocator;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Localized Bean Validation messages for JAX-RS ({@code messages*.properties} keys on forms).
 */
@Provider
public final class ValidationContextResolver implements ContextResolver<ValidationConfig> {

    @Override
    public ValidationConfig getContext(final Class<?> type) {
        final ValidationConfig config = new ValidationConfig();
        config.messageInterpolator(new LocaleAwareMessageInterpolator());
        return config;
    }

    private static final class LocaleAwareMessageInterpolator implements MessageInterpolator {

        private final MessageInterpolator delegate;

        private LocaleAwareMessageInterpolator() {
            delegate = Validation.byDefaultProvider()
                    .configure()
                    .messageInterpolator(new ResourceBundleMessageInterpolator(
                            new PlatformResourceBundleLocator("messages/messages")))
                    .buildValidatorFactory()
                    .getMessageInterpolator();
        }

        @Override
        public String interpolate(final String messageTemplate, final Context context) {
            return delegate.interpolate(messageTemplate, context, LocaleContextHolder.getLocale());
        }

        @Override
        public String interpolate(final String messageTemplate, final Context context, final Locale locale) {
            return delegate.interpolate(messageTemplate, context, LocaleContextHolder.getLocale());
        }
    }
}
