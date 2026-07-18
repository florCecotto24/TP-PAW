package ar.edu.itba.paw.webapp.dto.rest;

import org.springframework.core.env.Environment;

import ar.edu.itba.paw.models.util.rules.CbuRules;
import ar.edu.itba.paw.policy.CarGalleryUploadPolicy;
import ar.edu.itba.paw.policy.ChatAttachmentUploadPolicy;
import ar.edu.itba.paw.policy.ReservationTimingPolicy;
import ar.edu.itba.paw.webapp.config.properties.AppMoneyProperties;
import ar.edu.itba.paw.webapp.config.properties.AppValidationProperties;

/**
 * Public SPA limits mirrored from {@code application.properties} so the client can avoid
 * hardcoding validation policy (fallbacks remain documented in the SPA when this endpoint fails).
 */
public final class ClientConfigDto {

    private LinksDto links;
    private int cbuRequiredDigits;
    private int maxBillableDays;
    private int pickupLeadHours;
    private CarLimits car;
    private UploadLimits upload;
    private MoneyLimits money;
    private UserLimits user;
    private ChatLimits chat;
    private ReviewLimits review;
    private ListingLimits listing;

    public ClientConfigDto() {
    }

    public static ClientConfigDto from(
            final AppValidationProperties validation,
            final AppMoneyProperties moneyProps,
            final ReservationTimingPolicy reservationTiming,
            final CarGalleryUploadPolicy galleryUploadPolicy,
            final ChatAttachmentUploadPolicy chatAttachmentUploadPolicy,
            final Environment environment,
            final String selfUri) {
        final ClientConfigDto dto = new ClientConfigDto();
        dto.links = LinksDto.ofSelf(selfUri);
        dto.cbuRequiredDigits = CbuRules.REQUIRED_DIGIT_LENGTH;
        dto.maxBillableDays = reservationTiming.getMaxBillableDaysPerReservation();
        dto.pickupLeadHours = reservationTiming.getPickupLeadHours();
        dto.car = CarLimits.from(validation, galleryUploadPolicy);
        dto.upload = UploadLimits.from(galleryUploadPolicy, environment);
        dto.money = MoneyLimits.from(moneyProps);
        dto.user = UserLimits.from(validation);
        dto.chat = ChatLimits.from(validation, chatAttachmentUploadPolicy, environment);
        dto.review = ReviewLimits.from(validation);
        dto.listing = ListingLimits.from(validation);
        return dto;
    }

    public LinksDto getLinks() {
        return links;
    }

    public void setLinks(final LinksDto links) {
        this.links = links;
    }

    public int getCbuRequiredDigits() {
        return cbuRequiredDigits;
    }

    public void setCbuRequiredDigits(final int cbuRequiredDigits) {
        this.cbuRequiredDigits = cbuRequiredDigits;
    }

    public int getMaxBillableDays() {
        return maxBillableDays;
    }

    public void setMaxBillableDays(final int maxBillableDays) {
        this.maxBillableDays = maxBillableDays;
    }

    public int getPickupLeadHours() {
        return pickupLeadHours;
    }

    public void setPickupLeadHours(final int pickupLeadHours) {
        this.pickupLeadHours = pickupLeadHours;
    }

    public CarLimits getCar() {
        return car;
    }

    public void setCar(final CarLimits car) {
        this.car = car;
    }

    public UploadLimits getUpload() {
        return upload;
    }

    public void setUpload(final UploadLimits upload) {
        this.upload = upload;
    }

    public MoneyLimits getMoney() {
        return money;
    }

    public void setMoney(final MoneyLimits money) {
        this.money = money;
    }

    public UserLimits getUser() {
        return user;
    }

    public void setUser(final UserLimits user) {
        this.user = user;
    }

    public ChatLimits getChat() {
        return chat;
    }

    public void setChat(final ChatLimits chat) {
        this.chat = chat;
    }

    public ReviewLimits getReview() {
        return review;
    }

    public void setReview(final ReviewLimits review) {
        this.review = review;
    }

    public ListingLimits getListing() {
        return listing;
    }

    public void setListing(final ListingLimits listing) {
        this.listing = listing;
    }

    public static final class CarLimits {
        private int brandMinLength;
        private int brandMaxLength;
        private int modelMaxLength;
        private int plateMinLength;
        private int plateMaxLength;
        private int descriptionMaxLength;
        private int yearMin;
        private int galleryMaxItems;

        public CarLimits() {
        }

        private static CarLimits from(
                final AppValidationProperties validation,
                final CarGalleryUploadPolicy galleryUploadPolicy) {
            final CarLimits limits = new CarLimits();
            limits.brandMinLength = validation.carBrandMinLength();
            limits.brandMaxLength = validation.carBrandMaxLength();
            limits.modelMaxLength = validation.carModelMaxLength();
            limits.plateMinLength = validation.carPlateMinLength();
            limits.plateMaxLength = validation.carPlateMaxLength();
            limits.descriptionMaxLength = validation.carDescriptionMaxLength();
            limits.yearMin = validation.carYearMin();
            limits.galleryMaxItems = galleryUploadPolicy.getMaxItems();
            return limits;
        }

        public int getBrandMinLength() {
            return brandMinLength;
        }

        public void setBrandMinLength(final int brandMinLength) {
            this.brandMinLength = brandMinLength;
        }

        public int getBrandMaxLength() {
            return brandMaxLength;
        }

        public void setBrandMaxLength(final int brandMaxLength) {
            this.brandMaxLength = brandMaxLength;
        }

        public int getModelMaxLength() {
            return modelMaxLength;
        }

        public void setModelMaxLength(final int modelMaxLength) {
            this.modelMaxLength = modelMaxLength;
        }

        public int getPlateMinLength() {
            return plateMinLength;
        }

        public void setPlateMinLength(final int plateMinLength) {
            this.plateMinLength = plateMinLength;
        }

        public int getPlateMaxLength() {
            return plateMaxLength;
        }

        public void setPlateMaxLength(final int plateMaxLength) {
            this.plateMaxLength = plateMaxLength;
        }

        public int getDescriptionMaxLength() {
            return descriptionMaxLength;
        }

        public void setDescriptionMaxLength(final int descriptionMaxLength) {
            this.descriptionMaxLength = descriptionMaxLength;
        }

        public int getYearMin() {
            return yearMin;
        }

        public void setYearMin(final int yearMin) {
            this.yearMin = yearMin;
        }

        public int getGalleryMaxItems() {
            return galleryMaxItems;
        }

        public void setGalleryMaxItems(final int galleryMaxItems) {
            this.galleryMaxItems = galleryMaxItems;
        }
    }

    public static final class UploadLimits {
        private int maxImageMegabytes;
        private int maxCarGalleryVideoMegabytes;
        private int maxProfileDocumentMegabytes;
        private int maxPaymentReceiptMegabytes;

        public UploadLimits() {
        }

        private static UploadLimits from(
                final CarGalleryUploadPolicy galleryUploadPolicy,
                final Environment environment) {
            final UploadLimits limits = new UploadLimits();
            limits.maxImageMegabytes = galleryUploadPolicy.getMaxImageMegabytesRoundedUp();
            limits.maxCarGalleryVideoMegabytes = galleryUploadPolicy.getMaxVideoMegabytesRoundedUp();
            limits.maxProfileDocumentMegabytes = readMegabytes(
                    environment,
                    "app.upload.max-profile-document-megabytes",
                    5);
            limits.maxPaymentReceiptMegabytes = readMegabytes(
                    environment,
                    "app.upload.max-payment-receipt-megabytes",
                    5);
            return limits;
        }

        private static int readMegabytes(final Environment environment, final String key, final int fallback) {
            final Integer value = environment.getProperty(key, Integer.class);
            return value != null && value > 0 ? value : fallback;
        }

        public int getMaxImageMegabytes() {
            return maxImageMegabytes;
        }

        public void setMaxImageMegabytes(final int maxImageMegabytes) {
            this.maxImageMegabytes = maxImageMegabytes;
        }

        public int getMaxCarGalleryVideoMegabytes() {
            return maxCarGalleryVideoMegabytes;
        }

        public void setMaxCarGalleryVideoMegabytes(final int maxCarGalleryVideoMegabytes) {
            this.maxCarGalleryVideoMegabytes = maxCarGalleryVideoMegabytes;
        }

        public int getMaxProfileDocumentMegabytes() {
            return maxProfileDocumentMegabytes;
        }

        public void setMaxProfileDocumentMegabytes(final int maxProfileDocumentMegabytes) {
            this.maxProfileDocumentMegabytes = maxProfileDocumentMegabytes;
        }

        public int getMaxPaymentReceiptMegabytes() {
            return maxPaymentReceiptMegabytes;
        }

        public void setMaxPaymentReceiptMegabytes(final int maxPaymentReceiptMegabytes) {
            this.maxPaymentReceiptMegabytes = maxPaymentReceiptMegabytes;
        }
    }

    public static final class MoneyLimits {
        private String currency;
        private String formatLocale;

        public MoneyLimits() {
        }

        private static MoneyLimits from(final AppMoneyProperties moneyProps) {
            final MoneyLimits limits = new MoneyLimits();
            limits.currency = moneyProps.currency();
            limits.formatLocale = moneyProps.formatLocale();
            return limits;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(final String currency) {
            this.currency = currency;
        }

        public String getFormatLocale() {
            return formatLocale;
        }

        public void setFormatLocale(final String formatLocale) {
            this.formatLocale = formatLocale;
        }
    }

    public static final class UserLimits {
        private int displayNamePartMaxLength;
        private int profileAboutMaxLength;
        private int registrationPasswordMinLength;
        private int registrationPasswordMaxLength;
        private int registrationEmailMaxLength;
        private int profilePhoneMaxLength;

        public UserLimits() {
        }

        private static UserLimits from(final AppValidationProperties validation) {
            final UserLimits limits = new UserLimits();
            limits.displayNamePartMaxLength = validation.displayNamePartMaxLength();
            limits.profileAboutMaxLength = validation.profileAboutMaxLength();
            limits.registrationPasswordMinLength = validation.registrationPasswordMinLength();
            limits.registrationPasswordMaxLength = validation.registrationPasswordMaxLength();
            limits.registrationEmailMaxLength = validation.registrationEmailMaxLength();
            limits.profilePhoneMaxLength = validation.profilePhoneMaxLength();
            return limits;
        }

        public int getDisplayNamePartMaxLength() {
            return displayNamePartMaxLength;
        }

        public void setDisplayNamePartMaxLength(final int displayNamePartMaxLength) {
            this.displayNamePartMaxLength = displayNamePartMaxLength;
        }

        public int getProfileAboutMaxLength() {
            return profileAboutMaxLength;
        }

        public void setProfileAboutMaxLength(final int profileAboutMaxLength) {
            this.profileAboutMaxLength = profileAboutMaxLength;
        }

        public int getRegistrationPasswordMinLength() {
            return registrationPasswordMinLength;
        }

        public void setRegistrationPasswordMinLength(final int registrationPasswordMinLength) {
            this.registrationPasswordMinLength = registrationPasswordMinLength;
        }

        public int getRegistrationPasswordMaxLength() {
            return registrationPasswordMaxLength;
        }

        public void setRegistrationPasswordMaxLength(final int registrationPasswordMaxLength) {
            this.registrationPasswordMaxLength = registrationPasswordMaxLength;
        }

        public int getRegistrationEmailMaxLength() {
            return registrationEmailMaxLength;
        }

        public void setRegistrationEmailMaxLength(final int registrationEmailMaxLength) {
            this.registrationEmailMaxLength = registrationEmailMaxLength;
        }

        public int getProfilePhoneMaxLength() {
            return profilePhoneMaxLength;
        }

        public void setProfilePhoneMaxLength(final int profilePhoneMaxLength) {
            this.profilePhoneMaxLength = profilePhoneMaxLength;
        }
    }

    public static final class ChatLimits {
        private int maxAttachmentMegabytes;
        private int messageMaxLength;
        private int historyPageSize;

        public ChatLimits() {
        }

        private static ChatLimits from(
                final AppValidationProperties validation,
                final ChatAttachmentUploadPolicy chatAttachmentUploadPolicy,
                final Environment environment) {
            final ChatLimits limits = new ChatLimits();
            limits.maxAttachmentMegabytes = chatAttachmentUploadPolicy.getMaxMegabytesRoundedUp();
            limits.messageMaxLength = validation.reservationMessageMaxLength();
            limits.historyPageSize = readPositiveInt(environment, "app.reservation.chat.history-page-size", 12);
            return limits;
        }

        public int getMaxAttachmentMegabytes() {
            return maxAttachmentMegabytes;
        }

        public void setMaxAttachmentMegabytes(final int maxAttachmentMegabytes) {
            this.maxAttachmentMegabytes = maxAttachmentMegabytes;
        }

        public int getMessageMaxLength() {
            return messageMaxLength;
        }

        public void setMessageMaxLength(final int messageMaxLength) {
            this.messageMaxLength = messageMaxLength;
        }

        public int getHistoryPageSize() {
            return historyPageSize;
        }

        public void setHistoryPageSize(final int historyPageSize) {
            this.historyPageSize = historyPageSize;
        }
    }

    public static final class ReviewLimits {
        private int commentMaxLength;

        public ReviewLimits() {
        }

        private static ReviewLimits from(final AppValidationProperties validation) {
            final ReviewLimits limits = new ReviewLimits();
            limits.commentMaxLength = validation.reviewCommentMaxLength();
            return limits;
        }

        public int getCommentMaxLength() {
            return commentMaxLength;
        }

        public void setCommentMaxLength(final int commentMaxLength) {
            this.commentMaxLength = commentMaxLength;
        }
    }

    public static final class ListingLimits {
        private double pricePerDayMin;
        private int addressStreetMaxLength;
        private int addressNumberMaxLength;
        private int pricePerDayIntegerDigits;
        private int pricePerDayFractionDigits;

        public ListingLimits() {
        }

        private static ListingLimits from(final AppValidationProperties validation) {
            final ListingLimits limits = new ListingLimits();
            limits.pricePerDayMin = validation.listingPricePerDayMin().doubleValue();
            limits.addressStreetMaxLength = validation.listingAddressStreetMaxLength();
            limits.addressNumberMaxLength = validation.listingAddressNumberMaxLength();
            limits.pricePerDayIntegerDigits = validation.listingPricePerDayIntegerDigits();
            limits.pricePerDayFractionDigits = validation.listingPricePerDayFractionDigits();
            return limits;
        }

        public double getPricePerDayMin() {
            return pricePerDayMin;
        }

        public void setPricePerDayMin(final double pricePerDayMin) {
            this.pricePerDayMin = pricePerDayMin;
        }

        public int getAddressStreetMaxLength() {
            return addressStreetMaxLength;
        }

        public void setAddressStreetMaxLength(final int addressStreetMaxLength) {
            this.addressStreetMaxLength = addressStreetMaxLength;
        }

        public int getAddressNumberMaxLength() {
            return addressNumberMaxLength;
        }

        public void setAddressNumberMaxLength(final int addressNumberMaxLength) {
            this.addressNumberMaxLength = addressNumberMaxLength;
        }

        public int getPricePerDayIntegerDigits() {
            return pricePerDayIntegerDigits;
        }

        public void setPricePerDayIntegerDigits(final int pricePerDayIntegerDigits) {
            this.pricePerDayIntegerDigits = pricePerDayIntegerDigits;
        }

        public int getPricePerDayFractionDigits() {
            return pricePerDayFractionDigits;
        }

        public void setPricePerDayFractionDigits(final int pricePerDayFractionDigits) {
            this.pricePerDayFractionDigits = pricePerDayFractionDigits;
        }
    }

    private static int readPositiveInt(final Environment environment, final String key, final int fallback) {
        final Integer value = environment.getProperty(key, Integer.class);
        return value != null && value > 0 ? value : fallback;
    }
}
