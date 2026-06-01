package ar.edu.itba.paw.models.dto.profile;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.BiConsumer;

import ar.edu.itba.paw.models.dto.car.CarCard;

/**
 * Model attributes for the {@code counterpartyProfile} JSP; built in the service layer.
 *
 * The active-listings grid is intentionally exposed as raw {@link CarCard} rows rather
 * than as view-layer rows. The JSP renders them with {@code consumerCarCard.tag}, which
 * requires {@code VehicleCardView}; the controller is the right place to perform that
 * webapp-only conversion (the {@code models} module cannot depend on {@code webapp}).
 */
public final class CounterpartyProfilePageModel {

    private final String counterpartyForename;
    private final String counterpartySurname;
    private final String counterpartyAbout;
    private final Long counterpartyProfileImageId;
    private final String counterpartyMemberSinceDisplay;
    private final BigDecimal counterpartyAverageRating;
    private final boolean counterpartyLicenseValidated;
    private final boolean counterpartyIdentityValidated;
    private final List<ReviewItemDto> recentReviewComments;
    private final boolean showCounterpartyActiveListings;
    private final List<CarCard> activeOwnerCarCards;
    private final CounterpartyActiveCarsLoadMore counterpartyActiveCarsLoadMore;

    public CounterpartyProfilePageModel(
            final String counterpartyForename,
            final String counterpartySurname,
            final String counterpartyAbout,
            final Long counterpartyProfileImageId,
            final String counterpartyMemberSinceDisplay,
            final BigDecimal counterpartyAverageRating,
            final boolean counterpartyLicenseValidated,
            final boolean counterpartyIdentityValidated,
            final List<ReviewItemDto> recentReviewComments,
            final boolean showCounterpartyActiveListings,
            final List<CarCard> activeOwnerCarCards,
            final CounterpartyActiveCarsLoadMore counterpartyActiveCarsLoadMore) {
        this.counterpartyForename = counterpartyForename;
        this.counterpartySurname = counterpartySurname;
        this.counterpartyAbout = counterpartyAbout;
        this.counterpartyProfileImageId = counterpartyProfileImageId;
        this.counterpartyMemberSinceDisplay = counterpartyMemberSinceDisplay;
        this.counterpartyAverageRating = counterpartyAverageRating;
        this.counterpartyLicenseValidated = counterpartyLicenseValidated;
        this.counterpartyIdentityValidated = counterpartyIdentityValidated;
        this.recentReviewComments = List.copyOf(recentReviewComments);
        this.showCounterpartyActiveListings = showCounterpartyActiveListings;
        this.activeOwnerCarCards = List.copyOf(activeOwnerCarCards);
        this.counterpartyActiveCarsLoadMore =
                counterpartyActiveCarsLoadMore != null
                        ? counterpartyActiveCarsLoadMore
                        : CounterpartyActiveCarsLoadMore.none();
    }

    /** Raw owner car rows for the controller to convert into {@code VehicleCardView}s. */
    public List<CarCard> getActiveOwnerCarCards() {
        return activeOwnerCarCards;
    }

    /**
     * Adds every header / metadata attribute the JSP expects. The active-listings grid
     * itself is not added here because the webapp layer must convert {@link CarCard}
     * rows to {@code VehicleCardView}s before exposing them under
     * {@code counterpartyActiveListings}.
     */
    public final void populateModel(final BiConsumer<String, Object> putObject) {
        putObject.accept("counterpartyForename", counterpartyForename);
        putObject.accept("counterpartySurname", counterpartySurname);
        putObject.accept("counterpartyAbout", counterpartyAbout);
        putObject.accept("counterpartyProfileImageId", counterpartyProfileImageId);
        putObject.accept("counterpartyMemberSinceDisplay", counterpartyMemberSinceDisplay);
        putObject.accept("counterpartyAverageRating", counterpartyAverageRating);
        // Star floor used by counterpartyProfileHeader.tag for half-star rendering. Centralised
        // here so callers do not each compute `avg.longValue()` themselves.
        putObject.accept(
                "counterpartyRatingFloor",
                counterpartyAverageRating != null ? counterpartyAverageRating.longValue() : 0L);
        putObject.accept("counterpartyLicenseValidated", counterpartyLicenseValidated);
        putObject.accept("counterpartyIdentityValidated", counterpartyIdentityValidated);
        putObject.accept("recentReviewComments", recentReviewComments);
        putObject.accept("showCounterpartyActiveListings", showCounterpartyActiveListings);
        putObject.accept("counterpartyActiveCarsLoadMore", counterpartyActiveCarsLoadMore);
    }
}
