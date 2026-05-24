package ar.edu.itba.paw.exception.car;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AvailabilityRiderLeadViolationExceptionTest {

    @Test
    void testGetAvailabilityRowIndexReturnsConstructorInput() {
        // 1.Arrange / 2.Exercise
        final AvailabilityRiderLeadViolationException ex =
                new AvailabilityRiderLeadViolationException(3, "car.availability.leadViolation", 24);

        // 3.Assert
        Assertions.assertEquals(3, ex.getAvailabilityRowIndex());
    }

    @Test
    void testMessageCodeAndArgsArePassedThroughToRydenException() {
        // 1.Arrange / 2.Exercise
        final AvailabilityRiderLeadViolationException ex =
                new AvailabilityRiderLeadViolationException(0, "some.key", "a", 7);

        // 3.Assert
        Assertions.assertEquals("some.key", ex.getMessageCode());
        Assertions.assertArrayEquals(new Object[]{"a", 7}, ex.getMessageArgs());
    }

    @Test
    void testIsCarValidationExceptionForControllerAdviceMatching() {
        // 1.Arrange / 2.Exercise
        final AvailabilityRiderLeadViolationException ex =
                new AvailabilityRiderLeadViolationException(2, "k");

        // 3.Assert
        Assertions.assertTrue(ex instanceof CarValidationException);
    }
}
