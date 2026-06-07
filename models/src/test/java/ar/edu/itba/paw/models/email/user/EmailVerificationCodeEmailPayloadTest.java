package ar.edu.itba.paw.models.email.user;

import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class EmailVerificationCodeEmailPayloadTest {

    @Test
    void testBuildSucceedsWithAllRequiredFields() {
        // 1.Arrange / 2.Exercise
        final EmailVerificationCodeEmailPayload payload = EmailVerificationCodeEmailPayload.builder()
                .messageLocale(Locale.ENGLISH)
                .recipientEmail("user@example.com")
                .code("123456")
                .build();

        // 3.Assert
        Assertions.assertEquals(Locale.ENGLISH, payload.getMessageLocale());
        Assertions.assertEquals("user@example.com", payload.getRecipientEmail());
        Assertions.assertEquals("123456", payload.getCode());
    }

    @Test
    void testBuildRejectsNullMessageLocale() {
        // 1.Arrange
        final EmailVerificationCodeEmailPayload.Builder builder = EmailVerificationCodeEmailPayload.builder()
                .recipientEmail("user@example.com")
                .code("123456");

        // 2.Exercise / 3.Assert
        Assertions.assertThrows(NullPointerException.class, builder::build);
    }

    @Test
    void testBuildRejectsNullRecipientEmail() {
        // 1.Arrange
        final EmailVerificationCodeEmailPayload.Builder builder = EmailVerificationCodeEmailPayload.builder()
                .messageLocale(Locale.ENGLISH)
                .code("123456");

        // 2.Exercise / 3.Assert
        Assertions.assertThrows(NullPointerException.class, builder::build);
    }

    @Test
    void testBuildRejectsNullCode() {
        // 1.Arrange
        final EmailVerificationCodeEmailPayload.Builder builder = EmailVerificationCodeEmailPayload.builder()
                .messageLocale(Locale.ENGLISH)
                .recipientEmail("user@example.com");

        // 2.Exercise / 3.Assert
        Assertions.assertThrows(NullPointerException.class, builder::build);
    }
}
