package ar.edu.itba.paw.services;

public interface AdminService {

    /**
     * Promotes a normal user to an ADMIN.
     * @param targetUserId The ID of the user to promote.
     */
    void promoteToAdmin(long adminId, long targetUserId);

    /**
     * Creates a new user directly with the ADMIN role and sends them an invitation email containing their password.
     */
    void createAdmin(long adminId, String email, String forename, String surname, String rawPassword);

    /**
     * Blocks a user, preventing them from logging in, pausing all their active car listings,
     * and cancelling their upcoming reservations as a rider.
     * Throws an exception if the target user is the one who granted ADMIN status to the acting admin.
     */
    void blockUser(long adminId, long targetUserId);

    /**
     * Pauses a given car listing with the ADMIN_PAUSED status.
     */
    void pauseCar(long adminId, long carId);

    /**
     * Resumes a given car listing from the ADMIN_PAUSED status back to ACTIVE.
     */
    void resumeCar(long adminId, long carId);

}
