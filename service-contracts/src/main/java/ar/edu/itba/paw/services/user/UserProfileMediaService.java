package ar.edu.itba.paw.services.user;


import java.util.Optional;

import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.models.domain.user.UserDocumentType;
import ar.edu.itba.paw.models.dto.file.BinaryContent;

/**
 * Profile-picture and KYC-document writes/reads, extracted from {@link UserService} so the
 * latter stays focused on identity / auth / preference data.
 *
 * <p>The corresponding methods are kept on {@link UserService} (back-compat) and delegate
 * here. New callers SHOULD depend on this contract directly.</p>
 */
public interface UserProfileMediaService {

    /** See {@link UserService#updateProfilePicture(long, String, String, byte[])}. */
    void updateProfilePicture(long userId, String originalFilename, String contentType, byte[] data);

    /** See {@link UserService#clearProfilePicture(long)}. */
    void clearProfilePicture(long userId);

    /** See {@link UserService#uploadValidatedProfileDocument(long, UserDocumentType, String, String, byte[])}. */
    void uploadValidatedProfileDocument(
            long userId, UserDocumentType documentType, String originalFilename, String contentType, byte[] data);

    /** See {@link UserService#clearProfileDocument(long, UserDocumentType)}. */
    void clearProfileDocument(long userId, UserDocumentType documentType);

    /** See {@link UserService#findProfileDocument(long, UserDocumentType)}. */
    Optional<StoredFile> findProfileDocument(long userId, UserDocumentType documentType);

    /**
     * Same scoping as {@link #findProfileDocument} but returns a detached
     * {@link BinaryContent} value object so download endpoints don't leak the JPA entity.
     */
    Optional<BinaryContent> findProfileDocumentContent(long userId, UserDocumentType documentType);
}
