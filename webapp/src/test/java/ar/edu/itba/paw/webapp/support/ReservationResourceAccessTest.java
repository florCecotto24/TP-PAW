package ar.edu.itba.paw.webapp.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.services.reservation.ReservationService;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

/**
 * Authz for reservation visibility: participant vs stranger vs admin.
 * Strangers get a clean deny (AccessDenied → 403), never a leaked presence signal from this gate.
 */
@ExtendWith(MockitoExtension.class)
class ReservationResourceAccessTest {

    @Mock
    private ReservationService reservationService;

    @Mock
    private Reservation reservation;

    private ReservationResourceAccess access;
    private RydenUserDetails rider;
    private RydenUserDetails owner;
    private RydenUserDetails stranger;

    @BeforeEach
    void setUp() {
        access = new ReservationResourceAccess(reservationService);
        rider = viewer(11L);
        owner = viewer(22L);
        stranger = viewer(99L);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testAnonymousCannotViewReservation() {
        // 1.Arrange / 2.Act / 3.Assert
        assertFalse(access.canViewReservation(5L, null));
        assertThrows(AccessDeniedException.class, () -> access.requireViewReservation(5L, null));
    }

    @Test
    void testStrangerCannotViewReservation() {
        // 1.Arrange
        when(reservationService.getRiderReservationById(99L, 5L)).thenReturn(Optional.empty());
        when(reservationService.getOwnerReservationById(99L, 5L)).thenReturn(Optional.empty());

        // 2.Act / 3.Assert
        assertFalse(access.canViewReservation(5L, stranger));
        assertThrows(AccessDeniedException.class, () -> access.requireViewReservation(5L, stranger));
    }

    @Test
    void testRiderCanViewReservation() {
        // 1.Arrange
        when(reservationService.getRiderReservationById(11L, 5L)).thenReturn(Optional.of(reservation));

        // 2.Act / 3.Assert
        assertTrue(access.canViewReservation(5L, rider));
        assertTrue(access.isRider(5L, rider));
    }

    @Test
    void testOwnerCanViewReservation() {
        // 1.Arrange
        when(reservationService.getRiderReservationById(22L, 5L)).thenReturn(Optional.empty());
        when(reservationService.getOwnerReservationById(22L, 5L)).thenReturn(Optional.of(reservation));

        // 2.Act / 3.Assert
        assertTrue(access.canViewReservation(5L, owner));
        assertTrue(access.isOwner(5L, owner));
    }

    @Test
    void testAdminCanViewAnyReservation() {
        // 1.Arrange
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(
                        "admin@example.com",
                        "x",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        // 2.Act / 3.Assert
        assertTrue(access.canViewReservation(5L, stranger));
    }

    private static RydenUserDetails viewer(final long userId) {
        return new RydenUserDetails(userId, "u@example.com", "A", "B", "hash", List.of(), null);
    }
}
