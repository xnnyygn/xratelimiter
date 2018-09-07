package in.xnnyygn.xratelimiter.rpc;

import in.xnnyygn.xratelimiter.gossip.MemberEndpoint;
import in.xnnyygn.xratelimiter.rpc.messages.AbstractMessage;
import in.xnnyygn.xratelimiter.rpc.messages.RemoteMessage;

public interface Transporter {

    void initialize();

    <T extends AbstractMessage> void send(MemberEndpoint endpoint, T message);

    <M extends AbstractMessage, R extends AbstractMessage> void reply(RemoteMessage<M> remoteMessage, R response);

    void close();

}
