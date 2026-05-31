package ar.edu.itba.paw.webapp.validation.support;

/**
 * Strategy for resolving a maximum allowed byte count at runtime. Used by
 * {@link ar.edu.itba.paw.webapp.validation.constraint.MaxFileSize}: when the annotation references a
 * subtype of this interface, the {@code @Component} implementation is looked up from the Spring
 * context so policies that read from {@code application.properties} (e.g. profile-document upload
 * limit) stay the single source of truth instead of duplicating the byte count on the annotation.
 */
public interface FileSizeLimitProvider {

    /** @return the upper limit (in bytes) that a candidate {@code MultipartFile} may have, inclusive. */
    long getMaxBytes();

    /** @return the same limit rounded up to megabytes, used purely to render user-facing error messages. */
    int getMaxMegabytesRoundedUp();

    /**
     * Sentinel implementation used as the annotation default to mean "no provider configured".
     * Never instantiated; the validator detects this exact class and falls back to the static
     * {@code maxBytes()} attribute on the annotation.
     */
    final class None implements FileSizeLimitProvider {

        private None() {
        }

        @Override
        public long getMaxBytes() {
            throw new UnsupportedOperationException("sentinel");
        }

        @Override
        public int getMaxMegabytesRoundedUp() {
            throw new UnsupportedOperationException("sentinel");
        }
    }
}
