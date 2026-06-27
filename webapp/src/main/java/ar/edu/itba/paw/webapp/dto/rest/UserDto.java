package ar.edu.itba.paw.webapp.dto.rest;

import java.math.BigDecimal;

import javax.ws.rs.core.UriInfo;

import ar.edu.itba.paw.models.domain.user.User;

/**
 * Public REST representation ({@code application/vnd.paw.user.v1+json}).
 * Fixed schema: never includes email, CBU, phone, role, or blocked.
 */
public final class UserDto {

    private String forename;
    private String surname;
    private String memberSince;
    private Boolean emailVerified;
    private boolean licenseValidated;
    private boolean identityValidated;
    private boolean licenseUploaded;
    private boolean identityUploaded;
    private BigDecimal ratingAsRider;
    private BigDecimal ratingAsOwner;
    private LinksDto links;

    public UserDto() {
    }

    public static UserDto fromPublic(
            final User user,
            final UriInfo uriInfo,
            final BigDecimal ratingAsOwner,
            final BigDecimal ratingAsRider) {
        final UserDto dto = new UserDto();
        dto.forename = user.getForename();
        dto.surname = user.getSurname();
        dto.memberSince = user.getMemberSince().map(Object::toString).orElse(null);
        dto.emailVerified = user.getEmailValidated().orElse(Boolean.FALSE);
        dto.licenseValidated = user.isLicenseValidated();
        dto.identityValidated = user.isIdentityValidated();
        dto.licenseUploaded = user.getLicenseFileId().isPresent();
        dto.identityUploaded = user.getIdentityFileId().isPresent();
        dto.ratingAsOwner = ratingAsOwner;
        dto.ratingAsRider = ratingAsRider;
        dto.links = UserLinks.build(user, uriInfo);
        return dto;
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

    public String getMemberSince() {
        return memberSince;
    }

    public void setMemberSince(final String memberSince) {
        this.memberSince = memberSince;
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
