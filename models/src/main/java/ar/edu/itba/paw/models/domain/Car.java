package ar.edu.itba.paw.models.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
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

/** Vehicle inventory row: owner, plate, brand/model, and category enums for search filters. */
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

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cars_id_seq")
    @SequenceGenerator(name = "cars_id_seq", sequenceName = "cars_id_seq", allocationSize = 1)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 50)
    private String plate;

    @Column(nullable = false, length = 50)
    private String brand;

    @Column(name = "model", nullable = false, length = 50)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Powertrain powertrain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Transmission transmission;

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
        this.brand = b.brand;
        this.model = b.model;
        this.type = b.type;
        this.powertrain = b.powertrain;
        this.transmission = b.transmission;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private long id;
        private User owner;
        private String plate;
        private String brand;
        private String model;
        private Type type;
        private Powertrain powertrain;
        private Transmission transmission;

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

        public Builder brand(final String brand) {
            this.brand = brand;
            return this;
        }

        public Builder model(final String model) {
            this.model = model;
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

        public Car build() {
            Objects.requireNonNull(owner, "owner");
            Objects.requireNonNull(plate, "plate");
            Objects.requireNonNull(brand, "brand");
            Objects.requireNonNull(model, "model");
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(powertrain, "powertrain");
            Objects.requireNonNull(transmission, "transmission");
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

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
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

    public List<Listing> getListings() {
        return listings;
    }

    public List<CarPicture> getPictures() {
        return pictures;
    }

    @Override
    public String toString() {
        return "Car{" +
                "id=" + id +
                ", ownerId=" + owner.getId() +
                ", plate='" + plate + '\'' +
                ", brand='" + brand + '\'' +
                ", model='" + model + '\'' +
                ", type=" + type +
                ", powertrain=" + powertrain +
                ", transmission=" + transmission +
                '}';
    }
}
