package in.xnnyygn.xratelimiter;

public class DefaultTokenBucketRateLimiter implements TokenBucketRateLimiter {

    private final int capacity;
    private final int refillAmount;
    private final long refillTime;
    private long refilledAt;
    private int tokens;

    public DefaultTokenBucketRateLimiter(TokenBucketRateLimiterConfig config) {
        this(config.getCapacity(), config.getRefillAmount(), config.getRefillTime(), config.getInitialTokens());
    }

    public DefaultTokenBucketRateLimiter(int capacity, int refillAmount, long refillTime, int initialTokens) {
        this.capacity = capacity;
        this.refillAmount = refillAmount;
        this.refillTime = refillTime;
        this.refilledAt = System.currentTimeMillis();
        this.tokens = Math.min(initialTokens, capacity);
    }

    @Override
    public boolean take(int n) {
        refill();
        if (tokens >= n) {
            tokens -= n;
            return true;
        }
        return false;
    }

    private int refill() {
        long now = System.currentTimeMillis();
        int delta = (int) ((now - refilledAt) * refillAmount / refillTime);
        if (delta > 0) {
            tokens = Math.min(tokens + delta, capacity);
            refilledAt = now;
        }
        return tokens;
    }

    public int getTokens() {
        return refill();
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

}
