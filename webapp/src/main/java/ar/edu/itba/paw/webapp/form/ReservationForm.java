package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class ReservationForm {

    @NotNull
    private Long listingId;

    @NotNull
    private String fromDateTime;

    @NotNull
    private String untilDateTime;

    @NotNull
    private String deliveryLocation;

    @NotNull
    private String carName;

    @NotBlank(message = "{validation.email.notBlank}")
    @Email(message = "{validation.email.invalid}")
    @Size(max = 50, message = "{validation.email.size}")
    private String email;

    @NotBlank(message = "{validation.name.notBlank}")
    @Size(max = 50, message = "{validation.name.size}")
    private String name;

    @NotBlank(message = "{validation.surname.notBlank}")
    @Size(max = 50, message = "{validation.surname.size}")
    private String surname;

    public Long getListingId() {
        return listingId;
    }

    public void setListingId(Long listingId) {
        this.listingId = listingId;
    }

    public String getFromDateTime() {
        return fromDateTime;
    }

    public void setFromDateTime(String fromDateTime) {
        this.fromDateTime = fromDateTime;
    }

    public String getUntilDateTime() {
        return untilDateTime;
    }

    public void setUntilDateTime(String untilDateTime) {
        this.untilDateTime = untilDateTime;
    }

    public String getDeliveryLocation() {
        return deliveryLocation;
    }

    public void setDeliveryLocation(String deliveryLocation) {
        this.deliveryLocation = deliveryLocation;
    }

    public String getCarName() {
        return carName;
    }

    public void setCarName(String carName) {
        this.carName = carName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(final String surname) {
        this.surname = surname;
    }
}

