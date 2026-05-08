package ar.edu.itba.paw.models.domain;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
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
        GASOLINE, DIESEL, ELECTRIC, HYBRID
    }

    public enum Transmission implements PrettyNamed {
        MANUAL, AUTOMATIC, SEMI_AUTOMATIC;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cars_id_seq")
    @SequenceGenerator(name = "cars_id_seq", sequenceName = "cars_id_seq", allocationSize = 1)
    private long id;

    @Column(name = "owner_id", nullable = false)
    private long ownerId;

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

    /* package */ Car() {
        // For Hibernate
    }

    private Car(final Builder b) {
        this.id = b.id;
        this.ownerId = b.ownerId;
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
        private long ownerId;
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

        public Builder ownerId(final long ownerId) {
            this.ownerId = ownerId;
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

    public long getOwnerId() {
        return ownerId;
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

    @Override
    public String toString() {
        return "Car{" +
                "id=" + id +
                ", ownerId=" + ownerId +
                ", plate='" + plate + '\'' +
                ", brand='" + brand + '\'' +
                ", model='" + model + '\'' +
                ", type=" + type +
                ", powertrain=" + powertrain +
                ", transmission=" + transmission +
                '}';
    }
}
