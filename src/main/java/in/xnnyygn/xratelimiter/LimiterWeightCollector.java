package in.xnnyygn.xratelimiter;

import in.xnnyygn.xratelimiter.gossip.MemberEndpoint;

public interface LimiterWeightCollector {

    MultiLimiterConfig add(int round, MemberEndpoint endpoint, double idealWeight);

}
