package ar.edu.itba.paw.exception.listing;

import ar.edu.itba.paw.exception.MessageKeys;

public class DuplicatePlateException extends ListingValidationException {

    public DuplicatePlateException(final String plate) {
        super(MessageKeys.CAR_PLATE_ALREADY_EXISTS, plate);
    }
}
