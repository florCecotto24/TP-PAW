package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.NotNull;

import org.springframework.web.multipart.MultipartFile;

import ar.edu.itba.paw.models.domain.UserDocumentType;
import ar.edu.itba.paw.webapp.validation.constraint.NotEmptyFile;

/**
 * Spring MVC binding for {@code POST /profile/document}: which document is being uploaded
 * ({@link UserDocumentType}) plus the non-empty file. Field-level constraints replace the manual
 * {@code if (documentType == null || file == null || file.isEmpty())} branch the controller used
 * to perform after a bespoke {@code parseDocumentType(rawString)} call.
 */
public final class ProfileDocumentUploadForm {

    @NotNull(message = "{validation.documentType.notNull}")
    private UserDocumentType documentType;

    @NotEmptyFile(message = "{validation.file.required}")
    private MultipartFile documentFile;

    public UserDocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(final UserDocumentType documentType) {
        this.documentType = documentType;
    }

    public MultipartFile getDocumentFile() {
        return documentFile;
    }

    public void setDocumentFile(final MultipartFile documentFile) {
        this.documentFile = documentFile;
    }
}
