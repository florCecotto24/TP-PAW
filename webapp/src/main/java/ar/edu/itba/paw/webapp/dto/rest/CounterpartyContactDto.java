package ar.edu.itba.paw.webapp.dto.rest;

import javax.ws.rs.core.UriInfo;

import ar.edu.itba.paw.models.domain.user.User;

/**
 * Contact details for a reservation counterparty, exposed only to the other participant.
 */
public final class CounterpartyContactDto {

    private String forename;
    private String surname;
    private String email;
    private String phoneNumber;
    private LinksDto links;

    public CounterpartyContactDto() {
    }

    public static CounterpartyContactDto from(final User user, final UriInfo uriInfo) {
        final CounterpartyContactDto dto = new CounterpartyContactDto();
        dto.forename = user.getForename();
        dto.surname = user.getSurname();
        dto.email = user.getEmail();
        dto.phoneNumber = user.getPhoneNumber().orElse(null);
        dto.links = UserLinks.buildCounterparty(user, uriInfo);
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

    public LinksDto getLinks() {
        return links;
    }

    public void setLinks(final LinksDto links) {
        this.links = links;
    }
}
