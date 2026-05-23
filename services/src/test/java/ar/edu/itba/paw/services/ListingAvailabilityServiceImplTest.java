package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.exception.listing.ListingValidationException;
import ar.edu.itba.paw.exception.reservation.ReservationConflictException;
import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.persistence.ListingAvailabilityDao;

@ExtendWith(MockitoExtension.class)
public class ListingAvailabilityServiceImplTest {

    @Mock
    private ListingAvailabilityDao listingAvailabilityDao;

    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private ListingAvailabilityServiceImpl listingAvailabilityService;

    @Test
    public void testCreateReturnsPersistedRow() {
        final long listingId = 40L;
        final LocalDate start = LocalDate.of(2026, 6, 1);
        final LocalDate end = LocalDate.of(2026, 6, 30);
        final OffsetDateTime createdAt = OffsetDateTime.parse("2026-05-01T10:00:00Z");
        final OffsetDateTime updatedAt = OffsetDateTime.parse("2026-05-01T10:05:00Z");
        final Listing listingRef = Mockito.mock(Listing.class);
        Mockito.when(listingRef.getId()).thenReturn(listingId);
        final ListingAvailability row = new ListingAvailability(900L, listingRef, start, end, createdAt, updatedAt);
        Mockito.when(listingAvailabilityDao.create(listingId, start, end, null)).thenReturn(row);

        final ListingAvailability result = listingAvailabilityService.create(listingId, start, end);

        Assertions.assertEquals(row, result);
        Assertions.assertEquals(900L, result.getId());
        Assertions.assertEquals(listingId, result.getListingId());
        Assertions.assertEquals(start, result.getStartInclusive());
        Assertions.assertEquals(end, result.getEndInclusive());
    }

    @Test
    public void testFindByListingIdReturnsListFromDao() {
        final long listingId = 7L;
        final OffsetDateTime t = OffsetDateTime.parse("2026-04-01T12:00:00Z");
        final Listing listingRef = Mockito.mock(Listing.class);
        final ListingAvailability a = new ListingAvailability(1L, listingRef, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10), t, t);
        final ListingAvailability b = new ListingAvailability(2L, listingRef, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 15), t, t);
        Mockito.when(listingAvailabilityDao.findByListingId(listingId)).thenReturn(List.of(a, b));

        final List<ListingAvailability> result = listingAvailabilityService.findByListingId(listingId);

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(a, result.get(0));
        Assertions.assertEquals(b, result.get(1));
    }

    @Test
    public void testFindByListingIdsEndingOnOrAfterReturnsListFromDao() {
        final List<Long> ids = List.of(10L, 20L);
        final LocalDate minEnd = LocalDate.of(2026, 7, 1);
        final OffsetDateTime t = OffsetDateTime.parse("2026-05-02T08:00:00Z");
        final ListingAvailability row =
                new ListingAvailability(3L, Mockito.mock(Listing.class), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 1), t, t);
        Mockito.when(listingAvailabilityDao.findByListingIdsEndingOnOrAfter(ids, minEnd)).thenReturn(List.of(row));

        final List<ListingAvailability> result =
                listingAvailabilityService.findByListingIdsEndingOnOrAfter(ids, minEnd);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(row, result.get(0));
    }

    @Test
    public void testDeleteByListingIdDoesNotThrow() {
        final long listingId = 99L;
        Assertions.assertDoesNotThrow(() -> listingAvailabilityService.deleteByListingId(listingId));
    }

    // --- Phase 2 service contract tests below ---

    private static final long LISTING_ID = 42L;
    private static final BigDecimal PRICE = new BigDecimal("100.00");
    private static final LocalTime CHECK_IN = LocalTime.of(10, 0);
    private static final LocalTime CHECK_OUT = LocalTime.of(18, 0);

    @Test
    public void testApplyOwnerEditThrowsConflictWhenBlockingReservationOverlapsRemovedDay() {
        // 1. Arrange — old [1/8, 10/8] -> new [2/8, 9/8]; reservation on day 10/8 overlaps a withdrawn chunk.
        final LocalDate oldS = LocalDate.of(2026, 8, 1);
        final LocalDate oldE = LocalDate.of(2026, 8, 10);
        final LocalDate newS = LocalDate.of(2026, 8, 2);
        final LocalDate newE = LocalDate.of(2026, 8, 9);
        final Reservation blocking = Mockito.mock(Reservation.class);
        Mockito.when(blocking.getStartDate())
                .thenReturn(LocalDate.of(2026, 8, 10).atStartOfDay(AvailabilityPeriod.WALL_ZONE).toOffsetDateTime());
        Mockito.when(blocking.getEndDate())
                .thenReturn(LocalDate.of(2026, 8, 11).atStartOfDay(AvailabilityPeriod.WALL_ZONE).toOffsetDateTime());
        Mockito.when(reservationService.findBlockingReservationsByListingIdInRange(
                        Mockito.eq(LISTING_ID), Mockito.any(), Mockito.any()))
                .thenReturn(List.of(blocking));

        // 2. Act + Assert
        Assertions.assertThrows(ReservationConflictException.class, () ->
                listingAvailabilityService.applyOwnerEdit(
                        LISTING_ID, oldS, oldE, newS, newE, PRICE,
                        "Street", null, null, CHECK_IN, CHECK_OUT));
    }

    @Test
    public void testApplyOwnerEditDoesNotThrowWhenNoBlockingReservation() {
        // 1. Arrange — old [1/8, 10/8] -> new [2/8, 9/8]; no blocking reservations.
        Mockito.when(reservationService.findBlockingReservationsByListingIdInRange(
                        Mockito.eq(LISTING_ID), Mockito.any(), Mockito.any()))
                .thenReturn(Collections.emptyList());

        // 2. Act + Assert — internal createFull(...) calls return null mocks; service should not throw.
        Assertions.assertDoesNotThrow(() -> listingAvailabilityService.applyOwnerEdit(
                LISTING_ID,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 9),
                PRICE, "Street", null, null, CHECK_IN, CHECK_OUT));
    }

    @Test
    public void testApplyOwnerWithdrawAvailabilityThrowsValidationWhenTargetNotFound() {
        // 1. Arrange
        Mockito.when(listingAvailabilityDao.findById(555L)).thenReturn(Optional.empty());

        // 2. Act + Assert
        Assertions.assertThrows(ListingValidationException.class, () ->
                listingAvailabilityService.applyOwnerWithdrawAvailability(LISTING_ID, 555L));
    }

    @Test
    public void testApplyOwnerWithdrawAvailabilityThrowsValidationWhenTargetBelongsToAnotherListing() {
        // 1. Arrange — target listing.id is 999, not LISTING_ID.
        final Listing otherListing = Mockito.mock(Listing.class);
        Mockito.when(otherListing.getId()).thenReturn(999L);
        final ListingAvailability target = ListingAvailability.builder()
                .id(555L).listing(otherListing)
                .startInclusive(LocalDate.of(2026, 8, 1)).endInclusive(LocalDate.of(2026, 8, 10))
                .kind(ListingAvailability.Kind.OFFERED).build();
        Mockito.when(listingAvailabilityDao.findById(555L)).thenReturn(Optional.of(target));

        // 2. Act + Assert
        Assertions.assertThrows(ListingValidationException.class, () ->
                listingAvailabilityService.applyOwnerWithdrawAvailability(LISTING_ID, 555L));
    }

    @Test
    public void testApplyOwnerWithdrawAvailabilityThrowsValidationWhenTargetAlreadyWithdrawn() {
        // 1. Arrange
        final Listing listingRef = Mockito.mock(Listing.class);
        Mockito.when(listingRef.getId()).thenReturn(LISTING_ID);
        final ListingAvailability target = ListingAvailability.builder()
                .id(555L).listing(listingRef)
                .startInclusive(LocalDate.of(2026, 8, 1)).endInclusive(LocalDate.of(2026, 8, 10))
                .kind(ListingAvailability.Kind.WITHDRAWN).build();
        Mockito.when(listingAvailabilityDao.findById(555L)).thenReturn(Optional.of(target));

        // 2. Act + Assert
        Assertions.assertThrows(ListingValidationException.class, () ->
                listingAvailabilityService.applyOwnerWithdrawAvailability(LISTING_ID, 555L));
    }

    @Test
    public void testApplyOwnerWithdrawAvailabilityThrowsConflictWhenBlockingReservationOverlaps() {
        // 1. Arrange — target is OFFERED [1/8, 10/8]; reservation on day 5/8.
        final Listing listingRef = Mockito.mock(Listing.class);
        Mockito.when(listingRef.getId()).thenReturn(LISTING_ID);
        final ListingAvailability target = ListingAvailability.builder()
                .id(555L).listing(listingRef)
                .startInclusive(LocalDate.of(2026, 8, 1)).endInclusive(LocalDate.of(2026, 8, 10))
                .dayPrice(PRICE).startPointStreet("Street")
                .checkInTime(CHECK_IN).checkOutTime(CHECK_OUT)
                .kind(ListingAvailability.Kind.OFFERED).build();
        Mockito.when(listingAvailabilityDao.findById(555L)).thenReturn(Optional.of(target));
        final Reservation blocking = Mockito.mock(Reservation.class);
        Mockito.when(blocking.getStartDate())
                .thenReturn(LocalDate.of(2026, 8, 5).atStartOfDay(AvailabilityPeriod.WALL_ZONE).toOffsetDateTime());
        Mockito.when(blocking.getEndDate())
                .thenReturn(LocalDate.of(2026, 8, 6).atStartOfDay(AvailabilityPeriod.WALL_ZONE).toOffsetDateTime());
        Mockito.when(reservationService.findBlockingReservationsByListingIdInRange(
                        Mockito.eq(LISTING_ID), Mockito.any(), Mockito.any()))
                .thenReturn(List.of(blocking));

        // 2. Act + Assert
        Assertions.assertThrows(ReservationConflictException.class, () ->
                listingAvailabilityService.applyOwnerWithdrawAvailability(LISTING_ID, 555L));
    }

    @Test
    public void testFindEffectiveForDayDelegatesToDao() {
        final long listingId = 12L;
        final LocalDate day = LocalDate.of(2026, 8, 5);
        final OffsetDateTime t = OffsetDateTime.parse("2026-07-01T10:00:00Z");
        final ListingAvailability winner = new ListingAvailability(
                77L, Mockito.mock(Listing.class), day.minusDays(2), day.plusDays(2), t, t);
        Mockito.when(listingAvailabilityDao.findEffectiveForDay(listingId, day)).thenReturn(Optional.of(winner));

        final Optional<ListingAvailability> result = listingAvailabilityService.findEffectiveForDay(listingId, day);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertSame(winner, result.get());
    }

    @Test
    public void testFindOverlappingRangeDelegatesToDao() {
        final long listingId = 12L;
        final LocalDate from = LocalDate.of(2026, 8, 1);
        final LocalDate to = LocalDate.of(2026, 8, 31);
        final OffsetDateTime t = OffsetDateTime.parse("2026-07-01T10:00:00Z");
        final ListingAvailability row = new ListingAvailability(
                88L, Mockito.mock(Listing.class), from, to, t, t);
        Mockito.when(listingAvailabilityDao.findOverlappingRange(listingId, from, to)).thenReturn(List.of(row));

        final List<ListingAvailability> result = listingAvailabilityService.findOverlappingRange(listingId, from, to);

        Assertions.assertEquals(1, result.size());
        Assertions.assertSame(row, result.get(0));
    }
}
