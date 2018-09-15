package in.xnnyygn.xratelimiter;

import in.xnnyygn.xratelimiter.gossip.MemberEndpoint;
import in.xnnyygn.xratelimiter.rpc.Transporter;
import in.xnnyygn.xratelimiter.schedule.Scheduler;
import in.xnnyygn.xratelimiter.support.MessageDispatcher;

import java.util.Set;

public class DistributedTokenBucketRateLimiterArguments {

    private final MessageDispatcher messageDispatcher = new MessageDispatcher();
    private final MemberEndpoint selfEndpoint;
    private final Set<MemberEndpoint> endpoints;
    private final TokenBucketRateLimiterConfig globalConfig;
    private Scheduler scheduler;
    private Transporter transporter;

    public DistributedTokenBucketRateLimiterArguments(MemberEndpoint selfEndpoint, Set<MemberEndpoint> endpoints, TokenBucketRateLimiterConfig globalConfig) {
        if (!endpoints.contains(selfEndpoint)) {
            throw new IllegalArgumentException("self is not in endpoints");
        }
        int nEndpoint = endpoints.size();
        if (nEndpoint > globalConfig.getCapacity()) {
            throw new IllegalArgumentException("member count > total capacity");
        }
        this.selfEndpoint = selfEndpoint;
        this.endpoints = endpoints;
        this.globalConfig = globalConfig;
    }

    public MemberEndpoint getSelfEndpoint() {
        return selfEndpoint;
    }

    public Set<MemberEndpoint> getEndpoints() {
        return endpoints;
    }

    public TokenBucketRateLimiterConfig getGlobalConfig() {
        return globalConfig;
    }

    public MessageDispatcher getMessageDispatcher() {
        return messageDispatcher;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Transporter getTransporter() {
        return transporter;
    }

    public void setTransporter(Transporter transporter) {
        this.transporter = transporter;
    }

}
