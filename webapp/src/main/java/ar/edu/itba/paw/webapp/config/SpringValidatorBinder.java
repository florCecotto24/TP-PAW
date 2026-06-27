package ar.edu.itba.paw.webapp.config;

import javax.validation.Validator;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.springframework.stereotype.Component;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Supplies the Spring-managed {@link Validator} (custom constraint validators + message source)
 * to Jersey Bean Validation ({@code @Valid} on JAX-RS parameters).
 */
@Component
public final class SpringValidatorBinder extends AbstractBinder {

    private final Validator validator;

    public SpringValidatorBinder(final LocalValidatorFactoryBean localValidatorFactoryBean) {
        this.validator = localValidatorFactoryBean.getValidator();
    }

    @Override
    protected void configure() {
        bind(validator).to(Validator.class);
    }
}
