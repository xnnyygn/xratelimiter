package in.xnnyygn.xratelimiter;

import in.xnnyygn.xratelimiter.gossip.MemberEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoLimiterWeightCollector implements LimiterWeightCollector {

    private static final Logger logger = LoggerFactory.getLogger(NoLimiterWeightCollector.class);

    @Override
    public MultiLimiterConfig add(int round, MemberEndpoint endpoint, double idealWeight) {
        logger.debug("not collecting weights");
        return null;
    }

}
