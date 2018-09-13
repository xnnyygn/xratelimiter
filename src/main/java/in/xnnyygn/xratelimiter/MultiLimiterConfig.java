package in.xnnyygn.xratelimiter;

import in.xnnyygn.xratelimiter.gossip.MemberEndpoint;

import java.util.Map;

public class MultiLimiterConfig {

    private final int round;
    private final Map<MemberEndpoint, Integer> quotaMap;
    private final TokenBucketRateLimiterConfig globalConfig;

    public MultiLimiterConfig(int round, Map<MemberEndpoint, Integer> quotaMap, TokenBucketRateLimiterConfig globalConfig) {
        this.round = round;
        this.quotaMap = quotaMap;
        this.globalConfig = globalConfig;
    }

    public int getRound() {
        return round;
    }

    public Map<MemberEndpoint, Integer> getQuotaMap() {
        return quotaMap;
    }

    public TokenBucketRateLimiterConfig getGlobalConfig() {
        return globalConfig;
    }

    public Integer getQuota(MemberEndpoint endpoint) {
        return quotaMap.get(endpoint);
    }

    @Override
    public String toString() {
        return "MultiLimiterConfig{" +
                "round=" + round +
                ", globalConfig=" + globalConfig +
                ", quotaMap=" + quotaMap +
                '}';
    }

}
