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

import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.persistence.StoredFileDao;

@ExtendWith(MockitoExtension.class)
public class StoredFileServiceImplTest {

    @Mock
    private StoredFileDao storedFileDao;

    @InjectMocks
    private StoredFileServiceImpl storedFileService;

    @Test
    public void testCreateReturnsDaoRow() {
        final long uploaderId = 2L;
        final String name = "receipt.pdf";
        final String contentType = "application/pdf";
        final byte[] data = {1, 2, 3};
        final OffsetDateTime createdAt = OffsetDateTime.parse("2026-03-01T12:00:00Z");
        final StoredFile file = new StoredFile(99L, User.identities(uploaderId, "u@test.com", "U", "U"), name, contentType, data, createdAt);
        Mockito.when(storedFileDao.create(uploaderId, name, contentType, data)).thenReturn(file);

        final StoredFile result = storedFileService.create(uploaderId, name, contentType, data);

        Assertions.assertEquals(file, result);
        Assertions.assertEquals(99L, result.getId());
    }

    @Test
    public void testFindByIdDelegates() {
        final long id = 40L;
        final OffsetDateTime createdAt = OffsetDateTime.parse("2026-03-02T08:00:00Z");
        final StoredFile file = new StoredFile(id, User.identities(1L, "u@test.com", "U", "U"), "a.bin", "application/octet-stream", new byte[0], createdAt);
        Mockito.when(storedFileDao.findById(id)).thenReturn(Optional.of(file));

        final Optional<StoredFile> result = storedFileService.findById(id);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(file, result.get());
    }

    @Test
    public void testFindByIdWhenMissing() {
        Mockito.when(storedFileDao.findById(7L)).thenReturn(Optional.empty());

        final Optional<StoredFile> result = storedFileService.findById(7L);

        Assertions.assertTrue(result.isEmpty());
    }
}
