package ar.edu.itba.paw.services.user;


import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.user.InvalidProfileDocumentException;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.models.domain.file.Image;
import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.domain.user.UserDocumentType;
import ar.edu.itba.paw.models.dto.file.BinaryContent;
import ar.edu.itba.paw.policy.ProfileDocumentUploadPolicy;

import ar.edu.itba.paw.services.file.ImageService;
import ar.edu.itba.paw.services.file.StoredFileService;
/**
 * Profile-picture + KYC document writes/reads. Split out of {@link UserServiceImpl}.
 *
 * <p>All mutations run in their own transaction so that they can be invoked either through the
 * back-compat {@link UserService} façade or directly via this contract.</p>
 */
@Service
public class UserProfileMediaServiceImpl implements UserProfileMediaService {

    private final UserService userService;
    private final ImageService imageService;
    private final StoredFileService storedFileService;
    private final ProfileDocumentUploadPolicy profileDocumentUploadPolicy;

    /**
     * {@code @Lazy} on {@link UserService} breaks the bidirectional cycle with {@link UserServiceImpl}
     * (which already injects this media façade for {@code updateProfilePicture} / {@code uploadValidatedProfileDocument}).
     * Architectural rule: this service no longer touches {@code UserDao}; user-row FK mutations are funneled
     * through {@code UserService}'s profile-media setters.
     */
    @Autowired
    public UserProfileMediaServiceImpl(
            @Lazy final UserService userService,
            final ImageService imageService,
            final StoredFileService storedFileService,
            final ProfileDocumentUploadPolicy profileDocumentUploadPolicy) {
        this.userService = userService;
        this.imageService = imageService;
        this.storedFileService = storedFileService;
        this.profileDocumentUploadPolicy = profileDocumentUploadPolicy;
    }

    @Override
    @Transactional
    public void updateProfilePicture(
            final long userId,
            final String originalFilename,
            final String contentType,
            final byte[] data) {
        final User user = userService.getUserById(userId)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        final String safeName = originalFilename != null && !originalFilename.isBlank() ? originalFilename : "profile";
        final String safeType = contentType != null && !contentType.isBlank() ? contentType : "application/octet-stream";
        final Image created = imageService.createImage(safeName, safeType, data);
        final Long previousId = user.getProfilePictureId().orElse(null);
        userService.updateProfilePictureFk(userId, created.getId());
        if (previousId != null && !previousId.equals(created.getId())) {
            imageService.deleteImage(previousId);
        }
    }

    @Override
    @Transactional
    public void clearProfilePicture(final long userId) {
        final User user = userService.getUserById(userId)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        final Long previousId = user.getProfilePictureId().orElse(null);
        userService.updateProfilePictureFk(userId, null);
        if (previousId != null) {
            imageService.deleteImage(previousId);
        }
    }

    @Override
    @Transactional
    public void uploadValidatedProfileDocument(
            final long userId,
            final UserDocumentType documentType,
            final String originalFilename,
            final String contentType,
            final byte[] data) {
        final User user = userService.getUserById(userId)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        if (documentType == null
                || originalFilename == null || originalFilename.isBlank()
                || !StoredFile.isAllowedPaymentReceiptContentType(contentType)) {
            throw new InvalidProfileDocumentException(MessageKeys.USER_PROFILE_DOCUMENT_INVALID);
        }
        final int length = data == null ? 0 : data.length;
        if (length <= 0) {
            throw new InvalidProfileDocumentException(MessageKeys.USER_PROFILE_DOCUMENT_INVALID);
        }
        if (length > profileDocumentUploadPolicy.getMaxBytes()) {
            throw new InvalidProfileDocumentException(
                    MessageKeys.USER_PROFILE_DOCUMENT_TOO_LARGE,
                    profileDocumentUploadPolicy.getMaxMegabytesRoundedUp());
        }
        if (profileDocumentSlotOccupied(user, documentType)) {
            throw new InvalidProfileDocumentException(MessageKeys.USER_PROFILE_DOCUMENT_ALREADY_UPLOADED);
        }
        final StoredFile stored = storedFileService.create(userId, originalFilename, contentType, data);
        switch (documentType) {
            case LICENSE:
                userService.updateLicenseDocumentFk(userId, stored.getId(), true);
                return;
            case IDENTITY:
                userService.updateIdentityDocumentFk(userId, stored.getId(), true);
                return;
            default:
                throw new InvalidProfileDocumentException(MessageKeys.USER_PROFILE_DOCUMENT_INVALID);
        }
    }

    @Override
    @Transactional
    public void clearProfileDocument(final long userId, final UserDocumentType documentType) {
        userService.getUserById(userId)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        if (documentType == null) {
            throw new InvalidProfileDocumentException(MessageKeys.USER_PROFILE_DOCUMENT_INVALID);
        }
        switch (documentType) {
            case LICENSE:
                userService.clearLicenseDocumentFk(userId);
                return;
            case IDENTITY:
                userService.clearIdentityDocumentFk(userId);
                return;
            default:
                throw new InvalidProfileDocumentException(MessageKeys.USER_PROFILE_DOCUMENT_INVALID);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredFile> findProfileDocument(final long userId, final UserDocumentType documentType) {
        if (documentType == null) {
            return Optional.empty();
        }
        return userService.getUserById(userId)
                .flatMap(u -> switch (documentType) {
                    case LICENSE -> u.getLicenseFileId();
                    case IDENTITY -> u.getIdentityFileId();
                })
                .flatMap(storedFileService::findById);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BinaryContent> findProfileDocumentContent(
            final long userId, final UserDocumentType documentType) {
        return findProfileDocument(userId, documentType)
                .map(sf -> new BinaryContent(sf.getData(), sf.getContentType(), sf.getFileName()));
    }

    private static boolean profileDocumentSlotOccupied(final User user, final UserDocumentType documentType) {
        return switch (documentType) {
            case LICENSE -> user.getLicenseFileId().isPresent();
            case IDENTITY -> user.getIdentityFileId().isPresent();
        };
    }
}
