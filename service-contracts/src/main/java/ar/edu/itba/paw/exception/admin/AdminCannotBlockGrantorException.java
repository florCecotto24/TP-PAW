package ar.edu.itba.paw.exception.admin;

import ar.edu.itba.paw.exception.MessageKeys;

public final class AdminCannotBlockGrantorException extends AdminException {

    public AdminCannotBlockGrantorException() {
        super(MessageKeys.ADMIN_BLOCK_CANNOT_BLOCK_GRANTOR);
    }
}
