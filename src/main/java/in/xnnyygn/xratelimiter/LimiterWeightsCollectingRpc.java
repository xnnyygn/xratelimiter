package in.xnnyygn.xratelimiter;

import in.xnnyygn.xratelimiter.gossip.MemberEndpoint;
import in.xnnyygn.xratelimiter.rpc.messages.AbstractMessage;

import java.util.Map;
import java.util.Set;

public class LimiterWeightsCollectingRpc extends AbstractMessage {

    private final int round;
    private final Map<MemberEndpoint, Double> idealWeightMap;
    private final Set<MemberEndpoint> remainingEndpoints;

    public LimiterWeightsCollectingRpc(int round, Map<MemberEndpoint, Double> idealWeightMap, Set<MemberEndpoint> remainingEndpoints) {
        this.round = round;
        this.idealWeightMap = idealWeightMap;
        this.remainingEndpoints = remainingEndpoints;
    }

    public int getRound() {
        return round;
    }

    public Map<MemberEndpoint, Double> getIdealWeightMap() {
        return idealWeightMap;
    }

    public Set<MemberEndpoint> getRemainingEndpoints() {
        return remainingEndpoints;
    }

    @Override
    public String toString() {
        return "LimiterWeightsCollectingRpc{" +
                "round=" + round +
                ", idealWeightMap=" + idealWeightMap +
                ", remainingEndpoints=" + remainingEndpoints +
                '}';
    }

}
