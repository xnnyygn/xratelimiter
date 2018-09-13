package in.xnnyygn.xratelimiter;

import in.xnnyygn.xratelimiter.gossip.MemberEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@ThreadSafe
public class DefaultLimiterWeightCollector implements LimiterWeightCollector {

    private static final Logger logger = LoggerFactory.getLogger(DefaultLimiterWeightCollector.class);
    private final int round;
    private final TokenBucketRateLimiterConfig globalConfig;
    private final Set<MemberEndpoint> endpoints;
    @GuardedBy("this")
    private Map<MemberEndpoint, Double> idealWeightMap;

    public DefaultLimiterWeightCollector(int round, TokenBucketRateLimiterConfig globalConfig, Set<MemberEndpoint> endpoints) {
        if (endpoints.isEmpty()) {
            throw new IllegalArgumentException("endpoints cannot be empty");
        }
        this.round = round;
        this.globalConfig = globalConfig;
        this.endpoints = endpoints;
        this.idealWeightMap = new HashMap<>();
    }

    @Override
    public MultiLimiterConfig add(int round, MemberEndpoint endpoint, double idealWeight) {
        if (round != this.round || !endpoints.contains(endpoint)) {
            logger.warn("unexpected round ({} != {}) or endpoint {}", round, this.round, endpoint);
            return null;
        }
        synchronized (this) {
            idealWeightMap.put(endpoint, idealWeight);
            if (idealWeightMap.size() != endpoints.size()) {
                return null;
            }
            return buildMultiLimiterConfig();
        }
    }

    private MultiLimiterConfig buildMultiLimiterConfig() {
        double totalWeight = evaluateTotalWeight();
        Map<MemberEndpoint, Integer> quotaMap = new HashMap<>();
        for (MemberEndpoint endpoint : idealWeightMap.keySet()) {
            quotaMap.put(endpoint, (int) (idealWeightMap.get(endpoint) * globalConfig.getCapacity() / totalWeight));
        }
        return new MultiLimiterConfig(round, quotaMap, globalConfig);
    }

    private double evaluateTotalWeight() {
        double totalWeight = 0;
        for (Double idealWeight : idealWeightMap.values()) {
            totalWeight += idealWeight;
        }
        return totalWeight > 0.001 ? totalWeight : 1;
    }

}
