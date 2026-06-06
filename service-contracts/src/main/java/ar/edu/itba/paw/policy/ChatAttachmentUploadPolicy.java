package ar.edu.itba.paw.policy;

/** Max byte size for reservation chat attachment uploads. */
public interface ChatAttachmentUploadPolicy {

    int getMaxBytes();

    int getMaxMegabytesRoundedUp();
}
