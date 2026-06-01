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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import ar.edu.itba.paw.dto.GalleryMediaUpload;
import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.services.policy.CarGalleryUploadPolicy;
import ar.edu.itba.paw.models.util.media.CarGalleryMediaContentTypes;
import ar.edu.itba.paw.webapp.dto.PublishCarRetainedImage;
import ar.edu.itba.paw.webapp.form.PublishCarForm;

/**
 * Preserves pictures from the publish form between POST with errors (the browser does not repopulate {@code type=file}).
 * The session only saves references to temporary files on disk; the image bytes are not serialized in the session.
 */
@Component
public final class PublishCarPictureSessionStash {

    private static final Logger LOG = LoggerFactory.getLogger(PublishCarPictureSessionStash.class);

    /** Same key as historically used by {@code PublishCarFormController} (current sessions). */
    private static final String SESSION_ATTRIBUTE =
            "ar.edu.itba.paw.webapp.controller.PublishCarFormController.RETAINED_PICTURES";

    private static final String SESSION_COVER_INDEX_ATTRIBUTE =
            "ar.edu.itba.paw.webapp.controller.PublishCarFormController.COVER_PICTURE_INDEX";

    private static final Path STASH_ROOT =
            Path.of(System.getProperty("java.io.tmpdir"), "ryden-publish-stash");

    private final LocaleMessages localeMessages;

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
                        } catch (final IOException e) {
                            LOG.atDebug()
                                    .setMessage("Could not delete publish stash path {}: {}")
                                    .addArgument(p)
                                    .addArgument(e.toString())
                                    .setCause(e)
                                    .log();
                        }
                    });
                }
            }
        } catch (final IOException e) {
            LOG.warn("Could not walk or delete publish stash directory for session {}: {}", sessionId, e.toString(), e);
        }
    }

    public void clear(final HttpSession session) {
        deleteStashFilesForSessionId(session.getId());
        session.removeAttribute(SESSION_ATTRIBUTE);
        session.removeAttribute(SESSION_COVER_INDEX_ATTRIBUTE);
    }

    public int getCoverPictureIndex(final HttpSession session) {
        final Object raw = session.getAttribute(SESSION_COVER_INDEX_ATTRIBUTE);
        if (raw instanceof Integer idx && idx >= 0) {
            return idx;
        }
        return 0;
    }

    public void setCoverPictureIndex(final HttpSession session, final int coverIndex) {
        session.setAttribute(SESSION_COVER_INDEX_ATTRIBUTE, Math.max(0, coverIndex));
    }

    /**
     * Reorders the session stash so the cover image is first (retained-only retry path).
     */
    public void applyCoverFromForm(final PublishCarForm form, final HttpSession session) {
        final List<PublishCarRetainedImage> list = new ArrayList<>(readStash(session));
        if (list.isEmpty()) {
            return;
        }
        final int cover = resolveCoverIndexForRetained(list, form.getCoverPictureIndex());
        if (cover != 0) {
            session.setAttribute(SESSION_ATTRIBUTE, reorderToCover(list, cover));
        }
        setCoverPictureIndex(session, 0);
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

    public PublishCarRetainedImage getOrNull(final HttpSession session, final String token) {
        if (token == null || token.isBlank()) return null;
        return readStash(session).stream()
                .filter(r -> token.equals(r.stashToken()))
                .findFirst().orElse(null);
    }

    public List<String> getStashedTokens(final HttpSession session) {
        return readStash(session).stream()
                .map(PublishCarRetainedImage::stashToken)
                .toList();
    }

    public boolean removeByToken(final HttpSession session, final String token) {
        if (token == null || token.isBlank()) return false;
        final List<PublishCarRetainedImage> list = new ArrayList<>(readStash(session));
        final boolean removed = list.removeIf(r -> {
            if (token.equals(r.stashToken())) {
                try {
                    Files.deleteIfExists(stashFile(session.getId(), token));
                } catch (final IOException e) {
                    LOG.atDebug()
                            .setMessage("Could not delete stashed publish image file for token {}: {}")
                            .addArgument(token)
                            .addArgument(e.toString())
                            .setCause(e)
                            .log();
                }
                return true;
            }
            return false;
        });
        if (removed) {
            session.setAttribute(SESSION_ATTRIBUTE, list);
            if (list.isEmpty()) {
                session.removeAttribute(SESSION_COVER_INDEX_ATTRIBUTE);
            } else {
                final int cover = getCoverPictureIndex(session);
                if (cover >= list.size()) {
                    setCoverPictureIndex(session, defaultCoverIndexForRetained(list));
                }
            }
        }
        return removed;
    }

    public Optional<byte[]> readRetainedBytes(final HttpSession session, final String token) throws IOException {
        final PublishCarRetainedImage meta = getOrNull(session, token);
        if (meta == null || meta.stashToken().isBlank()) {
            return Optional.empty();
        }
        final Path file = stashFile(session.getId(), meta.stashToken());
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        return Optional.of(Files.readAllBytes(file));
    }

    public List<PublishCarRetainedImage> getStashedRetainedImages(final HttpSession session) {
        return readStash(session);
    }

    public static MediaType safeGalleryMediaType(final String contentType) {
        if (CarGalleryMediaContentTypes.isImageContentType(contentType)
                || CarGalleryMediaContentTypes.isVideoContentType(contentType, null)) {
            try {
                return MediaType.parseMediaType(contentType);
            } catch (final IllegalArgumentException e) {
                LOG.atDebug()
                        .setMessage("Unparseable gallery Content-Type '{}', using application/octet-stream: {}")
                        .addArgument(contentType)
                        .addArgument(e.toString())
                        .setCause(e)
                        .log();
            }
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    /** @deprecated use {@link #safeGalleryMediaType(String)} */
    public static MediaType safeImageMediaType(final String contentType) {
        return safeGalleryMediaType(contentType);
    }

    /**
     * If the request brings files, replaces the stash on disk; otherwise, keeps the previous stash.
     */
    public void syncFromForm(final PublishCarForm form, final HttpSession session) throws IOException {
        final List<GalleryMediaUpload> fromForm = toGalleryMediaUploads(form.getPictures());
        if (fromForm.isEmpty()) {
            return;
        }
        deleteStashedFilesOnly(session);
        final List<PublishCarRetainedImage> retained = new ArrayList<>(fromForm.size());
        for (final GalleryMediaUpload u : fromForm) {
            final String token = UUID.randomUUID().toString();
            writeStashFile(session.getId(), token, u.getData());
            retained.add(new PublishCarRetainedImage(token, u.getFilename(), u.getContentType()));
        }
        final int cover = resolveCoverIndexForUploads(fromForm, form.getCoverPictureIndex());
        session.setAttribute(SESSION_ATTRIBUTE, reorderToCover(retained, cover));
        setCoverPictureIndex(session, 0);
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

    public List<GalleryMediaUpload> resolveUploads(final PublishCarForm form, final HttpSession session)
            throws IOException {
        final List<GalleryMediaUpload> fromForm = toGalleryMediaUploads(form.getPictures());
        final List<GalleryMediaUpload> uploads;
        if (!fromForm.isEmpty()) {
            uploads = fromForm;
        } else {
            final List<PublishCarRetainedImage> stashed = readStash(session);
            if (stashed.isEmpty()) {
                return Collections.emptyList();
            }
            uploads = new ArrayList<>(stashed.size());
            for (final PublishCarRetainedImage r : stashed) {
                final Path file = stashFile(session.getId(), r.stashToken());
                if (!Files.isRegularFile(file)) {
                    throw new IOException("Missing stashed publish-car gallery media file");
                }
                uploads.add(new GalleryMediaUpload(r.filename(), r.contentType(), Files.readAllBytes(file)));
            }
        }
        final int cover = resolveCoverIndexForUploads(uploads, form.getCoverPictureIndex());
        setCoverPictureIndex(session, 0);
        return reorderToCover(uploads, cover);
    }

    public void validatePicturePresence(final PublishCarForm form, final HttpSession session, final BindingResult errors) {
        final int fromForm = countNonEmptyMultipart(form.getPictures());
        final int stashSize = readStash(session).size();
        if (fromForm > CarGalleryUploadPolicy.MAX_ITEMS) {
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
        if (stashSize > CarGalleryUploadPolicy.MAX_ITEMS) {
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
                } catch (final IOException e) {
                    LOG.atDebug()
                            .setMessage("Could not delete previous stashed publish image {}: {}")
                            .addArgument(r.stashToken())
                            .addArgument(e.toString())
                            .setCause(e)
                            .log();
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

    private static List<GalleryMediaUpload> toGalleryMediaUploads(final MultipartFile[] pictures) throws IOException {
        if (pictures == null) {
            return Collections.emptyList();
        }
        final List<GalleryMediaUpload> out = new ArrayList<>();
        for (final MultipartFile picture : pictures) {
            if (picture == null || picture.isEmpty()) {
                continue;
            }
            out.add(new GalleryMediaUpload(
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

    private static boolean isCoverEligible(final GalleryMediaUpload upload) {
        return upload != null && CarGalleryMediaContentTypes.isImageContentType(upload.getContentType());
    }

    private static boolean isCoverEligible(final PublishCarRetainedImage retained) {
        return retained != null && CarGalleryMediaContentTypes.isImageContentType(retained.contentType());
    }

    private static int defaultCoverIndexForUploads(final List<GalleryMediaUpload> items) {
        for (int i = 0; i < items.size(); i++) {
            if (isCoverEligible(items.get(i))) {
                return i;
            }
        }
        return 0;
    }

    private static int defaultCoverIndexForRetained(final List<PublishCarRetainedImage> items) {
        for (int i = 0; i < items.size(); i++) {
            if (isCoverEligible(items.get(i))) {
                return i;
            }
        }
        return 0;
    }

    private static int resolveCoverIndexForUploads(
            final List<GalleryMediaUpload> items,
            final Integer requestedIndex) {
        if (items.isEmpty()) {
            return 0;
        }
        final int defaultIdx = defaultCoverIndexForUploads(items);
        if (requestedIndex == null || requestedIndex < 0 || requestedIndex >= items.size()) {
            return defaultIdx;
        }
        return isCoverEligible(items.get(requestedIndex)) ? requestedIndex : defaultIdx;
    }

    private static int resolveCoverIndexForRetained(
            final List<PublishCarRetainedImage> items,
            final Integer requestedIndex) {
        if (items.isEmpty()) {
            return 0;
        }
        final int defaultIdx = defaultCoverIndexForRetained(items);
        if (requestedIndex == null || requestedIndex < 0 || requestedIndex >= items.size()) {
            return defaultIdx;
        }
        return isCoverEligible(items.get(requestedIndex)) ? requestedIndex : defaultIdx;
    }

    private static <T> List<T> reorderToCover(final List<T> items, final int coverIndex) {
        if (items.isEmpty() || coverIndex <= 0 || coverIndex >= items.size()) {
            return new ArrayList<>(items);
        }
        final List<T> out = new ArrayList<>(items.size());
        out.add(items.get(coverIndex));
        for (int i = 0; i < items.size(); i++) {
            if (i != coverIndex) {
                out.add(items.get(i));
            }
        }
        return out;
    }
}
