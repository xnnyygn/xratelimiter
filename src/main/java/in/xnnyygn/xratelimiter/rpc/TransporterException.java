package in.xnnyygn.xratelimiter.rpc;

public class TransporterException extends RuntimeException {

    public TransporterException(Throwable cause) {
        super(cause);
    }

    public TransporterException(String message, Throwable cause) {
        super(message, cause);
    }

}
