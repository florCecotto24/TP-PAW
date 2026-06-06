package ar.edu.itba.paw.models.dto.profile;


import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Page model populated by {@code ProfileViewService.loadProfileBaseModel} and consumed by
 * {@code ProfileController}'s {@code @ModelAttribute addUserBasics}. Centralises the values
 * that the {@code profile/*} JSPs read about the signed-in user: contact basics, profile
 * picture id, member-since display, document validation flags, and license / identity file
 * names. All optional fields are nullable; {@link #populateModel(BiConsumer)} respects the
 * original "{@code null} is not added" semantics for {@code profilePictureImageId} and the
 * two file names so existing JSP {@code <c:if>} checks continue to behave as before.
 */
public final class ProfileBaseModel {

    private final String userEmail;
    private final String userForename;
    private final String userSurname;
    private final Long profilePictureImageId;
    private final String profileMemberSinceDisplay;
    private final boolean licenseValidated;
    private final boolean identityValidated;
    private final String licenseFileName;
    private final String identityFileName;

    public ProfileBaseModel(
            final String userEmail,
            final String userForename,
            final String userSurname,
            final Long profilePictureImageId,
            final String profileMemberSinceDisplay,
            final boolean licenseValidated,
            final boolean identityValidated,
            final String licenseFileName,
            final String identityFileName) {
        this.userEmail = Objects.requireNonNull(userEmail, "userEmail");
        this.userForename = Objects.requireNonNull(userForename, "userForename");
        this.userSurname = Objects.requireNonNull(userSurname, "userSurname");
        this.profilePictureImageId = profilePictureImageId;
        this.profileMemberSinceDisplay = profileMemberSinceDisplay;
        this.licenseValidated = licenseValidated;
        this.identityValidated = identityValidated;
        this.licenseFileName = licenseFileName;
        this.identityFileName = identityFileName;
    }

    public String getUserEmail() { return userEmail; }

    public String getUserForename() { return userForename; }

    public String getUserSurname() { return userSurname; }

    public Optional<Long> getProfilePictureImageId() {
        return Optional.ofNullable(profilePictureImageId);
    }

    public Optional<String> getProfileMemberSinceDisplay() {
        return Optional.ofNullable(profileMemberSinceDisplay);
    }

    public boolean isLicenseValidated() { return licenseValidated; }

    public boolean isIdentityValidated() { return identityValidated; }

    public Optional<String> getLicenseFileName() {
        return Optional.ofNullable(licenseFileName);
    }

    public Optional<String> getIdentityFileName() {
        return Optional.ofNullable(identityFileName);
    }

    public void populateModel(final BiConsumer<String, Object> sink) {
        sink.accept("userEmail", userEmail);
        sink.accept("userForename", userForename);
        sink.accept("userSurname", userSurname);
        if (profilePictureImageId != null) {
            sink.accept("profilePictureImageId", profilePictureImageId);
        }
        if (profileMemberSinceDisplay != null) {
            sink.accept("profileMemberSinceDisplay", profileMemberSinceDisplay);
        }
        sink.accept("licenseValidated", licenseValidated);
        sink.accept("identityValidated", identityValidated);
        if (licenseFileName != null) {
            sink.accept("licenseFileName", licenseFileName);
        }
        if (identityFileName != null) {
            sink.accept("identityFileName", identityFileName);
        }
    }
}
