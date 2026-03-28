package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.CarPicture;
import java.util.Optional;

public interface CarPictureDao {

    CarPicture createCarPicture(final long carId, final long imageId, final int order);

    Optional<CarPicture> getCarPictureById(final long id);
}
