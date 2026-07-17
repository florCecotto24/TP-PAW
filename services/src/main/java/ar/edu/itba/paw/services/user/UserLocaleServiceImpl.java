package ar.edu.itba.paw.services.user;

import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.util.rules.SupportedLocales;

@Service
public class UserLocaleServiceImpl implements UserLocaleService {

    private final UserService userService;

    @Autowired
    public UserLocaleServiceImpl(@Lazy final UserService userService) {
        this.userService = userService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Locale> findUserPreferredLocale(final long userId) {
        return preferredLocaleFor(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Locale resolveMailLocale(final long userId) {
        return resolveMailLocaleOrElse(userId, Locale.ENGLISH);
    }

    @Override
    @Transactional(readOnly = true)
    public Locale resolveMailLocaleOrElse(final long userId, final Locale fallback) {
        final Locale fb = fallback != null ? fallback : Locale.ENGLISH;
        return preferredLocaleFor(userId).orElse(fb);
    }

    @Override
    @Transactional(readOnly = true)
    public Locale resolveMailLocaleFor(final User user) {
        if (user == null) {
            return Locale.ENGLISH;
        }
        return user.getLatestLocale()
                .flatMap(loc -> SupportedLocales.parse(loc.toLanguageTag()))
                .orElse(Locale.ENGLISH);
    }

    private Optional<Locale> preferredLocaleFor(final long userId) {
        return userService.getUserById(userId)
                .flatMap(User::getLatestLocale)
                .flatMap(loc -> SupportedLocales.parse(loc.toLanguageTag()));
    }
}
