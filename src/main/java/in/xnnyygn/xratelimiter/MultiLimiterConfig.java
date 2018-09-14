package in.xnnyygn.xratelimiter;

import in.xnnyygn.xratelimiter.gossip.MemberEndpoint;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MultiLimiterConfig {

    private final int round;
    private final Map<MemberEndpoint, TokenBucketRateLimiterConfig> configMap;

    public MultiLimiterConfig(int round, Map<MemberEndpoint, TokenBucketRateLimiterConfig> configMap) {
        this.round = round;
        this.configMap = configMap;
    }

    public static MultiLimiterConfig fromEndpoints(int round, Collection<MemberEndpoint> endpoints, TokenBucketRateLimiterConfig globalConfig) {
        if (endpoints.isEmpty()) {
            throw new IllegalArgumentException("endpoints is empty");
        }
        TokenBucketRateLimiterConfig config = globalConfig.divide(endpoints.size());
        Map<MemberEndpoint, TokenBucketRateLimiterConfig> map = new HashMap<>();
        for (MemberEndpoint endpoint : endpoints) {
            map.put(endpoint, config);
        }
        return new MultiLimiterConfig(round, map);
    }

    public static MultiLimiterConfig fromIdealWeights(int round, Map<MemberEndpoint, Double> idealWeightMap, TokenBucketRateLimiterConfig globalConfig) {
        double totalWeight = evaluateTotalWeight(idealWeightMap.values());
        Map<MemberEndpoint, TokenBucketRateLimiterConfig> quotaMap = new HashMap<>();
        double rate;
        for (MemberEndpoint endpoint : idealWeightMap.keySet()) {
            rate = idealWeightMap.get(endpoint) / totalWeight;
            quotaMap.put(endpoint, new TokenBucketRateLimiterConfig(
                    (int) (globalConfig.getCapacity() * rate),
                    (int) (globalConfig.getRefillAmount() * rate),
                    globalConfig.getRefillTime(),
                    0
            ));
        }
        return new MultiLimiterConfig(round, quotaMap);
    }

    private static double evaluateTotalWeight(Collection<Double> weights) {
        double totalWeight = 0;
        for (Double idealWeight : weights) {
            totalWeight += idealWeight;
        }
        return totalWeight > 0.001 ? totalWeight : 1;
    }

    public int getRound() {
        return round;
    }

    public Map<MemberEndpoint, TokenBucketRateLimiterConfig> getConfigMap() {
        return configMap;
    }

    public TokenBucketRateLimiterConfig getConfig(MemberEndpoint endpoint) {
        return configMap.get(endpoint);
    }

    @Override
    public String toString() {
        return "MultiLimiterConfig{" +
                "round=" + round +
                ", configMap=" + configMap +
                '}';
    }

}
