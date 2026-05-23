package ar.edu.itba.paw.models.domain;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Catalog row for vehicle brands. Brands seeded by the team or imported are marked {@code validated = true};
 * brands created on the fly through the publish-car flow ("Other") start as {@code validated = false} and
 * may be promoted by an admin later.
 */
@Entity
@Table(name = "car_brands")
public class CarBrand {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "car_brands_id_seq")
    @SequenceGenerator(name = "car_brands_id_seq", sequenceName = "car_brands_id_seq", allocationSize = 1)
    private long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private boolean validated;

    /* package */ CarBrand() {
        // For Hibernate
    }

    private CarBrand(final Builder b) {
        this.id = b.id;
        this.name = b.name;
        this.validated = b.validated;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private long id;
        private String name;
        private boolean validated;

        public Builder id(final long id) {
            this.id = id;
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

        public CarBrand build() {
            Objects.requireNonNull(name, "name");
            return new CarBrand(this);
        }
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isValidated() {
        return validated;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setValidated(final boolean validated) {
        this.validated = validated;
    }

    @Override
    public String toString() {
        return "CarBrand{id=" + id + ", name='" + name + "', validated=" + validated + '}';
    }
}
