package ar.edu.itba.paw.services;

import java.util.Locale;

import ar.edu.itba.paw.models.ReservationConfirmationPayload;

public interface EmailService {

    void sendReservationConfirmationEmail(ReservationConfirmationPayload payload);

    void sendEmailVerificationCode(String to, String code, Locale locale);

    void sendMigratedUserPassword(String to, String plainPassword, Locale locale);

    void sendPasswordResetCode(String to, String code, Locale locale);

    void sendReservationReminderEmail(final ReservationConfirmationPayload payload);
}
