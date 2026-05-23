package ar.edu.itba.paw.persistence.hibernate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.CarPicture;
import ar.edu.itba.paw.models.domain.Image;
import ar.edu.itba.paw.persistence.CarPictureDao;

@Transactional(readOnly = true)
@Repository
public class CarPictureJpaDao implements CarPictureDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public CarPicture createCarPicture(final long carId, final long imageId, final int displayOrder) {
        final Car carRef = em.getReference(Car.class, carId);
        final Image imageRef = em.getReference(Image.class, imageId);
        final OffsetDateTime now = OffsetDateTime.now();
        final CarPicture picture = new CarPicture(carRef, imageRef, displayOrder, now, now);
        em.persist(picture);
        return picture;
    }

    @Override
    public Optional<CarPicture> getCarPictureById(final long id) {
        return Optional.ofNullable(em.find(CarPicture.class, id));
    }

    @Override
    public List<CarPicture> getCarPicturesByCarId(final long carId) {
        return em.createQuery(
                        "FROM CarPicture cp WHERE cp.car.id = :carId ORDER BY cp.displayOrder ASC",
                        CarPicture.class)
                .setParameter("carId", carId)
                .getResultList();
    }
}
