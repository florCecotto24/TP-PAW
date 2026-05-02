package ar.edu.itba.paw.exception.user;

import ar.edu.itba.paw.exception.MessageKeys;

public final class InvalidCbuFormatException extends UserException {

    public InvalidCbuFormatException(final int requiredDigitLength) {
        super(MessageKeys.USER_PROFILE_CBU_INVALID, requiredDigitLength);
    }
}
