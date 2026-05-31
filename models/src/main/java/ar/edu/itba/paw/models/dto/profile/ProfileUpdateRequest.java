package ar.edu.itba.paw.models.dto.profile;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Immutable command capturing every field that the "edit profile" form can change in one shot.
 * Consumed by {@code UserService.updateProfile}, which applies all mutations inside a single
 * transaction so the profile cannot be left in a half-updated state.
 *
 * <p>{@code birthDate} is the only nullable field; the rest are normalized by the service layer
 * (trim, optional blanking) and therefore accepted as raw strings here.</p>
 */
public final class ProfileUpdateRequest {

    private final String forename;
    private final String surname;
    private final String phoneNumberRaw;
    private final LocalDate birthDate;
    private final String aboutRaw;
    private final String cbuRaw;

    private ProfileUpdateRequest(final Builder b) {
        this.forename = b.forename;
        this.surname = b.surname;
        this.phoneNumberRaw = b.phoneNumberRaw;
        this.birthDate = b.birthDate;
        this.aboutRaw = b.aboutRaw;
        this.cbuRaw = b.cbuRaw;
    }

    public String getForename() {
        return forename;
    }

    public String getSurname() {
        return surname;
    }

    public String getPhoneNumberRaw() {
        return phoneNumberRaw;
    }

    public Optional<LocalDate> getBirthDate() {
        return Optional.ofNullable(birthDate);
    }

    public String getAboutRaw() {
        return aboutRaw;
    }

    public String getCbuRaw() {
        return cbuRaw;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String forename;
        private String surname;
        private String phoneNumberRaw;
        private LocalDate birthDate;
        private String aboutRaw;
        private String cbuRaw;

        public Builder forename(final String value) {
            this.forename = value;
            return this;
        }

        public Builder surname(final String value) {
            this.surname = value;
            return this;
        }

        public Builder phoneNumberRaw(final String value) {
            this.phoneNumberRaw = value;
            return this;
        }

        public Builder birthDate(final LocalDate value) {
            this.birthDate = value;
            return this;
        }

        public Builder aboutRaw(final String value) {
            this.aboutRaw = value;
            return this;
        }

        public Builder cbuRaw(final String value) {
            this.cbuRaw = value;
            return this;
        }

        public ProfileUpdateRequest build() {
            return new ProfileUpdateRequest(this);
        }
    }
}
