package ar.edu.itba.paw.services;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.exception.user.VerificationCodeAlreadyActiveException;
import ar.edu.itba.paw.exception.user.VerificationCodeInvalidException;
import ar.edu.itba.paw.models.EmailNormalizer;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.EmailVerificationCodeDao;
import ar.edu.itba.paw.persistence.UserDao;

@Service
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationCodeDao emailVerificationCodeDao;
    private final UserDao userDao;
    private final EmailService emailService;

    @Autowired
    public EmailVerificationServiceImpl(
            final EmailVerificationCodeDao emailVerificationCodeDao,
            final UserDao userDao,
            final EmailService emailService) {
        this.emailVerificationCodeDao = emailVerificationCodeDao;
        this.userDao = userDao;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public void issueFreshVerificationCode(final long userId, final String email, final Locale locale) {
        emailVerificationCodeDao.deleteForUser(userId);
        insertAndSend(userId, email, locale);
    }

    @Override
    @Transactional
    public void ensurePendingVerificationCode(final long userId, final String email, final Locale locale) {
        final Instant now = Instant.now();
        if (emailVerificationCodeDao.hasActiveCode(userId, now)) {
            return;
        }
        emailVerificationCodeDao.deleteForUser(userId);
        insertAndSend(userId, email, locale);
    }

    @Override
    @Transactional
    public void resendVerificationCode(final long userId, final String email, final Locale locale) {
        final Instant now = Instant.now();
        if (emailVerificationCodeDao.hasActiveCode(userId, now)) {
            throw new VerificationCodeAlreadyActiveException(MessageKeys.USER_VERIFICATION_CODE_ALREADY_ACTIVE);
        }
        emailVerificationCodeDao.deleteForUser(userId);
        insertAndSend(userId, email, locale);
    }

    private void insertAndSend(final long userId, final String email, final Locale locale) {
        final String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        final Instant now = Instant.now();
        emailVerificationCodeDao.insert(userId, code, now.plus(CODE_TTL), now);
        final Locale mailLocale = locale != null ? locale : Locale.ENGLISH;
        emailService.sendEmailVerificationCode(email, code, mailLocale);
    }

    @Override
    @Transactional
    public long verifyEmailAndConsumeCode(final String email, final String code) {
        final String normalized = EmailNormalizer.normalize(email);
        final User user = userDao.findByEmail(normalized)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        final boolean ok = emailVerificationCodeDao.deleteIfValid(user.getId(), code, Instant.now());
        if (!ok) {
            throw new VerificationCodeInvalidException(MessageKeys.USER_VERIFICATION_CODE_INVALID);
        }
        userDao.updateEmailValidated(user.getId(), true);
        return user.getId();
    }
}
