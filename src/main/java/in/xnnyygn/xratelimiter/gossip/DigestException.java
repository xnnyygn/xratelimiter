package in.xnnyygn.xratelimiter.gossip;

public class DigestException extends RuntimeException {

    public DigestException(Throwable cause) {
        super(cause);
    }

    public DigestException(String message, Throwable cause) {
        super(message, cause);
    }

}
