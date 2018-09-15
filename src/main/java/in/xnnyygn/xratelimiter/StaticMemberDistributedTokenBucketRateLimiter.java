package in.xnnyygn.xratelimiter;

import in.xnnyygn.xratelimiter.gossip.MemberEndpoint;
import in.xnnyygn.xratelimiter.rpc.Transporter;
import in.xnnyygn.xratelimiter.rpc.messages.RemoteMessage;
import in.xnnyygn.xratelimiter.schedule.Scheduler;
import in.xnnyygn.xratelimiter.support.MessageDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.util.*;
import java.util.concurrent.ScheduledFuture;

public class StaticMemberDistributedTokenBucketRateLimiter implements TokenBucketRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(StaticMemberDistributedTokenBucketRateLimiter.class);

    private static final long REFRESH_TIMEOUT = 3000;
    private static final long SYNC_INTERVAL = 1000;

    private final MessageDispatcher messageDispatcher;
    private final MemberEndpoint selfEndpoint;
    private final MemberEndpointList endpointList;
    private final TokenBucketRateLimiterConfig globalConfig;

    private final Transporter transporter;
    private final SchedulerWrapper scheduler;

    private final RequestSampler requestSampler;
    private final TokenBucketRateLimiterWrapper limiter;
    private volatile MultiLimiterConfig multiLimiterConfig;

    public StaticMemberDistributedTokenBucketRateLimiter(DistributedTokenBucketRateLimiterArguments arguments) {
        this.selfEndpoint = arguments.getSelfEndpoint();
        this.endpointList = new MemberEndpointList(arguments.getEndpoints());
        this.globalConfig = arguments.getGlobalConfig();

        this.multiLimiterConfig = MultiLimiterConfig.fromEndpoints(1, arguments.getEndpoints(), globalConfig);
        this.limiter = new TokenBucketRateLimiterWrapper(multiLimiterConfig.getConfig(selfEndpoint));

        this.messageDispatcher = arguments.getMessageDispatcher();
        this.transporter = arguments.getTransporter();
        this.scheduler = new SchedulerWrapper(arguments.getScheduler());

        this.requestSampler = new RequestSampler(1024, 3000, 0.5);
    }

    public void initialize() {
        transporter.initialize();

        messageDispatcher.register(LimiterWeightsCollectingRpc.class, this::onReceiveLimiterWeightsCollectingRpc);
        messageDispatcher.register(MultiLimiterConfigSyncRpc.class, this::onReceiveMultiLimiterConfigSyncRpc);
        messageDispatcher.register(MultiLimiterConfigSyncResponse.class, this::onReceiveMultiLimiterConfigSyncResponse);

        scheduler.scheduleSyncTask(this::syncMultiLimiterConfig);
        scheduler.scheduleRefreshTimeout(this::onRefreshTimeout);
    }

    @Override
    public boolean take(int n) {
        requestSampler.add(n);
        return limiter.take(n);
    }

    void onRefreshTimeout() {
        if (endpointList.isNoOtherMember()) {
            return;
        }
        logger.info("start refresh");
        Set<MemberEndpoint> remainingEndpoints = endpointList.getOtherMemberEndpoints(selfEndpoint);
        transporter.send(nextEndpoint(remainingEndpoints), new LimiterWeightsCollectingRpc(
                multiLimiterConfig.getRound() + 1,
                Collections.singletonMap(selfEndpoint, calculateIdealWeight()),
                remainingEndpoints
        ));
    }

    private MemberEndpoint nextEndpoint(Set<MemberEndpoint> endpoints) {
        assert !endpoints.isEmpty();
        return endpoints.iterator().next();
    }

    private double calculateIdealWeight() {
        return requestSampler.averageRate();
    }

    void onReceiveLimiterWeightsCollectingRpc(RemoteMessage<LimiterWeightsCollectingRpc> message) {
        LimiterWeightsCollectingRpc rpc = message.get();
        int round = rpc.getRound();
        Set<MemberEndpoint> remainingEndpoints = rpc.getRemainingEndpoints();
        if (round < multiLimiterConfig.getRound() + 1 || !remainingEndpoints.contains(selfEndpoint)) {
            logger.warn("unexpected round or not the remaining endpoint");
            return;
        }
        scheduler.resetRefreshTimeout();
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
        limiter.reset(newConfig.getConfig(selfEndpoint));
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

        private final Runnable command;
        private final ScheduledFuture<?> future;
        private final long timestamp;

        RefreshTimeout(Runnable command, ScheduledFuture<?> future, long timestamp) {
            this.command = command;
            this.future = future;
            this.timestamp = timestamp;
        }

        Runnable getCommand() {
            return command;
        }

        boolean cancel() {
            return future.cancel(false);
        }

        long getTimestamp() {
            return timestamp;
        }

    }

    private static class SchedulerWrapper {

        private final Scheduler delegate;
        private volatile RefreshTimeout refreshTimeout;

        SchedulerWrapper(Scheduler delegate) {
            this.delegate = delegate;
        }

        private synchronized void scheduleRefreshTimeout(Runnable command, long expectedTimestamp) {
            if (refreshTimeout.getTimestamp() != expectedTimestamp) {
                return;
            }
            scheduleRefreshTimeout(command);
        }

        void scheduleRefreshTimeout(Runnable command) {
            long timestamp = System.currentTimeMillis();
            ScheduledFuture<?> future = delegate.schedule(() -> {
                command.run();
                scheduleRefreshTimeout(command, timestamp);
            }, REFRESH_TIMEOUT);
            refreshTimeout = new RefreshTimeout(command, future, timestamp);
        }

        void resetRefreshTimeout() {
            RefreshTimeout refreshTimeout = this.refreshTimeout;
            refreshTimeout.cancel();
            scheduleRefreshTimeout(refreshTimeout.getCommand(), refreshTimeout.getTimestamp());
        }

        void scheduleSyncTask(Runnable command) {
            delegate.scheduleWithFixedDelay(command, SYNC_INTERVAL, SYNC_INTERVAL);
        }

        void shutdown() {
            delegate.shutdown();
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

    @ThreadSafe
    private static class TokenBucketRateLimiterWrapper {

        private DefaultTokenBucketRateLimiter delegate;

        TokenBucketRateLimiterWrapper(TokenBucketRateLimiterConfig config) {
            this.delegate = new DefaultTokenBucketRateLimiter(config);
        }

        synchronized boolean take(int n) {
            return delegate.take(n);
        }

        synchronized void reset(TokenBucketRateLimiterConfig config) {
            int initialTokens = delegate.getTokens();
            delegate = new DefaultTokenBucketRateLimiter(
                    config.getCapacity(),
                    config.getRefillAmount(),
                    config.getRefillTime(),
                    initialTokens
            );
        }

    }

    @ThreadSafe
    private static class RequestSampler {

        private final Random random = new Random();
        private final double ratio;
        private final RequestSequence sequence;
        private volatile boolean sampling = false;

        RequestSampler(int capacity, long duration, double ratio) {
            this.ratio = ratio;
            this.sequence = new RequestSequence(capacity, duration);
        }

        void add(int n) {
            if (sampling || random.nextDouble() > ratio) {
                return;
            }
            synchronized (this) {
                sequence.add(n);
            }
        }

        double averageRate() {
            sampling = true;
            RequestSequence.Range range;
            synchronized (this) {
                range = sequence.average();
            }
            sampling = false;
            if (!range.isValid()) {
                return 0;
            }
            return (double) range.getSum() / (range.getEndTime() - range.getStartTime());
        }

        double maxRate(long window) {
            sampling = true;
            RequestSequence.Range range;
            synchronized (this) {
                range = sequence.max(window);
            }
            sampling = false;
            if (!range.isValid()) {
                return 0;
            }
            return (double) range.getSum() / (range.getEndTime() - range.getStartTime());
        }

    }

}
