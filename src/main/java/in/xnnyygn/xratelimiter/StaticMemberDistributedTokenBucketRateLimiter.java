package in.xnnyygn.xratelimiter;

import in.xnnyygn.xratelimiter.gossip.MemberEndpoint;
import in.xnnyygn.xratelimiter.rpc.DefaultTransporter;
import in.xnnyygn.xratelimiter.rpc.Transporter;
import in.xnnyygn.xratelimiter.rpc.messages.RemoteMessage;
import in.xnnyygn.xratelimiter.schedule.DefaultScheduler;
import in.xnnyygn.xratelimiter.schedule.Scheduler;
import in.xnnyygn.xratelimiter.support.MessageDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.GuardedBy;
import java.util.*;
import java.util.concurrent.ScheduledFuture;

public class StaticMemberDistributedTokenBucketRateLimiter implements TokenBucketRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(StaticMemberDistributedTokenBucketRateLimiter.class);

    private static final long REFRESH_TIMEOUT = 3000;
    private static final long SYNC_INTERVAL = 1000;

    private final MemberEndpoint selfEndpoint;
    private final MemberEndpointList endpointList;
    private final TokenBucketRateLimiterConfig globalConfig;

    private final MessageDispatcher messageDispatcher = new MessageDispatcher();
    private final Transporter transporter;
    private final Scheduler scheduler;

    private volatile RefreshTimeout refreshTimeout;
    private volatile MultiLimiterConfig multiLimiterConfig;

    private final Object limiterLock = new Object();
    @GuardedBy("limiterLock")
    private volatile DefaultTokenBucketRateLimiter delegate;

    /**
     * Create limiter.
     *
     * @param selfEndpoint self endpoint
     * @param endpoints    endpoints
     * @param globalConfig global config
     */
    public StaticMemberDistributedTokenBucketRateLimiter(MemberEndpoint selfEndpoint, Set<MemberEndpoint> endpoints,
                                                         TokenBucketRateLimiterConfig globalConfig) {
        if (!endpoints.contains(selfEndpoint)) {
            throw new IllegalArgumentException("self is not in endpoints");
        }
        int nEndpoint = endpoints.size();
        if (nEndpoint > globalConfig.getCapacity()) {
            throw new IllegalArgumentException("member count > total capacity");
        }
        this.selfEndpoint = selfEndpoint;
        this.endpointList = new MemberEndpointList(endpoints);
        this.globalConfig = globalConfig;

        this.multiLimiterConfig = MultiLimiterConfig.fromEndpoints(1, endpoints, globalConfig);
        this.delegate = new DefaultTokenBucketRateLimiter(multiLimiterConfig.getConfig(selfEndpoint));

        this.transporter = new DefaultTransporter(selfEndpoint, messageDispatcher);
        this.scheduler = new DefaultScheduler();
    }

    public void initialize() {
        transporter.initialize();

        messageDispatcher.register(LimiterWeightsCollectingRpc.class, this::onReceiveLimiterWeightsCollectingRpc);
        messageDispatcher.register(MultiLimiterConfigSyncRpc.class, this::onReceiveMultiLimiterConfigSyncRpc);
        messageDispatcher.register(MultiLimiterConfigSyncResponse.class, this::onReceiveMultiLimiterConfigSyncResponse);

        scheduler.scheduleWithFixedDelay(this::syncMultiLimiterConfig, SYNC_INTERVAL, SYNC_INTERVAL);
        scheduleRefreshTimeout();
    }

    private synchronized void scheduleRefreshTimeout(long expectedTimestamp) {
        if (refreshTimeout.getTimestamp() != expectedTimestamp) {
            return;
        }
        scheduleRefreshTimeout();
    }

    private void scheduleRefreshTimeout() {
        long timestamp = System.currentTimeMillis();
        ScheduledFuture<?> future = scheduler.schedule(() -> onRefreshTimeout(timestamp), REFRESH_TIMEOUT);
        refreshTimeout = new RefreshTimeout(future, timestamp);
    }

    private void resetRefreshTimeout() {
        RefreshTimeout refreshTimeout = this.refreshTimeout;
        refreshTimeout.cancel();
        scheduleRefreshTimeout(refreshTimeout.getTimestamp());
    }

    @Override
    public boolean take(int n) {
        synchronized (limiterLock) {
            return delegate.take(n);
        }
    }

    void onRefreshTimeout(long timestamp) {
        if (endpointList.isNoOtherMember()) { // no other member
            return;
        }
        Set<MemberEndpoint> remainingEndpoints = endpointList.getOtherMemberEndpoints(selfEndpoint);
        transporter.send(nextEndpoint(remainingEndpoints), new LimiterWeightsCollectingRpc(
                multiLimiterConfig.getRound() + 1,
                Collections.singletonMap(selfEndpoint, calculateIdealWeight()),
                remainingEndpoints
        ));
        scheduleRefreshTimeout(timestamp);
    }

    private MemberEndpoint nextEndpoint(Set<MemberEndpoint> endpoints) {
        assert !endpoints.isEmpty();
        return endpoints.iterator().next();
    }

    private double calculateIdealWeight() {
        throw new UnsupportedOperationException();
    }

    void onReceiveLimiterWeightsCollectingRpc(RemoteMessage<LimiterWeightsCollectingRpc> message) {
        LimiterWeightsCollectingRpc rpc = message.get();
        int round = rpc.getRound();
        Set<MemberEndpoint> remainingEndpoints = rpc.getRemainingEndpoints();
        if (round < multiLimiterConfig.getRound() + 1 || !remainingEndpoints.contains(selfEndpoint)) {
            logger.warn("unexpected round or not the remaining endpoint");
            return;
        }
        resetRefreshTimeout();
        Map<MemberEndpoint, Double> idealWeightMap = rpc.getIdealWeightMap();
        idealWeightMap.put(selfEndpoint, calculateIdealWeight());
        if (remainingEndpoints.size() == 1) {
            updateMultiLimiterConfig(MultiLimiterConfig.fromIdealWeights(round, idealWeightMap, globalConfig));
            syncMultiLimiterConfig();
        } else {
            remainingEndpoints.remove(selfEndpoint);
            transporter.send(nextEndpoint(remainingEndpoints), new LimiterWeightsCollectingRpc(
                    round, idealWeightMap, remainingEndpoints
            ));
        }
    }

    private void updateMultiLimiterConfig(MultiLimiterConfig newConfig) {
        multiLimiterConfig = newConfig;
        TokenBucketRateLimiterConfig config = newConfig.getConfig(selfEndpoint);
        synchronized (limiterLock) {
            int initialTokens = delegate.getTokens();
            delegate = new DefaultTokenBucketRateLimiter(
                    config.getCapacity(), config.getRefillAmount(), config.getRefillTime(), initialTokens
            );
        }
    }

    void syncMultiLimiterConfig() {
        if (endpointList.isNoOtherMember()) {
            return;
        }
        transporter.send(endpointList.getRandomEndpointExcept(selfEndpoint), new MultiLimiterConfigSyncRpc(multiLimiterConfig.getRound()));
    }

    void onReceiveMultiLimiterConfigSyncRpc(RemoteMessage<MultiLimiterConfigSyncRpc> message) {
        MultiLimiterConfig multiLimiterConfig = this.multiLimiterConfig;
        int localRound = multiLimiterConfig.getRound();
        if (message.get().getRound() >= localRound) {
            transporter.send(message.getSender(), new MultiLimiterConfigSyncResponse(localRound));
        } else {
            transporter.send(message.getSender(), new MultiLimiterConfigSyncResponse(localRound, multiLimiterConfig.getConfigMap()));
        }
    }

    void onReceiveMultiLimiterConfigSyncResponse(RemoteMessage<MultiLimiterConfigSyncResponse> message) {
        MultiLimiterConfigSyncResponse response = message.get();
        MultiLimiterConfig multiLimiterConfig = this.multiLimiterConfig;
        int localRound = multiLimiterConfig.getRound();
        if (response.getRound() > localRound) {
            updateMultiLimiterConfig(new MultiLimiterConfig(response.getRound(), response.getConfigMap()));
        } else if (response.getRound() < localRound) {
            transporter.send(message.getSender(), new MultiLimiterConfigSyncResponse(localRound, multiLimiterConfig.getConfigMap()));
        }
    }

    public void shutdown() {
        scheduler.shutdown();
        transporter.close();
    }

    private static class RefreshTimeout {

        private final ScheduledFuture<?> future;
        private final long timestamp;

        public RefreshTimeout(ScheduledFuture<?> future, long timestamp) {
            this.future = future;
            this.timestamp = timestamp;
        }

        public boolean cancel() {
            return future.cancel(false);
        }

        public long getTimestamp() {
            return timestamp;
        }

    }

    private static class MemberEndpointList {

        private final Random random = new Random();
        private final Set<MemberEndpoint> endpoints;

        MemberEndpointList(Set<MemberEndpoint> endpoints) {
            this.endpoints = endpoints;
        }

        boolean isNoOtherMember() {
            return endpoints.size() == 1;
        }

        Set<MemberEndpoint> getOtherMemberEndpoints(MemberEndpoint selfEndpoint) {
            Set<MemberEndpoint> endpoints = new HashSet<>(this.endpoints);
            endpoints.remove(selfEndpoint);
            return endpoints;
        }

        MemberEndpoint getRandomEndpointExcept(MemberEndpoint selfEndpoint) {
            Object[] endpointArray = endpoints.toArray();
            int n = endpointArray.length;
            MemberEndpoint endpoint;
            do {
                endpoint = (MemberEndpoint) endpointArray[random.nextInt(n)];
            } while (selfEndpoint.equals(endpoint));
            return endpoint;
        }

    }

}
