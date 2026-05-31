package ar.edu.itba.paw.services;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.user.CBUNotFoundException;
import ar.edu.itba.paw.exception.user.EmailAlreadyExistsException;
import ar.edu.itba.paw.exception.user.IncorrectCurrentPasswordException;
import ar.edu.itba.paw.exception.user.InvalidCbuFormatException;
import ar.edu.itba.paw.exception.user.InvalidProfileBirthDateException;
import ar.edu.itba.paw.exception.user.InvalidProfileDocumentException;
import ar.edu.itba.paw.exception.user.InvalidProfilePhoneException;
import ar.edu.itba.paw.exception.user.InvalidUserFieldLengthException;
import ar.edu.itba.paw.exception.user.RegistrationPasswordException;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.Image;
import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.domain.UserDocumentType;
import ar.edu.itba.paw.models.email.MigratedUserPasswordEmailPayload;
import ar.edu.itba.paw.models.util.CbuRules;
import ar.edu.itba.paw.models.util.EmailNormalizer;
import ar.edu.itba.paw.models.util.SupportedLocales;
import ar.edu.itba.paw.persistence.UserDao;
import ar.edu.itba.paw.services.policy.ProfileDocumentUploadPolicy;
import ar.edu.itba.paw.services.policy.UserValidationPolicy;


/**
 * User rows via {@link UserDao}; blobs, mail, verification, and listing side effects use peer services.
 * {@code @Lazy} breaks cycles with {@link EmailVerificationService} and {@link CarService}.
 */
@Service
public final class UserServiceImpl implements UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

    private static final char[] MIGRATION_PASSWORD_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789".toCharArray();
    private static final int MIGRATION_PASSWORD_LENGTH = 14;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserDao userDao;
    private final ImageService imageService;
    private final StoredFileService storedFileService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final ProfileDocumentUploadPolicy profileDocumentUploadPolicy;
    private final UserValidationPolicy validationPolicy;
    private final EmailVerificationService emailVerificationService;
    private final CarService carService;

    @Autowired
    public UserServiceImpl(
            final UserDao userDao,
            final ImageService imageService,
            final StoredFileService storedFileService,
            final EmailService emailService,
            final PasswordEncoder passwordEncoder,
            final ProfileDocumentUploadPolicy profileDocumentUploadPolicy,
            final UserValidationPolicy validationPolicy,
            @Lazy final EmailVerificationService emailVerificationService,
            @Lazy final CarService carService) {
        this.userDao = userDao;
        this.imageService = imageService;
        this.storedFileService = storedFileService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.profileDocumentUploadPolicy = profileDocumentUploadPolicy;
        this.validationPolicy = validationPolicy;
        this.emailVerificationService = emailVerificationService;
        this.carService = carService;
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
        assertRegistrationFieldLengths(normalizedEmail, forename, surname);
        assertNewPasswordPair(password, passwordConfirm);
        final User created = userDao.createUser(
                normalizedEmail,
                forename.trim(),
                surname.trim(),
                passwordEncoder.encode(password));
        LOGGER.atInfo().addArgument(created.getId()).log("Registered new user id={}");
        return created;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(final String email) {
        return userDao.findByEmail(EmailNormalizer.normalize(email));
    }

    @Override
    @Transactional
    public void markEmailVerified(final long userId) {
        userDao.getUserById(userId).orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        userDao.updateEmailValidated(userId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> getUserById(final long id) {
        return userDao.getUserById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmailForAuthentication(final String email) {
        return userDao.findByEmailForAuthentication(email);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findRoleNamesForUser(final long userId) {
        return userDao.findRoleNamesForUser(userId);
    }

    @Override
    @Transactional
    public void updateDisplayName(final long userId, final String forename, final String surname) {
        userDao.getUserById(userId).orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        assertDisplayNamePartLengths(forename, surname, true);
        userDao.updateUserName(userId, forename.trim(), surname.trim());
    }

    @Override
    @Transactional
    public void updatePhoneNumber(final long userId, final String phoneRaw) {
        userDao.getUserById(userId).orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        final String phone = normalizeOptionalPhone(phoneRaw);
        if (phone != null) {
            if (phone.length() > validationPolicy.getProfilePhoneMaxLength()
                    || !validationPolicy.getProfilePhonePattern().matcher(phone).matches()) {
                throw new InvalidProfilePhoneException(
                        MessageKeys.USER_PROFILE_PHONE_INVALID,
                        validationPolicy.getProfilePhoneMaxLength());
            }
        }
        userDao.updatePhoneNumber(userId, phone);
    }

    @Override
    @Transactional
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
    public void updateAbout(final long userId, final String aboutRaw) {
        userDao.getUserById(userId).orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        final String about = normalizeOptionalAbout(aboutRaw);
        if (about != null && about.length() > validationPolicy.getProfileAboutMaxLength()) {
            throw new InvalidUserFieldLengthException(
                    MessageKeys.USER_PROFILE_ABOUT_TOO_LONG,
                    validationPolicy.getProfileAboutMaxLength());
        }
        userDao.updateAbout(userId, about);
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
    public void uploadValidatedProfileDocument(
            final long userId,
            final UserDocumentType documentType,
            final String originalFilename,
            final String contentType,
            final byte[] data) {
        final User user = userDao.getUserById(userId).orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        if (documentType == null
                || originalFilename == null || originalFilename.isBlank()
                || !StoredFile.isAllowedPaymentReceiptContentType(contentType)) {
            throw new InvalidProfileDocumentException(MessageKeys.USER_PROFILE_DOCUMENT_INVALID);
        }
        final int length = data == null ? 0 : data.length;
        if (length <= 0) {
            throw new InvalidProfileDocumentException(MessageKeys.USER_PROFILE_DOCUMENT_INVALID);
        }
        if (length > profileDocumentUploadPolicy.getMaxBytes()) {
            throw new InvalidProfileDocumentException(
                    MessageKeys.USER_PROFILE_DOCUMENT_TOO_LARGE,
                    profileDocumentUploadPolicy.getMaxMegabytesRoundedUp());
        }
        if (profileDocumentSlotOccupied(user, documentType)) {
            throw new InvalidProfileDocumentException(MessageKeys.USER_PROFILE_DOCUMENT_ALREADY_UPLOADED);
        }
        final StoredFile stored = storedFileService.create(userId, originalFilename, contentType, data);
        switch (documentType) {
            case LICENSE:
                userDao.updateLicenseDocument(userId, stored.getId(), true);
                return;
            case IDENTITY:
                userDao.updateIdentityDocument(userId, stored.getId(), true);
                return;
            default:
                throw new InvalidProfileDocumentException(MessageKeys.USER_PROFILE_DOCUMENT_INVALID);
        }
    }

    private static boolean profileDocumentSlotOccupied(final User user, final UserDocumentType documentType) {
        return switch (documentType) {
            case LICENSE -> user.getLicenseFileId().isPresent();
            case IDENTITY -> user.getIdentityFileId().isPresent();
        };
    }

    @Override
    @Transactional
    public void clearProfileDocument(final long userId, final UserDocumentType documentType) {
        userDao.getUserById(userId).orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        if (documentType == null) {
            throw new InvalidProfileDocumentException(MessageKeys.USER_PROFILE_DOCUMENT_INVALID);
        }
        switch (documentType) {
            case LICENSE:
                userDao.clearLicenseDocument(userId);
                return;
            case IDENTITY:
                userDao.clearIdentityDocument(userId);
                return;
            default:
                throw new InvalidProfileDocumentException(MessageKeys.USER_PROFILE_DOCUMENT_INVALID);
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
    public void replacePasswordHash(final long userId, final String bcryptEncodedHash) {
        userDao.getUserById(userId).orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        userDao.updatePasswordHash(userId, bcryptEncodedHash);
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
        final Locale mailLocale = resolveMailLocaleOrElse(userId, locale != null ? locale : Locale.ENGLISH);
        emailService.sendMigratedUserPassword(MigratedUserPasswordEmailPayload.builder()
                .messageLocale(mailLocale)
                .recipientEmail(withHash.getEmail())
                .plainPassword(plain)
                .build());
    }

    private void assertRegistrationFieldLengths(final String normalizedEmail, final String forename, final String surname) {
        if (normalizedEmail.length() > validationPolicy.getRegistrationEmailMaxLength()) {
            throw new InvalidUserFieldLengthException(
                    MessageKeys.USER_REGISTRATION_EMAIL_TOO_LONG,
                    validationPolicy.getRegistrationEmailMaxLength());
        }
        assertDisplayNamePartLengths(forename, surname, false);
    }

    private void assertDisplayNamePartLengths(final String forename, final String surname, final boolean profileContext) {
        final int max = validationPolicy.getDisplayNamePartMaxLength();
        final String f = forename == null ? "" : forename.trim();
        final String s = surname == null ? "" : surname.trim();
        if (f.length() > max) {
            throw new InvalidUserFieldLengthException(
                    profileContext ? MessageKeys.USER_PROFILE_FORENAME_TOO_LONG : MessageKeys.USER_REGISTRATION_FORENAME_TOO_LONG,
                    max);
        }
        if (s.length() > max) {
            throw new InvalidUserFieldLengthException(
                    profileContext ? MessageKeys.USER_PROFILE_SURNAME_TOO_LONG : MessageKeys.USER_REGISTRATION_SURNAME_TOO_LONG,
                    max);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getUserCbu(long userId){
        Optional<String> cbu = userDao.getUserById(userId)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND))
                .getCbu();
        if (cbu.isEmpty() || cbu.get().isBlank()) {
            throw new CBUNotFoundException(userId);
        }
        return cbu.get();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findOwnerCbuForCar(final long carId) {
        final Optional<Long> ownerIdOpt = carService.getCarById(carId)
                .map(Car::getOwner)
                .map(User::getId);
        if (ownerIdOpt.isEmpty()) {
            LOGGER.atWarn().addArgument(carId).log("Car owner missing when resolving CBU for car (carId={})");
            return Optional.empty();
        }
        final long ownerId = ownerIdOpt.get();
        try {
            return Optional.of(getUserCbu(ownerId));
        } catch (final UserNotFoundException | CBUNotFoundException e) {
            LOGGER.atWarn()
                    .setCause(e)
                    .addArgument(carId)
                    .addArgument(ownerId)
                    .log("Owner CBU unavailable for car (carId={}, ownerId={})");
            return Optional.empty();
        }
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
        if (password.length() > validationPolicy.getRegistrationPasswordMaxLength()) {
            throw new RegistrationPasswordException(
                    MessageKeys.USER_REGISTRATION_PASSWORD_TOO_LONG,
                    validationPolicy.getRegistrationPasswordMaxLength());
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

    private static String normalizeOptionalAbout(final String aboutRaw) {
        if (!StringUtils.hasText(aboutRaw)) {
            return null;
        }
        final String trimmed = aboutRaw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    @Transactional
    public void updateLatestLocale(final long userId, final String localeTag) {
        if (localeTag == null || localeTag.isBlank()) {
            return;
        }
        String t = localeTag.trim();
        if (t.length() > 32) {
            t = t.substring(0, 32);
        }
        userDao.updateLatestLocale(userId, t);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Locale> findUserPreferredLocale(final long userId) {
        return getUserById(userId)
                .flatMap(User::getLatestLocaleTag)
                .flatMap(SupportedLocales::parse);
    }

    @Override
    @Transactional(readOnly = true)
    public Locale resolveMailLocale(final long userId) {
        return resolveMailLocaleOrElse(userId, Locale.ENGLISH);
    }

    @Override
    @Transactional(readOnly = true)
    public Locale resolveMailLocaleOrElse(final long userId, final Locale fallback) {
        final Locale fb = fallback != null ? fallback : Locale.ENGLISH;
        return findUserPreferredLocale(userId).orElse(fb);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasValidCbu(final User user) {
        if (user == null) {
            return false;
        }
        return user.getCbu().filter(CbuRules::isValidFormat).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUploadedLicenseAndIdentity(final User user) {
        if (user == null) {
            return false;
        }
        return user.getLicenseFileId().isPresent() && user.getIdentityFileId().isPresent();
    }

    @Override
    public boolean isValidCbuFormat(final String cbuRaw) {
        return CbuRules.isValidFormat(cbuRaw);
    }

    @Override
    @Transactional
    public void updateCbu(final long userId, final String cbu) {
        userDao.getUserById(userId).orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        final String normalized = cbu == null ? "" : cbu.trim();
        if (normalized.isEmpty()) {
            userDao.updateCbu(userId, null);
            carService.pauseCarsForMissingCbu(userId);
            return;
        }
        if (!CbuRules.isValidFormat(normalized)) {
            throw new InvalidCbuFormatException(CbuRules.REQUIRED_DIGIT_LENGTH);
        }
        userDao.updateCbu(userId, normalized);
        carService.resumeCarsForRestoredCbu(userId);
    }

    @Override
    @Transactional
    public User registerUserRequiringAccountConfirmation(
            final String email,
            final String forename,
            final String surname,
            final String password,
            final String passwordConfirm,
            final Locale locale) {
        final User created = registerUser(email, forename, surname, password, passwordConfirm);
        emailVerificationService.issueFreshVerificationCode(created.getId(), created.getEmail(), locale);
        return created;
    }

    @Override
    @Transactional
    public void ensureAccountConfirmationPrerequisites(final String email, final Locale locale) {
        if (email == null || email.isBlank()) {
            return;
        }
        findByEmail(email.trim())
                .ifPresent(u -> emailVerificationService.ensurePendingVerificationCode(
                        u.getId(), u.getEmail(), locale));
    }

    @Override
    @Transactional
    public boolean requestAccountConfirmationResend(final String email, final Locale locale) {
        if (email == null || email.isBlank()) {
            return false;
        }
        final Optional<User> userOpt = findByEmail(email.trim());
        if (userOpt.isEmpty()) {
            return false;
        }
        final User user = userOpt.get();
        emailVerificationService.resendVerificationCode(user.getId(), user.getEmail(), locale);
        return true;
    }

    @Override
    @Transactional
    public long completeAccountConfirmation(final String email, final String code) {
        return emailVerificationService.verifyEmailAndConsumeCode(email, code);
    }

    @Override
    @Transactional
    public void blockUser(final long userId) {
        userDao.blockUser(userId);
    }

    @Override
    @Transactional
    public void unblockUser(final long userId) {
        userDao.unblockUser(userId);
    }
}
