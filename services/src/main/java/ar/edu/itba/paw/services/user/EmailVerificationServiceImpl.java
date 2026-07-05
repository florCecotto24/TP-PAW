package ar.edu.itba.paw.services.user;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.exception.user.VerificationCodeAlreadyActiveException;
import ar.edu.itba.paw.exception.user.VerificationCodeInvalidException;
import ar.edu.itba.paw.models.email.user.EmailVerificationCodeEmailPayload;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.persistence.user.EmailVerificationCodeDao;

import ar.edu.itba.paw.services.email.EmailService;
/** Uses only {@link EmailVerificationCodeDao}; user and mail side effects go through {@link UserService} and {@link EmailService}. */
@Service
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailVerificationServiceImpl.class);

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationCodeDao emailVerificationCodeDao;
    private final EmailService emailService;
    private final UserService userService;

    @Autowired
    public EmailVerificationServiceImpl(
            final EmailVerificationCodeDao emailVerificationCodeDao,
            final EmailService emailService,
            final UserService userService) {
        this.emailVerificationCodeDao = emailVerificationCodeDao;
        this.emailService = emailService;
        this.userService = userService;
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
        final Locale fallback = locale != null ? locale : Locale.ENGLISH;
        final Locale mailLocale = userService.resolveMailLocaleOrElse(userId, fallback);
        emailService.sendEmailVerificationCode(EmailVerificationCodeEmailPayload.builder()
                .messageLocale(mailLocale)
                .recipientEmail(email)
                .code(code)
                .build());
    }

    @Override
    @Transactional
    public long verifyEmailAndConsumeCode(final String email, final String code) {
        final User user = userService.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        final boolean ok = emailVerificationCodeDao.deleteIfValid(user.getId(), code, Instant.now());
        if (!ok) {
            throw new VerificationCodeInvalidException(MessageKeys.USER_VERIFICATION_CODE_INVALID);
        }
        userService.markEmailVerified(user.getId());
        LOGGER.atInfo().addArgument(user.getId()).log("Email verified for user id={}");
        return user.getId();
    }
}
