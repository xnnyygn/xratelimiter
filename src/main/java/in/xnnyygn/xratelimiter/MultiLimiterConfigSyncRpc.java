package in.xnnyygn.xratelimiter;

import in.xnnyygn.xratelimiter.rpc.messages.AbstractMessage;

public class MultiLimiterConfigSyncRpc extends AbstractMessage {

    private final int round;

    public MultiLimiterConfigSyncRpc(int round) {
        this.round = round;
    }

    public int getRound() {
        return round;
    }

    @Override
    public String toString() {
        return "MultiLimiterConfigSyncRpc{" +
                "round=" + round +
                '}';
    }

}
