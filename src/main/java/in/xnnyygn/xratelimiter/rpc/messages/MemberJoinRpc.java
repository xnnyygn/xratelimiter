package in.xnnyygn.xratelimiter.rpc.messages;

import in.xnnyygn.xratelimiter.gossip.MemberEndpoint;

public class MemberJoinRpc extends AbstractMessage {

    private final MemberEndpoint endpoint;
    private final long timeJoined;

    public MemberJoinRpc(MemberEndpoint endpoint, long timeJoined) {
        this.endpoint = endpoint;
        this.timeJoined = timeJoined;
    }

    public MemberEndpoint getEndpoint() {
        return endpoint;
    }

    public long getTimeJoined() {
        return timeJoined;
    }

    @Override
    public String toString() {
        return "MemberJoinRpc{" +
                "endpoint=" + endpoint +
                ", timeJoined=" + timeJoined +
                '}';
    }

}
