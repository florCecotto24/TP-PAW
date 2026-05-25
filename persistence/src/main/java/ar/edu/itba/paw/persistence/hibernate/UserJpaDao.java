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
        final User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }
        user.setForename(forename);
        user.setSurname(surname);
    }

    @Override
    public void updatePhoneNumber(final long userId, final String phoneNumber) {
        final User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }
        user.setPhoneNumber(phoneNumber);
    }

    @Override
    public void updateBirthDate(final long userId, final LocalDate birthDate) {
        final User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }
        user.setBirthDate(birthDate);
    }

    @Override
    public void updateAbout(final long userId, final String about) {
        final User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }
        user.setAbout(about);
    }

    @Override
    public void updateProfilePictureId(final long userId, final Long profilePictureImageId) {
        final User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }
        user.setProfilePicture(
                profilePictureImageId != null ? em.getReference(Image.class, profilePictureImageId) : null);
    }

    @Override
    public void updateLicenseDocument(final long userId, final long fileId, final boolean validated) {
        final User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }
        user.setLicenseFile(em.getReference(StoredFile.class, fileId));
        user.setLicenseValidated(validated);
    }

    @Override
    public void clearLicenseDocument(final long userId) {
        final User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }
        user.setLicenseFile(null);
        user.setLicenseValidated(false);
    }

    @Override
    public void updateIdentityDocument(final long userId, final long fileId, final boolean validated) {
        final User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }
        user.setIdentityFile(em.getReference(StoredFile.class, fileId));
        user.setIdentityValidated(validated);
    }

    @Override
    public void clearIdentityDocument(final long userId) {
        final User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }
        user.setIdentityFile(null);
        user.setIdentityValidated(false);
    }

    @Override
    public void updateEmailValidated(final long userId, final boolean validated) {
        final User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }
        user.setEmailValidated(validated);
    }

    @Override
    public void updatePasswordHash(final long userId, final String passwordHash) {
        final User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }
        user.setPasswordHash(passwordHash);
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
        final User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }
        user.setLatestLocaleTag(localeTag);
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
        final User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }
        user.setCbu(cbu);
    }
}
