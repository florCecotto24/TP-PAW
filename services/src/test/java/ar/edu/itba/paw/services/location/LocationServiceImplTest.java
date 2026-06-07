package ar.edu.itba.paw.services.location;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.models.domain.location.Neighborhood;
import ar.edu.itba.paw.persistence.location.LocationDao;

@ExtendWith(MockitoExtension.class)
public class LocationServiceImplTest {

    @Mock
    private LocationDao locationDao;

    @InjectMocks
    private LocationServiceImpl locationService;

    @Test
    public void testResolveSearchNeighborhoodIdBlank() {
        Assertions.assertTrue(locationService.resolveSearchNeighborhoodId(null).isEmpty());
        Assertions.assertTrue(locationService.resolveSearchNeighborhoodId("   ").isEmpty());
    }

    @Test
    public void testResolveSearchNeighborhoodIdNonPositive() {
        Assertions.assertTrue(locationService.resolveSearchNeighborhoodId("0").isEmpty());
        Assertions.assertTrue(locationService.resolveSearchNeighborhoodId("-3").isEmpty());
    }

    @Test
    public void testResolveSearchNeighborhoodIdNotANumber() {
        Assertions.assertTrue(locationService.resolveSearchNeighborhoodId("x").isEmpty());
    }

    @Test
    public void testResolveSearchNeighborhoodIdWhenCatalogHasRow() {
        final Neighborhood n = new Neighborhood(5L, "Palermo");
        Mockito.when(locationDao.findNeighborhoodById(5L)).thenReturn(Optional.of(n));

        final Optional<Long> result = locationService.resolveSearchNeighborhoodId(" 5 ");

        Assertions.assertEquals(Optional.of(5L), result);
    }

    @Test
    public void testResolveSearchNeighborhoodIdWhenUnknownId() {
        Mockito.when(locationDao.findNeighborhoodById(9L)).thenReturn(Optional.empty());

        Assertions.assertTrue(locationService.resolveSearchNeighborhoodId("9").isEmpty());
    }

    @Test
    public void testResolveSearchNeighborhoodIdsDedupesAndKeepsOrder() {
        final Neighborhood a = new Neighborhood(1L, "A");
        final Neighborhood b = new Neighborhood(2L, "B");
        Mockito.when(locationDao.findNeighborhoodById(1L)).thenReturn(Optional.of(a));
        Mockito.when(locationDao.findNeighborhoodById(2L)).thenReturn(Optional.of(b));

        final List<Long> result = locationService.resolveSearchNeighborhoodIds(List.of("2", "1", "2", "bad", "0"));

        Assertions.assertEquals(List.of(2L, 1L), result);
    }

    @Test
    public void testResolveSearchNeighborhoodIdsFromNullArray() {
        Assertions.assertTrue(locationService.resolveSearchNeighborhoodIds((String[]) null).isEmpty());
    }

    @Test
    public void testFindAllNeighborhoodsDelegates() {
        final List<Neighborhood> list = List.of(new Neighborhood(1L, "X"));
        Mockito.when(locationDao.findAllNeighborhoods()).thenReturn(list);

        Assertions.assertEquals(list, locationService.findAllNeighborhoods());
    }
}
