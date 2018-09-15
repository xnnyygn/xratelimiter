package in.xnnyygn.xratelimiter;

import in.xnnyygn.xratelimiter.gossip.MemberEndpoint;
import in.xnnyygn.xratelimiter.rpc.Transporter;
import in.xnnyygn.xratelimiter.rpc.messages.AbstractMessage;
import in.xnnyygn.xratelimiter.rpc.messages.RemoteMessage;

import java.util.ArrayList;
import java.util.List;

public class MockTransporter implements Transporter {

    private final List<Message> messages = new ArrayList<>();

    @Override
    public void initialize() {
    }

    @Override
    public <T extends AbstractMessage> void send(MemberEndpoint endpoint, T message) {
        messages.add(new Message(endpoint, message));
    }

    @Override
    public <M extends AbstractMessage, R extends AbstractMessage> void reply(RemoteMessage<M> remoteMessage, R response) {
        messages.add(new Message(remoteMessage.getSender(), response));
    }

    @Override
    public void close() {
    }

    public List<Message> getMessages() {
        return messages;
    }

    public int getMessageSize() {
        return messages.size();
    }

    public Message getMessage(int index) {
        return messages.get(index);
    }

    public static class Message {

        private final MemberEndpoint recipient;
        private final AbstractMessage payload;

        Message(MemberEndpoint recipient, AbstractMessage payload) {
            this.recipient = recipient;
            this.payload = payload;
        }

        public MemberEndpoint getRecipient() {
            return recipient;
        }

        public AbstractMessage getPayload() {
            return payload;
        }

        @Override
        public String toString() {
            return "Message{" +
                    "payload=" + payload +
                    ", recipient=" + recipient +
                    '}';
        }

    }

}
