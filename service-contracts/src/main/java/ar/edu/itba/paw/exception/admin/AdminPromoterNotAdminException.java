package ar.edu.itba.paw.exception.admin;

import ar.edu.itba.paw.exception.MessageKeys;

public final class AdminPromoterNotAdminException extends AdminException {

    public AdminPromoterNotAdminException() {
        super(MessageKeys.ADMIN_PROMOTE_NOT_ADMIN);
    }
}
