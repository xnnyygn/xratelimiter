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
        if (totalWeight == 0.0) {
            totalWeight = 1;
        }
        Map<MemberEndpoint, TokenBucketRateLimiterConfig> quotaMap = new HashMap<>();
        double rate;
        int capacity;
        for (MemberEndpoint endpoint : idealWeightMap.keySet()) {
            rate = idealWeightMap.get(endpoint) / totalWeight;
            capacity = (int) (globalConfig.getCapacity() * rate);
            quotaMap.put(endpoint, new TokenBucketRateLimiterConfig(
                    capacity,
                    globalConfig.getRefillAmount(),
                    capacity != 0 ? (long) (globalConfig.getRefillTime() / rate) : globalConfig.getRefillTime(),
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
        return totalWeight;
    }

    public int getRound() {
        return round;
    }

    public Map<MemberEndpoint, TokenBucketRateLimiterConfig> getConfigMap() {
        return configMap;
    }

    public TokenBucketRateLimiterConfig getConfig(MemberEndpoint endpoint) {
        TokenBucketRateLimiterConfig config = configMap.get(endpoint);
        if (config == null) {
            throw new IllegalStateException("no config for " + endpoint);
        }
        return config;
    }

    @Override
    public String toString() {
        return "MultiLimiterConfig{" +
                "round=" + round +
                ", configMap=" + configMap +
                '}';
    }

}
