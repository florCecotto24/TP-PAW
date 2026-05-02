package ar.edu.itba.paw.models.dto.profile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/** Header block for the rider/owner profile panel: identity, ratings badge, about, member since, profile image id. */
public final class CounterpartyHeaderDto {

    private final String fullName;
    private final String forename;
    private final String surname;
    private final String badgeLabel;
    private final BigDecimal averageRating;
    private final long reviewCount;
    private final String about;
    private final LocalDate memberSince;
    private final String memberSinceDisplay;
    private final Long profileImageId;

    public CounterpartyHeaderDto(
            final String fullName,
            final String forename,
            final String surname,
            final String badgeLabel,
            final BigDecimal averageRating,
            final long reviewCount,
            final String about,
            final LocalDate memberSince,
            final String memberSinceDisplay,
            final Long profileImageId) {
        this.fullName = fullName;
        this.forename = forename;
        this.surname = surname;
        this.badgeLabel = badgeLabel;
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
        this.about = about;
        this.memberSince = memberSince;
        this.memberSinceDisplay = memberSinceDisplay;
        this.profileImageId = profileImageId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getForename() {
        return forename;
    }

    public String getSurname() {
        return surname;
    }

    public String getBadgeLabel() {
        return badgeLabel;
    }

    public BigDecimal getAverageRating() {
        return averageRating;
    }

    public long getReviewCount() {
        return reviewCount;
    }

    public Optional<String> getAbout() {
        return Optional.ofNullable(about);
    }

    public LocalDate getMemberSince() {
        return memberSince;
    }

    public Optional<String> getMemberSinceDisplay() {
        return Optional.ofNullable(memberSinceDisplay);
    }

    public Optional<Long> getProfileImageId() {
        return Optional.ofNullable(profileImageId);
    }
}

