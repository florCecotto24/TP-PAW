package ar.edu.itba.paw.services.user;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.user.PasswordResetCodeInvalidException;
import ar.edu.itba.paw.exception.user.RegistrationPasswordException;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.models.util.format.EmailNormalizer;
import ar.edu.itba.paw.models.email.user.PasswordResetCodeEmailPayload;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.policy.UserValidationPolicy;
import ar.edu.itba.paw.policy.VerificationCodePolicy;
import ar.edu.itba.paw.persistence.user.PasswordResetCodeDao;

import ar.edu.itba.paw.services.email.EmailService;
/** Uses only {@link PasswordResetCodeDao}; user lookup and password hash updates go through {@link UserService}. */
@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetServiceImpl.class);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PasswordResetCodeDao passwordResetCodeDao;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final UserValidationPolicy validationPolicy;
    private final VerificationCodePolicy verificationCodePolicy;
    private final UserService userService;
    private final OtpAttemptLimiter otpAttemptLimiter;

    @Autowired
    public PasswordResetServiceImpl(
            final PasswordResetCodeDao passwordResetCodeDao,
            final EmailService emailService,
            final PasswordEncoder passwordEncoder,
            final UserValidationPolicy validationPolicy,
            final VerificationCodePolicy verificationCodePolicy,
            final UserService userService,
            final OtpAttemptLimiter otpAttemptLimiter) {
        this.passwordResetCodeDao = passwordResetCodeDao;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.validationPolicy = validationPolicy;
        this.verificationCodePolicy = verificationCodePolicy;
        this.userService = userService;
        this.otpAttemptLimiter = otpAttemptLimiter;
    }

    @Override
    @Transactional
    public boolean initiatePasswordReset(final String email, final Locale locale) {
        final String normalized = EmailNormalizer.normalize(email);
        final Optional<User> userOpt = userService.findByEmail(normalized);
        if (userOpt.isEmpty()) {
            LOGGER.atDebug().log("Password reset requested for unknown address; responding uniformly (no-op).");
            return false;
        }
        final User user = userOpt.get();
        final Instant now = Instant.now();
        if (passwordResetCodeDao.hasActiveCode(user.getId(), now)) {
            LOGGER.atDebug().addArgument(user.getId())
                    .log("Active password reset code already present for user id={}; skipping re-issue (no-op).");
            return false;
        }
        passwordResetCodeDao.deleteForUser(user.getId());
        final String code = generateNumericCode();
        passwordResetCodeDao.insert(
                user.getId(), code, now.plus(verificationCodePolicy.getCodeTtl()), now);
        final Locale fallback = locale != null ? locale : Locale.ENGLISH;
        final Locale mailLocale = userService.resolveMailLocaleOrElse(user.getId(), fallback);
        emailService.sendPasswordResetCode(PasswordResetCodeEmailPayload.builder()
                .messageLocale(mailLocale)
                .recipientEmail(user.getEmail())
                .code(code)
                .build());
        LOGGER.atInfo().addArgument(user.getId()).log("Password reset code issued for user id={}");
        return true;
    }

    private String generateNumericCode() {
        final int length = verificationCodePolicy.getCodeLength();
        final int upperBound = (int) Math.pow(10, length);
        return String.format("%0" + length + "d", RANDOM.nextInt(upperBound));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean matchesResetCode(final String email, final String code) {
        if (email == null || email.isBlank() || code == null || code.isBlank()) {
            return false;
        }
        final String normalized = EmailNormalizer.normalize(email);
        otpAttemptLimiter.assertAllowed(normalized);
        final Optional<User> userOpt = userService.findByEmail(normalized);
        if (userOpt.isEmpty()) {
            return false;
        }
        final boolean ok = passwordResetCodeDao.matchesValidCode(userOpt.get().getId(), code, Instant.now());
        if (ok) {
            otpAttemptLimiter.clear(normalized);
        } else {
            otpAttemptLimiter.recordFailure(normalized);
        }
        return ok;
    }

    @Override
    @Transactional
    public void completePasswordReset(
            final String email,
            final String code,
            final String newPassword,
            final String newPasswordConfirm) {
        final String normalized = EmailNormalizer.normalize(email);
        otpAttemptLimiter.assertAllowed(normalized);
        final User user = userService.findByEmail(normalized)
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
            otpAttemptLimiter.recordFailure(normalized);
            throw new PasswordResetCodeInvalidException(MessageKeys.USER_PASSWORD_RESET_CODE_INVALID);
        }
        otpAttemptLimiter.clear(normalized);
        userService.replacePasswordHash(user.getId(), passwordEncoder.encode(newPassword));
        LOGGER.atInfo().addArgument(user.getId()).log("Password reset completed for user id={}");
    }
}
