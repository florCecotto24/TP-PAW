package ar.edu.itba.paw.persistence;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.Image;
import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.security.UserRole;
import ar.edu.itba.paw.models.util.format.EmailNormalizer;
import ar.edu.itba.paw.models.util.time.AppTimezone;

@Transactional(readOnly = true)
@Repository
public class UserJpaDao implements UserDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public User createUser(final String email, final String forename, final String surname) {
        return createUser(email, forename, surname, null);
    }

    @Override
    @Transactional
    public User createUser(final String email, final String forename, final String surname, final String passwordHash) {
        final String normalizedEmail = EmailNormalizer.normalize(email);
        final LocalDate memberSince = LocalDate.now(AppTimezone.WALL_ZONE);
        final User user = User.builder()
                .email(normalizedEmail)
                .forename(forename)
                .surname(surname)
                .passwordHash(passwordHash)
                .emailValidated(Boolean.FALSE)
                .memberSince(memberSince)
                .licenseValidated(false)
                .identityValidated(false)
                .userRole(UserRole.USER)
                .roleAssignedBy(null)
                .blocked(false)
                .build();
        em.persist(user);
        return user;
    }

    @Override
    @Transactional
    public User createAdminUser(final String email, final String forename, final String surname,
                                final String passwordHash, final Long assignedByUserId) {
        final String normalizedEmail = EmailNormalizer.normalize(email);
        final LocalDate memberSince = LocalDate.now(AppTimezone.WALL_ZONE);
        final User user = User.builder()
                .email(normalizedEmail)
                .forename(forename)
                .surname(surname)
                .passwordHash(passwordHash)
                .emailValidated(Boolean.TRUE)
                .memberSince(memberSince)
                .licenseValidated(false)
                .identityValidated(false)
                .userRole(UserRole.ADMIN)
                .roleAssignedBy(assignedByUserId)
                .blocked(false)
                .build();
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
    @Transactional
    public void updateUserName(final long userId, final String forename, final String surname) {
        final User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }
        user.setForename(forename);
        user.setSurname(surname);
    }

    @Override
    @Transactional
    public void updatePhoneNumber(final long userId, final String phoneNumber) {
        final User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }
        user.setPhoneNumber(phoneNumber);
    }

    @Override
    @Transactional
    public void updateBirthDate(final long userId, final LocalDate birthDate) {
        final User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }
        user.setBirthDate(birthDate);
    }

    @Override
    @Transactional
    public void updateAbout(final long userId, final String about) {
        final User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }
        user.setAbout(about);
    }

    @Override
    @Transactional
    public void updateProfilePictureId(final long userId, final Long profilePictureImageId) {
        final User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }
        user.setProfilePicture(
                profilePictureImageId != null ? em.getReference(Image.class, profilePictureImageId) : null);
    }

    @Override
    @Transactional
    public void updateLicenseDocument(final long userId, final long fileId, final boolean validated) {
        final User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }
        user.setLicenseFile(em.getReference(StoredFile.class, fileId));
        user.setLicenseValidated(validated);
    }

    @Override
    @Transactional
    public void clearLicenseDocument(final long userId) {
        final User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }
        user.setLicenseFile(null);
        user.setLicenseValidated(false);
    }

    @Override
    @Transactional
    public void updateIdentityDocument(final long userId, final long fileId, final boolean validated) {
        final User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }
        user.setIdentityFile(em.getReference(StoredFile.class, fileId));
        user.setIdentityValidated(validated);
    }

    @Override
    @Transactional
    public void clearIdentityDocument(final long userId) {
        final User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }
        user.setIdentityFile(null);
        user.setIdentityValidated(false);
    }

    @Override
    @Transactional
    public void updateEmailValidated(final long userId, final boolean validated) {
        final User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }
        user.setEmailValidated(validated);
    }

    @Override
    @Transactional
    public void updatePasswordHash(final long userId, final String passwordHash) {
        final User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }
        user.setPasswordHash(passwordHash);
    }

    @Override
    @Transactional
    public void updateLatestLocale(final long userId, final String localeTag) {
        final User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }
        user.setLatestLocaleTag(localeTag);
    }

    @Override
    @Transactional
    public void updateRatingAsRider(final long userId, final java.math.BigDecimal average) {
        final User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }
        user.setRatingAsRider(average);
    }

    @Override
    @Transactional
    public void updateRatingAsOwner(final long userId, final java.math.BigDecimal average) {
        final User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }
        user.setRatingAsOwner(average);
    }

    @Override
    public List<String> findRoleNamesForUser(final long userId) {
        // Persistence-name strings (e.g. "USER", "ADMIN") rather than the enum so the contract stays
        // compatible with the legacy multi-row user_roles table that this method shadowed; the only
        // caller (UserRoleAuthorities.fromDbRoleNames) re-parses with UserRole.fromPersistenceName.
        return getUserById(userId)
                .map(u -> java.util.Collections.singletonList(u.getUserRole().persistenceName()))
                .orElse(java.util.Collections.emptyList());
    }

    @Override
    @Transactional
    public void promoteToAdmin(final long userId, final Long assignedByUserId) {
        final User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }
        user.setUserRole(UserRole.ADMIN);
        user.setRoleAssignedBy(assignedByUserId);
    }

    @Override
    @Transactional
    public void blockUser(final long userId) {
        final User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }
        user.setBlocked(true);
    }

    @Override
    @Transactional
    public void unblockUser(final long userId) {
        final User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }
        user.setBlocked(false);
    }

    @Override
    public Page<User> findAllUsersPaginated(final int page, final int pageSize) {
        /*
         * 1+1 query pattern (project rule: no LIMIT/OFFSET on JPQL):
         * - Step 1 (native): paginate + order on the {@code users} table, projecting only the PKs
         *   for the requested window.
         * - Step 2 (JPQL):   hydrate the {@link User} entities by id. The reorder by id ASC happens
         *   in memory because {@code WHERE id IN :ids} does not preserve native-step ordering.
         */
        final long total = em.createQuery("SELECT COUNT(u) FROM User u", Long.class)
                .getSingleResult();
        @SuppressWarnings("unchecked")
        final List<Number> pageIds = em.createNativeQuery(
                        "SELECT u.id FROM users u ORDER BY u.id ASC LIMIT :limit OFFSET :offset")
                .setParameter("limit", pageSize)
                .setParameter("offset", page * pageSize)
                .getResultList();
        if (pageIds.isEmpty()) {
            return new Page<>(Collections.emptyList(), page, pageSize, total);
        }
        final List<Long> ids = pageIds.stream().map(Number::longValue).collect(Collectors.toList());
        final List<User> hydrated = em.createQuery("FROM User u WHERE u.id IN :ids", User.class)
                .setParameter("ids", ids)
                .getResultList();
        final List<User> sorted = hydrated.stream()
                .sorted(Comparator.comparingLong(User::getId))
                .collect(Collectors.toList());
        return new Page<>(sorted, page, pageSize, total);
    }

    @Override
    @Transactional
    public void updateCbu(final long userId, final String cbu) {
        final User user = em.find(User.class, userId);
        if (user == null) {
            return;
        }
        user.setCbu(cbu);
    }
}
