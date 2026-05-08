package ar.edu.itba.paw.persistence.hibernate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.CarPicture;
import ar.edu.itba.paw.persistence.CarPictureDao;

@Transactional
@Repository
public class CarPictureHibernateDao implements CarPictureDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public CarPicture createCarPicture(final long carId, final long imageId, final int displayOrder) {
        final OffsetDateTime now = OffsetDateTime.now();
        final CarPicture picture = new CarPicture(carId, imageId, displayOrder, now, now);
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
                        "FROM CarPicture cp WHERE cp.carId = :carId ORDER BY cp.displayOrder ASC",
                        CarPicture.class)
                .setParameter("carId", carId)
                .getResultList();
    }
}
