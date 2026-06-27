package ar.edu.itba.paw.webapp.form.user;

import ar.edu.itba.paw.webapp.validation.constraint.user.ValidProfilePicturePayload;

/** REST body for {@code PUT /users/{id}/profile-picture}. */
@ValidProfilePicturePayload
public final class ProfilePictureRestForm {

    private byte[] bytes;
    private String contentType;

    public ProfilePictureRestForm() {
    }

    public ProfilePictureRestForm(final byte[] bytes, final String contentType) {
        this.bytes = bytes;
        this.contentType = contentType;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(final byte[] bytes) {
        this.bytes = bytes;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }
}
