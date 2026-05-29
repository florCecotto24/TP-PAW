package ar.edu.itba.paw.services;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.exception.admin.AdminCannotBlockGrantorException;
import ar.edu.itba.paw.exception.admin.AdminCannotBlockSelfException;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.CarBrand;
import ar.edu.itba.paw.models.domain.CarModel;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.ReservationMessage;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.ReservationCard;
import ar.edu.itba.paw.models.email.ListingPausedByAdminOwnerEmailPayload;
import ar.edu.itba.paw.models.email.ListingRejectedByAdminOwnerEmailPayload;
import ar.edu.itba.paw.models.email.MigratedUserPasswordEmailPayload;
import ar.edu.itba.paw.models.security.UserRole;
import ar.edu.itba.paw.persistence.CarBrandDao;
import ar.edu.itba.paw.persistence.CarDao;
import ar.edu.itba.paw.persistence.CarModelDao;
import ar.edu.itba.paw.persistence.ReservationDao;
import ar.edu.itba.paw.persistence.ReservationMessageDao;
import ar.edu.itba.paw.persistence.UserDao;

/** Admin operations: user management, car moderation, catalog validation, and reservation inspection. */
@Service
@Transactional
public class AdminServiceImpl implements AdminService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final UserDao userDao;
    private final CarDao carDao;
    private final CarBrandDao carBrandDao;
    private final CarModelDao carModelDao;
    private final ReservationDao reservationDao;
    private final ReservationMessageDao reservationMessageDao;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final ReservationService reservationService;

    @Autowired
    public AdminServiceImpl(
            final UserDao userDao,
            final CarDao carDao,
            final CarBrandDao carBrandDao,
            final CarModelDao carModelDao,
            final ReservationDao reservationDao,
            final ReservationMessageDao reservationMessageDao,
            final EmailService emailService,
            final PasswordEncoder passwordEncoder,
            final ReservationService reservationService) {
        this.userDao = userDao;
        this.carDao = carDao;
        this.carBrandDao = carBrandDao;
        this.carModelDao = carModelDao;
        this.reservationDao = reservationDao;
        this.reservationMessageDao = reservationMessageDao;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.reservationService = reservationService;
    }

    @Override
    public void promoteUserToAdmin(final long targetUserId, final long assignedByUserId) {
        userDao.promoteToAdmin(targetUserId, assignedByUserId);
    }

    @Override
    public User createAdminUser(
            final String email,
            final String forename,
            final String surname,
            final String temporaryPassword,
            final long assignedByUserId,
            final Locale locale) {
        final User newUser = userDao.createUser(
                email, forename, surname, passwordEncoder.encode(temporaryPassword));
        userDao.promoteToAdmin(newUser.getId(), assignedByUserId);
        userDao.updateEmailValidated(newUser.getId(), true);
        emailService.sendMigratedUserPassword(
                MigratedUserPasswordEmailPayload.builder()
                        .messageLocale(locale)
                        .recipientEmail(email)
                        .plainPassword(temporaryPassword)
                        .build());
        return newUser;
    }

    @Override
    public void blockUser(final long targetUserId, final long actingAdminId) {
        if (actingAdminId == targetUserId) {
            throw new AdminCannotBlockSelfException();
        }
        final User target = userDao.getUserById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + targetUserId));
        target.getRoleAssignedBy().ifPresent(grantorId -> {
            if (grantorId == actingAdminId) {
                throw new AdminCannotBlockGrantorException();
            }
        });
        userDao.blockUser(targetUserId);
    }

    @Override
    public void unblockUser(final long targetUserId) {
        userDao.unblockUser(targetUserId);
    }

    @Override
    public void adminPauseCar(final long carId, final long actingAdminId, final Locale locale) {
        final Car car = carDao.getCarById(carId)
                .orElseThrow(() -> new IllegalArgumentException("Car not found: " + carId));
        if (car.getOwner().isAdmin()) {
            throw new IllegalArgumentException("Cannot admin-pause a car owned by another admin: " + carId);
        }
        car.setStatus(Car.Status.ADMIN_PAUSED);
        final List<Reservation> blocking = reservationService.findBlockingReservationsByCarId(carId);
        for (final Reservation r : blocking) {
            if (r.getStatus() == Reservation.Status.PENDING || r.getStatus() == Reservation.Status.ACCEPTED) {
                reservationService.cancelReservation(r.getId());
            }
        }
        LOGGER.info("Admin {} paused car {}", actingAdminId, carId);
        final User owner = car.getOwner();
        final String vehicleLabel = (car.getBrand() != null ? car.getBrand() : "")
                + (car.getBrand() != null && car.getModel() != null ? " " : "")
                + (car.getModel() != null ? car.getModel() : "");
        emailService.sendListingPausedByAdmin(
                ListingPausedByAdminOwnerEmailPayload.builder()
                        .messageLocale(locale)
                        .ownerEmail(owner.getEmail())
                        .ownerFullName(owner.getForename() + " " + owner.getSurname())
                        .vehicleLabel(vehicleLabel)
                        .carId(carId)
                        .build());
    }

    @Override
    public void adminResumeCar(final long carId, final long actingAdminId) {
        final Car car = carDao.getCarById(carId)
                .orElseThrow(() -> new IllegalArgumentException("Car not found: " + carId));
        if (car.getStatus() != Car.Status.ADMIN_PAUSED) {
            throw new IllegalStateException("Car is not admin-paused: " + carId);
        }
        car.setStatus(Car.Status.ACTIVE);
        LOGGER.info("Admin {} resumed car {}", actingAdminId, carId);
    }

    @Override
    public void validateCarBrand(final long brandId) {
        final CarBrand brand = carBrandDao.findById(brandId)
                .orElseThrow(() -> new IllegalArgumentException("Brand not found: " + brandId));
        brand.setValidated(true);
    }

    @Override
    public void rejectCarBrand(final long brandId) {
        carBrandDao.findById(brandId).ifPresent(brand -> {
            LOGGER.warn("Admin rejecting brand id={} name='{}'; cars referencing this brand/model become orphaned",
                    brandId, brand.getName());
            carBrandDao.deleteById(brandId);
        });
    }

    @Override
    public void validateCarModel(final long modelId) {
        final CarModel model = carModelDao.findById(modelId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));
        model.setValidated(true);
    }

    @Override
    public void rejectCarModel(final long modelId) {
        carModelDao.deleteById(modelId);
    }

    @Override
    public void validateCatalogEntry(final long modelId) {
        final CarModel model = carModelDao.findById(modelId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));
        if (!model.getBrand().isValidated()) {
            model.getBrand().setValidated(true);
        }
        model.setValidated(true);
    }

    @Override
    public void rejectCatalogEntry(final long modelId, final Locale locale) {
        final CarModel model = carModelDao.findById(modelId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));
        final CarBrand brand = model.getBrand();
        final boolean brandIsPending = !brand.isValidated();
        final boolean isLastModelForBrand = carModelDao.countByBrandId(brand.getId()) == 1;
        final boolean brandWillBeRejected = brandIsPending && isLastModelForBrand;
        final String brandName = brand.getName();
        final String modelName = model.getName();
        final List<Car> affectedCars = carDao.findCarsByModelId(modelId);
        final List<ListingRejectedByAdminOwnerEmailPayload> notifications = new java.util.ArrayList<>(affectedCars.size());
        for (final Car car : affectedCars) {
            final User owner = car.getOwner();
            final String vehicleLabel = brandName + " " + modelName;
            notifications.add(
                    ListingRejectedByAdminOwnerEmailPayload.builder()
                            .messageLocale(locale != null ? locale : Locale.ENGLISH)
                            .ownerEmail(owner.getEmail())
                            .ownerFullName(owner.getForename() + " " + owner.getSurname())
                            .vehicleLabel(vehicleLabel)
                            .carId(car.getId())
                            .brandName(brandName)
                            .modelName(modelName)
                            .brandRejected(brandWillBeRejected)
                            .modelRejected(true)
                            .build());
            car.setCarModel(null);
        }
        carModelDao.deleteById(modelId);
        if (brandWillBeRejected) {
            carBrandDao.deleteById(brand.getId());
        }
        for (final ListingRejectedByAdminOwnerEmailPayload payload : notifications) {
            emailService.sendListingRejectedByAdmin(payload);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> listUsers(final int page, final int pageSize) {
        return userDao.findAllUsersPaginated(page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarBrand> findPendingBrands() {
        return carBrandDao.findPendingOrdered();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarModel> findPendingModels() {
        return carModelDao.findPendingOrdered();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Car> findAdminPausedCars() {
        return carDao.findCarsByStatus(Car.Status.ADMIN_PAUSED);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservationCard> listAllReservations(final int page, final int pageSize) {
        return reservationDao.findAllReservationCards(page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Reservation> getReservationById(final long reservationId) {
        return reservationDao.getReservationById(reservationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationMessage> getAdminChatMessages(final long reservationId, final int offset, final int limit) {
        return reservationMessageDao.findByReservationIdOrderByCreatedAtAsc(reservationId, offset, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public long countReservationMessages(final long reservationId) {
        return reservationMessageDao.countByReservationId(reservationId);
    }

}
