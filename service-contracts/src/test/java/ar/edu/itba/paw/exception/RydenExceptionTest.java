package ar.edu.itba.paw.exception;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RydenExceptionTest {

    /**
     * Concrete subclass for exercising {@link RydenException}'s contract directly,
     * since {@code RydenException} is abstract.
     */
    private static final class TestException extends RydenException {
        TestException(final String messageCode, final Object... args) {
            super(messageCode, args);
        }
    }

    @Test
    void testGetMessageReturnsTheRawMessageCode() {
        // 1.Arrange / 2.Exercise
        final TestException ex = new TestException("some.error.key", 1, "two");

        // 3.Assert
        Assertions.assertEquals("some.error.key", ex.getMessage());
    }

    @Test
    void testGetMessageCodeReflectsConstructorInput() {
        // 1.Arrange / 2.Exercise
        final TestException ex = new TestException("user.account.notFound");

        // 3.Assert
        Assertions.assertEquals("user.account.notFound", ex.getMessageCode());
    }

    @Test
    void testGetMessageArgsContainsConstructorVarargs() {
        // 1.Arrange / 2.Exercise
        final TestException ex = new TestException("k", 22, "abc", 3.14);

        // 3.Assert
        Assertions.assertArrayEquals(new Object[]{22, "abc", 3.14}, ex.getMessageArgs());
    }

    @Test
    void testGetMessageArgsReturnsDefensiveCopySoExternalMutationDoesNotLeak() {
        // 1.Arrange
        final TestException ex = new TestException("k", "original");

        // 2.Exercise
        final Object[] borrowed = ex.getMessageArgs();
        borrowed[0] = "tampered";

        // 3.Assert
        Assertions.assertArrayEquals(new Object[]{"original"}, ex.getMessageArgs());
    }

    @Test
    void testConstructorTreatsNullVarargsAsEmptyArray() {
        // 1.Arrange / 2.Exercise
        final TestException ex = new TestException("k", (Object[]) null);

        // 3.Assert
        Assertions.assertNotNull(ex.getMessageArgs());
        Assertions.assertEquals(0, ex.getMessageArgs().length);
    }

    @Test
    void testConstructorAcceptsEmptyVarargs() {
        // 1.Arrange / 2.Exercise
        final TestException ex = new TestException("k");

        // 3.Assert
        Assertions.assertEquals(0, ex.getMessageArgs().length);
    }
}
