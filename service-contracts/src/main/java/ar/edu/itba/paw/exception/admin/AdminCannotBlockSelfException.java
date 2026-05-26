package ar.edu.itba.paw.exception.admin;

import ar.edu.itba.paw.exception.MessageKeys;

public final class AdminCannotBlockSelfException extends AdminException {

    public AdminCannotBlockSelfException() {
        super(MessageKeys.ADMIN_BLOCK_CANNOT_BLOCK_SELF);
    }
}

