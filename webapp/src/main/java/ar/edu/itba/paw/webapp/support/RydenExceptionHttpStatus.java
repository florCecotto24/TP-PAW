package ar.edu.itba.paw.webapp.support;

import javax.ws.rs.core.Response;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.RydenException;
import ar.edu.itba.paw.exception.car.CarBrandConflictException;
import ar.edu.itba.paw.exception.car.CarBrandNotFoundException;
import ar.edu.itba.paw.exception.car.CarModelConflictException;
import ar.edu.itba.paw.exception.car.CarModelNotFoundException;
import ar.edu.itba.paw.exception.car.CarNotFoundException;
import ar.edu.itba.paw.exception.car.CarPublishPrerequisitesMissingException;
import ar.edu.itba.paw.exception.car.CarStatusTransitionConflictException;
import ar.edu.itba.paw.exception.car.DuplicatePlateException;
import ar.edu.itba.paw.exception.reservation.ReservationAccessDeniedException;
import ar.edu.itba.paw.exception.reservation.ReservationCancelNotAllowedException;
import ar.edu.itba.paw.exception.reservation.ReservationConflictException;
import ar.edu.itba.paw.exception.reservation.ReservationMessageException;
import ar.edu.itba.paw.exception.reservation.RiderReservationException;
import ar.edu.itba.paw.exception.user.EmailAlreadyExistsException;
import ar.edu.itba.paw.exception.user.OtpAttemptsExceededException;
import ar.edu.itba.paw.exception.user.PasswordResetCodeInvalidException;
import ar.edu.itba.paw.exception.user.UserForbiddenException;
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
                || exception instanceof CarBrandConflictException
                || exception instanceof CarModelConflictException
                || exception instanceof CarStatusTransitionConflictException
                || exception instanceof ReservationConflictException
                || exception instanceof ReservationCancelNotAllowedException) {
            return Response.Status.CONFLICT;
        }
        if (exception instanceof VerificationCodeInvalidException
                || exception instanceof PasswordResetCodeInvalidException) {
            return Response.Status.UNAUTHORIZED;
        }
        if (exception instanceof OtpAttemptsExceededException) {
            return Response.Status.TOO_MANY_REQUESTS;
        }
        if (exception instanceof ReservationAccessDeniedException
                || exception instanceof CarPublishPrerequisitesMissingException
                || exception instanceof UserForbiddenException) {
            return Response.Status.FORBIDDEN;
        }
        if (exception instanceof ReservationMessageException messageException) {
            if (MessageKeys.RESERVATION_CHAT_NOT_PARTICIPANT.equals(messageException.getMessageCode())) {
                return Response.Status.FORBIDDEN;
            }
            if (MessageKeys.RESERVATION_CHAT_ATTACHMENT_NOT_FOUND.equals(messageException.getMessageCode())) {
                return Response.Status.NOT_FOUND;
            }
        }
        if (exception instanceof RiderReservationException riderException) {
            final String code = riderException.getMessageCode();
            if (MessageKeys.REVIEW_ALREADY_SUBMITTED.equals(code)
                    || MessageKeys.RESERVATION_PAYMENT_RECEIPT_CONFLICT.equals(code)
                    || MessageKeys.RESERVATION_REFUND_RECEIPT_CONFLICT.equals(code)
                    || MessageKeys.RESERVATION_PAYMENT_PROOF_DEADLINE_PASSED.equals(code)) {
                return Response.Status.CONFLICT;
            }
            if (MessageKeys.RESERVATION_RIDER_LISTING_NOT_FOUND.equals(code)
                    || MessageKeys.RESERVATION_RIDER_USER_NOT_FOUND.equals(code)
                    || MessageKeys.RESERVATION_PAYMENT_RECEIPT_NOT_FOUND.equals(code)) {
                return Response.Status.NOT_FOUND;
            }
        }
        return Response.Status.BAD_REQUEST;
    }
}
