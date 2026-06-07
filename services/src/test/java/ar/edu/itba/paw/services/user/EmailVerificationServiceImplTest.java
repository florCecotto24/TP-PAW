package ar.edu.itba.paw.services.user;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.exception.user.VerificationCodeAlreadyActiveException;
import ar.edu.itba.paw.exception.user.VerificationCodeInvalidException;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.email.user.EmailVerificationCodeEmailPayload;
import ar.edu.itba.paw.persistence.user.EmailVerificationCodeDao;

import ar.edu.itba.paw.services.email.EmailService;
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailVerificationServiceImplTest {

    private static final long USER_ID = 11L;
    private static final String EMAIL = "user@example.com";

    @Mock
    private EmailVerificationCodeDao dao;

    @Mock
    private EmailService emailService;

    @Mock
    private UserService userService;

    @InjectMocks
    private EmailVerificationServiceImpl service;

    private EmailVerificationCodeEmailPayload[] capturePayloadSlot() {
        final EmailVerificationCodeEmailPayload[] slot = new EmailVerificationCodeEmailPayload[1];
        final Answer<Void> recorder = invocation -> {
            slot[0] = invocation.getArgument(0);
            return null;
        };
        Mockito.doAnswer(recorder).when(emailService).sendEmailVerificationCode(Mockito.any());
        return slot;
    }

    @Test
    void testIssueFreshVerificationCodeProducesSixDigitCodeWithResolvedLocale() {
        // 1.Arrange
        Mockito.when(userService.resolveMailLocaleOrElse(USER_ID, Locale.ENGLISH)).thenReturn(new Locale("es"));
        final EmailVerificationCodeEmailPayload[] payloadSlot = capturePayloadSlot();

        // 2.Exercise
        service.issueFreshVerificationCode(USER_ID, EMAIL, null);

        // 3.Assert
        Assertions.assertNotNull(payloadSlot[0]);
        Assertions.assertEquals(new Locale("es"), payloadSlot[0].getMessageLocale());
        Assertions.assertEquals(EMAIL, payloadSlot[0].getRecipientEmail());
        final String code = payloadSlot[0].getCode();
        Assertions.assertNotNull(code);
        Assertions.assertEquals(6, code.length());
        Assertions.assertTrue(code.matches("\\d{6}"), "code must be six decimal digits");
    }

    @Test
    void testIssueFreshVerificationCodeUsesProvidedLocaleAsFallback() {
        // 1.Arrange
        Mockito.when(userService.resolveMailLocaleOrElse(Mockito.eq(USER_ID), Mockito.any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        final EmailVerificationCodeEmailPayload[] payloadSlot = capturePayloadSlot();

        // 2.Exercise
        service.issueFreshVerificationCode(USER_ID, EMAIL, new Locale("es"));

        // 3.Assert
        Assertions.assertEquals(new Locale("es"), payloadSlot[0].getMessageLocale());
    }

    @Test
    void testEnsurePendingVerificationCodeShortCircuitsWhenActiveCodeExists() {
        // 1.Arrange
        Mockito.when(dao.hasActiveCode(Mockito.eq(USER_ID), Mockito.any(Instant.class))).thenReturn(true);
        final EmailVerificationCodeEmailPayload[] slot = capturePayloadSlot();

        // 2.Exercise
        service.ensurePendingVerificationCode(USER_ID, EMAIL, Locale.ENGLISH);

        // 3.Assert
        Assertions.assertNull(slot[0]);
    }

    @Test
    void testEnsurePendingVerificationCodeIssuesNewCodeWhenNoneActive() {
        // 1.Arrange
        Mockito.when(dao.hasActiveCode(Mockito.eq(USER_ID), Mockito.any(Instant.class))).thenReturn(false);
        Mockito.when(userService.resolveMailLocaleOrElse(USER_ID, Locale.ENGLISH)).thenReturn(Locale.ENGLISH);
        final EmailVerificationCodeEmailPayload[] payloadSlot = capturePayloadSlot();

        // 2.Exercise
        service.ensurePendingVerificationCode(USER_ID, EMAIL, Locale.ENGLISH);

        // 3.Assert
        Assertions.assertNotNull(payloadSlot[0]);
        Assertions.assertEquals(EMAIL, payloadSlot[0].getRecipientEmail());
    }

    @Test
    void testResendVerificationCodeThrowsWhenAlreadyActive() {
        // 1.Arrange
        Mockito.when(dao.hasActiveCode(Mockito.eq(USER_ID), Mockito.any(Instant.class))).thenReturn(true);

        // 2.Exercise
        final VerificationCodeAlreadyActiveException ex = Assertions.assertThrows(
                VerificationCodeAlreadyActiveException.class,
                () -> service.resendVerificationCode(USER_ID, EMAIL, Locale.ENGLISH));

        // 3.Assert
        Assertions.assertEquals(MessageKeys.USER_VERIFICATION_CODE_ALREADY_ACTIVE, ex.getMessageCode());
    }

    @Test
    void testResendVerificationCodeProducesNewCodeWhenNoneActive() {
        // 1.Arrange
        Mockito.when(dao.hasActiveCode(Mockito.eq(USER_ID), Mockito.any(Instant.class))).thenReturn(false);
        Mockito.when(userService.resolveMailLocaleOrElse(USER_ID, Locale.ENGLISH)).thenReturn(Locale.ENGLISH);
        final EmailVerificationCodeEmailPayload[] payloadSlot = capturePayloadSlot();

        // 2.Exercise
        service.resendVerificationCode(USER_ID, EMAIL, Locale.ENGLISH);

        // 3.Assert
        Assertions.assertNotNull(payloadSlot[0]);
        Assertions.assertEquals(EMAIL, payloadSlot[0].getRecipientEmail());
        Assertions.assertTrue(payloadSlot[0].getCode().matches("\\d{6}"));
    }

    @Test
    void testVerifyEmailAndConsumeCodeThrowsWhenUserMissing() {
        // 1.Arrange
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // 2.Exercise
        final UserNotFoundException ex = Assertions.assertThrows(UserNotFoundException.class,
                () -> service.verifyEmailAndConsumeCode(EMAIL, "123456"));

        // 3.Assert
        Assertions.assertEquals(MessageKeys.USER_ACCOUNT_NOT_FOUND, ex.getMessageCode());
    }

    @Test
    void testVerifyEmailAndConsumeCodeThrowsWhenCodeInvalid() {
        // 1.Arrange
        final User user = User.identities(USER_ID, EMAIL, "Ada", "Lovelace");
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        Mockito.when(dao.deleteIfValid(Mockito.eq(USER_ID), Mockito.eq("000000"), Mockito.any(Instant.class)))
                .thenReturn(false);

        // 2.Exercise
        final VerificationCodeInvalidException ex = Assertions.assertThrows(
                VerificationCodeInvalidException.class,
                () -> service.verifyEmailAndConsumeCode(EMAIL, "000000"));

        // 3.Assert
        Assertions.assertEquals(MessageKeys.USER_VERIFICATION_CODE_INVALID, ex.getMessageCode());
    }

    @Test
    void testVerifyEmailAndConsumeCodeReturnsUserIdOnSuccess() {
        // 1.Arrange
        final User user = User.identities(USER_ID, EMAIL, "Ada", "Lovelace");
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        Mockito.when(dao.deleteIfValid(Mockito.eq(USER_ID), Mockito.eq("123456"), Mockito.any(Instant.class)))
                .thenReturn(true);

        // 2.Exercise
        final long returnedId = service.verifyEmailAndConsumeCode(EMAIL, "123456");

        // 3.Assert
        Assertions.assertEquals(USER_ID, returnedId);
    }
}
