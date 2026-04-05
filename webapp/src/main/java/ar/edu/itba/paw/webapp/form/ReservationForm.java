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

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 50, message = "Email must be at most 50 characters")
    private String email;

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "Name must be at most 50 characters")
    private String name;

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Surname must be at most 50 characters")
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

