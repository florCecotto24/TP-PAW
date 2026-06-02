package ar.edu.itba.paw.exception.admin;

import ar.edu.itba.paw.exception.MessageKeys;

public final class UserAlreadyAdminException extends AdminException {

    public UserAlreadyAdminException() {
        super(MessageKeys.ADMIN_PROMOTE_ALREADY_ADMIN);
    }
}
