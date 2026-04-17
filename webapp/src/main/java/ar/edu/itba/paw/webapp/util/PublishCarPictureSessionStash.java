package ar.edu.itba.paw.webapp.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import ar.edu.itba.paw.dto.ImageUpload;
import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.models.Image;
import ar.edu.itba.paw.webapp.dto.PublishCarRetainedImage;
import ar.edu.itba.paw.webapp.form.PublishCarForm;

/**
 * Preserves pictures from the publish form between POST with errors (the browser does not repopulate {@code type=file}).
 * The session only saves references to temporary files on disk; the image bytes are not serialized in the session.
 */
@Component
public class PublishCarPictureSessionStash {

    /** Same key as historically used by {@code PublishCarFormController} (current sessions). */
    private static final String SESSION_ATTRIBUTE =
            "ar.edu.itba.paw.webapp.controller.PublishCarFormController.RETAINED_PICTURES";

    private static final Path STASH_ROOT =
            Path.of(System.getProperty("java.io.tmpdir"), "ryden-publish-stash");

    private final LocaleMessages localeMessages;

    @Autowired
    public PublishCarPictureSessionStash(final LocaleMessages localeMessages) {
        this.localeMessages = localeMessages;
    }

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
                        } catch (final IOException ignored) {
                            // best effort
                        }
                    });
                }
            }
        } catch (final IOException ignored) {
            // best effort
        }
    }

    public void clear(final HttpSession session) {
        deleteStashFilesForSessionId(session.getId());
        session.removeAttribute(SESSION_ATTRIBUTE);
    }

    public int stashSize(final HttpSession session) {
        return readStash(session).size();
    }

    /**
     * @return metadata or {@code null} if the index does not exist
     */
    public PublishCarRetainedImage getOrNull(final HttpSession session, final int index) {
        final List<PublishCarRetainedImage> list = readStash(session);
        if (index < 0 || index >= list.size()) {
            return null;
        }
        return list.get(index);
    }

    public Optional<byte[]> readRetainedBytes(final HttpSession session, final int index) throws IOException {
        final PublishCarRetainedImage meta = getOrNull(session, index);
        if (meta == null || meta.stashToken().isBlank()) {
            return Optional.empty();
        }
        final Path file = stashFile(session.getId(), meta.stashToken());
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        return Optional.of(Files.readAllBytes(file));
    }

    public static MediaType safeImageMediaType(final String contentType) {
        if (Image.isImageContentType(contentType)) {
            try {
                return MediaType.parseMediaType(contentType);
            } catch (final IllegalArgumentException ignored) {
                // fall through
            }
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    /**
     * If the request brings files, replaces the stash on disk; otherwise, keeps the previous stash.
     */
    public void syncFromForm(final PublishCarForm form, final HttpSession session) throws IOException {
        final List<ImageUpload> fromForm = toImageUploads(form.getPictures());
        if (fromForm.isEmpty()) {
            return;
        }
        deleteStashedFilesOnly(session);
        final List<PublishCarRetainedImage> retained = new ArrayList<>(fromForm.size());
        for (final ImageUpload u : fromForm) {
            final String token = UUID.randomUUID().toString();
            writeStashFile(session.getId(), token, u.getData());
            retained.add(new PublishCarRetainedImage(token, u.getFilename(), u.getContentType()));
        }
        session.setAttribute(SESSION_ATTRIBUTE, retained);
    }

    public void trySyncFromForm(final PublishCarForm form, final HttpSession session, final BindingResult errors) {
        try {
            syncFromForm(form, session);
        } catch (final IOException e) {
            errors.reject(
                    MessageKeys.PUBLISH_IMAGES_READ,
                    localeMessages.msg(MessageKeys.PUBLISH_IMAGES_READ));
        }
    }

    public List<ImageUpload> resolveUploads(final PublishCarForm form, final HttpSession session) throws IOException {
        final List<ImageUpload> fromForm = toImageUploads(form.getPictures());
        if (!fromForm.isEmpty()) {
            return fromForm;
        }
        final List<PublishCarRetainedImage> stashed = readStash(session);
        if (stashed.isEmpty()) {
            return Collections.emptyList();
        }
        final List<ImageUpload> out = new ArrayList<>(stashed.size());
        for (final PublishCarRetainedImage r : stashed) {
            final Path file = stashFile(session.getId(), r.stashToken());
            if (!Files.isRegularFile(file)) {
                throw new IOException("Missing stashed publish-car image file");
            }
            out.add(new ImageUpload(r.filename(), r.contentType(), Files.readAllBytes(file)));
        }
        return out;
    }

    public void validatePicturePresence(final PublishCarForm form, final HttpSession session, final BindingResult errors) {
        final int fromForm = countNonEmptyMultipart(form.getPictures());
        final int stashSize = readStash(session).size();
        if (fromForm > 8) {
            errors.rejectValue(
                    "pictures",
                    "validation.pictures.size",
                    localeMessages.msg("validation.pictures.size"));
            return;
        }
        if (fromForm == 0 && stashSize == 0) {
            errors.rejectValue(
                    "pictures",
                    "validation.pictures.notNull",
                    localeMessages.msg("validation.pictures.notNull"));
            return;
        }
        if (stashSize > 8) {
            errors.rejectValue(
                    "pictures",
                    "validation.pictures.size",
                    localeMessages.msg("validation.pictures.size"));
        }
    }

    public static int countNonEmptyMultipart(final MultipartFile[] pictures) {
        if (pictures == null) {
            return 0;
        }
        int n = 0;
        for (final MultipartFile picture : pictures) {
            if (picture != null && !picture.isEmpty()) {
                n++;
            }
        }
        return n;
    }

    private void deleteStashedFilesOnly(final HttpSession session) {
        for (final PublishCarRetainedImage r : readStash(session)) {
            if (!r.stashToken().isBlank()) {
                try {
                    Files.deleteIfExists(stashFile(session.getId(), r.stashToken()));
                } catch (final IOException ignored) {
                    // best effort
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<PublishCarRetainedImage> readStash(final HttpSession session) {
        final Object raw = session.getAttribute(SESSION_ATTRIBUTE);
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            return List.of();
        }
        for (final Object o : list) {
            if (!(o instanceof PublishCarRetainedImage)) {
                return List.of();
            }
        }
        return (List<PublishCarRetainedImage>) raw;
    }

    private static List<ImageUpload> toImageUploads(final MultipartFile[] pictures) throws IOException {
        final List<ImageUpload> out = new ArrayList<>();
        if (pictures == null) {
            return out;
        }
        for (final MultipartFile picture : pictures) {
            if (picture == null || picture.isEmpty()) {
                continue;
            }
            out.add(new ImageUpload(
                    picture.getOriginalFilename(),
                    picture.getContentType(),
                    picture.getBytes()));
        }
        return out;
    }

    private static Path sessionStashDir(final String sessionId) {
        return STASH_ROOT.resolve(sessionDirSegment(sessionId));
    }

    private static Path stashFile(final String sessionId, final String token) {
        return sessionStashDir(sessionId).resolve(token + ".bin");
    }

    private static void writeStashFile(final String sessionId, final String token, final byte[] data) throws IOException {
        final Path dir = sessionStashDir(sessionId);
        Files.createDirectories(dir);
        final Path target = stashFile(sessionId, token);
        Files.write(target, data != null ? data : new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
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
