package ar.edu.itba.paw.models.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.AttributeConverter;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Converter;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/** Vehicle inventory row: owner, plate, car-model FK, category enums, lifecycle status, description, timestamps and rating cache. */
@Entity
@Table(name = "cars")
public class Car {
    public interface PrettyNamed {
        default String prettyName() {
            return ((Enum<?>) this).name().replace("_", " ").toLowerCase();
        }
    }

    public enum Type implements PrettyNamed {
        SEDAN, HATCHBACK, SUV, COUPE, CONVERTIBLE, WAGON, VAN, PICKUP
    }

    public enum Powertrain implements PrettyNamed {
        GASOLINE, DIESEL, ELECTRIC, HYBRID, CNG
    }

    public enum Transmission implements PrettyNamed {
        MANUAL, AUTOMATIC, SEMI_AUTOMATIC;
    }

    /**
     * Lifecycle status of a car:
     * 
     *   {@code ACTIVE}: published with at least one bookable availability and all required documents.
     *   {@code PAUSED}: paused by the owner; reversible.
     *   {@code ADMIN_PAUSED}: paused by an admin; only admins may un-pause.
     *   {@code LACK_DOC}: missing insurance, identity file or owner CBU; not bookable until completed.
     *   {@code UNAVAILABLE}: no remaining bookable availability dates.
     *   {@code DEACTIVATED}: definitive removal by the owner; irreversible.
     *
     */
    public enum Status {
        ACTIVE,
        PAUSED,
        ADMIN_PAUSED,
        LACK_DOC,
        UNAVAILABLE,
        DEACTIVATED
    }

    @Converter
    public static class StatusConverter implements AttributeConverter<Status, String> {
        @Override
        public String convertToDatabaseColumn(final Status attribute) {
            return attribute == null ? null : attribute.name().toLowerCase();
        }
        @Override
        public Status convertToEntityAttribute(final String dbData) {
            return dbData == null ? null : Status.valueOf(dbData.toUpperCase());
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cars_id_seq")
    @SequenceGenerator(name = "cars_id_seq", sequenceName = "cars_id_seq", allocationSize = 1)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 50)
    private String plate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Powertrain powertrain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Transmission transmission;

    @Convert(converter = StatusConverter.class)
    @Column(nullable = false, length = 40)
    private Status status;

    @Column(length = 200)
    private String description;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "rating_avg", precision = 4, scale = 2)
    private BigDecimal ratingAvg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id")
    private CarModel carModel;

    @OneToMany(mappedBy = "car", fetch = FetchType.LAZY)
    private List<Listing> listings = new ArrayList<>();

    @OneToMany(mappedBy = "car", fetch = FetchType.LAZY, orphanRemoval = true)
    private List<CarPicture> pictures = new ArrayList<>();

    /* package */ Car() {
        // For Hibernate
    }

    private Car(final Builder b) {
        this.id = b.id;
        this.owner = b.owner;
        this.plate = b.plate;
        this.type = b.type;
        this.powertrain = b.powertrain;
        this.transmission = b.transmission;
        this.status = b.status;
        this.description = b.description;
        this.createdAt = b.createdAt;
        this.updatedAt = b.updatedAt;
        this.ratingAvg = b.ratingAvg;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private long id;
        private User owner;
        private String plate;
        private Type type;
        private Powertrain powertrain;
        private Transmission transmission;
        private Status status;
        private String description;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        private BigDecimal ratingAvg;

        public Builder id(final long id) {
            this.id = id;
            return this;
        }

        public Builder owner(final User owner) {
            this.owner = owner;
            return this;
        }

        public Builder plate(final String plate) {
            this.plate = plate;
            return this;
        }

        public Builder type(final Type type) {
            this.type = type;
            return this;
        }

        public Builder powertrain(final Powertrain powertrain) {
            this.powertrain = powertrain;
            return this;
        }

        public Builder transmission(final Transmission transmission) {
            this.transmission = transmission;
            return this;
        }

        public Builder status(final Status status) {
            this.status = status;
            return this;
        }

        public Builder description(final String description) {
            this.description = description;
            return this;
        }

        public Builder createdAt(final OffsetDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(final OffsetDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder ratingAvg(final BigDecimal ratingAvg) {
            this.ratingAvg = ratingAvg;
            return this;
        }

        public Car build() {
            Objects.requireNonNull(owner, "owner");
            Objects.requireNonNull(plate, "plate");
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(powertrain, "powertrain");
            Objects.requireNonNull(transmission, "transmission");
            // Transitional defaults during Phase 1: legacy call sites (tests created before this refactor) do not
            // set status/timestamps. Phase 5 will rewire call sites and these defaults can be removed in favour
            // of strict Objects.requireNonNull checks.
            if (status == null) {
                status = Status.ACTIVE;
            }
            final OffsetDateTime now = OffsetDateTime.now();
            if (createdAt == null) {
                createdAt = now;
            }
            if (updatedAt == null) {
                updatedAt = createdAt;
            }
            return new Car(this);
        }
    }

    public long getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    /** Convenience accessor — returns {@code owner.getId()}. */
    public long getOwnerId() {
        return owner.getId();
    }

    public String getPlate() {
        return plate;
    }

    /**
     * Returns the brand name from the linked {@link CarModel}, or {@code null} when no catalog
     * model has been assigned yet (pre-backfill transition rows).
     */
    public String getBrand() {
        return carModel != null ? carModel.getBrand().getName() : null;
    }

    /**
     * Returns the model name from the linked {@link CarModel}, or {@code null} when no catalog
     * model has been assigned yet (pre-backfill transition rows).
     */
    public String getModel() {
        return carModel != null ? carModel.getName() : null;
    }

    public Type getType() {
        return type;
    }

    public Powertrain getPowertrain() {
        return powertrain;
    }

    public Transmission getTransmission() {
        return transmission;
    }

    public Status getStatus() {
        return status;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description).filter(s -> !s.isBlank());
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Optional<BigDecimal> getRatingAvg() {
        return Optional.ofNullable(ratingAvg);
    }

    public List<Listing> getListings() {
        return listings;
    }

    public List<CarPicture> getPictures() {
        return pictures;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setUpdatedAt(final OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setRatingAvg(final BigDecimal ratingAvg) {
        this.ratingAvg = ratingAvg;
    }

    public Optional<CarModel> getCarModel() {
        return Optional.ofNullable(carModel);
    }

    public void setCarModel(final CarModel carModel) {
        this.carModel = carModel;
    }

    @Override
    public String toString() {
        return "Car{" +
                "id=" + id +
                ", ownerId=" + owner.getId() +
                ", plate='" + plate + '\'' +
                ", brand='" + getBrand() + '\'' +
                ", model='" + getModel() + '\'' +
                ", type=" + type +
                ", powertrain=" + powertrain +
                ", transmission=" + transmission +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", ratingAvg=" + Objects.toString(ratingAvg) +
                '}';
    }
}
