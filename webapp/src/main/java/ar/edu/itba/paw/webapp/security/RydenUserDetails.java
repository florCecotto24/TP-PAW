package ar.edu.itba.paw.webapp.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class RydenUserDetails implements UserDetails {

    private final long userId;
    private final String email;
    private final String forename;
    private final String surname;
    private final String encodedPassword;
    private final Collection<? extends GrantedAuthority> authorities;

    public RydenUserDetails(
            final long userId,
            final String email,
            final String forename,
            final String surname,
            final String encodedPassword,
            final Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.email = email;
        this.forename = forename;
        this.surname = surname;
        this.encodedPassword = encodedPassword;
        this.authorities = authorities;
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
}
