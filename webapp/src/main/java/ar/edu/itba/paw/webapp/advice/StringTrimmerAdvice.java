package ar.edu.itba.paw.webapp.advice;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * Registers a global {@link StringTrimmerEditor} for every {@link WebDataBinder}, so {@code String}
 * fields bound from request parameters, path variables, and {@code @ModelAttribute} forms are
 * trimmed of leading/trailing whitespace before validation runs. Centralising this prevents the
 * pattern of repeating {@code form.getX().trim()} at every call site and avoids the latent bug
 * where {@code @NotBlank}/{@code @Size} validators see padded values from the user.
 *
 * Empty strings are preserved as-is (not converted to {@code null}) to keep existing form
 * semantics: {@code @NotBlank} already rejects blank values, and form fields that allow empty
 * input (e.g. optional updates that overwrite a value with the empty string) keep working.
 *
 * Runs at {@link org.springframework.core.Ordered#HIGHEST_PRECEDENCE} so the trimmer is the
 * first editor registered; controller-local {@code @InitBinder} methods still apply on top
 * (Spring composes editors per data binder).
 */
@ControllerAdvice
@Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public final class StringTrimmerAdvice {

    @InitBinder
    public void registerStringTrimmer(final WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(false));
    }
}
