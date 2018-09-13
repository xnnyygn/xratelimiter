package in.xnnyygn.xratelimiter;

public class SynchronizedTokenBucketRateLimiter implements TokenBucketRateLimiter {

    private final TokenBucketRateLimiter delegate;

    public SynchronizedTokenBucketRateLimiter(TokenBucketRateLimiter delegate) {
        this.delegate = delegate;
    }

    @Override
    public synchronized boolean take(int n) {
        return delegate.take(n);
    }

}
