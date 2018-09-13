package in.xnnyygn.xratelimiter;

import in.xnnyygn.xratelimiter.gossip.*;
import in.xnnyygn.xratelimiter.rpc.DefaultTransporter;
import in.xnnyygn.xratelimiter.rpc.Transporter;
import in.xnnyygn.xratelimiter.schedule.DefaultScheduler;
import in.xnnyygn.xratelimiter.schedule.Scheduler;
import in.xnnyygn.xratelimiter.support.MessageDispatcher;

import java.util.List;

public class StaticMemberDistributedTokenBucketRateLimiter implements TokenBucketRateLimiter {

    private final MemberEndpoint selfEndpoint;
    private final List<MemberEndpoint> endpoints;
    private final MessageDispatcher messageDispatcher = new MessageDispatcher();
    private final TokenBucketRateLimiterConfig limiterConfig;
    private final Transporter transporter;
    private final Scheduler scheduler;
    private volatile TokenBucketRateLimiter delegate;

    public StaticMemberDistributedTokenBucketRateLimiter(MemberEndpoint selfEndpoint, List<MemberEndpoint> endpoints,
                                                         TokenBucketRateLimiterConfig limiterConfig) {
        this.selfEndpoint = selfEndpoint;
        this.endpoints = endpoints;
        this.limiterConfig = limiterConfig;

        int n = endpoints.size();
        if (n > limiterConfig.getCapacity()) {
            throw new IllegalArgumentException("member count > total capacity");
        }

        this.delegate = new SynchronizedTokenBucketRateLimiter(new DefaultTokenBucketRateLimiter(limiterConfig.divide(n)));
        this.transporter = new DefaultTransporter(selfEndpoint, messageDispatcher);
        this.scheduler = new DefaultScheduler();
    }

    public void initialize() {
        transporter.initialize();
    }

    @Override
    public boolean take(int n) {
        return delegate.take(n);
    }

    public void shutdown() {
        scheduler.shutdown();
        transporter.close();
    }

}
