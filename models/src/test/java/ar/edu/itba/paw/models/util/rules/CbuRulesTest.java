package ar.edu.itba.paw.models.util.rules;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CbuRulesTest {

    @Test
    void testIsValidFormatReturnsFalseForNull() {
        // 1.Arrange / 2.Act
        final boolean result = CbuRules.isValidFormat(null);

        // 3.Assert
        Assertions.assertFalse(result);
    }

    @Test
    void testIsValidFormatAcceptsExactlyTwentyTwoDigits() {
        // 1.Arrange
        final String cbu = "0".repeat(CbuRules.REQUIRED_DIGIT_LENGTH);

        // 2.Act
        final boolean result = CbuRules.isValidFormat(cbu);

        // 3.Assert
        Assertions.assertTrue(result);
    }

    @Test
    void testIsValidFormatTrimsLeadingAndTrailingWhitespace() {
        // 1.Arrange
        final String cbu = "  " + "1".repeat(CbuRules.REQUIRED_DIGIT_LENGTH) + " \t";

        // 2.Act
        final boolean result = CbuRules.isValidFormat(cbu);

        // 3.Assert
        Assertions.assertTrue(result);
    }

    @Test
    void testIsValidFormatRejectsTooFewDigits() {
        // 1.Arrange
        final String cbu = "1".repeat(CbuRules.REQUIRED_DIGIT_LENGTH - 1);

        // 2.Act
        final boolean result = CbuRules.isValidFormat(cbu);

        // 3.Assert
        Assertions.assertFalse(result);
    }

    @Test
    void testIsValidFormatRejectsTooManyDigits() {
        // 1.Arrange
        final String cbu = "1".repeat(CbuRules.REQUIRED_DIGIT_LENGTH + 1);

        // 2.Act
        final boolean result = CbuRules.isValidFormat(cbu);

        // 3.Assert
        Assertions.assertFalse(result);
    }

    @Test
    void testIsValidFormatRejectsNonDigitCharacters() {
        // 1.Arrange: 22-char string with one letter.
        final String cbu = "1".repeat(CbuRules.REQUIRED_DIGIT_LENGTH - 1) + "A";

        // 2.Act
        final boolean result = CbuRules.isValidFormat(cbu);

        // 3.Assert
        Assertions.assertFalse(result);
    }

    @Test
    void testIsValidFormatRejectsInternalSpaces() {
        // 1.Arrange
        final String cbu = "1".repeat(11) + " " + "1".repeat(10);

        // 2.Act
        final boolean result = CbuRules.isValidFormat(cbu);

        // 3.Assert
        Assertions.assertFalse(result);
    }
}
