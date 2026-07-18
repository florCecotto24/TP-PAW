package ar.edu.itba.paw.services.user;

import java.security.SecureRandom;
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
import ar.edu.itba.paw.models.util.format.EmailNormalizer;
import ar.edu.itba.paw.persistence.user.EmailVerificationCodeDao;
import ar.edu.itba.paw.policy.VerificationCodePolicy;

import ar.edu.itba.paw.services.email.EmailService;
/** Uses only {@link EmailVerificationCodeDao}; user and mail side effects go through {@link UserService} and {@link EmailService}. */
@Service
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailVerificationServiceImpl.class);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationCodeDao emailVerificationCodeDao;
    private final EmailService emailService;
    private final UserService userService;
    private final UserLocaleService userLocaleService;
    private final VerificationCodePolicy verificationCodePolicy;
    private final OtpAttemptLimiter otpAttemptLimiter;

    @Autowired
    public EmailVerificationServiceImpl(
            final EmailVerificationCodeDao emailVerificationCodeDao,
            final EmailService emailService,
            final UserService userService,
            final UserLocaleService userLocaleService,
            final VerificationCodePolicy verificationCodePolicy,
            final OtpAttemptLimiter otpAttemptLimiter) {
        this.emailVerificationCodeDao = emailVerificationCodeDao;
        this.emailService = emailService;
        this.userService = userService;
        this.userLocaleService = userLocaleService;
        this.verificationCodePolicy = verificationCodePolicy;
        this.otpAttemptLimiter = otpAttemptLimiter;
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

    @Override
    @Transactional
    public void issuePublicVerificationCode(final long userId, final Locale locale) {
        final User user = userService.getUserById(userId).orElse(null);
        if (user == null) {
            LOGGER.atDebug().addArgument(userId)
                    .log("Public verification-code request for unknown user id={} (silent no-op)");
            return;
        }
        if (Boolean.TRUE.equals(user.getEmailValidated().orElse(false))) {
            LOGGER.atDebug().addArgument(userId)
                    .log("Public verification-code request for already verified user id={} (silent no-op)");
            return;
        }
        final Instant now = Instant.now();
        if (emailVerificationCodeDao.hasActiveCode(userId, now)) {
            LOGGER.atDebug().addArgument(userId)
                    .log("Public verification-code request for user id={} with an active code (no-op)");
            return;
        }
        // Issuance quota (separate key space from failed-verify lockout). Over-quota → silent 200
        // (anti-enumeration); distinct from Basic OTP attempt limiting.
        final String issuanceKey = OtpAttemptLimiter.issuanceKey(user.getEmail());
        try {
            otpAttemptLimiter.assertAllowed(issuanceKey);
        } catch (final ar.edu.itba.paw.exception.user.OtpAttemptsExceededException ex) {
            LOGGER.atDebug().addArgument(userId)
                    .log("Public verification-code issuance rate-limited for user id={} (silent no-op)");
            return;
        }
        emailVerificationCodeDao.deleteForUser(userId);
        try {
            insertAndSend(userId, user.getEmail(), locale);
            otpAttemptLimiter.recordFailure(issuanceKey);
        } catch (final RuntimeException e) {
            LOGGER.atWarn()
                    .addArgument(userId)
                    .setCause(e)
                    .log("Failed to dispatch public verification code for user id={}; responding success anyway");
        }
    }

    private void insertAndSend(final long userId, final String email, final Locale locale) {
        final String code = generateNumericCode();
        final Instant now = Instant.now();
        emailVerificationCodeDao.insert(
                userId, code, now.plus(verificationCodePolicy.getCodeTtl()), now);
        final Locale fallback = locale != null ? locale : Locale.ENGLISH;
        final Locale mailLocale = userLocaleService.resolveMailLocaleOrElse(userId, fallback);
        emailService.sendEmailVerificationCode(EmailVerificationCodeEmailPayload.builder()
                .messageLocale(mailLocale)
                .recipientEmail(email)
                .code(code)
                .build());
    }

    private String generateNumericCode() {
        final int length = verificationCodePolicy.getCodeLength();
        final int upperBound = (int) Math.pow(10, length);
        return String.format("%0" + length + "d", RANDOM.nextInt(upperBound));
    }

    @Override
    @Transactional
    public long verifyEmailAndConsumeCode(final String email, final String code) {
        final String normalized = EmailNormalizer.normalize(email);
        otpAttemptLimiter.assertAllowed(normalized);
        final User user = userService.findByEmail(normalized)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        final boolean ok = emailVerificationCodeDao.deleteIfValid(user.getId(), code, Instant.now());
        if (!ok) {
            otpAttemptLimiter.recordFailure(normalized);
            throw new VerificationCodeInvalidException(MessageKeys.USER_VERIFICATION_CODE_INVALID);
        }
        otpAttemptLimiter.clear(normalized);
        userService.markEmailVerified(user.getId());
        LOGGER.atInfo().addArgument(user.getId()).log("Email verified for user id={}");
        return user.getId();
    }
}
