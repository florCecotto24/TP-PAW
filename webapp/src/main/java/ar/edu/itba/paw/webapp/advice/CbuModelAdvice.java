package ar.edu.itba.paw.webapp.advice;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import ar.edu.itba.paw.models.util.CbuRules;

/** Exposes CBU digit length from {@link CbuRules} for forms that validate Argentine bank account numbers. */
@ControllerAdvice
public final class CbuModelAdvice {

    @ModelAttribute("cbuRequiredDigits")
    public int cbuRequiredDigits() {
        return CbuRules.REQUIRED_DIGIT_LENGTH;
    }
}
