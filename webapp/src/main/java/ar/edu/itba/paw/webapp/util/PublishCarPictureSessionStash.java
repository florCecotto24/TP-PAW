package ar.edu.itba.paw.webapp.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
* Keeps pictures from the publish car form in the HTTP session between POST with errors
* (the browser does not repopulate {@code input type=file}). It is the responsibility of <em>presentation</em>,
 * not the domain: it does not know about listing rules or prices; only bytes + metadata in session.
 */
@Component
public class PublishCarPictureSessionStash {

    /** Same key as historically used by {@code PublishCarFormController} (current sessions). */
    private static final String SESSION_ATTRIBUTE =
            "ar.edu.itba.paw.webapp.controller.PublishCarFormController.RETAINED_PICTURES";

    private final LocaleMessages localeMessages;

    @Autowired
    public PublishCarPictureSessionStash(final LocaleMessages localeMessages) {
        this.localeMessages = localeMessages;
    }

    public void clear(final HttpSession session) {
        session.removeAttribute(SESSION_ATTRIBUTE);
    }

    public int stashSize(final HttpSession session) {
        return readStash(session).size();
    }

    /**
     * @return image or {@code null} if the index does not exist
     */
    public PublishCarRetainedImage getOrNull(final HttpSession session, final int index) {
        final List<PublishCarRetainedImage> list = readStash(session);
        if (index < 0 || index >= list.size()) {
            return null;
        }
        return list.get(index);
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
     * If the request brings files, replaces the stash; if not, keeps the previous stash.
     */
    public void syncFromForm(final PublishCarForm form, final HttpSession session) throws IOException {
        final List<ImageUpload> fromForm = toImageUploads(form.getPictures());
        if (fromForm.isEmpty()) {
            return;
        }
        final List<PublishCarRetainedImage> retained = new ArrayList<>(fromForm.size());
        for (final ImageUpload u : fromForm) {
            retained.add(new PublishCarRetainedImage(u.getFilename(), u.getContentType(), u.getData()));
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
            out.add(new ImageUpload(r.filename(), r.contentType(), r.data()));
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
}
