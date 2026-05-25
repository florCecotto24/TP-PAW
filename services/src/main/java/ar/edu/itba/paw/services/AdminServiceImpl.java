package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.persistence.CarDao;
import ar.edu.itba.paw.persistence.ReservationDao;
import ar.edu.itba.paw.persistence.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {

    private final UserDao userDao;
    private final CarDao carDao;
    private final ReservationDao reservationDao;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AdminServiceImpl(UserDao userDao, CarDao carDao, ReservationDao reservationDao, EmailService emailService, PasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.carDao = carDao;
        this.reservationDao = reservationDao;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void promoteToAdmin(long adminId, long targetUserId) {
        User admin = userDao.getUserById(adminId).orElseThrow(() -> new IllegalArgumentException("Admin not found"));
        User target = userDao.getUserById(targetUserId).orElseThrow(() -> new IllegalArgumentException("Target user not found"));

        if (target.getUserRole() == User.Role.ADMIN) {
            return;
        }

        target.setUserRole(User.Role.ADMIN);
        target.setRoleAssignedBy(admin);
    }

    @Override
    @Transactional
    public void createAdmin(long adminId, String email, String forename, String surname, String rawPassword) {
        User admin = userDao.getUserById(adminId).orElseThrow(() -> new IllegalArgumentException("Admin not found"));
        
        String encodedPassword = passwordEncoder.encode(rawPassword);
        User newAdmin = userDao.createUser(email, forename, surname, encodedPassword);
        newAdmin.setUserRole(User.Role.ADMIN);
        newAdmin.setRoleAssignedBy(admin);
        newAdmin.setEmailValidated(true);

        emailService.sendNewAdminPassword(new ar.edu.itba.paw.models.email.NewAdminPasswordEmailPayload(
            newAdmin.getEmail(), rawPassword, java.util.Locale.ENGLISH
        ));
    }

    @Override
    @Transactional
    public void blockUser(long adminId, long targetUserId) {
        User admin = userDao.getUserById(adminId).orElseThrow(() -> new IllegalArgumentException("Admin not found"));
        User target = userDao.getUserById(targetUserId).orElseThrow(() -> new IllegalArgumentException("Target user not found"));

        if (admin.getRoleAssignedBy().isPresent() && admin.getRoleAssignedBy().get().getId() == targetUserId) {
            throw new IllegalArgumentException("Cannot block the user who promoted you to ADMIN");
        }

        target.setBlocked(true);

        // Pause all active listings
        List<Car> cars = target.getCars();
        for (Car car : cars) {
            if (car.getStatus() == Car.Status.ACTIVE) {
                car.setStatus(Car.Status.ADMIN_PAUSED);
            }
        }

        // Cancel upcoming reservations as rider
        List<Reservation> reservations = target.getReservationsAsRider();
        for (Reservation res : reservations) {
            if (res.getStatus() == Reservation.Status.PENDING || res.getStatus() == Reservation.Status.ACCEPTED) {
                res.setStatus(Reservation.Status.CANCELLED_BY_ADMIN);
                // We should also trigger the cancellation email, this might require ReservationService or MailPayloads.
            }
        }
    }

    @Override
    @Transactional
    public void pauseCar(long adminId, long carId) {
        Car car = carDao.getCarById(carId).orElseThrow(() -> new IllegalArgumentException("Car not found"));
        if (car.getStatus() == Car.Status.ACTIVE || car.getStatus() == Car.Status.PAUSED) {
            car.setStatus(Car.Status.ADMIN_PAUSED);
        }
    }

    @Override
    @Transactional
    public void resumeCar(long adminId, long carId) {
        Car car = carDao.getCarById(carId).orElseThrow(() -> new IllegalArgumentException("Car not found"));
        if (car.getStatus() == Car.Status.ADMIN_PAUSED) {
            car.setStatus(Car.Status.ACTIVE);
        }
    }
}
