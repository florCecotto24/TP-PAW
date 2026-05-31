package ar.edu.itba.paw.webapp.validation.support;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import ar.edu.itba.paw.dto.GalleryMediaUpload;
import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.models.util.media.CarGalleryMediaContentTypes;
import ar.edu.itba.paw.services.policy.CarGalleryUploadPolicy;
import ar.edu.itba.paw.webapp.util.LocaleMessages;

/** Validates mixed photo/video uploads for the publish-car gallery. */
@Component
public final class MultipartGalleryMediaValidation {

    private final CarGalleryUploadPolicy carGalleryUploadPolicy;
    private final LocaleMessages localeMessages;

    public MultipartGalleryMediaValidation(
            final CarGalleryUploadPolicy carGalleryUploadPolicy, final LocaleMessages localeMessages) {
        this.carGalleryUploadPolicy = carGalleryUploadPolicy;
        this.localeMessages = localeMessages;
    }

    public boolean validateFilesAreGalleryMedia(
            final MultipartFile[] files, final BindingResult errors, final String fieldName) {
        if (files == null) {
            return true;
        }
        for (final MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            if (!CarGalleryMediaContentTypes.isAllowed(file.getContentType(), file.getOriginalFilename())) {
                errors.rejectValue(
                        fieldName,
                        MessageKeys.CAR_GALLERY_MEDIA_INVALID_TYPE,
                        localeMessages.msg(MessageKeys.CAR_GALLERY_MEDIA_INVALID_TYPE));
                return false;
            }
        }
        return true;
    }

    public boolean validateFilesWithinMaxSize(final MultipartFile[] files, final BindingResult errors) {
        if (files == null) {
            return true;
        }
        for (final MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            if (!validateSingleFileSize(file, errors)) {
                return false;
            }
        }
        return true;
    }

    public boolean validateGalleryMediaUploadsAreAllowed(
            final List<GalleryMediaUpload> uploads, final BindingResult errors, final String fieldName) {
        if (uploads == null || uploads.isEmpty()) {
            return true;
        }
        for (final GalleryMediaUpload upload : uploads) {
            if (!CarGalleryMediaContentTypes.isAllowed(upload.getContentType(), upload.getFilename())) {
                errors.rejectValue(
                        fieldName,
                        MessageKeys.CAR_GALLERY_MEDIA_INVALID_TYPE,
                        localeMessages.msg(MessageKeys.CAR_GALLERY_MEDIA_INVALID_TYPE));
                return false;
            }
        }
        return true;
    }

    public boolean validateGalleryMediaUploadsWithinMaxSize(
            final List<GalleryMediaUpload> uploads, final BindingResult errors) {
        if (uploads == null || uploads.isEmpty()) {
            return true;
        }
        for (final GalleryMediaUpload upload : uploads) {
            final long size = upload.getData().length;
            if (CarGalleryMediaContentTypes.isImageContentType(upload.getContentType())) {
                if (size > carGalleryUploadPolicy.getMaxImageBytes()) {
                    rejectImageTooLarge(errors);
                    return false;
                }
            } else if (size > carGalleryUploadPolicy.getMaxVideoBytes()) {
                rejectVideoTooLarge(errors);
                return false;
            }
        }
        return true;
    }

    private boolean validateSingleFileSize(final MultipartFile file, final BindingResult errors) {
        final long size = file.getSize();
        if (size < 0) {
            return true;
        }
        if (CarGalleryMediaContentTypes.isImageContentType(file.getContentType())) {
            if (size > carGalleryUploadPolicy.getMaxImageBytes()) {
                rejectImageTooLarge(errors);
                return false;
            }
        } else if (size > carGalleryUploadPolicy.getMaxVideoBytes()) {
            rejectVideoTooLarge(errors);
            return false;
        }
        return true;
    }

    private void rejectImageTooLarge(final BindingResult errors) {
        final int maxMb = carGalleryUploadPolicy.getMaxImageMegabytesRoundedUp();
        errors.reject(
                MessageKeys.IMAGE_FILE_TOO_LARGE,
                new Object[] { maxMb },
                localeMessages.msg(MessageKeys.IMAGE_FILE_TOO_LARGE, maxMb));
    }

    private void rejectVideoTooLarge(final BindingResult errors) {
        final int maxMb = carGalleryUploadPolicy.getMaxVideoMegabytesRoundedUp();
        errors.reject(
                MessageKeys.CAR_GALLERY_VIDEO_TOO_LARGE,
                new Object[] { maxMb },
                localeMessages.msg(MessageKeys.CAR_GALLERY_VIDEO_TOO_LARGE, maxMb));
    }
}
