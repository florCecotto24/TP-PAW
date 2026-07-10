package ar.edu.itba.paw.exception.email;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class EmailMessagingExceptionTest {

    @Test
    void testIsCheckedExceptionAndNotARuntimeException() {
        // 1.Arrange / 2.Act
        final Object ex = EmailMessagingException.withMessage("boom");

        // 3.Assert
        Assertions.assertTrue(ex instanceof Exception);
        Assertions.assertFalse(ex instanceof RuntimeException);
    }

    @Test
    void testWithMessagePropagatesMessageAndLeavesCauseNull() {
        // 1.Arrange / 2.Act
        final EmailMessagingException ex = EmailMessagingException.withMessage("template render failed");

        // 3.Assert
        Assertions.assertEquals("template render failed", ex.getMessage());
        Assertions.assertNull(ex.getCause());
    }

    @Test
    void testWithMessageAndCausePropagatesBoth() {
        // 1.Arrange
        final IllegalStateException cause = new IllegalStateException("smtp dead");

        // 2.Act
        final EmailMessagingException ex = EmailMessagingException.withMessageAndCause("send failed", cause);

        // 3.Assert
        Assertions.assertEquals("send failed", ex.getMessage());
        Assertions.assertSame(cause, ex.getCause());
    }

    @Test
    void testWrappingPropagatesCauseAndDerivesMessage() {
        // 1.Arrange
        final IllegalStateException cause = new IllegalStateException("smtp dead");

        // 2.Act
        final EmailMessagingException ex = EmailMessagingException.wrapping(cause);

        // 3.Assert
        Assertions.assertSame(cause, ex.getCause());
        Assertions.assertNotNull(ex.getMessage());
        Assertions.assertTrue(ex.getMessage().contains("smtp dead"));
    }
}
