package ar.edu.itba.paw.models.domain;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

/** Spring Security role row (maps {@code user_roles}). */
@Entity
@Table(name = "user_roles")
public class UserRoleAssignment {

    @EmbeddedId
    private Pk id;

    /* package */ UserRoleAssignment() {
    }

    public UserRoleAssignment(final long userId, final String role) {
        this.id = new Pk(userId, role);
    }

    public Pk getId() {
        return id;
    }

    @Embeddable
    public static final class Pk implements Serializable {

        private static final long serialVersionUID = 1L;

        @Column(name = "user_id", nullable = false)
        private long userId;

        @Column(name = "role", nullable = false, length = 50)
        private String role;

        /* package */ Pk() {
        }

        public Pk(final long userId, final String role) {
            this.userId = userId;
            this.role = role;
        }

        public long getUserId() {
            return userId;
        }

        public String getRole() {
            return role;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final Pk pk = (Pk) o;
            return userId == pk.userId && Objects.equals(role, pk.role);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, role);
        }
    }
}
