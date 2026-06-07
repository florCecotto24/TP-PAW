package ar.edu.itba.paw.models.util.search;

import java.math.BigDecimal;
import java.util.List;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.user.User;

/**
 * Raw, controller-side input for a public car search. Acts as a parameter object so the service
 * contract stops growing positional arguments (Effective Java Item 2: prefer builders to long
 * parameter lists).
 *
 * The class is immutable and field-renaming friendly: the controller fills the builder once with
 * the bound request parameters and the service consumes it to produce a {@link CarSearchCriteria}.
 * Bounds and normalisation (rating-band whitelisting, enum-token conversion, flexible-date parsing,
 * neighborhood fuzzy-matching, etc.) happen inside the service - not here.
 */
public final class CarSearchRequest {

    private final String query;
    private final List<Car.Type> categories;
    private final List<Car.Transmission> transmissions;
    private final List<Car.Powertrain> powertrains;
    private final BigDecimal priceMin;
    private final BigDecimal priceMax;
    private final List<String> ratingBands;
    private final String from;
    private final String until;
    private final int page;
    private final int uiPageSize;
    private final String sort;
    private final User viewer;
    private final List<Long> neighborhoodIds;
    private final boolean flexible;
    private final String flexMonth;
    private final Integer flexDays;

    private CarSearchRequest(final Builder b) {
        this.query = b.query;
        this.categories = b.categories;
        this.transmissions = b.transmissions;
        this.powertrains = b.powertrains;
        this.priceMin = b.priceMin;
        this.priceMax = b.priceMax;
        this.ratingBands = b.ratingBands;
        this.from = b.from;
        this.until = b.until;
        this.page = b.page;
        this.uiPageSize = b.uiPageSize;
        this.sort = b.sort;
        this.viewer = b.viewer;
        this.neighborhoodIds = b.neighborhoodIds;
        this.flexible = b.flexible;
        this.flexMonth = b.flexMonth;
        this.flexDays = b.flexDays;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getQuery() { return query; }
    public List<Car.Type> getCategories() { return categories; }
    public List<Car.Transmission> getTransmissions() { return transmissions; }
    public List<Car.Powertrain> getPowertrains() { return powertrains; }
    public BigDecimal getPriceMin() { return priceMin; }
    public BigDecimal getPriceMax() { return priceMax; }
    public List<String> getRatingBands() { return ratingBands; }
    public String getFrom() { return from; }
    public String getUntil() { return until; }
    public int getPage() { return page; }
    public int getUiPageSize() { return uiPageSize; }
    public String getSort() { return sort; }
    public User getViewer() { return viewer; }
    public List<Long> getNeighborhoodIds() { return neighborhoodIds; }
    public boolean isFlexible() { return flexible; }
    public String getFlexMonth() { return flexMonth; }
    public Integer getFlexDays() { return flexDays; }

    public static final class Builder {
        private String query;
        private List<Car.Type> categories;
        private List<Car.Transmission> transmissions;
        private List<Car.Powertrain> powertrains;
        private BigDecimal priceMin;
        private BigDecimal priceMax;
        private List<String> ratingBands;
        private String from;
        private String until;
        private int page;
        private int uiPageSize;
        private String sort;
        private User viewer;
        private List<Long> neighborhoodIds;
        private boolean flexible;
        private String flexMonth;
        private Integer flexDays;

        public Builder query(final String v) { this.query = v; return this; }
        public Builder categories(final List<Car.Type> v) { this.categories = v; return this; }
        public Builder transmissions(final List<Car.Transmission> v) { this.transmissions = v; return this; }
        public Builder powertrains(final List<Car.Powertrain> v) { this.powertrains = v; return this; }
        public Builder priceMin(final BigDecimal v) { this.priceMin = v; return this; }
        public Builder priceMax(final BigDecimal v) { this.priceMax = v; return this; }
        public Builder ratingBands(final List<String> v) { this.ratingBands = v; return this; }
        public Builder from(final String v) { this.from = v; return this; }
        public Builder until(final String v) { this.until = v; return this; }
        public Builder page(final int v) { this.page = v; return this; }
        public Builder uiPageSize(final int v) { this.uiPageSize = v; return this; }
        public Builder sort(final String v) { this.sort = v; return this; }
        public Builder viewer(final User v) { this.viewer = v; return this; }
        public Builder neighborhoodIds(final List<Long> v) { this.neighborhoodIds = v; return this; }
        public Builder flexible(final boolean v) { this.flexible = v; return this; }
        public Builder flexMonth(final String v) { this.flexMonth = v; return this; }
        public Builder flexDays(final Integer v) { this.flexDays = v; return this; }

        public CarSearchRequest build() {
            return new CarSearchRequest(this);
        }
    }
}
