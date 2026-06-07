package ar.edu.itba.paw.webapp.support.facade;

import java.io.IOException;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import ar.edu.itba.paw.exception.RydenException;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.domain.user.UserDocumentType;
import ar.edu.itba.paw.services.user.UserService;
import ar.edu.itba.paw.webapp.util.LocaleMessages;

/**
 * Centralises the single/paired upload of {@link UserDocumentType#LICENSE} and
 * {@link UserDocumentType#IDENTITY} files. Before this facade, the same multipart-to-bytes
 * conversion plus {@link UserService#uploadValidatedProfileDocument(long, UserDocumentType, String, String, byte[])}
 * call sequence was duplicated in five places: {@code ProfileController.uploadProfileDocument},
 * {@code ProfileController.uploadProfileDocuments} (×2, license + identity),
 * {@code ReservationFormController.bookingDocuments} (×2, license + identity), and
 * {@code PublishCarFormController.quickIdentity}.
 *
 * <p>The facade owns:
 * <ul>
 *   <li>Skipping empty/missing {@link MultipartFile} parts.</li>
 *   <li>Optional idempotency (don't re-upload if the user already has the doc) for AJAX modals.</li>
 *   <li>Reading the multipart payload and forwarding it to the user service.</li>
 *   <li>Mapping {@link RydenException} / {@link IOException} onto a typed {@link UploadOutcome}
 *       so callers stay declarative.</li>
 * </ul>
 *
 * Callers are still responsible for deciding the HTTP response shape (flash attributes, status
 * codes, response headers) based on the returned outcome.
 */
@Component
public final class UserBookingDocumentsFacade {

    public enum Status { OK, SKIPPED_EMPTY, SKIPPED_ALREADY_PRESENT, BUSINESS_ERROR, READ_ERROR }

    private final UserService userService;
    private final LocaleMessages localeMessages;

    public UserBookingDocumentsFacade(final UserService userService, final LocaleMessages localeMessages) {
        this.userService = userService;
        this.localeMessages = localeMessages;
    }

    /**
     * Attempts to upload a single license/identity multipart part for the given user.
     *
     * @param skipIfAlreadyPresent if {@code true}, the upload is skipped (treated as success-like)
     *                             when the user already has a stored file of {@code documentType}.
     *                             AJAX modal flows want this; the profile flow does not.
     */
    public UploadOutcome attemptUpload(
            final long userId,
            final UserDocumentType documentType,
            final MultipartFile file,
            final boolean skipIfAlreadyPresent) {
        if (isMissingOrEmpty(file)) {
            return UploadOutcome.skippedEmpty();
        }
        if (skipIfAlreadyPresent && userAlreadyHas(userId, documentType)) {
            return UploadOutcome.skippedAlreadyPresent();
        }
        final byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (final IOException e) {
            return UploadOutcome.readError(e);
        }
        try {
            userService.uploadValidatedProfileDocument(
                    userId, documentType, file.getOriginalFilename(), file.getContentType(), bytes);
            return UploadOutcome.ok();
        } catch (final RydenException e) {
            return UploadOutcome.businessError(localeMessages.msg(e));
        }
    }

    /**
     * Performs the booking-pair upload (license + identity) used by both the profile multi-upload
     * form and the reservation booking modal. After all uploads run, refreshes the user from
     * persistence so callers can render the post-state of {@code Needs-License}/{@code Needs-Identity}
     * indicators without an extra round-trip.
     */
    public BookingPairOutcome attemptBookingPair(
            final long userId,
            final MultipartFile licenseFile,
            final MultipartFile identityFile,
            final boolean skipIfAlreadyPresent) {
        final UploadOutcome licenseOutcome = attemptUpload(userId, UserDocumentType.LICENSE, licenseFile, skipIfAlreadyPresent);
        if (licenseOutcome.isHardError()) {
            return BookingPairOutcome.failure(licenseOutcome, licenseOutcome, UploadOutcome.skippedEmpty(),
                    snapshotStateOrEmpty(userId));
        }
        final UploadOutcome identityOutcome = attemptUpload(userId, UserDocumentType.IDENTITY, identityFile, skipIfAlreadyPresent);
        if (identityOutcome.isHardError()) {
            return BookingPairOutcome.failure(identityOutcome, licenseOutcome, identityOutcome,
                    snapshotStateOrEmpty(userId));
        }
        return BookingPairOutcome.success(licenseOutcome, identityOutcome, snapshotStateOrEmpty(userId));
    }

    private boolean userAlreadyHas(final long userId, final UserDocumentType documentType) {
        return userService.getUserById(userId)
                .map(user -> switch (documentType) {
                    case LICENSE -> user.getLicenseFileId().isPresent();
                    case IDENTITY -> user.getIdentityFileId().isPresent();
                })
                .orElse(false);
    }

    private DocumentsState snapshotStateOrEmpty(final long userId) {
        final Optional<User> userOpt = userService.getUserById(userId);
        if (userOpt.isEmpty()) {
            return new DocumentsState(true, true, false);
        }
        final User user = userOpt.get();
        return new DocumentsState(
                user.getLicenseFileId().isEmpty(),
                user.getIdentityFileId().isEmpty(),
                userService.hasUploadedLicenseAndIdentity(user));
    }

    private static boolean isMissingOrEmpty(final MultipartFile file) {
        return file == null || file.isEmpty();
    }

    /** Per-file upload result. */
    public static final class UploadOutcome {

        private final Status status;
        private final IOException readExceptionOrNull;
        private final String localizedBusinessMessageOrNull;

        private UploadOutcome(
                final Status status,
                final IOException readExceptionOrNull,
                final String localizedBusinessMessageOrNull) {
            this.status = status;
            this.readExceptionOrNull = readExceptionOrNull;
            this.localizedBusinessMessageOrNull = localizedBusinessMessageOrNull;
        }

        public Status getStatus() { return status; }

        public boolean isOk() { return status == Status.OK; }

        public boolean isHardError() {
            return status == Status.BUSINESS_ERROR || status == Status.READ_ERROR;
        }

        public Optional<IOException> getReadException() { return Optional.ofNullable(readExceptionOrNull); }

        public Optional<String> getLocalizedBusinessMessage() {
            return Optional.ofNullable(localizedBusinessMessageOrNull);
        }

        static UploadOutcome ok() { return new UploadOutcome(Status.OK, null, null); }

        static UploadOutcome skippedEmpty() { return new UploadOutcome(Status.SKIPPED_EMPTY, null, null); }

        static UploadOutcome skippedAlreadyPresent() {
            return new UploadOutcome(Status.SKIPPED_ALREADY_PRESENT, null, null);
        }

        static UploadOutcome businessError(final String localizedMessage) {
            return new UploadOutcome(Status.BUSINESS_ERROR, null, localizedMessage);
        }

        static UploadOutcome readError(final IOException ex) {
            return new UploadOutcome(Status.READ_ERROR, ex, null);
        }
    }

    /**
     * Post-upload snapshot used by the booking modal AJAX path so the response can advertise
     * which documents are still missing via {@code X-Ryden-Needs-*} headers.
     */
    public record DocumentsState(boolean needsLicense, boolean needsIdentity, boolean hasBoth) { }

    /** Combined outcome of a license + identity paired upload. */
    public static final class BookingPairOutcome {

        private final UploadOutcome licenseOutcome;
        private final UploadOutcome identityOutcome;
        private final UploadOutcome firstHardErrorOrNull;
        private final DocumentsState stateAfter;

        private BookingPairOutcome(
                final UploadOutcome licenseOutcome,
                final UploadOutcome identityOutcome,
                final UploadOutcome firstHardErrorOrNull,
                final DocumentsState stateAfter) {
            this.licenseOutcome = licenseOutcome;
            this.identityOutcome = identityOutcome;
            this.firstHardErrorOrNull = firstHardErrorOrNull;
            this.stateAfter = stateAfter;
        }

        public Optional<UploadOutcome> getFirstHardError() { return Optional.ofNullable(firstHardErrorOrNull); }

        public DocumentsState getStateAfter() { return stateAfter; }

        /** Both parts were missing/empty: nothing was attempted. */
        public boolean nothingAttempted() {
            return licenseOutcome.getStatus() == Status.SKIPPED_EMPTY
                    && identityOutcome.getStatus() == Status.SKIPPED_EMPTY;
        }

        /** At least one part actually persisted a new file. */
        public boolean uploadedSomething() {
            return licenseOutcome.isOk() || identityOutcome.isOk();
        }

        static BookingPairOutcome success(
                final UploadOutcome licenseOutcome,
                final UploadOutcome identityOutcome,
                final DocumentsState stateAfter) {
            return new BookingPairOutcome(licenseOutcome, identityOutcome, null, stateAfter);
        }

        static BookingPairOutcome failure(
                final UploadOutcome firstHardError,
                final UploadOutcome licenseOutcome,
                final UploadOutcome identityOutcome,
                final DocumentsState stateAfter) {
            return new BookingPairOutcome(licenseOutcome, identityOutcome, firstHardError, stateAfter);
        }
    }
}
