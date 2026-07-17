package ar.edu.itba.paw.webapp.dto.rest;

import java.math.BigDecimal;
import java.util.Locale;

import javax.ws.rs.core.UriInfo;

import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.security.UserRole;
import ar.edu.itba.paw.webapp.util.RestUriUtils;

/**
 * Private REST representation ({@code application/vnd.paw.user.private.v1+json}).
 * Fixed schema: always includes email, CBU, phone, role, blocked, etc.
 * Only returned when the caller is self or admin and requests this MIME type.
 */
public final class UserPrivateDto {

    private String forename;
    private String surname;
    private String email;
    private String phoneNumber;
    private String birthDate;
    private String about;
    private String cbu;
    private String memberSince;
    private String latestLocale;
    private Boolean emailVerified;
    private boolean licenseValidated;
    private boolean identityValidated;
    private boolean licenseUploaded;
    private boolean identityUploaded;
    private boolean blocked;
    private String role;
    private BigDecimal ratingAsRider;
    private BigDecimal ratingAsOwner;
    private LinksDto links;

    public UserPrivateDto() {
    }

    public static Builder builder(final User user, final UriInfo uriInfo) {
        return new Builder(user, uriInfo);
    }

    public static final class Builder {

        private final User user;
        private final UriInfo uriInfo;
        private BigDecimal ratingAsOwner;
        private BigDecimal ratingAsRider;
        private String blockedOverdueReservationUri;

        private Builder(final User user, final UriInfo uriInfo) {
            this.user = user;
            this.uriInfo = uriInfo;
        }

        public Builder ratings(final BigDecimal ratingAsOwner, final BigDecimal ratingAsRider) {
            this.ratingAsOwner = ratingAsOwner;
            this.ratingAsRider = ratingAsRider;
            return this;
        }

        /**
         * When the account is blocked and exactly one reservation has an overdue refund proof, its
         * canonical reservation URI (deep-link target for the blocked banner CTA).
         */
        public Builder blockedOverdueReservationUri(final String blockedOverdueReservationUri) {
            this.blockedOverdueReservationUri = blockedOverdueReservationUri;
            return this;
        }

        public UserPrivateDto build() {
            final UserPrivateDto dto = new UserPrivateDto();
            dto.forename = user.getForename();
            dto.surname = user.getSurname();
            dto.email = user.getEmail();
            dto.phoneNumber = user.getPhoneNumber().orElse(null);
            dto.birthDate = user.getBirthDate().map(Object::toString).orElse(null);
            dto.about = user.getAbout().orElse(null);
            dto.cbu = user.getCbu().orElse(null);
            dto.memberSince = user.getMemberSince().map(Object::toString).orElse(null);
            dto.latestLocale = user.getLatestLocale().map(Locale::toLanguageTag).orElse(null);
            dto.emailVerified = user.getEmailValidated().orElse(Boolean.FALSE);
            dto.licenseValidated = user.isLicenseValidated();
            dto.identityValidated = user.isIdentityValidated();
            dto.licenseUploaded = user.getLicenseFileId().isPresent();
            dto.identityUploaded = user.getIdentityFileId().isPresent();
            dto.blocked = user.isBlocked();
            dto.role = user.getUserRole() == null
                    ? UserRole.USER.name().toLowerCase(Locale.ROOT)
                    : user.getUserRole().name().toLowerCase(Locale.ROOT);
            dto.ratingAsOwner = ratingAsOwner;
            dto.ratingAsRider = ratingAsRider;
            dto.links = UserLinks.build(user, uriInfo)
                    .withRelated("identityDocument",
                            RestUriUtils.userDocumentUri(uriInfo, user.getId(), "identity").toString())
                    .withRelated("licenseDocument",
                            RestUriUtils.userDocumentUri(uriInfo, user.getId(), "license").toString());
            if (user.isBlocked() && blockedOverdueReservationUri != null) {
                dto.links = dto.links.withRelated("blocked-overdue-reservation", blockedOverdueReservationUri);
            }
            return dto;
        }
    }

    public String getForename() {
        return forename;
    }

    public void setForename(final String forename) {
        this.forename = forename;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(final String surname) {
        this.surname = surname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(final String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(final String birthDate) {
        this.birthDate = birthDate;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(final String about) {
        this.about = about;
    }

    public String getCbu() {
        return cbu;
    }

    public void setCbu(final String cbu) {
        this.cbu = cbu;
    }

    public String getMemberSince() {
        return memberSince;
    }

    public void setMemberSince(final String memberSince) {
        this.memberSince = memberSince;
    }

    public String getLatestLocale() {
        return latestLocale;
    }

    public void setLatestLocale(final String latestLocale) {
        this.latestLocale = latestLocale;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(final Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public boolean isLicenseValidated() {
        return licenseValidated;
    }

    public void setLicenseValidated(final boolean licenseValidated) {
        this.licenseValidated = licenseValidated;
    }

    public boolean isIdentityValidated() {
        return identityValidated;
    }

    public void setIdentityValidated(final boolean identityValidated) {
        this.identityValidated = identityValidated;
    }

    public boolean isLicenseUploaded() {
        return licenseUploaded;
    }

    public void setLicenseUploaded(final boolean licenseUploaded) {
        this.licenseUploaded = licenseUploaded;
    }

    public boolean isIdentityUploaded() {
        return identityUploaded;
    }

    public void setIdentityUploaded(final boolean identityUploaded) {
        this.identityUploaded = identityUploaded;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(final boolean blocked) {
        this.blocked = blocked;
    }

    public String getRole() {
        return role;
    }

    public void setRole(final String role) {
        this.role = role;
    }

    public BigDecimal getRatingAsRider() {
        return ratingAsRider;
    }

    public void setRatingAsRider(final BigDecimal ratingAsRider) {
        this.ratingAsRider = ratingAsRider;
    }

    public BigDecimal getRatingAsOwner() {
        return ratingAsOwner;
    }

    public void setRatingAsOwner(final BigDecimal ratingAsOwner) {
        this.ratingAsOwner = ratingAsOwner;
    }

    public LinksDto getLinks() {
        return links;
    }

    public void setLinks(final LinksDto links) {
        this.links = links;
    }
}
