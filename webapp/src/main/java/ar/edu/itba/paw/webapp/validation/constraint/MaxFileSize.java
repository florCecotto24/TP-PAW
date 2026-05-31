package ar.edu.itba.paw.webapp.validation.constraint;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.MaxFileSizeValidator;
import ar.edu.itba.paw.webapp.validation.support.FileSizeLimitProvider;

/**
 * Validates that a {@code MultipartFile}'s byte length is within an upper bound. Empty / null files
 * are skipped — pair with {@link NotEmptyFile} when presence is required. The bound is either a
 * compile-time {@code maxBytes()} or a runtime value resolved by a {@link FileSizeLimitProvider}
 * Spring bean (useful when the limit comes from {@code application.properties}).
 */
@Documented
@Constraint(validatedBy = MaxFileSizeValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxFileSize {

    /** Default message key; gets the {@code maxMb} value as {@code {0}} when interpolated. */
    String message() default "{validation.file.tooLarge}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** Static upper bound in bytes; ignored when {@link #sizeProvider()} resolves to a non-default class. */
    long maxBytes() default 0L;

    /**
     * Class of a Spring-managed {@link FileSizeLimitProvider}; when set, its
     * {@link FileSizeLimitProvider#getMaxBytes()} overrides {@link #maxBytes()}.
     */
    Class<? extends FileSizeLimitProvider> sizeProvider() default FileSizeLimitProvider.None.class;
}
