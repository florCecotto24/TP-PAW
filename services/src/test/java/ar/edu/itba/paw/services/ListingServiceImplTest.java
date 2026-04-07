package ar.edu.itba.paw.services;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.listing.ListingValidationException;
import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.ListingDetail;
import ar.edu.itba.paw.persistence.CarDao;
import ar.edu.itba.paw.persistence.ListingAvailabilityDao;
import ar.edu.itba.paw.persistence.ListingDao;
import ar.edu.itba.paw.persistence.ReservationDao;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class ListingServiceImplTest {

    @Mock
    private ListingDao listingDao;

    @Mock
    private ListingAvailabilityDao listingAvailabilityDao;

    @Mock
    private CarDao carDao;

    @Mock
    private ReservationDao reservationDao;

    @InjectMocks
    private ListingServiceImpl listingService;


    @Test
    public void testGetListingByIdWhenListingExists(){
        // 1. Arrange
        final Listing listing = new Listing(1L, "Test Listing", 1L, OffsetDateTime.now(), OffsetDateTime.now(), Listing.Status.ACTIVE, new BigDecimal("100.00"), "Test Start Point", "Test Description", LocalTime.of(10, 0), LocalTime.of(18, 0));
        Mockito.when(listingDao.getListingById(1L)).thenReturn(Optional.of(listing));

        // 2. Execute
        final Optional<Listing> result = listingService.getListingById(1L);

        // 3. Assert
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(listing, result.get());
        Assertions.assertEquals(listing.getId(), result.get().getId());
        Assertions.assertEquals(listing.getTitle(), result.get().getTitle());
        Assertions.assertEquals(listing.getCarId(), result.get().getCarId());
        Assertions.assertEquals(listing.getCreatedAt(), result.get().getCreatedAt());
        Assertions.assertEquals(listing.getUpdatedAt(), result.get().getUpdatedAt());
        Assertions.assertEquals(listing.getStatus(), result.get().getStatus());
        Assertions.assertEquals(listing.getDayPrice(), result.get().getDayPrice());
        Assertions.assertEquals(listing.getStartPoint(), result.get().getStartPoint());
    }

    @Test
    public void testGetListingByIdWhenListingDoesNotExist(){
        // 1. Arrange
        Mockito.when(listingDao.getListingById(Mockito.anyLong())).thenReturn(Optional.empty());

        // 2. Execute
        final Optional<Listing> result = listingService.getListingById(1L);

        // 3. Assert
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testGetListingDetailByIdWhenListingExists(){
        // 1. Arrange

        // 2. Execute

        // 3. Assert
    }

    @Test
    public void testGetListingDetailByIdWhenListingDoesNotExist(){

    }

    @Test
    public void testFindAvailabilityByListingIdWhenListingExists(){

    }

    @Test
    public void testFindAvailabilityByListingIdWhenListingDoesNotExist(){

    }


    @Test
    public void testGetBookableWallAvailabilityPeriodsWhenListingExists(){

    }

    @Test
    public void testGetBookableWallAvailabilityPeriodsWhenListingDoesNotExist(){

    }

    @Test
    public void testReservationIntervalFitsListingAvailabilityWhenListingExistsAndAvailabilityExistsAndIntervalFits(){

    }


    @Test
    public void testReservationIntervalFitsListingAvailabilityWhenListingExistsAndAvailabilityExistsAndIntervalDoesNotFit(){

    }

    @Test
    public void testReservationIntervalFitsListingAvailabilityWhenListingExistsAndAvailabilityDoesNotExist(){

    }

    @Test
    public void testReservationIntervalFitsListingAvailabilityWhenListingDoesNotExist(){

    }

    @Test
    public void testGetAllListingsWhenThereAreListings(){
    }

    @Test
    public void testGetAllListingsWhenThereAreNoListings(){
    }


}
