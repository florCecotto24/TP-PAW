package ar.edu.itba.paw.webapp.support.facade;

import java.io.IOException;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.RydenException;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.policy.ProfileDocumentUploadPolicy;
import ar.edu.itba.paw.webapp.util.LocaleMessages;

/**
 * Centralises the size / read / business-error handling for car-insurance multipart uploads.
 *
 * Before this facade {@code MyCarsController.uploadInsurance} (full page reload variant) and
 * {@code MyCarsController.quickInsurance} (AJAX variant) duplicated three identical steps:
 * empty / max-size guard against {@link ProfileDocumentUploadPolicy};
 * {@code CarService.uploadValidatedCarInsuranceDocument} with {@code getBytes()} extraction;
 * fan-out into typed flash / header messages for {@link RydenException} and {@link IOException}.
 * Callers are still responsible for the ownership check and for translating the outcome into the
 * exact HTTP shape they want (redirect + flash, or {@code 204 / 400 + X-Ryden-Error} header).
 */
@Component
public final class CarInsuranceUploadFacade {

    public enum Status { OK, MISSING_FILE, TOO_LARGE, BUSINESS_ERROR, READ_ERROR }

    private final CarService carService;
    private final ProfileDocumentUploadPolicy profileDocumentUploadPolicy;
    private final LocaleMessages localeMessages;
    private final String supportEmail;

    public CarInsuranceUploadFacade(
            final CarService carService,
            final ProfileDocumentUploadPolicy profileDocumentUploadPolicy,
            final LocaleMessages localeMessages,
            @org.springframework.beans.factory.annotation.Value("${app.support.email}") final String supportEmail) {
        this.carService = carService;
        this.profileDocumentUploadPolicy = profileDocumentUploadPolicy;
        this.localeMessages = localeMessages;
        this.supportEmail = supportEmail;
    }

    public InsuranceUploadOutcome attemptUpload(
            final long ownerUserId, final long carId, final MultipartFile insuranceFile) {
        if (insuranceFile == null || insuranceFile.isEmpty()) {
            return InsuranceUploadOutcome.missing(localeMessages.msg(MessageKeys.CAR_INSURANCE_INVALID));
        }
        if (insuranceFile.getSize() > profileDocumentUploadPolicy.getMaxBytes()) {
            final int maxMb = profileDocumentUploadPolicy.getMaxMegabytesRoundedUp();
            return InsuranceUploadOutcome.tooLarge(
                    localeMessages.msg(MessageKeys.CAR_INSURANCE_TOO_LARGE, maxMb));
        }
        final byte[] bytes;
        try {
            bytes = insuranceFile.getBytes();
        } catch (final IOException e) {
            return InsuranceUploadOutcome.readError(
                    localeMessages.msg(MessageKeys.PUBLISH_FAILED, supportEmail));
        }
        try {
            carService.uploadValidatedCarInsuranceDocument(
                    ownerUserId, carId, insuranceFile.getOriginalFilename(),
                    insuranceFile.getContentType(), bytes);
            return InsuranceUploadOutcome.ok();
        } catch (final RydenException e) {
            return InsuranceUploadOutcome.businessError(localeMessages.msg(e));
        }
    }

    public static final class InsuranceUploadOutcome {

        private final Status status;
        private final String localizedMessageOrNull;

        private InsuranceUploadOutcome(final Status status, final String localizedMessageOrNull) {
            this.status = status;
            this.localizedMessageOrNull = localizedMessageOrNull;
        }

        public Status getStatus() { return status; }

        public boolean isOk() { return status == Status.OK; }

        public Optional<String> getLocalizedMessage() { return Optional.ofNullable(localizedMessageOrNull); }

        static InsuranceUploadOutcome ok() { return new InsuranceUploadOutcome(Status.OK, null); }

        static InsuranceUploadOutcome missing(final String msg) {
            return new InsuranceUploadOutcome(Status.MISSING_FILE, msg);
        }

        static InsuranceUploadOutcome tooLarge(final String msg) {
            return new InsuranceUploadOutcome(Status.TOO_LARGE, msg);
        }

        static InsuranceUploadOutcome businessError(final String msg) {
            return new InsuranceUploadOutcome(Status.BUSINESS_ERROR, msg);
        }

        static InsuranceUploadOutcome readError(final String msg) {
            return new InsuranceUploadOutcome(Status.READ_ERROR, msg);
        }
    }
}
