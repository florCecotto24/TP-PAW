package ar.edu.itba.paw.services;

import ar.edu.itba.paw.persistence.CarPictureDao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CarPictureServiceImplTest {

    @Mock
    private CarPictureDao carPictureDao;

    @Mock
    private ImageService imageService;

    @InjectMocks
    private CarPictureServiceImpl carPictureService;

    @Test
    public void testCreateCarPictureWhenImageExists(){
        // 1. Arrange

        // 2. Execute

        // 3. Assert

    }

    @Test
    public void testCreateCarPictureWhenImageDoesNotExist(){
        // 1. Arrange

        // 2. Execute

        // 3. Assert

    }

    @Test
    public void testGetCarPictureByCarIdWhenCarExists() {
        // 1. Arrange

        // 2. Execute

        // 3. Assert

    }

    @Test
    public void testGetCarPictureByCarIdWhenCarDoesNotExist() {
        // 1. Arrange

        // 2. Execute

        // 3. Assert
    }


    @Test
    public void testGetCarPicturesByCarIdWhenCarExists() {
        // 1. Arrange

        // 2. Execute

        // 3. Assert
    }

    @Test
    public void testGetCarPicturesByCarIdWhenCarDoesNotExist() {        
        // 1. Arrange

        // 2. Execute

        // 3. Assert
    }
}
