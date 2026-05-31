package ar.edu.itba.paw.services.policy;

/** Max byte size for reservation payment receipt uploads. */
public interface PaymentReceiptUploadPolicy {

    int getMaxBytes();

    int getMaxMegabytesRoundedUp();
}
