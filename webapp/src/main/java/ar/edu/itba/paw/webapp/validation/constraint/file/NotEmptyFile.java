package ar.edu.itba.paw.webapp.validation.constraint.file;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.file.NotEmptyFileValidator;

/**
 * Validates that a {@code MultipartFile} is non-null and has at least one byte of content. The
 * default Bean Validation {@code @NotNull} / {@code @NotEmpty} constraints do not cover
 * {@link org.springframework.web.multipart.MultipartFile#isEmpty()}, so this constraint encapsulates
 * the canonical "file must be present" check so controllers can drop the {@code if (file == null || file.isEmpty())}
 * boilerplate in favour of a single declarative annotation on the form field.
 */
@Documented
@Constraint(validatedBy = NotEmptyFileValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface NotEmptyFile {

    String message() default "{validation.file.required}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
