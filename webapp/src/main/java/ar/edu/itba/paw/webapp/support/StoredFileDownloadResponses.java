package ar.edu.itba.paw.webapp.support;


import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ar.edu.itba.paw.models.dto.file.BinaryContent;

/**
 * Shared MIME parsing + filename sanitization + ResponseEntity assembly for the various
 * stored-file download handlers (payment/refund receipts, profile documents, etc).
 *
 * Operates on {@link BinaryContent} (a transaction-detached holder of bytes + metadata)
 * rather than on the JPA {@code StoredFile} entity. Keeps the call site decoupled from
 * persistence concerns: callers fetch the content through service helpers and pass the
 * immutable holder here, so the response builder never imports the JPA entity
 *
 * <p>Centralises the three pieces of boilerplate that every controller download handler
 * duplicated: parsing the persisted Content-Type into a {@link MediaType} (with a safe
 * fallback to {@link MediaType#APPLICATION_OCTET_STREAM}), building a sanitized inline
 * {@code Content-Disposition}, and producing the {@link ResponseEntity} with the bytes.</p>
 */
public final class StoredFileDownloadResponses {

    private static final Logger LOG = LoggerFactory.getLogger(StoredFileDownloadResponses.class);

    private static final String DEFAULT_FALLBACK_FILENAME = "file";
    private static final int MAX_FILENAME_LENGTH = 120;

    private StoredFileDownloadResponses() { }

    /**
     * Builds an {@code inline} download response for the given file. The {@code logContext}
     * is included in the warning emitted when the persisted Content-Type cannot be parsed.
     */
    public static ResponseEntity<byte[]> inlineDownload(
            final BinaryContent file, final String logContext) {
        final MediaType contentType = safeMediaType(file.getContentType(), logContext);
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        final String safeName = sanitizeFileName(file.getFileName());
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safeName + "\"");
        return new ResponseEntity<>(file.getBytes(), headers, HttpStatus.OK);
    }

    /**
     * Variant that does NOT add a {@code Content-Disposition} header — useful for endpoints
     * that just want to serve the raw bytes (e.g. profile documents shown via {@code <embed>}).
     * Sets {@code Content-Length} alongside the media type.
     */
    public static ResponseEntity<byte[]> rawBytes(
            final BinaryContent file, final String logContext) {
        final MediaType contentType = safeMediaType(file.getContentType(), logContext);
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        headers.setContentLength(file.getSize());
        return new ResponseEntity<>(file.getBytes(), headers, HttpStatus.OK);
    }

    /** Optional-aware wrapper: returns 404 when {@code fileOpt} is empty. */
    public static ResponseEntity<byte[]> inlineOr404(
            final Optional<BinaryContent> fileOpt, final String logContext) {
        return fileOpt
                .map(sf -> inlineDownload(sf, logContext))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Optional-aware wrapper for {@link #rawBytes(BinaryContent, String)}. */
    public static ResponseEntity<byte[]> rawBytesOr404(
            final Optional<BinaryContent> fileOpt, final String logContext) {
        return fileOpt
                .map(sf -> rawBytes(sf, logContext))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static MediaType safeMediaType(final String rawContentType, final String logContext) {
        if (rawContentType == null || rawContentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(rawContentType);
        } catch (final IllegalArgumentException e) {
            LOG.atDebug()
                    .setMessage("Invalid stored Content-Type {} [{}]")
                    .addArgument(logContext)
                    .addArgument(rawContentType)
                    .setCause(e)
                    .log();
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private static String sanitizeFileName(final String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_FALLBACK_FILENAME;
        }
        final String trimmed = raw.trim().replace("\"", "'").replaceAll("[\\\\/:*?|<>\\r\\n]+", "_");
        return trimmed.length() > MAX_FILENAME_LENGTH ? trimmed.substring(0, MAX_FILENAME_LENGTH) : trimmed;
    }
}
