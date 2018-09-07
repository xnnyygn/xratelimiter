package in.xnnyygn.xratelimiter.gossip;

public interface MemberEventListener {

    void onChanged(MemberEvent event);

    void onMerged();

}
