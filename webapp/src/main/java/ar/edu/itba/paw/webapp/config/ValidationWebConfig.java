package ar.edu.itba.paw.webapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the shared {@link LocalValidatorFactoryBean} as the MVC {@link Validator} for {@code @Valid} on controllers.
 */
@Configuration
public class ValidationWebConfig implements WebMvcConfigurer {

    private final LocalValidatorFactoryBean localValidatorFactoryBean;

    public ValidationWebConfig(final LocalValidatorFactoryBean localValidatorFactoryBean) {
        this.localValidatorFactoryBean = localValidatorFactoryBean;
    }

    @Override
    public Validator getValidator() {
        return localValidatorFactoryBean;
    }
}
