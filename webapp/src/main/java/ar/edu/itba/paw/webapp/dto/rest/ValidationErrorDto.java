package ar.edu.itba.paw.webapp.dto.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.validation.ConstraintViolation;

import ar.edu.itba.paw.webapp.api.common.VndMediaType;

/**
 * Validation error body ({@code openapi.yaml} {@code ValidationErrorDto}).
 */
public final class ValidationErrorDto {

    private int status = 400;
    private String message;
    private List<FieldError> errors = new ArrayList<>();

    public ValidationErrorDto() {
    }

    public static String mediaType() {
        return VndMediaType.VALIDATION_ERROR_V1_JSON;
    }

    public static ValidationErrorDto fromConstraintViolations(
            final Collection<ConstraintViolation<?>> violations) {
        final ValidationErrorDto dto = new ValidationErrorDto();
        dto.message = "Validation failed.";
        for (final ConstraintViolation<?> violation : violations) {
            final FieldError fieldError = new FieldError();
            fieldError.field = normalizePropertyPath(violation.getPropertyPath().toString());
            fieldError.message = violation.getMessage();
            dto.errors.add(fieldError);
        }
        return dto;
    }

    private static String normalizePropertyPath(final String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return rawPath;
        }
        final int lastDot = rawPath.lastIndexOf('.');
        return lastDot >= 0 ? rawPath.substring(lastDot + 1) : rawPath;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(final int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public List<FieldError> getErrors() {
        return errors;
    }

    public void setErrors(final List<FieldError> errors) {
        this.errors = errors;
    }

    public static final class FieldError {
        private String field;
        private String message;

        public String getField() {
            return field;
        }

        public void setField(final String field) {
            this.field = field;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(final String message) {
            this.message = message;
        }
    }
}
