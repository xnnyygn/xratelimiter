package in.xnnyygn.xratelimiter;

public class TokenBucketRateLimiterConfig {

    private final int capacity;
    private final int refillAmount;
    private final long refillTime;
    private final int initialTokens;

    public TokenBucketRateLimiterConfig(int capacity, int refillAmount, long refillTime, int initialTokens) {
        this.capacity = capacity;
        this.refillAmount = refillAmount;
        this.refillTime = refillTime;
        this.initialTokens = initialTokens;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getRefillAmount() {
        return refillAmount;
    }

    public long getRefillTime() {
        return refillTime;
    }

    public int getInitialTokens() {
        return initialTokens;
    }

    public TokenBucketRateLimiterConfig divide(int n) {
        return new TokenBucketRateLimiterConfig(
                capacity / n,
                refillAmount,
                refillTime * n,
                initialTokens / n
        );
    }

}
