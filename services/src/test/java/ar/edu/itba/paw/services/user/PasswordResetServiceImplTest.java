package ar.edu.itba.paw.services.user;

import java.time.Instant;
import java.util.List;
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
import ar.edu.itba.paw.models.email.user.PasswordResetCodeEmailPayload;
import ar.edu.itba.paw.persistence.user.PasswordResetCodeDao;
import ar.edu.itba.paw.policy.UserValidationPolicy;
import ar.edu.itba.paw.policy.VerificationCodePolicy;

import ar.edu.itba.paw.services.support.RecordingEmailService;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    private static final long USER_ID = 7L;
    private static final String EMAIL = "user@example.com";

    @Mock
    private PasswordResetCodeDao dao;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserService userService;

    private RecordingEmailService emailService;
    private PasswordResetServiceImpl service;
    private UserValidationPolicy validationPolicy;

    @BeforeEach
    void setUp() {
        // RecordingEmailService is the state-based stand-in mandated by TEST-8: instead of a
        // doAnswer captor over the EmailService mock, each test asserts directly on the lists
        // exposed by the fake.
        emailService = new RecordingEmailService();
        validationPolicy = UserValidationPolicy.fromValidatedConfiguration(
                8, 72, 200, 50, 30, 500, "^[0-9+]+$");
        service = new PasswordResetServiceImpl(
                dao, emailService, passwordEncoder, validationPolicy,
                VerificationCodePolicy.fromValidatedConfiguration(6, 5), userService,
                OtpAttemptLimiter.forTests(8, java.time.Duration.ofMinutes(15)));
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
        Mockito.when(dao.hasActiveCode(Mockito.eq(USER_ID), Mockito.any(Instant.class))).thenReturn(true);

        // 2.Act
        final boolean result = service.initiatePasswordReset(EMAIL, Locale.ENGLISH);

        // 3.Assert
        Assertions.assertFalse(result);
        Assertions.assertTrue(emailService.passwordResetCodes().isEmpty());
    }

    @Test
    void testInitiatePasswordResetSendsMailWithSixDigitCodeAndResolvedLocale() {
        // 1.Arrange
        final User user = User.identities(USER_ID, EMAIL, "Ada", "Lovelace");
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        Mockito.when(dao.hasActiveCode(Mockito.eq(USER_ID), Mockito.any(Instant.class))).thenReturn(false);
        Mockito.when(userService.resolveMailLocaleOrElse(USER_ID, Locale.ENGLISH)).thenReturn(new Locale("es"));

        // 2.Act
        final boolean result = service.initiatePasswordReset(EMAIL, Locale.ENGLISH);

        // 3.Assert
        Assertions.assertTrue(result);
        final List<PasswordResetCodeEmailPayload> sent = emailService.passwordResetCodes();
        Assertions.assertEquals(1, sent.size());
        final PasswordResetCodeEmailPayload payload = sent.get(0);
        Assertions.assertEquals(new Locale("es"), payload.getMessageLocale());
        Assertions.assertEquals(EMAIL, payload.getRecipientEmail());
        Assertions.assertTrue(payload.getCode().matches("\\d{6}"));
    }

    @Test
    void testInitiatePasswordResetUsesEnglishFallbackWhenLocaleNull() {
        // 1.Arrange
        final User user = User.identities(USER_ID, EMAIL, "Ada", "Lovelace");
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        Mockito.when(dao.hasActiveCode(Mockito.eq(USER_ID), Mockito.any(Instant.class))).thenReturn(false);
        Mockito.when(userService.resolveMailLocaleOrElse(Mockito.eq(USER_ID), Mockito.any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        // 2.Act
        service.initiatePasswordReset(EMAIL, null);

        // 3.Assert
        Assertions.assertEquals(Locale.ENGLISH,
                emailService.passwordResetCodes().get(0).getMessageLocale());
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
        Mockito.when(dao.deleteIfValid(
                        Mockito.eq(USER_ID),
                        Mockito.eq(OtpCodeDigest.sha256Hex("badcde")),
                        Mockito.any(Instant.class)))
                .thenReturn(false);

        // 2.Act
        final PasswordResetCodeInvalidException ex = Assertions.assertThrows(
                PasswordResetCodeInvalidException.class,
                () -> service.completePasswordReset(EMAIL, "badcde", "newPass12", "newPass12"));

        // 3.Assert
        Assertions.assertEquals(MessageKeys.USER_PASSWORD_RESET_CODE_INVALID, ex.getMessageCode());
    }

    @Test
    void testCompletePasswordResetSucceedsOnHappyPath() {
        // 1.Arrange
        final User user = User.identities(USER_ID, EMAIL, "Ada", "Lovelace");
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        Mockito.when(dao.deleteIfValid(
                        Mockito.eq(USER_ID),
                        Mockito.eq(OtpCodeDigest.sha256Hex("123456")),
                        Mockito.any(Instant.class)))
                .thenReturn(true);
        Mockito.when(passwordEncoder.encode("newPass12")).thenReturn("hashed");

        // 2.Act / 3.Assert
        Assertions.assertDoesNotThrow(
                () -> service.completePasswordReset(EMAIL, "123456", "newPass12", "newPass12"));
    }
}
