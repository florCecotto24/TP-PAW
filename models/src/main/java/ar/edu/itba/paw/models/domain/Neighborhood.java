package ar.edu.itba.paw.models.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/** City neighborhood row for listing location pickers. */
@Entity
@Table(name = "neighborhoods")
public class Neighborhood {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "neighborhoods_id_seq")
    @SequenceGenerator(name = "neighborhoods_id_seq", sequenceName = "neighborhoods_id_seq", allocationSize = 1)
    private long id;

    @Column(nullable = false, length = 160)
    private String name;

    /* package */ Neighborhood() {
        // For Hibernate
    }

    public Neighborhood(final long id, final String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
