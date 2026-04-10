package ar.edu.itba.paw.services;

import java.util.Locale;

import ar.edu.itba.paw.models.ReservationConfirmationPayload;

public interface EmailService {

    void sendReservationConfirmationEmail(ReservationConfirmationPayload payload);

    void sendEmailVerificationCode(String to, String code, Locale locale);
}
