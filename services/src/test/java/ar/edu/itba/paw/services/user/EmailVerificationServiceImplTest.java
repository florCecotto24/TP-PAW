package ar.edu.itba.paw.services.user;

import java.time.Duration;
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

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.exception.user.VerificationCodeAlreadyActiveException;
import ar.edu.itba.paw.exception.user.VerificationCodeInvalidException;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.email.user.EmailVerificationCodeEmailPayload;
import ar.edu.itba.paw.persistence.user.EmailVerificationCodeDao;
import ar.edu.itba.paw.policy.VerificationCodePolicy;

import ar.edu.itba.paw.services.support.RecordingEmailService;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceImplTest {

    private static final long USER_ID = 11L;
    private static final String EMAIL = "user@example.com";

    @Mock
    private EmailVerificationCodeDao dao;

    @Mock
    private UserService userService;

    private RecordingEmailService emailService;
    private EmailVerificationServiceImpl service;

    @BeforeEach
    void wireServiceWithRecordingEmail() {
        // State-based double instead of a Mockito captor: rule TEST-8 forbids doAnswer-style
        // payload capture, so the test reads the recorded payload directly from the fake.
        emailService = new RecordingEmailService();
        service = new EmailVerificationServiceImpl(
                dao, emailService, userService, VerificationCodePolicy.fromValidatedConfiguration(6, 5),
                OtpAttemptLimiter.forTests(8, Duration.ofMinutes(15)));
    }

    @Test
    void testIssueFreshVerificationCodeProducesSixDigitCodeWithResolvedLocale() {
        // 1.Arrange
        Mockito.when(userService.resolveMailLocaleOrElse(USER_ID, Locale.ENGLISH)).thenReturn(new Locale("es"));

        // 2.Act
        service.issueFreshVerificationCode(USER_ID, EMAIL, null);
        final List<EmailVerificationCodeEmailPayload> sent = emailService.emailVerificationCodes();
        final EmailVerificationCodeEmailPayload payload = sent.get(0);

        // 3.Assert
        Assertions.assertEquals(1, sent.size());
        Assertions.assertEquals(new Locale("es"), payload.getMessageLocale());
        Assertions.assertEquals(EMAIL, payload.getRecipientEmail());
        Assertions.assertNotNull(payload.getCode());
        Assertions.assertEquals(6, payload.getCode().length());
        Assertions.assertTrue(payload.getCode().matches("\\d{6}"), "code must be six decimal digits");
    }

    @Test
    void testIssueFreshVerificationCodeUsesProvidedLocaleAsFallback() {
        // 1.Arrange
        Mockito.when(userService.resolveMailLocaleOrElse(Mockito.eq(USER_ID), Mockito.any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        // 2.Act
        service.issueFreshVerificationCode(USER_ID, EMAIL, new Locale("es"));

        // 3.Assert
        Assertions.assertEquals(new Locale("es"),
                emailService.emailVerificationCodes().get(0).getMessageLocale());
    }

    @Test
    void testEnsurePendingVerificationCodeShortCircuitsWhenActiveCodeExists() {
        // 1.Arrange
        Mockito.when(dao.hasActiveCode(Mockito.eq(USER_ID), Mockito.any(Instant.class))).thenReturn(true);

        // 2.Act
        service.ensurePendingVerificationCode(USER_ID, EMAIL, Locale.ENGLISH);

        // 3.Assert
        Assertions.assertTrue(emailService.emailVerificationCodes().isEmpty(),
                "no email should have been sent when a verification code is already active");
    }

    @Test
    void testEnsurePendingVerificationCodeIssuesNewCodeWhenNoneActive() {
        // 1.Arrange
        Mockito.when(dao.hasActiveCode(Mockito.eq(USER_ID), Mockito.any(Instant.class))).thenReturn(false);
        Mockito.when(userService.resolveMailLocaleOrElse(USER_ID, Locale.ENGLISH)).thenReturn(Locale.ENGLISH);

        // 2.Act
        service.ensurePendingVerificationCode(USER_ID, EMAIL, Locale.ENGLISH);

        // 3.Assert
        Assertions.assertEquals(1, emailService.emailVerificationCodes().size());
        Assertions.assertEquals(EMAIL, emailService.emailVerificationCodes().get(0).getRecipientEmail());
    }

    @Test
    void testResendVerificationCodeThrowsWhenAlreadyActive() {
        // 1.Arrange
        Mockito.when(dao.hasActiveCode(Mockito.eq(USER_ID), Mockito.any(Instant.class))).thenReturn(true);

        // 2.Act / 3.Assert
        final VerificationCodeAlreadyActiveException ex = Assertions.assertThrows(
                VerificationCodeAlreadyActiveException.class,
                () -> service.resendVerificationCode(USER_ID, EMAIL, Locale.ENGLISH));

        Assertions.assertEquals(MessageKeys.USER_VERIFICATION_CODE_ALREADY_ACTIVE, ex.getMessageCode());
    }

    @Test
    void testResendVerificationCodeProducesNewCodeWhenNoneActive() {
        // 1.Arrange
        Mockito.when(dao.hasActiveCode(Mockito.eq(USER_ID), Mockito.any(Instant.class))).thenReturn(false);
        Mockito.when(userService.resolveMailLocaleOrElse(USER_ID, Locale.ENGLISH)).thenReturn(Locale.ENGLISH);

        // 2.Act
        service.resendVerificationCode(USER_ID, EMAIL, Locale.ENGLISH);

        // 3.Assert
        final List<EmailVerificationCodeEmailPayload> sent = emailService.emailVerificationCodes();
        Assertions.assertEquals(1, sent.size());
        Assertions.assertEquals(EMAIL, sent.get(0).getRecipientEmail());
        Assertions.assertTrue(sent.get(0).getCode().matches("\\d{6}"));
    }

    @Test
    void testVerifyEmailAndConsumeCodeThrowsWhenUserMissing() {
        // 1.Arrange
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // 2.Act / 3.Assert
        final UserNotFoundException ex = Assertions.assertThrows(UserNotFoundException.class,
                () -> service.verifyEmailAndConsumeCode(EMAIL, "123456"));

        Assertions.assertEquals(MessageKeys.USER_ACCOUNT_NOT_FOUND, ex.getMessageCode());
    }

    @Test
    void testVerifyEmailAndConsumeCodeThrowsWhenCodeInvalid() {
        // 1.Arrange
        final User user = User.identities(USER_ID, EMAIL, "Ada", "Lovelace");
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        Mockito.when(dao.deleteIfValid(Mockito.eq(USER_ID), Mockito.eq("000000"), Mockito.any(Instant.class)))
                .thenReturn(false);

        // 2.Act / 3.Assert
        final VerificationCodeInvalidException ex = Assertions.assertThrows(
                VerificationCodeInvalidException.class,
                () -> service.verifyEmailAndConsumeCode(EMAIL, "000000"));

        Assertions.assertEquals(MessageKeys.USER_VERIFICATION_CODE_INVALID, ex.getMessageCode());
    }

    @Test
    void testVerifyEmailAndConsumeCodeReturnsUserIdOnSuccess() {
        // 1.Arrange
        final User user = User.identities(USER_ID, EMAIL, "Ada", "Lovelace");
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        Mockito.when(dao.deleteIfValid(Mockito.eq(USER_ID), Mockito.eq("123456"), Mockito.any(Instant.class)))
                .thenReturn(true);

        // 2.Act
        final long returnedId = service.verifyEmailAndConsumeCode(EMAIL, "123456");

        // 3.Assert
        Assertions.assertEquals(USER_ID, returnedId);
    }
}
