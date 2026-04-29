package ar.edu.itba.paw.exception.user;

public final class CBUNotFoundException extends RuntimeException {
    public CBUNotFoundException(long userId) {
        super("CBU not found for userId = " + userId);
    }
}
