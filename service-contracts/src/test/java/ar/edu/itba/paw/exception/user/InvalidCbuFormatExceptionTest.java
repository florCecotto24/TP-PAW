package ar.edu.itba.paw.exception.user;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ar.edu.itba.paw.exception.MessageKeys;

class InvalidCbuFormatExceptionTest {

    @Test
    void testConstructorSetsCanonicalCbuFormatMessageKey() {
        // 1.Arrange / 2.Act
        final InvalidCbuFormatException ex = new InvalidCbuFormatException(22);

        // 3.Assert
        Assertions.assertEquals(MessageKeys.USER_PROFILE_CBU_INVALID, ex.getMessageCode());
    }

    @Test
    void testConstructorPassesRequiredDigitLengthAsTheOnlyMessageArg() {
        // 1.Arrange / 2.Act
        final InvalidCbuFormatException ex = new InvalidCbuFormatException(22);

        // 3.Assert
        Assertions.assertArrayEquals(new Object[]{22}, ex.getMessageArgs());
    }

    @Test
    void testIsAUserExceptionForControllerAdviceMatching() {
        // 1.Arrange / 2.Act
        final InvalidCbuFormatException ex = new InvalidCbuFormatException(22);

        // 3.Assert
        Assertions.assertTrue(ex instanceof UserException);
    }
}
