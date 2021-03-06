package in.xnnyygn.xratelimiter.rpc.messages;

import in.xnnyygn.xratelimiter.gossip.MemberEndpoint;

public class RemoteMessage<T extends AbstractMessage> {

    private final T message;
    private final MemberEndpoint sender;

    public RemoteMessage(T message, MemberEndpoint sender) {
        this.message = message;
        this.sender = sender;
    }

    public Class<? extends AbstractMessage> getPayloadClass() {
        return message.getClass();
    }

    public T get() {
        return message;
    }

    public MemberEndpoint getSender() {
        return sender;
    }

    @Override
    public String toString() {
        return "RemoteMessage{" +
                "sender=" + sender +
                ", message=" + message +
                '}';
    }

}
