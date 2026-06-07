package ar.edu.itba.paw.webapp.form.user;

import org.springframework.web.multipart.MultipartFile;

import ar.edu.itba.paw.webapp.validation.constraint.file.NotEmptyFile;

/**
 * Spring MVC binding for {@code POST /profile/picture}: a single non-empty image file. Wrapping the
 * upload in a form bean (instead of a bare {@code @RequestParam MultipartFile}) lets Bean Validation
 * own the "file is required" check via {@link NotEmptyFile} so the controller no longer needs to
 * test {@code if (file == null || file.isEmpty())} by hand.
 */
public final class ProfilePictureUploadForm {

    @NotEmptyFile(message = "{profile.picture.required}")
    private MultipartFile profilePicture;

    public MultipartFile getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(final MultipartFile profilePicture) {
        this.profilePicture = profilePicture;
    }
}
