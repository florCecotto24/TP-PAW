package ar.edu.itba.paw.models.dto.car.detail;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.BiConsumer;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.models.dto.car.CarGalleryMediaItem;
import ar.edu.itba.paw.models.dto.Page;

/**
 * Everything the public {@code car/carDetail} JSP needs that depends on the persistence/service
 * layer. Assembled by {@code CarDetailViewService} so the controller stops orchestrating eight
 * services and producing a 23-attribute {@code ModelAndView} by hand.
 *
 * <p>Similar listings are exposed as raw {@link CarCard}s on purpose — the JSP needs them as
 * {@code VehicleCardView} (a webapp-layer DTO), so the controller does that final conversion
 * and adds {@code similarListings} to the model itself. Everything else flows through
 * {@link #populateModel(BiConsumer)} in one shot.</p>
 */
public final class CarDetailPageModel {

    private final Car car;
    private final String carTitle;
    private final BigDecimal carMinEffectiveDayPrice;
    private final boolean carPriceIsVariable;
    private final String carRatingLabel;
    private final String carReviewCountLabel;
    private final Page<CarReviewRow> carReviewPage;
    private final String reviewsView;
    private final User owner;
    private final Long ownerProfileImageId;
    private final List<CarGalleryMediaItem> carGalleryMedia;
    private final boolean hasBookableDays;
    private final String bookableWallRangesJson;
    private final List<CarCard> similarListings;
    private final String similarSearchUrl;
    private final int maxReservationBillableDays;
    private final boolean isOwnerRequesting;
    private final boolean currentUserIsAdmin;
    private final boolean carIsFavoritable;
    private final boolean carIsFavorited;

    private CarDetailPageModel(final Builder b) {
        this.car = b.car;
        this.carTitle = b.carTitle;
        this.carMinEffectiveDayPrice = b.carMinEffectiveDayPrice;
        this.carPriceIsVariable = b.carPriceIsVariable;
        this.carRatingLabel = b.carRatingLabel;
        this.carReviewCountLabel = b.carReviewCountLabel;
        this.carReviewPage = b.carReviewPage;
        this.reviewsView = b.reviewsView;
        this.owner = b.owner;
        this.ownerProfileImageId = b.ownerProfileImageId;
        this.carGalleryMedia = List.copyOf(b.carGalleryMedia);
        this.hasBookableDays = b.hasBookableDays;
        this.bookableWallRangesJson = b.bookableWallRangesJson;
        this.similarListings = List.copyOf(b.similarListings);
        this.similarSearchUrl = b.similarSearchUrl;
        this.maxReservationBillableDays = b.maxReservationBillableDays;
        this.isOwnerRequesting = b.isOwnerRequesting;
        this.currentUserIsAdmin = b.currentUserIsAdmin;
        this.carIsFavoritable = b.carIsFavoritable;
        this.carIsFavorited = b.carIsFavorited;
    }

    public List<CarCard> getSimilarListings() {
        return similarListings;
    }

    /**
     * Adds every attribute the JSP renders directly. {@code similarListings} is intentionally
     * omitted: the controller converts it into the view-layer {@code VehicleCardView} and adds
     * it under the same key.
     */
    public void populateModel(final BiConsumer<String, Object> putObject) {
        putObject.accept("car", car);
        putObject.accept("carTitle", carTitle);
        putObject.accept("carMinEffectiveDayPrice", carMinEffectiveDayPrice);
        putObject.accept("carPriceIsVariable", carPriceIsVariable);
        putObject.accept("carRatingLabel", carRatingLabel);
        putObject.accept("carReviewCountLabel", carReviewCountLabel);
        putObject.accept("carReviewPage", carReviewPage);
        putObject.accept("reviewsView", reviewsView);
        putObject.accept("owner", owner);
        putObject.accept("ownerProfileImageId", ownerProfileImageId);
        putObject.accept("carGalleryMedia", carGalleryMedia);
        putObject.accept("hasBookableDays", hasBookableDays);
        putObject.accept("bookableWallRangesJson", bookableWallRangesJson);
        putObject.accept("similarSearchUrl", similarSearchUrl);
        putObject.accept("maxReservationBillableDays", maxReservationBillableDays);
        putObject.accept("isOwnerRequesting", isOwnerRequesting);
        putObject.accept("currentUserIsAdmin", currentUserIsAdmin);
        putObject.accept("carIsFavoritable", carIsFavoritable);
        putObject.accept("carIsFavorited", carIsFavorited);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Car car;
        private String carTitle;
        private BigDecimal carMinEffectiveDayPrice;
        private boolean carPriceIsVariable;
        private String carRatingLabel;
        private String carReviewCountLabel;
        private Page<CarReviewRow> carReviewPage;
        private String reviewsView;
        private User owner;
        private Long ownerProfileImageId;
        private List<CarGalleryMediaItem> carGalleryMedia = List.of();
        private boolean hasBookableDays;
        private String bookableWallRangesJson;
        private List<CarCard> similarListings = List.of();
        private String similarSearchUrl;
        private int maxReservationBillableDays;
        private boolean isOwnerRequesting;
        private boolean currentUserIsAdmin;
        private boolean carIsFavoritable;
        private boolean carIsFavorited;

        public Builder car(final Car v) { this.car = v; return this; }
        public Builder carTitle(final String v) { this.carTitle = v; return this; }
        public Builder carMinEffectiveDayPrice(final BigDecimal v) { this.carMinEffectiveDayPrice = v; return this; }
        public Builder carPriceIsVariable(final boolean v) { this.carPriceIsVariable = v; return this; }
        public Builder carRatingLabel(final String v) { this.carRatingLabel = v; return this; }
        public Builder carReviewCountLabel(final String v) { this.carReviewCountLabel = v; return this; }
        public Builder carReviewPage(final Page<CarReviewRow> v) { this.carReviewPage = v; return this; }
        public Builder reviewsView(final String v) { this.reviewsView = v; return this; }
        public Builder owner(final User v) { this.owner = v; return this; }
        public Builder ownerProfileImageId(final Long v) { this.ownerProfileImageId = v; return this; }
        public Builder carGalleryMedia(final List<CarGalleryMediaItem> v) { this.carGalleryMedia = v; return this; }
        public Builder hasBookableDays(final boolean v) { this.hasBookableDays = v; return this; }
        public Builder bookableWallRangesJson(final String v) { this.bookableWallRangesJson = v; return this; }
        public Builder similarListings(final List<CarCard> v) { this.similarListings = v; return this; }
        public Builder similarSearchUrl(final String v) { this.similarSearchUrl = v; return this; }
        public Builder maxReservationBillableDays(final int v) { this.maxReservationBillableDays = v; return this; }
        public Builder isOwnerRequesting(final boolean v) { this.isOwnerRequesting = v; return this; }
        public Builder currentUserIsAdmin(final boolean v) { this.currentUserIsAdmin = v; return this; }
        public Builder carIsFavoritable(final boolean v) { this.carIsFavoritable = v; return this; }
        public Builder carIsFavorited(final boolean v) { this.carIsFavorited = v; return this; }

        public CarDetailPageModel build() {
            return new CarDetailPageModel(this);
        }
    }
}
