package ar.edu.itba.paw.policy;

/** Max byte size for profile verification document uploads. */
public interface ProfileDocumentUploadPolicy {

    int getMaxBytes();

    int getMaxMegabytesRoundedUp();
}
