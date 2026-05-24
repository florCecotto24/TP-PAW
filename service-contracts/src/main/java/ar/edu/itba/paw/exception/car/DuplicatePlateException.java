package ar.edu.itba.paw.exception.car;

import ar.edu.itba.paw.exception.MessageKeys;

public class DuplicatePlateException extends CarValidationException {

    public DuplicatePlateException(final String plate) {
        super(MessageKeys.CAR_PLATE_ALREADY_EXISTS, plate);
    }
}
