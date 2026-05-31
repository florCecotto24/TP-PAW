package ar.edu.itba.paw.models;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ar.edu.itba.paw.models.util.format.EmailNormalizer;

class EmailNormalizerTest {

    @Test
    void testNormalizeTrimsAndLowercasesUsingRootLocale() {

        //Arrange
        String email1= "UsEr.Name+tag@Example.COM";
        String email2="user.name+tag@example.com";

        // Exercise
        final String result = EmailNormalizer.normalize(email1);
        // Assert
        Assertions.assertEquals(email2, result);
    }

    @Test
    void testNormalizeThrowsWhenEmailIsNull() {

        //Arrange
        String nullEmail=null;

        // Exercise & Assert
        Assertions.assertThrows(NullPointerException.class, () -> EmailNormalizer.normalize(nullEmail));
    }
}
