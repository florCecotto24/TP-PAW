package ar.edu.itba.paw.webapp.security.jwt;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

final class JwtAuthorityCodec {

    private static final String SEPARATOR = ",";

    private JwtAuthorityCodec() {
    }

    static String encode(final RydenUserDetails principal) {
        return principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(SEPARATOR));
    }

    static java.util.Collection<SimpleGrantedAuthority> decode(final String email, final String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return java.util.List.of();
        }
        final java.util.List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
        for (final String authority : encoded.split(SEPARATOR)) {
            if (!authority.isBlank()) {
                authorities.add(new SimpleGrantedAuthority(authority.trim()));
            }
        }
        return authorities;
    }
}
