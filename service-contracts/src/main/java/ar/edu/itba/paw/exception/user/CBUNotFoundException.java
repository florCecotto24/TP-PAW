package ar.edu.itba.paw.exception.user;

public class CBUNotFoundException extends RuntimeException {
    public CBUNotFoundException(long userId) {
        super("CBU not found for userId = " + userId);
    }
}
