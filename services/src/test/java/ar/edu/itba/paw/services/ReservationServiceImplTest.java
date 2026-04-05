package ar.edu.itba.paw.services;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.reservation.ReservationConflictException;
import ar.edu.itba.paw.models.Reservation;
import ar.edu.itba.paw.persistence.ReservationDao;

@ExtendWith(MockitoExtension.class)
public class ReservationServiceImplTest {

    private static final OffsetDateTime START = OffsetDateTime.parse("2026-06-01T10:00:00Z");
    private static final OffsetDateTime END = OffsetDateTime.parse("2026-06-05T18:00:00Z");
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-05-01T12:00:00Z");
    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.parse("2026-05-01T12:00:00Z");

    @Mock
    private ReservationDao reservationDao;

    @Mock
    private ListingService listingService;

    @Mock
    private UserService userService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ReservationServiceImpl reservationService;



    @Test
    public void testCreateReservationWhenNoOverlapReturnsCreatedReservation() {
        // 1. Arrange
        final long riderId = 1L;
        final long listingId = 2L;
        final Reservation created = new Reservation(
                10L, riderId, listingId, START, END, Reservation.Status.ACCEPTED, CREATED_AT, UPDATED_AT);

        Mockito.when(reservationDao.hasActiveOverlap(listingId, START, END)).thenReturn(false);
        Mockito.when(reservationDao.createReservation(
                riderId, listingId, START, END, Reservation.Status.ACCEPTED)).thenReturn(created);

        // 2. Execute
        final Reservation result = reservationService.createReservation(
                riderId, listingId, START, END, Reservation.Status.ACCEPTED);

        // 3. Assert
        Assertions.assertEquals(10L, result.getId());
        Assertions.assertEquals(riderId, result.getRiderId());
        Assertions.assertEquals(listingId, result.getListingId());
        Assertions.assertEquals(START, result.getStartDate());
        Assertions.assertEquals(END, result.getEndDate());
        Assertions.assertEquals(Reservation.Status.ACCEPTED, result.getStatus());
    }

    @Test
    public void testCreateReservationWhenOverlapThrowsReservationConflictException() {
        // 1. Arrange
        Mockito.when(reservationDao.hasActiveOverlap(2L, START, END)).thenReturn(true);

        // 2. Execute
        final ReservationConflictException thrown = Assertions.assertThrows(ReservationConflictException.class,
                () -> reservationService.createReservation(1L, 2L, START, END, Reservation.Status.ACCEPTED));

        // 3. Assert
        Assertions.assertEquals(MessageKeys.RESERVATION_CONFLICT_OVERLAP, thrown.getMessageCode());
    }

    @Test
    public void testGetReservationByIdWhenReservationExists() {
        // 1. Arrange
        final Reservation reservation = new Reservation(
                1L, 1L, 1L, START, END, Reservation.Status.ACCEPTED, CREATED_AT, UPDATED_AT);
        Mockito.when(reservationDao.getReservationById(1L)).thenReturn(Optional.of(reservation));

        // 2. Execute
        final Optional<Reservation> result = reservationService.getReservationById(1L);

        // 3. Assert
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(reservation, result.get());
        Assertions.assertEquals(1L, result.get().getId());
        Assertions.assertEquals(Reservation.Status.ACCEPTED, result.get().getStatus());
    }

    @Test
    public void testGetReservationByIdWhenReservationDoesNotExist() {
        // 1. Arrange
        Mockito.when(reservationDao.getReservationById(Mockito.anyLong())).thenReturn(Optional.empty());

        // 2. Execute
        final Optional<Reservation> result = reservationService.getReservationById(1L);

        // 3. Assert
        Assertions.assertFalse(result.isPresent());
    }
}
