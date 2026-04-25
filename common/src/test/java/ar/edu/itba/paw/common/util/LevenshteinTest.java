package ar.edu.itba.paw.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LevenshteinTest {

    @Test
    public void distanceBothNullIsZero() {
        Assertions.assertEquals(0, Levenshtein.distance(null, null));
    }

    @Test
    public void distanceOneNullIsOtherLength() {
        Assertions.assertEquals(3, Levenshtein.distance(null, "abc"));
        Assertions.assertEquals(3, Levenshtein.distance("abc", null));
    }

    @Test
    public void distanceEmptyStrings() {
        Assertions.assertEquals(0, Levenshtein.distance("", ""));
        Assertions.assertEquals(2, Levenshtein.distance("", "ab"));
    }

    @Test
    public void distanceEqualStringsIsZero() {
        Assertions.assertEquals(0, Levenshtein.distance("palermo", "palermo"));
    }

    @Test
    public void distanceKnownExamples() {
        Assertions.assertEquals(1, Levenshtein.distance("palermo", "palerm0"));
        Assertions.assertEquals(3, Levenshtein.distance("kitten", "sitting"));
    }
}
