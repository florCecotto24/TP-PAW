package ar.edu.itba.paw.webapp.validation.support;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import ar.edu.itba.paw.dto.ImageUpload;
import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.models.domain.Image;
import ar.edu.itba.paw.services.file.ImageService;
import ar.edu.itba.paw.webapp.util.LocaleMessages;

/**
 * Controller helper: rejects non-image multipart parts and enforces count/size/type using {@link ImageService}.
 */
@Component
public final class MultipartImageValidation {

    private final ImageService imageService;
    private final LocaleMessages localeMessages;

    public MultipartImageValidation(final ImageService imageService, final LocaleMessages localeMessages) {
        this.imageService = imageService;
        this.localeMessages = localeMessages;
    }


    public boolean validateFilesAreImages(
            final MultipartFile[] files,
            final BindingResult errors,
            final String fieldName) {
        if (files == null) {
            return true;
        }
        for (final MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            if (!Image.isImageContentType(file.getContentType())) {
                errors.rejectValue(
                        fieldName,
                        "validation.pictures.mustBeImage",
                        localeMessages.msg("validation.pictures.mustBeImage"));
                return false;
            }
        }
        return true;
    }

    public boolean validateFilesWithinMaxSize(final MultipartFile[] files, final BindingResult errors) {
        if (files == null) {
            return true;
        }
        final long max = imageService.getMaxImageBytes();
        final long maxMb = imageService.getMaxImageMegabytesRoundedUp();
        for (final MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            final long size = file.getSize();
            if (size >= 0 && size > max) {
                errors.reject(
                        MessageKeys.IMAGE_FILE_TOO_LARGE,
                        new Object[] { maxMb },
                        localeMessages.msg(MessageKeys.IMAGE_FILE_TOO_LARGE, maxMb));
                return false;
            }
        }
        return true;
    }

    public boolean validateImageUploadsAreImages(
            final List<ImageUpload> uploads,
            final BindingResult errors,
            final String fieldName) {
        if (uploads == null || uploads.isEmpty()) {
            return true;
        }
        for (final ImageUpload upload : uploads) {
            if (!Image.isImageContentType(upload.getContentType())) {
                errors.rejectValue(
                        fieldName,
                        "validation.pictures.mustBeImage",
                        localeMessages.msg("validation.pictures.mustBeImage"));
                return false;
            }
        }
        return true;
    }

    public boolean validateImageUploadsWithinMaxSize(final List<ImageUpload> uploads, final BindingResult errors) {
        if (uploads == null || uploads.isEmpty()) {
            return true;
        }
        final long max = imageService.getMaxImageBytes();
        final long maxMb = imageService.getMaxImageMegabytesRoundedUp();
        for (final ImageUpload upload : uploads) {
            final long size = upload.getData().length;
            if (size > max) {
                errors.reject(
                        MessageKeys.IMAGE_FILE_TOO_LARGE,
                        new Object[] { maxMb },
                        localeMessages.msg(MessageKeys.IMAGE_FILE_TOO_LARGE, maxMb));
                return false;
            }
        }
        return true;
    }


    public Optional<SingleMultipartImageIssue> validateNonEmptyFile(final MultipartFile file) {
        final long max = imageService.getMaxImageBytes();
        final long size = file.getSize();
        if (size >= 0 && size > max) {
            final long maxMb = imageService.getMaxImageMegabytesRoundedUp();
            return Optional.of(SingleMultipartImageIssue.tooLarge(
                    localeMessages.msg(MessageKeys.IMAGE_FILE_TOO_LARGE, maxMb)));
        }
        if (!Image.isImageContentType(file.getContentType())) {
            return Optional.of(SingleMultipartImageIssue.notImage("profile.picture.notImage"));
        }
        return Optional.empty();
    }


    public static final class SingleMultipartImageIssue {

        private final String resolvedMessage;
        private final String messageCode;

        private SingleMultipartImageIssue(final String resolvedMessage, final String messageCode) {
            this.resolvedMessage = resolvedMessage;
            this.messageCode = messageCode;
        }

        public static SingleMultipartImageIssue tooLarge(final String resolvedMessage) {
            return new SingleMultipartImageIssue(resolvedMessage, null);
        }

        public static SingleMultipartImageIssue notImage(final String messageCode) {
            return new SingleMultipartImageIssue(null, messageCode);
        }

        public String getResolvedMessage() {
            return resolvedMessage;
        }

        public String getMessageCode() {
            return messageCode;
        }
    }
}
