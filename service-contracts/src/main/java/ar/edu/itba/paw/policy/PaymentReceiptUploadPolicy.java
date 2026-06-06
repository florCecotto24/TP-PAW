package ar.edu.itba.paw.policy;

/** Max byte size for reservation payment receipt uploads. */
public interface PaymentReceiptUploadPolicy {

    int getMaxBytes();

    int getMaxMegabytesRoundedUp();
}
