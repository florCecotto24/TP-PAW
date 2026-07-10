package ar.edu.itba.paw.webapp.support;

import javax.ws.rs.core.Response;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.RydenException;
import ar.edu.itba.paw.exception.car.CarBrandNotFoundException;
import ar.edu.itba.paw.exception.car.CarModelNotFoundException;
import ar.edu.itba.paw.exception.car.CarNotFoundException;
import ar.edu.itba.paw.exception.car.CarPublishPrerequisitesMissingException;
import ar.edu.itba.paw.exception.car.DuplicatePlateException;
import ar.edu.itba.paw.exception.reservation.ReservationAccessDeniedException;
import ar.edu.itba.paw.exception.reservation.ReservationCancelNotAllowedException;
import ar.edu.itba.paw.exception.reservation.ReservationConflictException;
import ar.edu.itba.paw.exception.reservation.ReservationMessageException;
import ar.edu.itba.paw.exception.user.EmailAlreadyExistsException;
import ar.edu.itba.paw.exception.user.PasswordResetCodeInvalidException;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.exception.user.VerificationCodeInvalidException;

/**
 * Maps domain {@link RydenException} subtypes to HTTP status codes for JAX-RS mappers.
 */
public final class RydenExceptionHttpStatus {

    private RydenExceptionHttpStatus() {
    }

    public static Response.Status statusFor(final RydenException exception) {
        if (exception instanceof UserNotFoundException
                || exception instanceof CarBrandNotFoundException
                || exception instanceof CarModelNotFoundException
                || exception instanceof CarNotFoundException) {
            return Response.Status.NOT_FOUND;
        }
        if (exception instanceof EmailAlreadyExistsException
                || exception instanceof DuplicatePlateException
                || exception instanceof ReservationConflictException
                || exception instanceof ReservationCancelNotAllowedException) {
            return Response.Status.CONFLICT;
        }
        if (exception instanceof VerificationCodeInvalidException
                || exception instanceof PasswordResetCodeInvalidException) {
            return Response.Status.UNAUTHORIZED;
        }
        if (exception instanceof ReservationAccessDeniedException
                || exception instanceof CarPublishPrerequisitesMissingException) {
            return Response.Status.FORBIDDEN;
        }
        if (exception instanceof ReservationMessageException messageException) {
            if (MessageKeys.RESERVATION_CHAT_NOT_PARTICIPANT.equals(messageException.getMessageCode())) {
                return Response.Status.FORBIDDEN;
            }
        }
        return Response.Status.BAD_REQUEST;
    }
}
