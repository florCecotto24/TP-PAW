package ar.edu.itba.paw.models.util.search;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ReservationHubStatusWhitelistTest {

    @Test
    public void testNormalizeStatusQueryParamTrimsAndLowercasesWhitelisted() {
        Assertions.assertEquals("pending", ReservationHubStatusWhitelist.normalizeStatusQueryParam("  PeNdIng  "));
        Assertions.assertEquals("accepted", ReservationHubStatusWhitelist.normalizeStatusQueryParam("ACCEPTED"));
    }

    @Test
    public void testNormalizeStatusQueryParamReturnsNullWhenUnknownOrBlank() {
        Assertions.assertNull(ReservationHubStatusWhitelist.normalizeStatusQueryParam("bogus"));
        Assertions.assertNull(ReservationHubStatusWhitelist.normalizeStatusQueryParam(""));
        Assertions.assertNull(ReservationHubStatusWhitelist.normalizeStatusQueryParam(null));
    }

    @Test
    public void testContainsRecognizesWhitelistedLowercaseTokens() {
        Assertions.assertTrue(ReservationHubStatusWhitelist.contains("finished"));
        Assertions.assertFalse(ReservationHubStatusWhitelist.contains("bogus"));
    }
}
