package ar.edu.itba.paw.services;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.user.EmailAlreadyExistsException;
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

    private final UserDao userDao;
    private final ImageService imageService;
    private final PasswordEncoder passwordEncoder;
    private final UserValidationPolicy validationPolicy;

    @Autowired
    public UserServiceImpl(
            final UserDao userDao,
            final ImageService imageService,
            final PasswordEncoder passwordEncoder,
            final UserValidationPolicy validationPolicy) {
        this.userDao = userDao;
        this.imageService = imageService;
        this.passwordEncoder = passwordEncoder;
        this.validationPolicy = validationPolicy;
    }

    @Override
    public User createUser(final String email, final String forename, final String surname) {
        final String normalizedEmail = EmailNormalizer.normalize(email);
        if (userDao.findByEmail(normalizedEmail).isPresent()) {
            throw new EmailAlreadyExistsException(MessageKeys.USER_EMAIL_ALREADY_EXISTS);
        }
        return userDao.createUser(normalizedEmail, forename, surname, null);
    }

    @Override
    public Optional<User> findByEmail(final String email) {
        return userDao.findByEmail(EmailNormalizer.normalize(email));
    }

    @Override
    @Transactional
    public void setRegistrationPassword(final long userId, final String password, final String passwordConfirm) {
        final User user = userDao.getUserById(userId).orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        if (!Boolean.TRUE.equals(user.getEmailValidated().orElse(false))) {
            throw new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND);
        }
        if (user.getPasswordHash().filter(h -> !h.isBlank()).isPresent()) {
            throw new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND);
        }
        if (password == null || passwordConfirm == null || !password.equals(passwordConfirm)) {
            throw new RegistrationPasswordException(MessageKeys.USER_REGISTRATION_PASSWORD_MISMATCH);
        }
        if (password.length() < validationPolicy.getRegistrationPasswordMinLength()) {
            throw new RegistrationPasswordException(
                    MessageKeys.USER_REGISTRATION_PASSWORD_TOO_SHORT,
                    validationPolicy.getRegistrationPasswordMinLength());
        }
        userDao.updatePasswordHash(userId, passwordEncoder.encode(password));
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

    private static String normalizeOptionalPhone(final String phoneRaw) {
        if (!StringUtils.hasText(phoneRaw)) {
            return null;
        }
        final String trimmed = phoneRaw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
