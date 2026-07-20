package ar.edu.itba.paw.services.user;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.user.PasswordResetCodeInvalidException;
import ar.edu.itba.paw.exception.user.RegistrationPasswordException;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.models.util.security.OtpCodeDigest;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.policy.UserValidationPolicy;
import ar.edu.itba.paw.policy.VerificationCodePolicy;

import ar.edu.itba.paw.services.email.EmailService;
import ar.edu.itba.paw.services.support.InMemoryPasswordResetCodeDao;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    private static final long USER_ID = 7L;
    private static final String EMAIL = "user@example.com";

    // In-memory fake per AGENTS.md TEST-8: the persisted digest row is the observable — tests
    // read it back through the fake's stored state instead of capturing mock interactions.
    private InMemoryPasswordResetCodeDao dao;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserService userService;

    @Mock
    private UserLocaleService userLocaleService;

    private PasswordResetServiceImpl service;
    private UserValidationPolicy validationPolicy;

    @BeforeEach
    void setUp() {
        dao = new InMemoryPasswordResetCodeDao();
        validationPolicy = UserValidationPolicy.fromValidatedConfiguration(
                8, 72, 200, 50, 30, 500, "^[0-9+]+$");
        service = new PasswordResetServiceImpl(
                dao, emailService, passwordEncoder, validationPolicy,
                VerificationCodePolicy.fromValidatedConfiguration(6, 5), userService,
                userLocaleService,
                OtpAttemptLimiter.forTests(8, Duration.ofMinutes(15)));
    }

    @Test
    void testInitiatePasswordResetReturnsFalseWhenUserMissing() {
        // 1.Arrange
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // 2.Act
        final boolean result = service.initiatePasswordReset("  USER@Example.COM ", Locale.ENGLISH);

        // 3.Assert
        Assertions.assertFalse(result);
    }

    @Test
    void testInitiatePasswordResetNoOpsWhenActiveCodeAlreadyExists() {
        // 1.Arrange
        final User user = User.identities(USER_ID, EMAIL, "Ada", "Lovelace");
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        final String existingDigest = OtpCodeDigest.sha256Hex("111111");
        dao.seedCode(USER_ID, existingDigest, Instant.now().plus(Duration.ofMinutes(5)));

        // 2.Act
        final boolean result = service.initiatePasswordReset(EMAIL, Locale.ENGLISH);

        // 3.Assert
        Assertions.assertFalse(result);
        Assertions.assertEquals(Optional.of(existingDigest), dao.storedCodeFor(USER_ID),
                "the still-active digest must be kept untouched");
    }

    @Test
    void testInitiatePasswordResetPersistsCodeDigestOnly() {
        // 1.Arrange
        final User user = User.identities(USER_ID, EMAIL, "Ada", "Lovelace");
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        Mockito.when(userLocaleService.resolveMailLocaleOrElse(USER_ID, Locale.ENGLISH)).thenReturn(new Locale("es"));

        // 2.Act
        final boolean result = service.initiatePasswordReset(EMAIL, Locale.ENGLISH);

        // 3.Assert
        Assertions.assertTrue(result);
        final String stored = dao.storedCodeFor(USER_ID).orElseThrow();
        Assertions.assertTrue(stored.matches("[0-9a-f]{64}"),
                "the persisted row must be the SHA-256 hex digest, never the plaintext code");
    }

    @Test
    void testInitiatePasswordResetReissuesWhenStoredCodeExpired() {
        // 1.Arrange
        final User user = User.identities(USER_ID, EMAIL, "Ada", "Lovelace");
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        dao.seedCode(USER_ID, "EXPIRED-SENTINEL", Instant.now().minusSeconds(60));
        Mockito.when(userLocaleService.resolveMailLocaleOrElse(Mockito.eq(USER_ID), Mockito.any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        // 2.Act
        final boolean result = service.initiatePasswordReset(EMAIL, null);

        // 3.Assert
        Assertions.assertTrue(result);
        Assertions.assertTrue(dao.storedCodeFor(USER_ID).orElseThrow().matches("[0-9a-f]{64}"),
                "the expired row must be replaced by a fresh digest");
    }

    @Test
    void testCompletePasswordResetThrowsWhenUserMissing() {
        // 1.Arrange
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // 2.Act
        final UserNotFoundException ex = Assertions.assertThrows(UserNotFoundException.class,
                () -> service.completePasswordReset(EMAIL, "123456", "newPass12", "newPass12"));

        // 3.Assert
        Assertions.assertEquals(MessageKeys.USER_ACCOUNT_NOT_FOUND, ex.getMessageCode());
    }

    @Test
    void testCompletePasswordResetThrowsWhenPasswordsDoNotMatch() {
        // 1.Arrange
        final User user = User.identities(USER_ID, EMAIL, "Ada", "Lovelace");
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        // 2.Act
        final RegistrationPasswordException ex = Assertions.assertThrows(RegistrationPasswordException.class,
                () -> service.completePasswordReset(EMAIL, "123456", "newPass12", "different"));

        // 3.Assert
        Assertions.assertEquals(MessageKeys.USER_REGISTRATION_PASSWORD_MISMATCH, ex.getMessageCode());
    }

    @Test
    void testCompletePasswordResetThrowsWhenNewPasswordIsNull() {
        // 1.Arrange
        final User user = User.identities(USER_ID, EMAIL, "Ada", "Lovelace");
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        // 2.Act / 3.Assert
        Assertions.assertThrows(RegistrationPasswordException.class,
                () -> service.completePasswordReset(EMAIL, "123456", null, "x"));
    }

    @Test
    void testCompletePasswordResetThrowsWhenConfirmationIsNull() {
        // 1.Arrange
        final User user = User.identities(USER_ID, EMAIL, "Ada", "Lovelace");
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        // 2.Act / 3.Assert
        Assertions.assertThrows(RegistrationPasswordException.class,
                () -> service.completePasswordReset(EMAIL, "123456", "x", null));
    }

    @Test
    void testCompletePasswordResetThrowsWhenPasswordTooShort() {
        // 1.Arrange
        final User user = User.identities(USER_ID, EMAIL, "Ada", "Lovelace");
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        // 2.Act
        final RegistrationPasswordException ex = Assertions.assertThrows(RegistrationPasswordException.class,
                () -> service.completePasswordReset(EMAIL, "123456", "short", "short"));

        // 3.Assert
        Assertions.assertEquals(MessageKeys.USER_REGISTRATION_PASSWORD_TOO_SHORT, ex.getMessageCode());
        Assertions.assertArrayEquals(new Object[]{8}, ex.getMessageArgs());
    }

    @Test
    void testCompletePasswordResetThrowsWhenPasswordTooLong() {
        // 1.Arrange
        final User user = User.identities(USER_ID, EMAIL, "Ada", "Lovelace");
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        final String tooLong = "a".repeat(73);

        // 2.Act
        final RegistrationPasswordException ex = Assertions.assertThrows(RegistrationPasswordException.class,
                () -> service.completePasswordReset(EMAIL, "123456", tooLong, tooLong));

        // 3.Assert
        Assertions.assertEquals(MessageKeys.USER_REGISTRATION_PASSWORD_TOO_LONG, ex.getMessageCode());
        Assertions.assertArrayEquals(new Object[]{72}, ex.getMessageArgs());
    }

    @Test
    void testCompletePasswordResetThrowsWhenCodeIsInvalid() {
        // 1.Arrange
        final User user = User.identities(USER_ID, EMAIL, "Ada", "Lovelace");
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        dao.seedCode(USER_ID, OtpCodeDigest.sha256Hex("123456"), Instant.now().plus(Duration.ofMinutes(5)));

        // 2.Act
        final PasswordResetCodeInvalidException ex = Assertions.assertThrows(
                PasswordResetCodeInvalidException.class,
                () -> service.completePasswordReset(EMAIL, "badcde", "newPass12", "newPass12"));

        // 3.Assert
        Assertions.assertEquals(MessageKeys.USER_PASSWORD_RESET_CODE_INVALID, ex.getMessageCode());
    }

    @Test
    void testCompletePasswordResetConsumesResetCodeOnHappyPath() {
        // 1.Arrange
        final User user = User.identities(USER_ID, EMAIL, "Ada", "Lovelace");
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        dao.seedCode(USER_ID, OtpCodeDigest.sha256Hex("123456"), Instant.now().plus(Duration.ofMinutes(5)));
        Mockito.when(passwordEncoder.encode("newPass12")).thenReturn("hashed");

        // 2.Act
        service.completePasswordReset(EMAIL, "123456", "newPass12", "newPass12");

        // 3.Assert
        Assertions.assertTrue(dao.storedCodeFor(USER_ID).isEmpty(),
                "the reset code must be consumed (deleted) once the password is replaced");
    }
}
