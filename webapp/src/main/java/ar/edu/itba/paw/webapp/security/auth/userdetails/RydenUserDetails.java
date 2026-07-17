package ar.edu.itba.paw.webapp.security.auth.userdetails;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** {@link UserDetails} view of a domain user (id, names, encoded password, authorities). */
public final class RydenUserDetails implements UserDetails {

    private final long userId;
    private final String email;
    private final String forename;
    private final String surname;
    private final String encodedPassword;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Long roleAssignedBy;
    private final int passwordVersion;

    public RydenUserDetails(
            final long userId,
            final String email,
            final String forename,
            final String surname,
            final String encodedPassword,
            final Collection<? extends GrantedAuthority> authorities,
            final Long roleAssignedBy,
            final int passwordVersion) {
        this.userId = userId;
        this.email = email;
        this.forename = forename;
        this.surname = surname;
        this.encodedPassword = encodedPassword;
        this.authorities = authorities;
        this.roleAssignedBy = roleAssignedBy;
        this.passwordVersion = passwordVersion;
    }

    /**
     * Builder for {@link RydenUserDetails}. Added to align with project-wide use of the Builder pattern
     * for multi-argument value objects (does not replace the existing constructor; both are supported).
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private long userId;
        private String email;
        private String forename;
        private String surname;
        private String encodedPassword;
        private Collection<? extends GrantedAuthority> authorities;
        private Long roleAssignedBy;
        private int passwordVersion;

        public Builder userId(final long userId) {
            this.userId = userId;
            return this;
        }

        public Builder email(final String email) {
            this.email = email;
            return this;
        }

        public Builder forename(final String forename) {
            this.forename = forename;
            return this;
        }

        public Builder surname(final String surname) {
            this.surname = surname;
            return this;
        }

        public Builder encodedPassword(final String encodedPassword) {
            this.encodedPassword = encodedPassword;
            return this;
        }

        public Builder authorities(final Collection<? extends GrantedAuthority> authorities) {
            this.authorities = authorities;
            return this;
        }

        public Builder roleAssignedBy(final Long roleAssignedBy) {
            this.roleAssignedBy = roleAssignedBy;
            return this;
        }

        public Builder passwordVersion(final int passwordVersion) {
            this.passwordVersion = passwordVersion;
            return this;
        }

        public RydenUserDetails build() {
            Objects.requireNonNull(email, "email");
            Objects.requireNonNull(authorities, "authorities");
            return new RydenUserDetails(
                    userId, email, forename, surname, encodedPassword, authorities, roleAssignedBy, passwordVersion);
        }
    }

    public long getUserId() {
        return userId;
    }

    public String getForename() {
        return forename;
    }

    public String getSurname() {
        return surname;
    }

    public int getPasswordVersion() {
        return passwordVersion;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return encodedPassword;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public Optional<Long> getRoleAssignedBy() {
        return Optional.ofNullable(roleAssignedBy);
    }
}
