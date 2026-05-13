package ar.edu.itba.paw.persistence.hibernate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.Image;
import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.security.UserRole;
import ar.edu.itba.paw.models.util.EmailNormalizer;
import ar.edu.itba.paw.persistence.UserDao;

@Transactional
@Repository
public class UserJpaDao implements UserDao {

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
        user.getRoles().add(UserRole.USER.persistenceName());
        em.persist(user);
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
        final Image pictureRef = profilePictureImageId != null
                ? em.getReference(Image.class, profilePictureImageId)
                : null;
        em.createQuery("UPDATE User u SET u.profilePicture = :picture WHERE u.id = :id")
                .setParameter("picture", pictureRef)
                .setParameter("id", userId)
                .executeUpdate();
    }

    @Override
    public void updateLicenseDocument(final long userId, final long fileId, final boolean validated) {
        final StoredFile fileRef = em.getReference(StoredFile.class, fileId);
        em.createQuery("UPDATE User u SET u.licenseFile = :file, u.licenseValidated = :validated WHERE u.id = :id")
                .setParameter("file", fileRef)
                .setParameter("validated", validated)
                .setParameter("id", userId)
                .executeUpdate();
    }

    @Override
    public void clearLicenseDocument(final long userId) {
        em.createQuery("UPDATE User u SET u.licenseFile = null, u.licenseValidated = false WHERE u.id = :id")
                .setParameter("id", userId)
                .executeUpdate();
    }

    @Override
    public void updateIdentityDocument(final long userId, final long fileId, final boolean validated) {
        final StoredFile fileRef = em.getReference(StoredFile.class, fileId);
        em.createQuery("UPDATE User u SET u.identityFile = :file, u.identityValidated = :validated WHERE u.id = :id")
                .setParameter("file", fileRef)
                .setParameter("validated", validated)
                .setParameter("id", userId)
                .executeUpdate();
    }

    @Override
    public void clearIdentityDocument(final long userId) {
        em.createQuery("UPDATE User u SET u.identityFile = null, u.identityValidated = false WHERE u.id = :id")
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
                        "SELECT l.car.owner FROM Listing l WHERE l.id = :listingId",
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

    @Override
    public List<String> findRoleNamesForUser(final long userId) {
        return getUserById(userId)
                .map(u -> new ArrayList<>(u.getRoles()))
                .orElse(new ArrayList<>());
    }

    @Override
    public void insertUserRole(final long userId, final UserRole role) {
        final User user = em.find(User.class, userId);
        if (user != null) {
            user.getRoles().add(role.persistenceName());
        }
    }

    @Override
    public void updateCbu(final long userId, final String cbu) {
        em.createQuery("UPDATE User u SET u.cbu = :cbu WHERE u.id = :id")
                .setParameter("cbu", cbu)
                .setParameter("id", userId)
                .executeUpdate();
    }
}
