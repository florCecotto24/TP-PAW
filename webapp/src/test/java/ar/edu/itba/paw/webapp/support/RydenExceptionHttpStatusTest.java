package ar.edu.itba.paw.webapp.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.car.CarNotFoundException;
import ar.edu.itba.paw.exception.car.DuplicatePlateException;
import ar.edu.itba.paw.exception.reservation.ReservationConflictException;
import ar.edu.itba.paw.exception.user.EmailAlreadyExistsException;
import ar.edu.itba.paw.exception.user.PasswordResetCodeInvalidException;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.exception.user.VerificationCodeInvalidException;

class RydenExceptionHttpStatusTest {

    @Test
    void testNotFoundSubtypesMapTo404() {
        // 1.Arrange
        final UserNotFoundException userMissing = new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND);
        final CarNotFoundException carMissing = new CarNotFoundException(1L);

        // 2.Act
        final Response.Status userStatus = RydenExceptionHttpStatus.statusFor(userMissing);
        final Response.Status carStatus = RydenExceptionHttpStatus.statusFor(carMissing);

        // 3.Assert
        assertEquals(Response.Status.NOT_FOUND, userStatus);
        assertEquals(Response.Status.NOT_FOUND, carStatus);
    }

    @Test
    void testConflictSubtypesMapTo409() {
        // 1.Arrange
        final EmailAlreadyExistsException emailTaken =
                new EmailAlreadyExistsException(MessageKeys.USER_EMAIL_ALREADY_EXISTS);
        final DuplicatePlateException duplicatePlate = new DuplicatePlateException("ABC123");
        final ReservationConflictException overlap = new ReservationConflictException(
                MessageKeys.RESERVATION_CONFLICT_OVERLAP);

        // 2.Act
        final Response.Status emailStatus = RydenExceptionHttpStatus.statusFor(emailTaken);
        final Response.Status plateStatus = RydenExceptionHttpStatus.statusFor(duplicatePlate);
        final Response.Status overlapStatus = RydenExceptionHttpStatus.statusFor(overlap);

        // 3.Assert
        assertEquals(Response.Status.CONFLICT, emailStatus);
        assertEquals(Response.Status.CONFLICT, plateStatus);
        assertEquals(Response.Status.CONFLICT, overlapStatus);
    }

    @Test
    void testOtpInvalidSubtypesMapTo401() {
        // 1.Arrange
        final VerificationCodeInvalidException verificationInvalid =
                new VerificationCodeInvalidException(MessageKeys.USER_VERIFICATION_CODE_INVALID);
        final PasswordResetCodeInvalidException resetInvalid =
                new PasswordResetCodeInvalidException(MessageKeys.USER_PASSWORD_RESET_CODE_INVALID);

        // 2.Act
        final Response.Status verificationStatus = RydenExceptionHttpStatus.statusFor(verificationInvalid);
        final Response.Status resetStatus = RydenExceptionHttpStatus.statusFor(resetInvalid);

        // 3.Assert
        assertEquals(Response.Status.UNAUTHORIZED, verificationStatus);
        assertEquals(Response.Status.UNAUTHORIZED, resetStatus);
    }
}
