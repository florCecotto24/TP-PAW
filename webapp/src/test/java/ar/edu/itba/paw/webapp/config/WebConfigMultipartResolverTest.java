package ar.edu.itba.paw.webapp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class WebConfigMultipartResolverTest {

    private static final int BYTES_PER_MIB = 1048576;

    @Test
    void testResolveMaxMultipartRequestBytesReadsMegabytesProperty() {
        // 1.Arrange
        final MockEnvironment environment = new MockEnvironment();
        environment.setProperty("app.upload.max-multipart-request-megabytes", "225");
        environment.setProperty("app.upload.bytes-per-binary-megabyte", String.valueOf(BYTES_PER_MIB));

        // 2.Act
        final long resolved = WebConfig.resolveMaxMultipartRequestBytes(environment);

        // 3.Assert
        assertEquals(225L * BYTES_PER_MIB, resolved);
    }

    @Test
    void testResolveMaxMultipartRequestBytesFallbackWhenMegabytesUnset() {
        // 1.Arrange
        final MockEnvironment environment = new MockEnvironment();

        // 2.Act
        final long resolved = WebConfig.resolveMaxMultipartRequestBytes(environment);

        // 3.Assert
        assertEquals(188743680L, resolved);
    }

    @Test
    void testResolveMaxMultipartRequestBytesPrefersExplicitBytes() {
        // 1.Arrange
        final MockEnvironment environment = new MockEnvironment();
        environment.setProperty("app.upload.max-multipart-request-bytes", "50000000");
        environment.setProperty("app.upload.max-multipart-request-megabytes", "225");

        // 2.Act
        final long resolved = WebConfig.resolveMaxMultipartRequestBytes(environment);

        // 3.Assert
        assertEquals(50_000_000L, resolved);
    }
}
