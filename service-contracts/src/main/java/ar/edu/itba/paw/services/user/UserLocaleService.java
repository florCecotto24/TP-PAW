package ar.edu.itba.paw.services.user;

import java.util.Locale;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.user.User;

/**
 * Mail/UI locale resolution without touching {@code UserDao} directly — loads users via
 * {@link UserService#getUserById(long)} when only an id is known.
 */
public interface UserLocaleService {

    Optional<Locale> findUserPreferredLocale(long userId);

    Locale resolveMailLocale(long userId);

    Locale resolveMailLocaleOrElse(long userId, Locale fallback);

    Locale resolveMailLocaleFor(User user);
}
