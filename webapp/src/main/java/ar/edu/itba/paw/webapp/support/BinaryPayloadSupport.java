package ar.edu.itba.paw.webapp.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.webapp.form.common.OctetStreamBodyForm;

/** Reads and validates octet-stream bodies via Bean Validation, with a hard size ceiling. */
@Component
public final class BinaryPayloadSupport {

    private static final int READ_CHUNK_BYTES = 8192;

    private final FormValidationSupport formValidationSupport;
    private final long maxBytes;

    public BinaryPayloadSupport(
            final FormValidationSupport formValidationSupport,
            final Environment environment) {
        this.formValidationSupport = formValidationSupport;
        this.maxBytes = resolveMaxBytes(environment);
    }

    public byte[] readValidatedBody(final InputStream body) throws IOException {
        final byte[] bytes = body == null ? new byte[0] : readBounded(body, maxBytes);
        formValidationSupport.validate(OctetStreamBodyForm.of(bytes));
        return bytes;
    }

    private static byte[] readBounded(final InputStream body, final long maxBytes) throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final byte[] chunk = new byte[READ_CHUNK_BYTES];
        long total = 0L;
        int read;
        while ((read = body.read(chunk)) >= 0) {
            total += read;
            if (total > maxBytes) {
                throw new WebApplicationException(Response.Status.REQUEST_ENTITY_TOO_LARGE);
            }
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    private static long resolveMaxBytes(final Environment environment) {
        final String bytesProperty = environment.getProperty("app.upload.max-multipart-request-bytes");
        if (bytesProperty != null && !bytesProperty.isBlank()) {
            return Long.parseLong(bytesProperty.trim());
        }
        final Long megabytes = environment.getProperty("app.upload.max-multipart-request-megabytes", Long.class);
        final int bytesPerMegabyte =
                environment.getProperty("app.upload.bytes-per-binary-megabyte", Integer.class, 1048576);
        if (megabytes != null && megabytes > 0) {
            return megabytes * (long) bytesPerMegabyte;
        }
        return 188743680L;
    }
}
