package ar.edu.itba.paw.services.user.view;


import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.profile.ProfileBaseModel;

import ar.edu.itba.paw.services.file.StoredFileService;
import ar.edu.itba.paw.services.user.UserService;
/**
 * Builds the {@link ProfileBaseModel} consumed by the {@code profile/*} JSPs. Centralises what was
 * previously inlined inside {@code ProfileController#addUserBasics}: image id, member-since
 * formatting, document validation flags, and license / identity stored file names.
 */
@Service
public final class ProfileViewServiceImpl implements ProfileViewService {

    private static final DateTimeFormatter MEMBER_SINCE_PATTERN = DateTimeFormatter.ofPattern("LLLL uuuu");
    private static final DateTimeFormatter BIRTH_DATE_PATTERN_ES = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter BIRTH_DATE_PATTERN_EN = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private final UserService userService;
    private final StoredFileService storedFileService;

    @Autowired
    public ProfileViewServiceImpl(
            final UserService userService,
            final StoredFileService storedFileService) {
        this.userService = userService;
        this.storedFileService = storedFileService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProfileBaseModel> loadProfileBaseModel(final long userId, final Locale locale) {
        final Locale safeLocale = locale != null ? locale : Locale.getDefault();
        return userService.getUserById(userId).map(user -> buildModel(user, safeLocale));
    }

    private ProfileBaseModel buildModel(final User user, final Locale locale) {
        final Long profilePictureImageId = user.getProfilePictureId().orElse(null);
        final String memberSinceDisplay = user.getMemberSince()
                .map(ms -> ms.format(MEMBER_SINCE_PATTERN.withLocale(locale)))
                .orElse(null);
        final String birthDateDisplay = user.getBirthDate()
                .map(bd -> bd.format(birthDatePatternFor(locale)))
                .orElse(null);
        final String licenseFileName = user.getLicenseFileId()
                .flatMap(storedFileService::findById)
                .map(sf -> sf.getFileName())
                .orElse(null);
        final String identityFileName = user.getIdentityFileId()
                .flatMap(storedFileService::findById)
                .map(sf -> sf.getFileName())
                .orElse(null);
        return new ProfileBaseModel(
                user.getEmail(),
                user.getForename(),
                user.getSurname(),
                profilePictureImageId,
                memberSinceDisplay,
                birthDateDisplay,
                user.isLicenseValidated(),
                user.isIdentityValidated(),
                licenseFileName,
                identityFileName);
    }

    private static DateTimeFormatter birthDatePatternFor(final Locale locale) {
        return "es".equals(locale.getLanguage()) ? BIRTH_DATE_PATTERN_ES : BIRTH_DATE_PATTERN_EN;
    }
}
