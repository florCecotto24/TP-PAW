package ar.edu.itba.paw.services.user;


import java.security.SecureRandom;
import java.math.BigDecimal;
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
import ar.edu.itba.paw.exception.admin.AdminPromoterNotAdminException;
import ar.edu.itba.paw.exception.admin.UserAlreadyAdminException;
import ar.edu.itba.paw.exception.user.CBUNotFoundException;
import ar.edu.itba.paw.exception.user.EmailAlreadyExistsException;
import ar.edu.itba.paw.exception.user.IncorrectCurrentPasswordException;
import ar.edu.itba.paw.exception.user.InvalidCbuFormatException;
import ar.edu.itba.paw.exception.user.InvalidProfileBirthDateException;
import ar.edu.itba.paw.exception.user.InvalidProfilePhoneException;
import ar.edu.itba.paw.exception.user.InvalidUserFieldLengthException;
import ar.edu.itba.paw.exception.user.RegistrationPasswordException;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.models.security.UserRole;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.domain.user.UserDocumentType;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.file.BinaryContent;
import ar.edu.itba.paw.models.dto.profile.ProfileUpdateRequest;
import ar.edu.itba.paw.models.email.admin.AdminPromotedEmailPayload;
import ar.edu.itba.paw.models.email.user.MigratedUserPasswordEmailPayload;
import ar.edu.itba.paw.models.util.time.AppTimezone;
import ar.edu.itba.paw.models.util.rules.CbuRules;
import ar.edu.itba.paw.models.util.format.EmailNormalizer;
import ar.edu.itba.paw.models.util.rules.SupportedLocales;
import ar.edu.itba.paw.persistence.user.UserDao;
import ar.edu.itba.paw.policy.UserValidationPolicy;


import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.email.EmailService;
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
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final UserValidationPolicy validationPolicy;
    private final EmailVerificationService emailVerificationService;
    private final CarService carService;
    private final UserProfileMediaService userProfileMediaService;

    @Autowired
    public UserServiceImpl(
            final UserDao userDao,
            final EmailService emailService,
            final PasswordEncoder passwordEncoder,
            final UserValidationPolicy validationPolicy,
            @Lazy final EmailVerificationService emailVerificationService,
            @Lazy final CarService carService,
            @Lazy final UserProfileMediaService userProfileMediaService) {
        this.userDao = userDao;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.validationPolicy = validationPolicy;
        this.emailVerificationService = emailVerificationService;
        this.carService = carService;
        this.userProfileMediaService = userProfileMediaService;
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
    public List<UserRole> findRolesForUser(final long userId) {
        return userDao.findRolesForUser(userId);
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
            final LocalDate today = LocalDate.now(AppTimezone.WALL_ZONE);
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
    public void updateProfile(final long userId, final ProfileUpdateRequest request) {
        // Single-transaction wrapper around the five per-field updaters. Calling them via `this.`
        // bypasses the AOP proxy, so the @Transactional annotations on the inner methods are NOT
        // re-applied — which is exactly what we want: everything participates in the outer tx and
        // a failure halfway through rolls back the whole batch (the bug the controller used to have
        // when it called each updater in sequence outside a tx).
        updateDisplayName(userId, request.getForename(), request.getSurname());
        updatePhoneNumber(userId, request.getPhoneNumberRaw());
        updateBirthDate(userId, request.getBirthDate().orElse(null));
        updateAbout(userId, request.getAboutRaw());
        updateCbu(userId, request.getCbuRaw());
    }

    @Override
    @Transactional
    public void updateProfilePicture(
            final long userId,
            final String originalFilename,
            final String contentType,
            final byte[] data) {
        userProfileMediaService.updateProfilePicture(userId, originalFilename, contentType, data);
    }

    @Override
    @Transactional
    public void clearProfilePicture(final long userId) {
        userProfileMediaService.clearProfilePicture(userId);
    }

    @Override
    @Transactional
    public void uploadValidatedProfileDocument(
            final long userId,
            final UserDocumentType documentType,
            final String originalFilename,
            final String contentType,
            final byte[] data) {
        userProfileMediaService.uploadValidatedProfileDocument(userId, documentType, originalFilename, contentType, data);
    }

    @Override
    @Transactional
    public void clearProfileDocument(final long userId, final UserDocumentType documentType) {
        userProfileMediaService.clearProfileDocument(userId, documentType);
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
    @Transactional
    public void updateRatingAsRider(final long userId, final BigDecimal average) {
        userDao.updateRatingAsRider(userId, average);
    }

    @Override
    @Transactional
    public void updateRatingAsOwner(final long userId, final BigDecimal average) {
        userDao.updateRatingAsOwner(userId, average);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Locale> findUserPreferredLocale(final long userId) {
        return preferredLocaleFor(userId);
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
        return preferredLocaleFor(userId).orElse(fb);
    }

    /**
     * Private helper shared by {@link #findUserPreferredLocale(long)} and
     * {@link #resolveMailLocaleOrElse(long, Locale)}. Extracted so the latter no longer self-invokes
     * the public {@code findUserPreferredLocale}, which would skip the Spring proxy and ignore its
     * {@code @Transactional(readOnly = true)} annotation if propagation rules ever changed.
     */
    private Optional<Locale> preferredLocaleFor(final long userId) {
        return getUserById(userId)
                .flatMap(User::getLatestLocale)
                .flatMap(loc -> SupportedLocales.parse(loc.toLanguageTag()));
    }

    @Override
    @Transactional(readOnly = true)
    public Locale resolveMailLocaleFor(final User user) {
        if (user == null) {
            return Locale.ENGLISH;
        }
        return user.getLatestLocale()
                .flatMap(loc -> SupportedLocales.parse(loc.toLanguageTag()))
                .orElse(Locale.ENGLISH);
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
    @Transactional(readOnly = true)
    public boolean meetsPublishingPrerequisites(final User user) {
        if (user == null) {
            return false;
        }
        return hasValidCbu(user) && user.getIdentityFileId().isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredFile> findProfileDocument(final long userId, final UserDocumentType documentType) {
        return userProfileMediaService.findProfileDocument(userId, documentType);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BinaryContent> findProfileDocumentContent(
            final long userId, final UserDocumentType documentType) {
        return userProfileMediaService.findProfileDocumentContent(userId, documentType);
    }

    // Intentionally not @Transactional: pure static-format validation delegating to CbuRules
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

    @Override
    @Transactional
    public void promoteToAdmin(final long targetUserId, final long assignedByUserId) {
        final User granting = userDao.getUserById(assignedByUserId)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        if (!granting.isAdmin()) {
            throw new AdminPromoterNotAdminException();
        }
        final User target = userDao.getUserById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        if (target.isAdmin()) {
            throw new UserAlreadyAdminException();
        }
        userDao.updateUserRoleAndGrantor(targetUserId, UserRole.ADMIN, assignedByUserId);
        final Locale mailLocale = resolveMailLocaleOrElse(targetUserId, Locale.ENGLISH);
        emailService.sendAdminPromoted(AdminPromotedEmailPayload.builder()
                .messageLocale(mailLocale)
                .recipientEmail(target.getEmail())
                .recipientFullName(target.getForename() + " " + target.getSurname())
                .grantedByFullName(granting.getForename() + " " + granting.getSurname())
                .targetUserId(targetUserId)
                .build());
    }

    @Override
    @Transactional
    public User createUserWithEncodedPassword(
            final String email, final String forename, final String surname, final String bcryptEncodedHash) {
        return userDao.createUser(email, forename, surname, bcryptEncodedHash);
    }

    @Override
    @Transactional
    public User createAdminUserWithEncodedPassword(
            final String email, final String forename, final String surname,
            final String bcryptEncodedHash, final long assignedByUserId) {
        final User granting = userDao.getUserById(assignedByUserId)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        if (!granting.isAdmin()) {
            throw new AdminPromoterNotAdminException();
        }
        // Service decides the role/verification state that distinguishes an admin-created account
        // from self-registration; UserDao#createUser persists what it is told.
        return userDao.createUser(email, forename, surname, bcryptEncodedHash,
                UserRole.ADMIN, true, assignedByUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> findAllUsersPaginated(final int page, final int pageSize) {
        return userDao.findAllUsersPaginated(page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> findUsersPaginated(
            final int page,
            final int pageSize,
            final Boolean blocked,
            final UserRole role,
            final String query) {
        return userDao.findUsersPaginated(page, pageSize, blocked, role, query);
    }

    @Override
    @Transactional
    public void demoteFromAdmin(final long targetUserId, final long assignedByUserId) {
        final User granting = userDao.getUserById(assignedByUserId)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        if (!granting.isAdmin()) {
            throw new AdminPromoterNotAdminException();
        }
        final User target = userDao.getUserById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        if (!target.isAdmin()) {
            return;
        }
        userDao.updateUserRoleAndGrantor(targetUserId, UserRole.USER, null);
    }

    @Override
    @Transactional
    public void updateIdentityValidated(final long userId, final boolean validated) {
        userDao.getUserById(userId).orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        userDao.updateIdentityValidated(userId, validated);
    }

    @Override
    @Transactional
    public void updateLicenseValidated(final long userId, final boolean validated) {
        userDao.getUserById(userId).orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        userDao.updateLicenseValidated(userId, validated);
    }

    // -----------------------------------------------------------------------------------------------------------
    // Profile-media-orchestrated operations on user rows (see contract Javadoc): each call is a thin transactional
    // pass-through to UserDao so {@link UserProfileMediaServiceImpl} can mutate FKs without injecting UserDao itself.
    // -----------------------------------------------------------------------------------------------------------

    @Override
    @Transactional
    public void updateProfilePictureFk(final long userId, final Long imageId) {
        userDao.updateProfilePictureId(userId, imageId);
    }

    @Override
    @Transactional
    public void updateLicenseDocumentFk(final long userId, final long storedFileId, final boolean validated) {
        userDao.updateLicenseDocument(userId, storedFileId, validated);
    }

    @Override
    @Transactional
    public void updateIdentityDocumentFk(final long userId, final long storedFileId, final boolean validated) {
        userDao.updateIdentityDocument(userId, storedFileId, validated);
    }

    @Override
    @Transactional
    public void clearLicenseDocumentFk(final long userId) {
        userDao.clearLicenseDocument(userId);
    }

    @Override
    @Transactional
    public void clearIdentityDocumentFk(final long userId) {
        userDao.clearIdentityDocument(userId);
    }

    @Override
    @Transactional
    public void deleteUser(final long userId) {
        userDao.getUserById(userId).orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        userDao.deleteUser(userId);
    }
}
