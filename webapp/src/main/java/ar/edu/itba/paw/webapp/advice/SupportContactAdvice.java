package ar.edu.itba.paw.webapp.advice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes the application-wide support contact email as a model attribute, so every JSP can render
 * it (and pass it as a {@code <spring:message arguments="${supportEmail}"/>} argument) without
 * hard-coding the address. The actual value lives in {@code application.properties} under
 * {@code app.support.email}.
 */
@ControllerAdvice
public final class SupportContactAdvice {

    private final String supportEmail;

    public SupportContactAdvice(@Value("${app.support.email}") final String supportEmail) {
        this.supportEmail = supportEmail;
    }

    @ModelAttribute("supportEmail")
    public String supportEmail() {
        return supportEmail;
    }
}
