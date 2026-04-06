package ar.edu.itba.paw.webapp.util;

import ar.edu.itba.paw.exception.RydenException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class LocaleMessages {

    private final MessageSource messageSource;

    @Autowired
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
