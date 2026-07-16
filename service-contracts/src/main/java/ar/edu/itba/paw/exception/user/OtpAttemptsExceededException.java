package ar.edu.itba.paw.exception.user;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.RydenException;

/** Raised when too many failed OTP attempts were recorded for an email within the lockout window. */
public final class OtpAttemptsExceededException extends RydenException {

    public OtpAttemptsExceededException() {
        super(MessageKeys.USER_OTP_ATTEMPTS_EXCEEDED);
    }
}
