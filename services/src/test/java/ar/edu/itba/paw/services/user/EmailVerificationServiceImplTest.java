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

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.exception.user.VerificationCodeAlreadyActiveException;
import ar.edu.itba.paw.exception.user.VerificationCodeInvalidException;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.util.rules.SupportedLocales;
import ar.edu.itba.paw.policy.VerificationCodePolicy;

import ar.edu.itba.paw.services.email.EmailService;
import ar.edu.itba.paw.services.support.InMemoryEmailVerificationCodeDao;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceImplTest {

    private static final long USER_ID = 11L;
    private static final String EMAIL = "user@example.com";

    // In-memory fake per AGENTS.md TEST-8: the persisted code row is the observable — tests read
    // it back through the fake's stored state instead of capturing interactions with a mock DAO.
    private InMemoryEmailVerificationCodeDao dao;

    @Mock
    private EmailService emailService;

    @Mock
    private UserService userService;

    @Mock
    private UserLocaleService userLocaleService;

    private EmailVerificationServiceImpl service;

    @BeforeEach
    void setUp() {
        dao = new InMemoryEmailVerificationCodeDao();
        service = new EmailVerificationServiceImpl(
                dao, emailService, userService, userLocaleService,
                VerificationCodePolicy.fromValidatedConfiguration(6, 5),
                OtpAttemptLimiter.forTests(8, Duration.ofMinutes(15)));
    }

    @Test
    void testIssueFreshVerificationCodePersistsSixDigitCode() {
        // 1.Arrange
        Mockito.when(userLocaleService.resolveMailLocaleOrElse(USER_ID, SupportedLocales.DEFAULT))
                .thenReturn(new Locale("es"));

        // 2.Act
        service.issueFreshVerificationCode(USER_ID, EMAIL, null);

        // 3.Assert
        final Optional<String> stored = dao.storedCodeFor(USER_ID);
        Assertions.assertTrue(stored.isPresent());
        Assertions.assertTrue(stored.get().matches("\\d{6}"),
                "persisted code must be six decimal digits");
    }

    @Test
    void testIssueFreshVerificationCodeReplacesPreviouslyStoredCode() {
        // 1.Arrange
        dao.seedCode(USER_ID, "STALE-SENTINEL", Instant.now().plus(Duration.ofMinutes(5)));
        Mockito.when(userLocaleService.resolveMailLocaleOrElse(USER_ID, Locale.ENGLISH))
                .thenReturn(Locale.ENGLISH);

        // 2.Act
        service.issueFreshVerificationCode(USER_ID, EMAIL, Locale.ENGLISH);

        // 3.Assert
        Assertions.assertTrue(dao.storedCodeFor(USER_ID).orElseThrow().matches("\\d{6}"),
                "a fresh six-digit code must replace the previously stored one");
    }

    @Test
    void testEnsurePendingVerificationCodeShortCircuitsWhenActiveCodeExists() {
        // 1.Arrange
        dao.seedCode(USER_ID, "111111", Instant.now().plus(Duration.ofMinutes(5)));

        // 2.Act
        service.ensurePendingVerificationCode(USER_ID, EMAIL, Locale.ENGLISH);

        // 3.Assert
        Assertions.assertEquals(Optional.of("111111"), dao.storedCodeFor(USER_ID),
                "the already active code must be kept untouched");
    }

    @Test
    void testEnsurePendingVerificationCodeIssuesNewCodeWhenNoneActive() {
        // 1.Arrange
        Mockito.when(userLocaleService.resolveMailLocaleOrElse(USER_ID, Locale.ENGLISH))
                .thenReturn(Locale.ENGLISH);

        // 2.Act
        service.ensurePendingVerificationCode(USER_ID, EMAIL, Locale.ENGLISH);

        // 3.Assert
        Assertions.assertTrue(dao.storedCodeFor(USER_ID).orElseThrow().matches("\\d{6}"),
                "a six-digit code must be persisted when none was active");
    }

    @Test
    void testResendVerificationCodeThrowsWhenAlreadyActive() {
        // 1.Arrange
        dao.seedCode(USER_ID, "111111", Instant.now().plus(Duration.ofMinutes(5)));

        // 2.Act / 3.Assert
        final VerificationCodeAlreadyActiveException ex = Assertions.assertThrows(
                VerificationCodeAlreadyActiveException.class,
                () -> service.resendVerificationCode(USER_ID, EMAIL, Locale.ENGLISH));

        Assertions.assertEquals(MessageKeys.USER_VERIFICATION_CODE_ALREADY_ACTIVE, ex.getMessageCode());
    }

    @Test
    void testResendVerificationCodePersistsNewCodeWhenNoneActive() {
        // 1.Arrange
        dao.seedCode(USER_ID, "EXPIRED-SENTINEL", Instant.now().minusSeconds(60));
        Mockito.when(userLocaleService.resolveMailLocaleOrElse(USER_ID, Locale.ENGLISH))
                .thenReturn(Locale.ENGLISH);

        // 2.Act
        service.resendVerificationCode(USER_ID, EMAIL, Locale.ENGLISH);

        // 3.Assert
        Assertions.assertTrue(dao.storedCodeFor(USER_ID).orElseThrow().matches("\\d{6}"),
                "the expired row must be replaced by a fresh six-digit code");
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
        dao.seedCode(USER_ID, "123456", Instant.now().plus(Duration.ofMinutes(5)));

        // 2.Act / 3.Assert
        final VerificationCodeInvalidException ex = Assertions.assertThrows(
                VerificationCodeInvalidException.class,
                () -> service.verifyEmailAndConsumeCode(EMAIL, "000000"));

        Assertions.assertEquals(MessageKeys.USER_VERIFICATION_CODE_INVALID, ex.getMessageCode());
    }

    @Test
    void testVerifyEmailAndConsumeCodeThrowsWhenCodeExpired() {
        // 1.Arrange
        final User user = User.identities(USER_ID, EMAIL, "Ada", "Lovelace");
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        dao.seedCode(USER_ID, "123456", Instant.now().minusSeconds(60));

        // 2.Act / 3.Assert
        final VerificationCodeInvalidException ex = Assertions.assertThrows(
                VerificationCodeInvalidException.class,
                () -> service.verifyEmailAndConsumeCode(EMAIL, "123456"));

        Assertions.assertEquals(MessageKeys.USER_VERIFICATION_CODE_INVALID, ex.getMessageCode());
    }

    @Test
    void testVerifyEmailAndConsumeCodeReturnsUserIdOnSuccess() {
        // 1.Arrange
        final User user = User.identities(USER_ID, EMAIL, "Ada", "Lovelace");
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        dao.seedCode(USER_ID, "123456", Instant.now().plus(Duration.ofMinutes(5)));

        // 2.Act
        final long returnedId = service.verifyEmailAndConsumeCode(EMAIL, "123456");

        // 3.Assert
        Assertions.assertEquals(USER_ID, returnedId);
    }

    @Test
    void testVerifyEmailAndConsumeCodeConsumesStoredCode() {
        // 1.Arrange
        final User user = User.identities(USER_ID, EMAIL, "Ada", "Lovelace");
        Mockito.when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        dao.seedCode(USER_ID, "123456", Instant.now().plus(Duration.ofMinutes(5)));

        // 2.Act
        service.verifyEmailAndConsumeCode(EMAIL, "123456");

        // 3.Assert
        Assertions.assertTrue(dao.storedCodeFor(USER_ID).isEmpty(),
                "the code must be consumed (deleted) after a successful verification");
    }
}
