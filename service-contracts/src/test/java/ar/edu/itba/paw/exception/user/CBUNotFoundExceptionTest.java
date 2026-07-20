package ar.edu.itba.paw.exception.user;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.RydenException;

class CBUNotFoundExceptionTest {

    @Test
    void testUsesTheCbuNotFoundMessageCode() {
        // 1.Arrange / 2.Act
        final CBUNotFoundException ex = new CBUNotFoundException(42L);

        // 3.Assert — resolves through the i18n exception bundle, no hardcoded English text.
        Assertions.assertEquals(MessageKeys.USER_CBU_NOT_FOUND, ex.getMessageCode());
    }

    @Test
    void testIsPartOfTheRydenExceptionFamily() {
        // 1.Arrange / 2.Act
        final Object ex = new CBUNotFoundException(1L);

        // 3.Assert — every domain exception maps through the RydenException handlers.
        Assertions.assertTrue(ex instanceof RydenException);
    }

    @Test
    void testExposesTheUserIdForLogging() {
        // 1.Arrange / 2.Act
        final CBUNotFoundException ex = new CBUNotFoundException(42L);

        // 3.Assert
        Assertions.assertEquals(42L, ex.getUserId());
    }
}
