package ar.edu.itba.paw.exception;

public abstract class RydenException extends RuntimeException {

    private final String messageCode;
    private final Object[] args;

    protected RydenException(final String messageCode, final Object... args) {
        super(messageCode);
        this.messageCode = messageCode;
        this.args = args == null ? new Object[0] : args.clone();
    }

    public final String getMessageCode() {
        return messageCode;
    }

    public final Object[] getMessageArgs() {
        return args.clone();
    }
}
