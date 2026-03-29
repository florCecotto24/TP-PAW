package ar.edu.itba.paw.services;

import java.time.OffsetDateTime;

public interface EmailService {

    void notifyListingOwnerNewReservation(String ownerEmail, String listingTitle, long reservationId, String riderEmail);

    void notifyRiderReservationDetails(
            String riderEmail,
            String riderFullName,
            long reservationId,
            String listingTitle,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            String deliveryLocation,
            String ownerName,
            String ownerEmail);
}
