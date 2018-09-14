package in.xnnyygn.xratelimiter;

import in.xnnyygn.xratelimiter.gossip.MemberEndpoint;
import in.xnnyygn.xratelimiter.rpc.messages.AbstractMessage;

import java.util.Collections;
import java.util.Map;

public class MultiLimiterConfigSyncResponse extends AbstractMessage {

    private final int round;
    private final Map<MemberEndpoint, TokenBucketRateLimiterConfig> configMap;

    public MultiLimiterConfigSyncResponse(int round) {
        this(round, Collections.emptyMap());
    }

    public MultiLimiterConfigSyncResponse(int round, Map<MemberEndpoint, TokenBucketRateLimiterConfig> configMap) {
        this.round = round;
        this.configMap = configMap;
    }

    public int getRound() {
        return round;
    }

    public Map<MemberEndpoint, TokenBucketRateLimiterConfig> getConfigMap() {
        return configMap;
    }

}
