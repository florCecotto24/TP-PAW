package ar.edu.itba.paw.services.reservation;


import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.domain.user.User;

/**
 * Sanity coverage for {@link ReservationQueryServiceImpl}: focuses on small pure-logic
 * methods (id lookups, reminder-window passthrough) that don't exercise the larger query
 * paths covered by the DAO integration tests.
 *
 * <p>Architectural rule: this service no longer touches {@code ReservationDao}; tests mock
 * {@link ReservationService} (the sole DAO owner) instead.</p>
 */
@ExtendWith(MockitoExtension.class)
class ReservationQueryServiceImplTest {

    private static final OffsetDateTime START = OffsetDateTime.parse("2026-06-01T10:00:00Z");
    private static final OffsetDateTime END = OffsetDateTime.parse("2026-06-05T18:00:00Z");
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-05-01T12:00:00Z");
    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.parse("2026-05-01T12:00:00Z");
    private static final BigDecimal TOTAL_PRICE = new BigDecimal("200");

    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private ReservationQueryServiceImpl queryService;

    @Test
    void testGetReservationByIdWhenReservationExists() {
        final Reservation reservation = Reservation.builder()
                .id(1L)
                .rider(User.identities(1L, "r@test.com", "R", "Rider"))
                .car(Mockito.mock(Car.class))
                .startDate(START)
                .endDate(END)
                .status(Reservation.Status.ACCEPTED)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .totalPrice(TOTAL_PRICE)
                .build();
        Mockito.when(reservationService.getReservationById(1L)).thenReturn(Optional.of(reservation));

        final Optional<Reservation> result = queryService.getReservationById(1L);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(reservation, result.get());
        Assertions.assertEquals(1L, result.get().getId());
        Assertions.assertEquals(Reservation.Status.ACCEPTED, result.get().getStatus());
    }

    @Test
    void testGetReservationByIdWhenReservationDoesNotExist() {
        Mockito.when(reservationService.getReservationById(1L)).thenReturn(Optional.empty());

        final Optional<Reservation> result = queryService.getReservationById(1L);

        Assertions.assertFalse(result.isPresent());
    }

    @Test
    void testFindReminderReservationsReturnsDaoListForSameWindow() {
        final OffsetDateTime from = OffsetDateTime.parse("2026-06-02T03:00:00Z");
        final OffsetDateTime to = OffsetDateTime.parse("2026-06-03T03:00:00Z");
        final List<Reservation> expected = List.of(
                Reservation.builder()
                        .id(7L)
                        .rider(User.identities(1L, "r@test.com", "R", "Rider"))
                        .car(Mockito.mock(Car.class))
                        .startDate(from)
                        .endDate(END)
                        .status(Reservation.Status.ACCEPTED)
                        .createdAt(CREATED_AT)
                        .updatedAt(UPDATED_AT)
                        .totalPrice(TOTAL_PRICE)
                        .build());
        Mockito.when(reservationService.findReminderReservations(from, to)).thenReturn(expected);

        final List<Reservation> result = queryService.findReminderReservations(from, to);

        Assertions.assertEquals(expected, result);
    }

    @Test
    void testGetRiderReservationByIdReturnsEmptyWhenServiceReturnsEmpty() {
        // The rider-scope filter now lives in {@link ReservationServiceImpl#getRiderReservationById};
        // {@link ReservationQueryServiceImpl} is a pass-through, so this test just asserts the
        // delegation: when the facade returns empty (rider mismatch or unknown id), so does the
        // query service.
        final long riderId = 42L;
        final long reservationId = 7L;
        Mockito.when(reservationService.getRiderReservationById(riderId, reservationId))
                .thenReturn(Optional.empty());

        Assertions.assertTrue(queryService.getRiderReservationById(riderId, reservationId).isEmpty());
    }
}
