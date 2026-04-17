package ar.edu.itba.paw.services;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.user.EmailAlreadyExistsException;
import ar.edu.itba.paw.exception.user.IncorrectCurrentPasswordException;
import ar.edu.itba.paw.exception.user.InvalidProfileBirthDateException;
import ar.edu.itba.paw.exception.user.InvalidProfilePhoneException;
import ar.edu.itba.paw.exception.user.RegistrationPasswordException;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.EmailNormalizer;
import ar.edu.itba.paw.models.Image;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserValidationPolicy;
import ar.edu.itba.paw.persistence.UserDao;

@Service
public class UserServiceImpl implements UserService {

    private static final char[] MIGRATION_PASSWORD_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789".toCharArray();
    private static final int MIGRATION_PASSWORD_LENGTH = 14;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserDao userDao;
    private final ImageService imageService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final UserValidationPolicy validationPolicy;

    @Autowired
    public UserServiceImpl(
            final UserDao userDao,
            final ImageService imageService,
            final EmailService emailService,
            final PasswordEncoder passwordEncoder,
            final UserValidationPolicy validationPolicy) {
        this.userDao = userDao;
        this.imageService = imageService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.validationPolicy = validationPolicy;
    }

    @Override
    @Transactional
    public User registerUser(
            final String email,
            final String forename,
            final String surname,
            final String password,
            final String passwordConfirm) {
        final String normalizedEmail = EmailNormalizer.normalize(email);
        if (userDao.findByEmail(normalizedEmail).isPresent()) {
            throw new EmailAlreadyExistsException(MessageKeys.USER_EMAIL_ALREADY_EXISTS);
        }
        assertNewPasswordPair(password, passwordConfirm);
        return userDao.createUser(
                normalizedEmail,
                forename.trim(),
                surname.trim(),
                passwordEncoder.encode(password));
    }

    @Override
    public Optional<User> findByEmail(final String email) {
        return userDao.findByEmail(EmailNormalizer.normalize(email));
    }

    @Override
    public Optional<User> getUserById(final long id) {
        return userDao.getUserById(id);
    }

    @Override
    public Optional<User> findByEmailForAuthentication(final String email) {
        return userDao.findByEmailForAuthentication(email);
    }

    @Override
    public Optional<User> getListingOwner(final long listingId) {
        return userDao.getListingOwner(listingId);
    }

    @Override
    @Transactional
    public void updateDisplayName(final long userId, final String forename, final String surname) {
        userDao.getUserById(userId).orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        userDao.updateUserName(userId, forename.trim(), surname.trim());
    }

    @Override
    public void updatePhoneNumber(final long userId, final String phoneRaw) {
        userDao.getUserById(userId).orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        final String phone = normalizeOptionalPhone(phoneRaw);
        if (phone != null) {
            if (phone.length() > validationPolicy.getProfilePhoneMaxLength()
                    || !validationPolicy.getProfilePhonePattern().matcher(phone).matches()) {
                throw new InvalidProfilePhoneException(MessageKeys.USER_PROFILE_PHONE_INVALID);
            }
        }
        userDao.updatePhoneNumber(userId, phone);
    }

    @Override
    public void updateBirthDate(final long userId, final LocalDate birthDate) {
        userDao.getUserById(userId).orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        if (birthDate != null) {
            final LocalDate today = LocalDate.now(AvailabilityPeriod.WALL_ZONE);
            if (birthDate.isAfter(today)) {
                throw new InvalidProfileBirthDateException(MessageKeys.USER_PROFILE_BIRTH_DATE_FUTURE);
            }
        }
        userDao.updateBirthDate(userId, birthDate);
    }

    @Override
    @Transactional
    public void updateProfilePicture(
            final long userId,
            final String originalFilename,
            final String contentType,
            final byte[] data) {
        final User user = userDao.getUserById(userId).orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        final String safeName = originalFilename != null && !originalFilename.isBlank() ? originalFilename : "profile";
        final String safeType = contentType != null && !contentType.isBlank() ? contentType : "application/octet-stream";
        final Image created = imageService.createImage(safeName, safeType, data);
        final Long previousId = user.getProfilePictureId().orElse(null);
        userDao.updateProfilePictureId(userId, created.getId());
        if (previousId != null && !previousId.equals(created.getId())) {
            imageService.deleteImage(previousId);
        }
    }

    @Override
    @Transactional
    public void clearProfilePicture(final long userId) {
        final User user = userDao.getUserById(userId).orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        final Long previousId = user.getProfilePictureId().orElse(null);
        userDao.updateProfilePictureId(userId, null);
        if (previousId != null) {
            imageService.deleteImage(previousId);
        }
    }

    @Override
    @Transactional
    public void changePassword(
            final long userId,
            final String currentPassword,
            final String newPassword,
            final String newPasswordConfirm) {
        final User basic = userDao.getUserById(userId).orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        final User withHash = userDao.findByEmailForAuthentication(basic.getEmail())
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        final String hash = withHash.getPasswordHash().filter(h -> !h.isBlank())
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, hash)) {
            throw new IncorrectCurrentPasswordException(MessageKeys.USER_PASSWORD_CURRENT_INCORRECT);
        }
        assertNewPasswordPair(newPassword, newPasswordConfirm);
        userDao.updatePasswordHash(userId, passwordEncoder.encode(newPassword));
    }

    @Override
    @Transactional
    public void assignRandomPasswordAndEmailForLegacyUser(final long userId, final String email, final Locale locale) {
        final String normalized = EmailNormalizer.normalize(email);
        final User withHash = userDao.findByEmailForAuthentication(normalized)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        if (withHash.getId() != userId) {
            throw new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND);
        }
        if (withHash.getPasswordHash().filter(s -> !s.isBlank()).isPresent()) {
            return;
        }
        final String plain = randomMigrationPlainPassword();
        userDao.updatePasswordHash(userId, passwordEncoder.encode(plain));
        userDao.updateEmailValidated(userId, true);
        emailService.sendMigratedUserPassword(withHash.getEmail(), plain, locale);
    }

    private void assertNewPasswordPair(final String password, final String passwordConfirm) {
        if (password == null || passwordConfirm == null || !password.equals(passwordConfirm)) {
            throw new RegistrationPasswordException(MessageKeys.USER_REGISTRATION_PASSWORD_MISMATCH);
        }
        if (password.length() < validationPolicy.getRegistrationPasswordMinLength()) {
            throw new RegistrationPasswordException(
                    MessageKeys.USER_REGISTRATION_PASSWORD_TOO_SHORT,
                    validationPolicy.getRegistrationPasswordMinLength());
        }
    }

    private static String randomMigrationPlainPassword() {
        final char[] buf = new char[MIGRATION_PASSWORD_LENGTH];
        for (int i = 0; i < buf.length; i++) {
            buf[i] = MIGRATION_PASSWORD_ALPHABET[SECURE_RANDOM.nextInt(MIGRATION_PASSWORD_ALPHABET.length)];
        }
        return new String(buf);
    }

    private static String normalizeOptionalPhone(final String phoneRaw) {
        if (!StringUtils.hasText(phoneRaw)) {
            return null;
        }
        final String trimmed = phoneRaw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
