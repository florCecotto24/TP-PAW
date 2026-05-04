package ar.edu.itba.paw.models.dto.profile;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.BiConsumer;

/** Model attributes for the {@code counterpartyProfile} JSP; built in the service layer. */
public final class CounterpartyProfilePageModel {

    private final String counterpartyForename;
    private final String counterpartySurname;
    private final String counterpartyAbout;
    private final Long counterpartyProfileImageId;
    private final String counterpartyMemberSinceDisplay;
    private final BigDecimal counterpartyAverageRating;
    private final boolean counterpartyLicenseValidated;
    private final boolean counterpartyIdentityValidated;
    private final List<String> recentReviewComments;
    private final boolean showCounterpartyActiveListings;
    private final List<CounterpartyActiveListingCardRow> counterpartyActiveListings;
    private final CounterpartyActiveListingsLoadMore counterpartyActiveListingsLoadMore;

    public CounterpartyProfilePageModel(
            final String counterpartyForename,
            final String counterpartySurname,
            final String counterpartyAbout,
            final Long counterpartyProfileImageId,
            final String counterpartyMemberSinceDisplay,
            final BigDecimal counterpartyAverageRating,
            final boolean counterpartyLicenseValidated,
            final boolean counterpartyIdentityValidated,
            final List<String> recentReviewComments,
            final boolean showCounterpartyActiveListings,
            final List<CounterpartyActiveListingCardRow> counterpartyActiveListings,
            final CounterpartyActiveListingsLoadMore counterpartyActiveListingsLoadMore) {
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
        this.counterpartyActiveListings = List.copyOf(counterpartyActiveListings);
        this.counterpartyActiveListingsLoadMore =
                counterpartyActiveListingsLoadMore != null
                        ? counterpartyActiveListingsLoadMore
                        : CounterpartyActiveListingsLoadMore.none();
    }

    public final void populateModel(final BiConsumer<String, Object> putObject) {
        putObject.accept("counterpartyForename", counterpartyForename);
        putObject.accept("counterpartySurname", counterpartySurname);
        putObject.accept("counterpartyAbout", counterpartyAbout);
        putObject.accept("counterpartyProfileImageId", counterpartyProfileImageId);
        putObject.accept("counterpartyMemberSinceDisplay", counterpartyMemberSinceDisplay);
        putObject.accept("counterpartyAverageRating", counterpartyAverageRating);
        putObject.accept("counterpartyLicenseValidated", counterpartyLicenseValidated);
        putObject.accept("counterpartyIdentityValidated", counterpartyIdentityValidated);
        putObject.accept("recentReviewComments", recentReviewComments);
        putObject.accept("showCounterpartyActiveListings", showCounterpartyActiveListings);
        putObject.accept("counterpartyActiveListings", counterpartyActiveListings);
        putObject.accept("counterpartyActiveListingsLoadMore", counterpartyActiveListingsLoadMore);
    }
}
