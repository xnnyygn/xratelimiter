package in.xnnyygn.xratelimiter;

public interface TokenBucketRateLimiter {

    boolean take(int n);

}
