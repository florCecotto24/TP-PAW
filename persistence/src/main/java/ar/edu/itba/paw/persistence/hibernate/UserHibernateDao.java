package ar.edu.itba.paw.persistence.hibernate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.security.UserRole;
import ar.edu.itba.paw.models.util.EmailNormalizer;
import ar.edu.itba.paw.persistence.UserDao;

@Transactional
@Repository
public class UserHibernateDao implements UserDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public User createUser(final String email, final String forename, final String surname) {
        return createUser(email, forename, surname, null);
    }

    @Override
    public User createUser(final String email, final String forename, final String surname, final String passwordHash) {
        final String normalizedEmail = EmailNormalizer.normalize(email);
        final LocalDate memberSince = LocalDate.now(AvailabilityPeriod.WALL_ZONE);
        final User user = User.builder()
                .email(normalizedEmail)
                .forename(forename)
                .surname(surname)
                .passwordHash(passwordHash)
                .emailValidated(Boolean.FALSE)
                .memberSince(memberSince)
                .licenseValidated(Boolean.FALSE)
                .identityValidated(Boolean.FALSE)
                .build();
        em.persist(user);
        em.flush();
        insertUserRole(user.getId(), UserRole.USER);
        return user;
    }

    @Override
    public Optional<User> getUserById(final long id) {
        return Optional.ofNullable(em.find(User.class, id));
    }

    @Override
    public Optional<User> findByEmail(final String email) {
        final String normalizedEmail = EmailNormalizer.normalize(email);
        return em.createQuery("FROM User u WHERE u.email = :email", User.class)
                .setParameter("email", normalizedEmail)
                .getResultList().stream().findFirst();
    }

    @Override
    public Optional<User> findByEmailForAuthentication(final String email) {
        return findByEmail(email);
    }

    @Override
    public void updateUserName(final long userId, final String forename, final String surname) {
        em.createQuery("UPDATE User u SET u.forename = :forename, u.surname = :surname WHERE u.id = :id")
                .setParameter("forename", forename)
                .setParameter("surname", surname)
                .setParameter("id", userId)
                .executeUpdate();
    }

    @Override
    public void updatePhoneNumber(final long userId, final String phoneNumber) {
        em.createQuery("UPDATE User u SET u.phoneNumber = :phoneNumber WHERE u.id = :id")
                .setParameter("phoneNumber", phoneNumber)
                .setParameter("id", userId)
                .executeUpdate();
    }

    @Override
    public void updateBirthDate(final long userId, final LocalDate birthDate) {
        em.createQuery("UPDATE User u SET u.birthDate = :birthDate WHERE u.id = :id")
                .setParameter("birthDate", birthDate)
                .setParameter("id", userId)
                .executeUpdate();
    }

    @Override
    public void updateAbout(final long userId, final String about) {
        em.createQuery("UPDATE User u SET u.about = :about WHERE u.id = :id")
                .setParameter("about", about)
                .setParameter("id", userId)
                .executeUpdate();
    }

    @Override
    public void updateProfilePictureId(final long userId, final Long profilePictureImageId) {
        em.createQuery("UPDATE User u SET u.profilePictureId = :pictureId WHERE u.id = :id")
                .setParameter("pictureId", profilePictureImageId)
                .setParameter("id", userId)
                .executeUpdate();
    }

    @Override
    public void updateLicenseDocument(final long userId, final long fileId, final boolean validated) {
        em.createQuery("UPDATE User u SET u.licenseFileId = :fileId, u.licenseValidated = :validated WHERE u.id = :id")
                .setParameter("fileId", fileId)
                .setParameter("validated", validated)
                .setParameter("id", userId)
                .executeUpdate();
    }

    @Override
    public void clearLicenseDocument(final long userId) {
        em.createQuery("UPDATE User u SET u.licenseFileId = null, u.licenseValidated = false WHERE u.id = :id")
                .setParameter("id", userId)
                .executeUpdate();
    }

    @Override
    public void updateIdentityDocument(final long userId, final long fileId, final boolean validated) {
        em.createQuery("UPDATE User u SET u.identityFileId = :fileId, u.identityValidated = :validated WHERE u.id = :id")
                .setParameter("fileId", fileId)
                .setParameter("validated", validated)
                .setParameter("id", userId)
                .executeUpdate();
    }

    @Override
    public void clearIdentityDocument(final long userId) {
        em.createQuery("UPDATE User u SET u.identityFileId = null, u.identityValidated = false WHERE u.id = :id")
                .setParameter("id", userId)
                .executeUpdate();
    }

    @Override
    public void updateEmailValidated(final long userId, final boolean validated) {
        em.createQuery("UPDATE User u SET u.emailValidated = :validated WHERE u.id = :id")
                .setParameter("validated", validated)
                .setParameter("id", userId)
                .executeUpdate();
    }

    @Override
    public void updatePasswordHash(final long userId, final String passwordHash) {
        em.createQuery("UPDATE User u SET u.passwordHash = :passwordHash WHERE u.id = :id")
                .setParameter("passwordHash", passwordHash)
                .setParameter("id", userId)
                .executeUpdate();
    }

    @Override
    public Optional<User> getListingOwner(final long listingId) {
        return em.createQuery(
                        "SELECT u FROM User u, Car c, Listing l WHERE c.ownerId = u.id AND l.carId = c.id AND l.id = :listingId",
                        User.class)
                .setParameter("listingId", listingId)
                .getResultList().stream().findAny();
    }

    @Override
    public void updateLatestLocale(final long userId, final String localeTag) {
        em.createQuery("UPDATE User u SET u.latestLocaleTag = :localeTag WHERE u.id = :id")
                .setParameter("localeTag", localeTag)
                .setParameter("id", userId)
                .executeUpdate();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> findRoleNamesForUser(final long userId) {
        return (List<String>) em.createNativeQuery(
                        "SELECT role FROM user_roles WHERE user_id = :userId ORDER BY role")
                .setParameter("userId", userId)
                .getResultList();
    }

    @Override
    public void insertUserRole(final long userId, final UserRole role) {
        em.createNativeQuery("INSERT INTO user_roles (user_id, role) VALUES (:userId, :role)")
                .setParameter("userId", userId)
                .setParameter("role", role.persistenceName())
                .executeUpdate();
    }

    @Override
    public void updateCbu(final long userId, final String cbu) {
        em.createQuery("UPDATE User u SET u.cbu = :cbu WHERE u.id = :id")
                .setParameter("cbu", cbu)
                .setParameter("id", userId)
                .executeUpdate();
    }
}
