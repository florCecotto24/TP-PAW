package ar.edu.itba.paw.webapp.support;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ElementKind;
import javax.validation.Path;
import javax.validation.Validator;
import javax.validation.metadata.ConstraintDescriptor;

import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

/**
 * Runs Bean Validation with explicit groups on REST request bodies, then optional Spring {@link org.springframework.validation.Validator}s
 * (same cross-field rules as legacy MVC forms).
 */
@Component
public final class FormValidationSupport {

    private final Validator validator;
    private final List<org.springframework.validation.Validator> springValidators;

    public FormValidationSupport(
            final Validator validator,
            final List<org.springframework.validation.Validator> springValidators) {
        this.validator = validator;
        this.springValidators = springValidators;
    }

    public <T> void validate(final T form, final Class<?>... groups) {
        final Set<ConstraintViolation<T>> violations = groups.length == 0
                ? validator.validate(form)
                : validator.validate(form, groups);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        validateWithSpringValidators(form);
    }

    private <T> void validateWithSpringValidators(final T form) {
        final BindingResult errors = new BeanPropertyBindingResult(form, form.getClass().getSimpleName());
        for (final org.springframework.validation.Validator springValidator : springValidators) {
            if (springValidator.supports(form.getClass())) {
                springValidator.validate(form, errors);
            }
        }
        if (errors.hasErrors()) {
            throw toConstraintViolationException(errors);
        }
    }

    private static ConstraintViolationException toConstraintViolationException(final BindingResult errors) {
        final Set<ConstraintViolation<?>> violations = new LinkedHashSet<>();
        for (final FieldError fieldError : errors.getFieldErrors()) {
            violations.add(new SpringFieldConstraintViolation(
                    fieldError.getField(),
                    fieldError.getDefaultMessage(),
                    errors.getTarget()));
        }
        return new ConstraintViolationException(violations);
    }

    private static final class SpringFieldConstraintViolation implements ConstraintViolation<Object> {

        private final String field;
        private final String message;
        private final Object rootBean;

        private SpringFieldConstraintViolation(final String field, final String message, final Object rootBean) {
            this.field = field;
            this.message = message;
            this.rootBean = rootBean;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public String getMessageTemplate() {
            return message;
        }

        @Override
        public Object getRootBean() {
            return rootBean;
        }

        @Override
        public Class<Object> getRootBeanClass() {
            @SuppressWarnings("unchecked")
            final Class<Object> type = (Class<Object>) rootBean.getClass();
            return type;
        }

        @Override
        public Object getLeafBean() {
            return rootBean;
        }

        @Override
        public Object[] getExecutableParameters() {
            return null;
        }

        @Override
        public Object getExecutableReturnValue() {
            return null;
        }

        @Override
        public Path getPropertyPath() {
            return () -> {
                final List<Path.Node> nodes = new ArrayList<>();
                nodes.add(new Path.Node() {
                    @Override
                    public String getName() {
                        return field;
                    }

                    @Override
                    public boolean isInIterable() {
                        return false;
                    }

                    @Override
                    public Integer getIndex() {
                        return null;
                    }

                    @Override
                    public Object getKey() {
                        return null;
                    }

                    @Override
                    public ElementKind getKind() {
                        return ElementKind.PROPERTY;
                    }

                    @Override
                    public <T extends Path.Node> T as(final Class<T> nodeType) {
                        return nodeType.cast(this);
                    }
                });
                return nodes.iterator();
            };
        }

        @Override
        public Object getInvalidValue() {
            return null;
        }

        @Override
        public ConstraintDescriptor<?> getConstraintDescriptor() {
            return null;
        }

        @Override
        public <U> U unwrap(final Class<U> type) {
            throw new UnsupportedOperationException();
        }
    }
}
