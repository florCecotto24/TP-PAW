package ar.edu.itba.paw.services.user;


import java.util.List;
import java.util.Locale;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.reservation.ReservationCard;
import ar.edu.itba.paw.models.dto.reservation.ReservationMessageDto;
import ar.edu.itba.paw.models.util.search.ReservationSearchCriteria;

/** Admin operations: user management, car moderation, catalog validation, and reservation inspection. */
public interface AdminService {

    void promoteUserToAdmin(long targetUserId, long assignedByUserId);

    void demoteUserFromAdmin(long targetUserId, long assignedByUserId);

    /**
     * Provisions a pre-verified admin. A random unusable password hash is stored; the invitee
     * sets their password via the existing password-reset OTP email (never mailed in clear).
     */
    User createAdminUser(String email, String forename, String surname,
                         long assignedByUserId, Locale locale);

    void blockUser(long targetUserId, long actingAdminId);

    void unblockUser(long targetUserId, long actingAdminId);

    /**
     * Deletes a user account. Applies the same grantor/self guards as {@link #blockUser}.
     */
    void deleteUser(long targetUserId, long actingAdminId);

    void adminPauseCar(long carId, long actingAdminId, Locale locale);

    void adminResumeCar(long carId, long actingAdminId);

    void validateCarBrand(long brandId);

    void rejectCarBrand(long brandId);

    /**
     * Validates a pending catalog entry: validates the model and, if its brand is also pending, validates the brand.
     * Notifies by email each affected car owner indicating whether only the model or both brand and model were validated.
     * {@code locale} is the fallback request locale.
     */
    void validateCatalogEntry(long modelId, Locale locale);

    /**
     * Rejects a pending catalog entry: removes the model and, if its brand is pending and has no remaining models,
     * removes the brand too. Notifies by email each affected car owner indicating whether the rejection targeted
     * only the model or both brand and model. {@code locale} is the fallback request locale.
     */
    void rejectCatalogEntry(long modelId, Locale locale);

    Page<User> listUsers(int page, int pageSize, Boolean blocked, String role, String query);

    Page<Car> listCars(int page, int pageSize);

    Page<ReservationCard> listAllReservations(ReservationSearchCriteria criteria);

    Optional<Reservation> getReservationById(long reservationId);

    List<ReservationMessageDto> getAdminChatMessages(long reservationId, int offset, int limit);

    List<ReservationMessageDto> getChatMessagesAfter(long reservationId, long afterMessageId, int limit);

    long countReservationMessages(long reservationId);
}
