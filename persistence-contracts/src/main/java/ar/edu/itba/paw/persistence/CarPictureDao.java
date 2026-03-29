package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.CarPicture;
import java.util.List;
import java.util.Optional;

public interface CarPictureDao {

    CarPicture createCarPicture(final long carId, final long imageId, final int displayOrder);

    Optional<CarPicture> getCarPictureById(final long id);
    
    List<CarPicture> getCarPicturesByCarId(final long carId);
}
