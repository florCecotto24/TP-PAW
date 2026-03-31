package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.ReservationConfirmationPayload;

public interface EmailService {

    void sendReservationConfirmationEmail(ReservationConfirmationPayload payload);
}
