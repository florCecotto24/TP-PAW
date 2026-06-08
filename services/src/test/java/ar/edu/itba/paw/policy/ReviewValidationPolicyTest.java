package ar.edu.itba.paw.policy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ReviewValidationPolicyTest {

    @Test
    void testFromValidatedCommentMaxLengthRejectsZero() {
        // 1.Arrange / 2.Act / 3.Assert
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> ReviewValidationPolicy.fromValidatedCommentMaxLength(0));
    }

    @Test
    void testFromValidatedCommentMaxLengthRejectsNegative() {
        // 1.Arrange / 2.Act / 3.Assert
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> ReviewValidationPolicy.fromValidatedCommentMaxLength(-10));
    }

    @Test
    void testFromValidatedCommentMaxLengthAcceptsOne() {
        // 1.Arrange / 2.Act
        final ReviewValidationPolicy policy = ReviewValidationPolicy.fromValidatedCommentMaxLength(1);

        // 3.Assert
        Assertions.assertEquals(1, policy.getCommentMaxLength());
    }

    @Test
    void testFromValidatedCommentMaxLengthExposesValueViaGetter() {
        // 1.Arrange / 2.Act
        final ReviewValidationPolicy policy = ReviewValidationPolicy.fromValidatedCommentMaxLength(500);

        // 3.Assert
        Assertions.assertEquals(500, policy.getCommentMaxLength());
    }
}
