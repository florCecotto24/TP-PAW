package ar.edu.itba.paw.webapp.util;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.RydenException;

@Component
public final class LocaleMessages {

    private final MessageSource messageSource;

    public LocaleMessages(final MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String msg(final RydenException ex) {
        return msg(ex.getMessageCode(), ex.getMessageArgs());
    }

    public String msg(final String code, final Object... args) {
        final Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, code, locale);
    }
}
