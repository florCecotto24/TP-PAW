package ar.edu.itba.paw.webapp.security.auth.userdetails;

import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.security.UserRole;
import ar.edu.itba.paw.services.user.UserService;

/** Loads {@link RydenUserDetails} by email for Spring Security. */
public final class RydenUserDetailsService implements UserDetailsService {

    private final UserService userService;

    public RydenUserDetailsService(final UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(final String email) throws UsernameNotFoundException {
        final User user = userService.findByEmailForAuthentication(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user for email"));
        if (!Boolean.TRUE.equals(user.getEmailValidated().orElse(false))) {
            throw new UsernameNotFoundException("Email not validated");
        }
        // NOTE: blocked users are intentionally allowed to authenticate so that owners blocked by the
        // overdue-refund-proof sweep can still log in, see the dashboard banner, and upload the missing
        // refund receipts (which auto-unblocks them, see ReservationServiceImpl#unblockOwnerIfNoMore...).
        // Defense-in-depth: every owner-side mutation that could re-introduce bookability is guarded at
        // the service layer (publishCar, createCarAvailabilityPeriods, applyOwnerEditByCar, uploadValidatedCarInsuranceDocument)
        // and "GET /publish-car" redirects blocked owners away from the publish form.
        final String hash = user.getPasswordHash()
                .filter(h -> !h.isBlank())
                .orElseThrow(() -> new UsernameNotFoundException("User has no password"));
        final List<UserRole> roles = userService.findRolesForUser(user.getId());
        final List<GrantedAuthority> authorities = UserRoleAuthorities.fromUserRoles(roles);
        return new RydenUserDetails(
                user.getId(),
                user.getEmail(),
                user.getForename(),
                user.getSurname(),
                hash,
                authorities,
                user.getRoleAssignedBy().orElse(null));
    }
}
