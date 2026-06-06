package ar.edu.itba.paw.webapp.validation.support;

import org.springframework.stereotype.Component;

import ar.edu.itba.paw.policy.ProfileDocumentUploadPolicy;

/**
 * Adapts {@link ProfileDocumentUploadPolicy} (which reads
 * {@code app.upload.max-profile-document-megabytes} from {@code application.properties}) to the
 * {@link FileSizeLimitProvider} contract so {@link ar.edu.itba.paw.webapp.validation.constraint.MaxFileSize}
 * can enforce the profile-document size cap declaratively on any {@code MultipartFile} field.
 */
@Component
public final class ProfileDocumentFileSizeLimitProvider implements FileSizeLimitProvider {

    private final ProfileDocumentUploadPolicy policy;

    public ProfileDocumentFileSizeLimitProvider(final ProfileDocumentUploadPolicy policy) {
        this.policy = policy;
    }

    @Override
    public long getMaxBytes() {
        return policy.getMaxBytes();
    }

    @Override
    public int getMaxMegabytesRoundedUp() {
        return policy.getMaxMegabytesRoundedUp();
    }
}
