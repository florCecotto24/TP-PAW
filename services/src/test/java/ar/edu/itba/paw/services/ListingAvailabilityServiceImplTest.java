package ar.edu.itba.paw.services;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.persistence.ListingAvailabilityDao;

@ExtendWith(MockitoExtension.class)
public class ListingAvailabilityServiceImplTest {

    @Mock
    private ListingAvailabilityDao listingAvailabilityDao;

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
}
