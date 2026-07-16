package ar.edu.itba.paw.models.domain.car;

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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import ar.edu.itba.paw.models.domain.internal.EntityEquality;

/**
 * Catalog row for vehicle models. Each model belongs to a {@link CarBrand} and is locked to a body {@link Car.Type}.
 * Models seeded by the team or imported are marked {@code validated = true}; models created on the fly through the
 * publish-car flow ("Other") start as {@code validated = false}.
 */
@Entity
@Table(name = "car_models")
public class CarModel {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "car_models_id_seq")
    @SequenceGenerator(name = "car_models_id_seq", sequenceName = "car_models_id_seq", allocationSize = 1)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private CarBrand brand;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private boolean validated;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Car.Type type;

    /* package */ CarModel() {
        // For Hibernate
    }

    private CarModel(final Builder b) {
        this.id = b.id;
        this.brand = b.brand;
        this.name = b.name;
        this.validated = b.validated;
        this.type = b.type;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private long id;
        private CarBrand brand;
        private String name;
        private boolean validated;
        private Car.Type type;

        public Builder id(final long id) {
            this.id = id;
            return this;
        }

        public Builder brand(final CarBrand brand) {
            this.brand = brand;
            return this;
        }

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder validated(final boolean validated) {
            this.validated = validated;
            return this;
        }

        public Builder type(final Car.Type type) {
            this.type = type;
            return this;
        }

        public CarModel build() {
            Objects.requireNonNull(brand, "brand");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(type, "type");
            return new CarModel(this);
        }
    }

    public long getId() {
        return id;
    }

    public CarBrand getBrand() {
        return brand;
    }

    /** Convenience accessor — returns {@code brand.getId()}. */
    public long getBrandId() {
        return brand.getId();
    }

    public String getName() {
        return name;
    }

    public boolean isValidated() {
        return validated;
    }

    public Car.Type getType() {
        return type;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setValidated(final boolean validated) {
        this.validated = validated;
    }

    public void setType(final Car.Type type) {
        this.type = type;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CarModel)) {
            return false;
        }
        return EntityEquality.equalsByLongId(this, getId(), ((CarModel) o).getId());
    }

    @Override
    public int hashCode() {
        return EntityEquality.hashByLongId(this, id);
    }

    @Override
    public String toString() {
        return "CarModel{id=" + id
                + ", brandId=" + (brand == null ? null : brand.getId())
                + ", name='" + name + "', validated=" + validated
                + ", type=" + type + '}';
    }
}
