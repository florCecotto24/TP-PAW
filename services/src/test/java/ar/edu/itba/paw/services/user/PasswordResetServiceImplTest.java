package ar.edu.itba.paw.services.user;

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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;
import org.springframework.security.crypto.password.PasswordEncoder;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.user.PasswordResetCodeInvalidException;
import ar.edu.itba.paw.exception.user.RegistrationPasswordException;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.exception.user.VerificationCodeAlreadyActiveException;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.email.PasswordResetCodeEmailPayload;
import ar.edu.itba.paw.persistence.PasswordResetCodeDao;
import ar.edu.itba.paw.policy.UserValidationPolicy;

import ar.edu.itba.paw.services.email.EmailService;
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PasswordResetServiceImplTest {

    private static final long USER_ID = 7L;
    private static final String EMAIL = "user@example.com";

    @Mock
    private PasswordResetCodeDao dao;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserService userService;

    private PasswordResetServiceImpl service;
    private UserValidationPolicy validationPolicy;

    @BeforeEach
    void setUp() {
        validationPolicy = UserValidationPolicy.fromValidatedConfiguration(
                8, 72, 200, 50, 30, 500, "^[0-9+]+$");
        service = new PasswordResetServiceImpl(dao, emailService, passwordEncoder, validationPolicy, userService);
    }

    private PasswordResetCodeEmailPayload[] capturePayloadSlot() {
        final PasswordResetCodeEmailPayload[] slot = new PasswordResetCodeEmailPayload[1];
        final Answer<Void> recorder = invocation -> {
            slot[0] = invocation.getArgument(0);
            return null;
        };
        Mockito.doAnswer(recorder).when(emailService).sendPasswordResetCode(Mockito.any());
        return slot;
    }

    @Test
    void testInitiatePasswordResetReturnsFalseWhenUserMissing() {
        // 1.Arrange
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // 2.Exercise
        final boolean result = service.initiatePasswordReset("  USER@Example.COM ", Locale.ENGLISH);

        // 3.Assert
        Assertions.assertFalse(result);
    }

    @Test
    void testInitiatePasswordResetThrowsWhenActiveCodeAlreadyExists() {
        // 1.Arrange
        final User user = User.identities(USER_ID, EMAIL, "Ada", "Lovelace");
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        Mockito.when(dao.hasActiveCode(Mockito.eq(USER_ID), Mockito.any(Instant.class))).thenReturn(true);

        // 2.Exercise
        final VerificationCodeAlreadyActiveException ex = Assertions.assertThrows(
                VerificationCodeAlreadyActiveException.class,
                () -> service.initiatePasswordReset(EMAIL, Locale.ENGLISH));

        // 3.Assert
        Assertions.assertEquals(MessageKeys.USER_PASSWORD_RESET_CODE_ALREADY_ACTIVE, ex.getMessageCode());
    }

    @Test
    void testInitiatePasswordResetSendsMailWithSixDigitCodeAndResolvedLocale() {
        // 1.Arrange
        final User user = User.identities(USER_ID, EMAIL, "Ada", "Lovelace");
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        Mockito.when(dao.hasActiveCode(Mockito.eq(USER_ID), Mockito.any(Instant.class))).thenReturn(false);
        Mockito.when(userService.resolveMailLocaleOrElse(USER_ID, Locale.ENGLISH)).thenReturn(new Locale("es"));
        final PasswordResetCodeEmailPayload[] slot = capturePayloadSlot();

        // 2.Exercise
        final boolean result = service.initiatePasswordReset(EMAIL, Locale.ENGLISH);

        // 3.Assert
        Assertions.assertTrue(result);
        Assertions.assertNotNull(slot[0]);
        Assertions.assertEquals(new Locale("es"), slot[0].getMessageLocale());
        Assertions.assertEquals(EMAIL, slot[0].getRecipientEmail());
        Assertions.assertTrue(slot[0].getCode().matches("\\d{6}"));
    }

    @Test
    void testInitiatePasswordResetUsesEnglishFallbackWhenLocaleNull() {
        // 1.Arrange
        final User user = User.identities(USER_ID, EMAIL, "Ada", "Lovelace");
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        Mockito.when(dao.hasActiveCode(Mockito.eq(USER_ID), Mockito.any(Instant.class))).thenReturn(false);
        Mockito.when(userService.resolveMailLocaleOrElse(Mockito.eq(USER_ID), Mockito.any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        final PasswordResetCodeEmailPayload[] slot = capturePayloadSlot();

        // 2.Exercise
        service.initiatePasswordReset(EMAIL, null);

        // 3.Assert
        Assertions.assertEquals(Locale.ENGLISH, slot[0].getMessageLocale());
    }

    @Test
    void testCompletePasswordResetThrowsWhenUserMissing() {
        // 1.Arrange
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // 2.Exercise
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

        // 2.Exercise
        final RegistrationPasswordException ex = Assertions.assertThrows(RegistrationPasswordException.class,
                () -> service.completePasswordReset(EMAIL, "123456", "newPass12", "different"));

        // 3.Assert
        Assertions.assertEquals(MessageKeys.USER_REGISTRATION_PASSWORD_MISMATCH, ex.getMessageCode());
    }

    @Test
    void testCompletePasswordResetThrowsWhenPasswordsAreNull() {
        // 1.Arrange
        final User user = User.identities(USER_ID, EMAIL, "Ada", "Lovelace");
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        // 2.Exercise / 3.Assert
        Assertions.assertThrows(RegistrationPasswordException.class,
                () -> service.completePasswordReset(EMAIL, "123456", null, "x"));
        Assertions.assertThrows(RegistrationPasswordException.class,
                () -> service.completePasswordReset(EMAIL, "123456", "x", null));
    }

    @Test
    void testCompletePasswordResetThrowsWhenPasswordTooShort() {
        // 1.Arrange
        final User user = User.identities(USER_ID, EMAIL, "Ada", "Lovelace");
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        // 2.Exercise
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

        // 2.Exercise
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
        Mockito.when(dao.deleteIfValid(Mockito.eq(USER_ID), Mockito.eq("badcde"), Mockito.any(Instant.class)))
                .thenReturn(false);

        // 2.Exercise
        final PasswordResetCodeInvalidException ex = Assertions.assertThrows(
                PasswordResetCodeInvalidException.class,
                () -> service.completePasswordReset(EMAIL, "badcde", "newPass12", "newPass12"));

        // 3.Assert
        Assertions.assertEquals(MessageKeys.USER_PASSWORD_RESET_CODE_INVALID, ex.getMessageCode());
    }

    @Test
    void testCompletePasswordResetReplacesPasswordHashOnHappyPath() {
        // 1.Arrange
        final User user = User.identities(USER_ID, EMAIL, "Ada", "Lovelace");
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        Mockito.when(dao.deleteIfValid(Mockito.eq(USER_ID), Mockito.eq("123456"), Mockito.any(Instant.class)))
                .thenReturn(true);
        Mockito.when(passwordEncoder.encode("newPass12")).thenReturn("hashed");
        final String[] hashSlot = new String[1];
        Mockito.doAnswer((Answer<Void>) inv -> {
            hashSlot[0] = inv.getArgument(1);
            return null;
        }).when(userService).replacePasswordHash(Mockito.eq(USER_ID), Mockito.anyString());

        // 2.Exercise
        service.completePasswordReset(EMAIL, "123456", "newPass12", "newPass12");

        // 3.Assert
        Assertions.assertEquals("hashed", hashSlot[0]);
    }
}
