package ar.edu.itba.paw.services;

import java.time.LocalDate;
import java.util.Optional;

import ar.edu.itba.paw.models.User;

public interface UserService {

    /** @throws ar.edu.itba.paw.exception.user.EmailAlreadyExistsException if the normalized email is already registered */
    User createUser(final String email, final String forename, final String surname);

    Optional<User> findByEmail(String email);

    void setRegistrationPassword(long userId, String password, String passwordConfirm);

    Optional<User> getUserById(final long id);

    /** Loads {@link User#getPasswordHash()} for Spring Security; empty if unknown email or no password set. */
    Optional<User> findByEmailForAuthentication(final String email);

    Optional<User> getListingOwner(final long listingId);

    /**
     * Phone: digits and {@code +} only, max 20 chars. Blank input is stored as {@code null}.
     */
    void updatePhoneNumber(long userId, String phoneRaw);

    /**
     * {@code birthDate} may be {@code null} to clear. Must not be after today in {@link ar.edu.itba.paw.models.AvailabilityPeriod#WALL_ZONE}.
     */
    void updateBirthDate(long userId, LocalDate birthDate);

    /**
     * Stores a new profile picture and removes the previous image row when present.
     * @throws ar.edu.itba.paw.exception.image.ImageValidationException when payload exceeds configured max size
     */
    void updateProfilePicture(long userId, String originalFilename, String contentType, byte[] data);
}
