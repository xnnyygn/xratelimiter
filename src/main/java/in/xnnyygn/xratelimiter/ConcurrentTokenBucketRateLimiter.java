package in.xnnyygn.xratelimiter;

import java.util.concurrent.atomic.AtomicLong;

public class ConcurrentTokenBucketRateLimiter implements TokenBucketRateLimiter {

    private static final int TIMESTAMP_OFFSET = 22;
    private static final long TOKENS_MASK = (1 << 22) - 1;

    private final int capacity;
    private final int refillAmount;
    private final long refillTime;
    private final long epoch;

    private final AtomicLong atomicTimestampAndTokens;

    public ConcurrentTokenBucketRateLimiter(TokenBucketRateLimiterConfig config) {
        this(config.getCapacity(), config.getRefillAmount(), config.getRefillTime(), config.getInitialTokens());
    }

    public ConcurrentTokenBucketRateLimiter(int capacity, int refillAmount, long refillTime, int initialTokens) {
        this.capacity = capacity;
        this.refillAmount = refillAmount;
        this.refillTime = refillTime;
        epoch = System.currentTimeMillis();
        atomicTimestampAndTokens = new AtomicLong(Math.min(initialTokens, capacity));
    }

    @Override
    public boolean take(int n) {
        boolean result;
        long timestampAndTokens;
        long refilledAt;
        int tokens;
        long now;
        int delta;
        do {
            result = false;
            timestampAndTokens = atomicTimestampAndTokens.get();
            refilledAt = timestampAndTokens >> TIMESTAMP_OFFSET;
            tokens = (int) (timestampAndTokens & TOKENS_MASK);
            now = System.currentTimeMillis() - epoch;
            delta = (int) ((now - refilledAt) * refillAmount / refillTime);
            if (delta == 0 && tokens < n) {
                return false;
            }
            if (delta > 0) {
                tokens = Math.min(tokens + delta, capacity);
                refilledAt = now;
            }
            if (tokens >= n) {
                tokens -= n;
                result = true;
            }
        } while (!atomicTimestampAndTokens.compareAndSet(
                timestampAndTokens, ((refilledAt << TIMESTAMP_OFFSET) | tokens)));
        return result;
    }

    public int getTokens() {
        return (int) (atomicTimestampAndTokens.get() & TOKENS_MASK);
    }

}
