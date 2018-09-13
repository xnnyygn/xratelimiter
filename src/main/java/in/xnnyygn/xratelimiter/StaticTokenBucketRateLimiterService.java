package in.xnnyygn.xratelimiter;

import java.util.HashMap;
import java.util.Map;

public class StaticTokenBucketRateLimiterService {

    private final Map<String, TokenBucketRateLimiter> limiterMap;

    public StaticTokenBucketRateLimiterService(Map<String, TokenBucketRateLimiter> limiterMap) {
        this.limiterMap = limiterMap;
    }

    public boolean take(String key, int n) {
        TokenBucketRateLimiter limiter = limiterMap.get(key);
        return limiter != null && limiter.take(n);
    }

    public static class Builder {

        private final Map<String, TokenBucketRateLimiter> limiterMap = new HashMap<>();

        public Builder add(String key, TokenBucketRateLimiter limiter) {
            limiterMap.put(key, limiter);
            return this;
        }

        public StaticTokenBucketRateLimiterService build() {
            return new StaticTokenBucketRateLimiterService(limiterMap);
        }

    }

}
