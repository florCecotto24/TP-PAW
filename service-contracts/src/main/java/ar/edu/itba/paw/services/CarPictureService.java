package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.CarPicture;

import java.util.Optional;

public interface CarPictureService {
    CarPicture createCarPicture(final long carId, final long imageId, final int order);

    Optional<CarPicture> getCarPictureById(final long id);
}
