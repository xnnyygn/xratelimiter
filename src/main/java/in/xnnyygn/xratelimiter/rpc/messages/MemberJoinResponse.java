package in.xnnyygn.xratelimiter.rpc.messages;

import in.xnnyygn.xratelimiter.gossip.Member;

import javax.annotation.Nonnull;
import java.util.Collection;

public class MemberJoinResponse extends AbstractMessage {

    private final Collection<Member> members;

    public MemberJoinResponse(@Nonnull Collection<Member> members) {
        super();
        this.members = members;
    }

    public Collection<Member> getMembers() {
        return members;
    }

    @Override
    public String toString() {
        return "MemberJoinResponse{" +
                "members.size=" + members.size() +
                '}';
    }
}
