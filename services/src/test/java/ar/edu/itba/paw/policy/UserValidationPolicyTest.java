package ar.edu.itba.paw.policy;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UserValidationPolicyTest {

    @Test
    void testFromValidatedConfigurationBuildsPolicyWithGetters() {
        // 1.Arrange / 2.Exercise
        final UserValidationPolicy policy = UserValidationPolicy.fromValidatedConfiguration(
                8, 64, 200, 50, 30, 500, "^\\+?\\d{6,30}$");

        // 3.Assert
        Assertions.assertEquals(8, policy.getRegistrationPasswordMinLength());
        Assertions.assertEquals(64, policy.getRegistrationPasswordMaxLength());
        Assertions.assertEquals(200, policy.getRegistrationEmailMaxLength());
        Assertions.assertEquals(50, policy.getDisplayNamePartMaxLength());
        Assertions.assertEquals(30, policy.getProfilePhoneMaxLength());
        Assertions.assertEquals(500, policy.getProfileAboutMaxLength());
        final Pattern pattern = policy.getProfilePhonePattern();
        Assertions.assertNotNull(pattern);
        Assertions.assertTrue(pattern.matcher("+541123456789").matches());
        Assertions.assertFalse(pattern.matcher("abc").matches());
    }

    @Test
    void testFromValidatedConfigurationRejectsZeroPasswordMinLength() {
        // 1.Arrange / 2.Exercise / 3.Assert
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UserValidationPolicy.fromValidatedConfiguration(0, 64, 200, 50, 30, 500, ".*"));
    }

    @Test
    void testFromValidatedConfigurationRejectsPasswordMaxLessThanMin() {
        // 1.Arrange / 2.Exercise / 3.Assert
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UserValidationPolicy.fromValidatedConfiguration(10, 5, 200, 50, 30, 500, ".*"));
    }

    @Test
    void testFromValidatedConfigurationAllowsPasswordMaxEqualToMin() {
        // 1.Arrange / 2.Exercise
        final UserValidationPolicy policy = UserValidationPolicy.fromValidatedConfiguration(
                10, 10, 200, 50, 30, 500, ".*");

        // 3.Assert
        Assertions.assertEquals(10, policy.getRegistrationPasswordMinLength());
        Assertions.assertEquals(10, policy.getRegistrationPasswordMaxLength());
    }

    @Test
    void testFromValidatedConfigurationRejectsZeroEmailMaxLength() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UserValidationPolicy.fromValidatedConfiguration(8, 64, 0, 50, 30, 500, ".*"));
    }

    @Test
    void testFromValidatedConfigurationRejectsZeroDisplayNameMax() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UserValidationPolicy.fromValidatedConfiguration(8, 64, 200, 0, 30, 500, ".*"));
    }

    @Test
    void testFromValidatedConfigurationRejectsZeroProfilePhoneMax() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UserValidationPolicy.fromValidatedConfiguration(8, 64, 200, 50, 0, 500, ".*"));
    }

    @Test
    void testFromValidatedConfigurationRejectsZeroProfileAboutMax() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UserValidationPolicy.fromValidatedConfiguration(8, 64, 200, 50, 30, 0, ".*"));
    }

    @Test
    void testFromValidatedConfigurationRejectsInvalidRegex() {
        // 1.Arrange / 2.Exercise / 3.Assert: unmatched bracket
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UserValidationPolicy.fromValidatedConfiguration(8, 64, 200, 50, 30, 500, "[abc"));
    }
}
