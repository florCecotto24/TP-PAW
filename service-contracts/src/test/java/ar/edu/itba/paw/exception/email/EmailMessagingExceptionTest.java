package ar.edu.itba.paw.exception.email;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class EmailMessagingExceptionTest {

    @Test
    void testIsCheckedExceptionAndNotARuntimeException() {
        // 1.Arrange / 2.Act
        final Object ex = new EmailMessagingException("boom");

        // 3.Assert
        Assertions.assertTrue(ex instanceof Exception);
        Assertions.assertFalse(ex instanceof RuntimeException);
    }

    @Test
    void testMessageOnlyConstructorPropagatesMessageAndLeavesCauseNull() {
        // 1.Arrange / 2.Act
        final EmailMessagingException ex = new EmailMessagingException("template render failed");

        // 3.Assert
        Assertions.assertEquals("template render failed", ex.getMessage());
        Assertions.assertNull(ex.getCause());
    }

    @Test
    void testMessageAndCauseConstructorPropagatesBoth() {
        // 1.Arrange
        final IllegalStateException cause = new IllegalStateException("smtp dead");

        // 2.Act
        final EmailMessagingException ex = new EmailMessagingException("send failed", cause);

        // 3.Assert
        Assertions.assertEquals("send failed", ex.getMessage());
        Assertions.assertSame(cause, ex.getCause());
    }

    @Test
    void testCauseOnlyConstructorPropagatesCauseAndDerivesMessage() {
        // 1.Arrange
        final IllegalStateException cause = new IllegalStateException("smtp dead");

        // 2.Act
        final EmailMessagingException ex = new EmailMessagingException(cause);

        // 3.Assert
        Assertions.assertSame(cause, ex.getCause());
        Assertions.assertNotNull(ex.getMessage());
        Assertions.assertTrue(ex.getMessage().contains("smtp dead"));
    }
}
