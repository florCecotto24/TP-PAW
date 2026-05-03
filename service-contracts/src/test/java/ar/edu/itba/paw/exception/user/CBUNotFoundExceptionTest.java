package ar.edu.itba.paw.exception.user;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ar.edu.itba.paw.exception.RydenException;

class CBUNotFoundExceptionTest {

    @Test
    void testGetMessageContainsTheUserIdInThePersistedFormat() {
        // 1.Arrange / 2.Exercise
        final CBUNotFoundException ex = new CBUNotFoundException(42L);

        // 3.Assert
        Assertions.assertEquals("CBU not found for userId = 42", ex.getMessage());
    }

    @Test
    void testIsRuntimeExceptionAndNotPartOfRydenFamily() {
        // 1.Arrange / 2.Exercise
        final Object ex = new CBUNotFoundException(1L);

        // 3.Assert
        Assertions.assertTrue(ex instanceof RuntimeException);
        Assertions.assertFalse(ex instanceof RydenException);
    }
}
