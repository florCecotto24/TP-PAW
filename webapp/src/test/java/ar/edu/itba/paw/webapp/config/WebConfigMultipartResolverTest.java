package ar.edu.itba.paw.webapp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class WebConfigMultipartResolverTest {

    private static final int BYTES_PER_MIB = 1048576;

    @Test
    void testResolveMaxMultipartRequestBytesReadsMegabytesProperty() {
        final MockEnvironment environment = new MockEnvironment();
        environment.setProperty("app.upload.max-multipart-request-megabytes", "225");
        environment.setProperty("app.upload.bytes-per-binary-megabyte", String.valueOf(BYTES_PER_MIB));

        assertEquals(225L * BYTES_PER_MIB, WebConfig.resolveMaxMultipartRequestBytes(environment));
    }

    @Test
    void testResolveMaxMultipartRequestBytesFallbackWhenMegabytesUnset() {
        final MockEnvironment environment = new MockEnvironment();

        assertEquals(188743680L, WebConfig.resolveMaxMultipartRequestBytes(environment));
    }

    @Test
    void testResolveMaxMultipartRequestBytesPrefersExplicitBytes() {
        final MockEnvironment environment = new MockEnvironment();
        environment.setProperty("app.upload.max-multipart-request-bytes", "50000000");
        environment.setProperty("app.upload.max-multipart-request-megabytes", "225");

        assertEquals(50_000_000L, WebConfig.resolveMaxMultipartRequestBytes(environment));
    }
}
