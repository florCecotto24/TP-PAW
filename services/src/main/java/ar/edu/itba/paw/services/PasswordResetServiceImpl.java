package ar.edu.itba.paw.services;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.user.PasswordResetCodeInvalidException;
import ar.edu.itba.paw.exception.user.RegistrationPasswordException;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.exception.user.VerificationCodeAlreadyActiveException;
import ar.edu.itba.paw.models.util.EmailNormalizer;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.services.policy.UserValidationPolicy;
import ar.edu.itba.paw.persistence.PasswordResetCodeDao;
import ar.edu.itba.paw.persistence.UserDao;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PasswordResetCodeDao passwordResetCodeDao;
    private final UserDao userDao;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final UserValidationPolicy validationPolicy;
    private final UserService userService;

    @Autowired
    public PasswordResetServiceImpl(
            final PasswordResetCodeDao passwordResetCodeDao,
            final UserDao userDao,
            final EmailService emailService,
            final PasswordEncoder passwordEncoder,
            final UserValidationPolicy validationPolicy,
            final UserService userService) {
        this.passwordResetCodeDao = passwordResetCodeDao;
        this.userDao = userDao;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.validationPolicy = validationPolicy;
        this.userService = userService;
    }

    @Override
    @Transactional
    public boolean requestCode(final String email, final Locale locale) {
        final String normalized = EmailNormalizer.normalize(email);
        final Optional<User> userOpt = userDao.findByEmail(normalized);
        if (userOpt.isEmpty()) {
            return false;
        }
        final User user = userOpt.get();
        final Instant now = Instant.now();
        if (passwordResetCodeDao.hasActiveCode(user.getId(), now)) {
            throw new VerificationCodeAlreadyActiveException(MessageKeys.USER_PASSWORD_RESET_CODE_ALREADY_ACTIVE);
        }
        passwordResetCodeDao.deleteForUser(user.getId());
        final String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        passwordResetCodeDao.insert(user.getId(), code, now.plus(CODE_TTL), now);
        final Locale fallback = locale != null ? locale : Locale.ENGLISH;
        final Locale mailLocale = userService.resolveMailLocaleOrElse(user.getId(), fallback);
        emailService.sendPasswordResetCode(user.getEmail(), code, mailLocale);
        return true;
    }

    @Override
    @Transactional
    public void resetPassword(
            final String email,
            final String code,
            final String newPassword,
            final String newPasswordConfirm) {
        final String normalized = EmailNormalizer.normalize(email);
        final User user = userDao.findByEmail(normalized)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        if (newPassword == null || newPasswordConfirm == null || !newPassword.equals(newPasswordConfirm)) {
            throw new RegistrationPasswordException(MessageKeys.USER_REGISTRATION_PASSWORD_MISMATCH);
        }
        if (newPassword.length() < validationPolicy.getRegistrationPasswordMinLength()) {
            throw new RegistrationPasswordException(
                    MessageKeys.USER_REGISTRATION_PASSWORD_TOO_SHORT,
                    validationPolicy.getRegistrationPasswordMinLength());
        }
        if (newPassword.length() > validationPolicy.getRegistrationPasswordMaxLength()) {
            throw new RegistrationPasswordException(
                    MessageKeys.USER_REGISTRATION_PASSWORD_TOO_LONG,
                    validationPolicy.getRegistrationPasswordMaxLength());
        }
        final boolean ok = passwordResetCodeDao.deleteIfValid(user.getId(), code, Instant.now());
        if (!ok) {
            throw new PasswordResetCodeInvalidException(MessageKeys.USER_PASSWORD_RESET_CODE_INVALID);
        }
        userDao.updatePasswordHash(user.getId(), passwordEncoder.encode(newPassword));
    }
}
