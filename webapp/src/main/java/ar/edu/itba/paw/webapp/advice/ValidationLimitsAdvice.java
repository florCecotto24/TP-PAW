package ar.edu.itba.paw.webapp.advice;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import ar.edu.itba.paw.policy.CarGalleryUploadPolicy;
import ar.edu.itba.paw.policy.CarValidationPolicy;
import ar.edu.itba.paw.policy.ListingFormValidationPolicy;
import ar.edu.itba.paw.policy.MoneyFormatPolicy;
import ar.edu.itba.paw.policy.ReservationFormValidationPolicy;
import ar.edu.itba.paw.policy.ReservationMessageValidationPolicy;
import ar.edu.itba.paw.policy.ReviewValidationPolicy;
import ar.edu.itba.paw.policy.UserValidationPolicy;
import ar.edu.itba.paw.policy.VerificationCodePolicy;

/**
 * Exposes every {@code maxlength} / {@code minlength} bound surfaced by JSPs as a model attribute, sourced
 * exclusively from the policy beans. Adding a new field amounts to a one-line accessor here, so no JSP ever
 * needs to hardcode a numeric literal.
 */
@ControllerAdvice
public final class ValidationLimitsAdvice {

    private final UserValidationPolicy userValidationPolicy;
    private final ReviewValidationPolicy reviewValidationPolicy;
    private final ReservationMessageValidationPolicy reservationMessageValidationPolicy;
    private final CarValidationPolicy carValidationPolicy;
    private final ListingFormValidationPolicy listingFormValidationPolicy;
    private final ReservationFormValidationPolicy reservationFormValidationPolicy;
    private final VerificationCodePolicy verificationCodePolicy;
    private final CarGalleryUploadPolicy carGalleryUploadPolicy;
    private final MoneyFormatPolicy moneyFormatPolicy;

    public ValidationLimitsAdvice(
            final UserValidationPolicy userValidationPolicy,
            final ReviewValidationPolicy reviewValidationPolicy,
            final ReservationMessageValidationPolicy reservationMessageValidationPolicy,
            final CarValidationPolicy carValidationPolicy,
            final ListingFormValidationPolicy listingFormValidationPolicy,
            final ReservationFormValidationPolicy reservationFormValidationPolicy,
            final VerificationCodePolicy verificationCodePolicy,
            final CarGalleryUploadPolicy carGalleryUploadPolicy,
            final MoneyFormatPolicy moneyFormatPolicy) {
        this.userValidationPolicy = userValidationPolicy;
        this.reviewValidationPolicy = reviewValidationPolicy;
        this.reservationMessageValidationPolicy = reservationMessageValidationPolicy;
        this.carValidationPolicy = carValidationPolicy;
        this.listingFormValidationPolicy = listingFormValidationPolicy;
        this.reservationFormValidationPolicy = reservationFormValidationPolicy;
        this.verificationCodePolicy = verificationCodePolicy;
        this.carGalleryUploadPolicy = carGalleryUploadPolicy;
        this.moneyFormatPolicy = moneyFormatPolicy;
    }

    @ModelAttribute("userDisplayNamePartMaxLength")
    public int userDisplayNamePartMaxLength() {
        return userValidationPolicy.getDisplayNamePartMaxLength();
    }

    @ModelAttribute("userEmailMaxLength")
    public int userEmailMaxLength() {
        return userValidationPolicy.getRegistrationEmailMaxLength();
    }

    @ModelAttribute("userPasswordMaxLength")
    public int userPasswordMaxLength() {
        return userValidationPolicy.getRegistrationPasswordMaxLength();
    }

    @ModelAttribute("userPhoneMaxLength")
    public int userPhoneMaxLength() {
        return userValidationPolicy.getProfilePhoneMaxLength();
    }

    @ModelAttribute("userAboutMaxLength")
    public int userAboutMaxLength() {
        return userValidationPolicy.getProfileAboutMaxLength();
    }

    @ModelAttribute("reviewCommentMaxLength")
    public int reviewCommentMaxLength() {
        return reviewValidationPolicy.getCommentMaxLength();
    }

    @ModelAttribute("chatMessageMaxLength")
    public int chatMessageMaxLength() {
        return reservationMessageValidationPolicy.getBodyMaxLength();
    }

    @ModelAttribute("carBrandMinLength")
    public int carBrandMinLength() {
        return carValidationPolicy.getBrandMinLength();
    }

    @ModelAttribute("carBrandMaxLength")
    public int carBrandMaxLength() {
        return carValidationPolicy.getBrandMaxLength();
    }

    @ModelAttribute("carModelMaxLength")
    public int carModelMaxLength() {
        return carValidationPolicy.getModelMaxLength();
    }

    @ModelAttribute("carPlateMinLength")
    public int carPlateMinLength() {
        return carValidationPolicy.getPlateMinLength();
    }

    @ModelAttribute("carPlateMaxLength")
    public int carPlateMaxLength() {
        return carValidationPolicy.getPlateMaxLength();
    }

    @ModelAttribute("carDescriptionMaxLength")
    public int carDescriptionMaxLength() {
        return carValidationPolicy.getDescriptionMaxLength();
    }

    @ModelAttribute("carYearMin")
    public int carYearMin() {
        return carValidationPolicy.getYearMin();
    }

    @ModelAttribute("listingAddressStreetMaxLength")
    public int listingAddressStreetMaxLength() {
        return listingFormValidationPolicy.getAddressStreetMaxLength();
    }

    @ModelAttribute("listingAddressNumberMaxLength")
    public int listingAddressNumberMaxLength() {
        return listingFormValidationPolicy.getAddressNumberMaxLength();
    }

    @ModelAttribute("listingPricePerDayMin")
    public String listingPricePerDayMin() {
        return listingFormValidationPolicy.getPricePerDayMin().toPlainString();
    }

    @ModelAttribute("listingPricePerDayMax")
    public String listingPricePerDayMax() {
        return listingFormValidationPolicy.getPricePerDayMaxValue().toPlainString();
    }

    @ModelAttribute("listingPricePerDayIntegerDigits")
    public int listingPricePerDayIntegerDigits() {
        return listingFormValidationPolicy.getPricePerDayIntegerDigits();
    }

    @ModelAttribute("listingPricePerDayFractionDigits")
    public int listingPricePerDayFractionDigits() {
        return listingFormValidationPolicy.getPricePerDayFractionDigits();
    }

    @ModelAttribute("listingMinimumRentalDaysMin")
    public int listingMinimumRentalDaysMin() {
        return listingFormValidationPolicy.getMinimumRentalDaysMin();
    }

    @ModelAttribute("listingMinimumRentalDaysMax")
    public int listingMinimumRentalDaysMax() {
        return listingFormValidationPolicy.getMinimumRentalDaysMax();
    }

    @ModelAttribute("listingAvailabilityRowsMax")
    public int listingAvailabilityRowsMax() {
        return listingFormValidationPolicy.getAvailabilityRowsMax();
    }

    @ModelAttribute("reservationDeliveryLocationMaxLength")
    public int reservationDeliveryLocationMaxLength() {
        return reservationFormValidationPolicy.getDeliveryLocationMaxLength();
    }

    @ModelAttribute("reservationCarNameMaxLength")
    public int reservationCarNameMaxLength() {
        return reservationFormValidationPolicy.getCarNameMaxLength();
    }

    @ModelAttribute("reservationDatetimeInputMaxLength")
    public int reservationDatetimeInputMaxLength() {
        return reservationFormValidationPolicy.getDatetimeInputMaxLength();
    }

    @ModelAttribute("verificationCodeLength")
    public int verificationCodeLength() {
        return verificationCodePolicy.getCodeLength();
    }

    @ModelAttribute("verificationCodePattern")
    public String verificationCodePattern() {
        return verificationCodePolicy.getCodePatternString();
    }

    @ModelAttribute("carGalleryMaxItems")
    public int carGalleryMaxItems() {
        return carGalleryUploadPolicy.getMaxItems();
    }

    @ModelAttribute("moneyCurrencyCode")
    public String moneyCurrencyCode() {
        return moneyFormatPolicy.getCurrencyCode();
    }

    @ModelAttribute("moneyFormatLocale")
    public String moneyFormatLocale() {
        return moneyFormatPolicy.getFormatLocale().toLanguageTag();
    }
}
