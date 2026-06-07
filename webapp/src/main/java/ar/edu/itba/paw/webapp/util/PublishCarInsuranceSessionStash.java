package ar.edu.itba.paw.webapp.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Optional;
import java.util.stream.Stream;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import ar.edu.itba.paw.webapp.dto.PublishCarRetainedInsurance;
import ar.edu.itba.paw.webapp.form.car.PublishCarForm;

/**
 * Preserves the optional insurance file from the publish form between POST submissions with errors:
 * the browser does not repopulate {@code <input type="file">}, so we persist the bytes to disk under a
 * directory keyed by the session id, and keep the filename/content-type metadata in the session.
 * Mirrors the design of {@link PublishCarPictureSessionStash} but accepts at most one file at a time.
 */
@Component
public final class PublishCarInsuranceSessionStash {

    private static final Logger LOG = LoggerFactory.getLogger(PublishCarInsuranceSessionStash.class);

    private static final String SESSION_ATTRIBUTE = "ryden.publishCar.retainedInsurance";

    private static final Path STASH_ROOT =
            Path.of(System.getProperty("java.io.tmpdir"), "ryden-publish-insurance-stash");

    private static final String STASH_FILENAME = "insurance.bin";

    public static void deleteStashFilesForSessionId(final String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        try {
            final Path dir = sessionStashDir(sessionId);
            if (Files.isDirectory(dir)) {
                try (Stream<Path> walk = Files.walk(dir)) {
                    walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (final IOException e) {
                            LOG.atDebug()
                                    .setMessage("Could not delete insurance stash path {}: {}")
                                    .addArgument(p)
                                    .addArgument(e.toString())
                                    .setCause(e)
                                    .log();
                        }
                    });
                }
            }
        } catch (final IOException e) {
            LOG.warn("Could not walk or delete insurance stash directory for session {}: {}",
                    sessionId, e.toString(), e);
        }
    }

    public void clear(final HttpSession session) {
        deleteStashFilesForSessionId(session.getId());
        session.removeAttribute(SESSION_ATTRIBUTE);
    }

    public PublishCarRetainedInsurance getOrNull(final HttpSession session) {
        final Object raw = session.getAttribute(SESSION_ATTRIBUTE);
        return (raw instanceof PublishCarRetainedInsurance r) ? r : null;
    }

    public boolean isStashed(final HttpSession session) {
        return getOrNull(session) != null;
    }

    public Optional<byte[]> readRetainedBytes(final HttpSession session) throws IOException {
        final PublishCarRetainedInsurance meta = getOrNull(session);
        if (meta == null) {
            return Optional.empty();
        }
        final Path file = stashFile(session.getId());
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        return Optional.of(Files.readAllBytes(file));
    }

    /**
     * If the request brings a non-empty insurance file, persists it and replaces any previously stashed
     * file. If the request brings no file, keeps the previously stashed one untouched.
     */
    public void syncFromForm(final PublishCarForm form, final HttpSession session) throws IOException {
        final MultipartFile incoming = form != null ? form.getInsuranceFile() : null;
        if (incoming == null || incoming.isEmpty()) {
            return;
        }
        writeStashFile(session.getId(), incoming.getBytes());
        session.setAttribute(SESSION_ATTRIBUTE, new PublishCarRetainedInsurance(
                incoming.getOriginalFilename(),
                incoming.getContentType(),
                incoming.getSize()));
    }

    /**
     * Best-effort variant of {@link #syncFromForm(PublishCarForm, HttpSession)} that swallows IO errors:
     * a transient stash failure should not mask the original validation error path.
     */
    public void trySyncFromForm(final PublishCarForm form, final HttpSession session) {
        try {
            syncFromForm(form, session);
        } catch (final IOException e) {
            LOG.atWarn()
                    .setMessage("Could not stash insurance file for session={}: {}")
                    .addArgument(session.getId())
                    .addArgument(e.toString())
                    .setCause(e)
                    .log();
        }
    }

    private static Path sessionStashDir(final String sessionId) {
        return STASH_ROOT.resolve(sessionDirSegment(sessionId));
    }

    private static Path stashFile(final String sessionId) {
        return sessionStashDir(sessionId).resolve(STASH_FILENAME);
    }

    private static void writeStashFile(final String sessionId, final byte[] data) throws IOException {
        final Path dir = sessionStashDir(sessionId);
        Files.createDirectories(dir);
        final Path target = stashFile(sessionId);
        Files.write(target, data != null ? data : new byte[0],
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static String sessionDirSegment(final String sessionId) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            final byte[] digest = md.digest(sessionId.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
