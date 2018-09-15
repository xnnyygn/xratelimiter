package in.xnnyygn.xratelimiter;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import in.xnnyygn.xratelimiter.gossip.MemberEndpoint;
import in.xnnyygn.xratelimiter.rpc.messages.RemoteMessage;
import in.xnnyygn.xratelimiter.schedule.DefaultScheduler;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StaticMemberDistributedTokenBucketRateLimiterTest {

    private MockTransporter mockTransporter = new MockTransporter();
    private StaticMemberDistributedTokenBucketRateLimiter limiter;

    @Before
    public void setUp() throws Exception {
        DistributedTokenBucketRateLimiterArguments limiterArguments = new DistributedTokenBucketRateLimiterArguments(
                new MemberEndpoint("localhost", 5302),
                ImmutableSet.of(
                        new MemberEndpoint("localhost", 5302),
                        new MemberEndpoint("localhost", 5303),
                        new MemberEndpoint("localhost", 5304)
                ),
                new TokenBucketRateLimiterConfig(60, 10, 1000, 0)
        );
        limiterArguments.setTransporter(mockTransporter);
        limiterArguments.setScheduler(new NullScheduler());

        limiter = new StaticMemberDistributedTokenBucketRateLimiter(limiterArguments);
    }

    @Test
    public void testRefresh1() {
        limiter.onRefreshTimeout();
        assertEquals(1, mockTransporter.getMessageSize());
    }

    @Test
    public void testRefresh2() {
        limiter.initialize();
        Map<MemberEndpoint, Double> idealWeightMap = new HashMap<>();
        idealWeightMap.put(new MemberEndpoint("localhost", 5304), 0.0);
        limiter.onReceiveLimiterWeightsCollectingRpc(new RemoteMessage<>(
                new LimiterWeightsCollectingRpc(
                        2,
                        idealWeightMap,
                        new HashSet<>(Arrays.asList(
                                new MemberEndpoint("localhost", 5302),
                                new MemberEndpoint("localhost", 5303)
                        ))
                ),
                new MemberEndpoint("localhost", 5304))
        );
        assertEquals(1, mockTransporter.getMessageSize());
        MockTransporter.Message message = mockTransporter.getMessage(0);
        assertEquals(new MemberEndpoint("localhost", 5303), message.getRecipient());
        LimiterWeightsCollectingRpc rpc = (LimiterWeightsCollectingRpc) message.getPayload();
        assertEquals(1, rpc.getRemainingEndpoints().size());
    }

    @Test
    public void testRefresh3() {
        limiter.initialize();
        Map<MemberEndpoint, Double> idealWeightMap = new HashMap<>();
        idealWeightMap.put(new MemberEndpoint("localhost", 5304), 0.0);
        idealWeightMap.put(new MemberEndpoint("localhost", 5303), 0.0);
        limiter.onReceiveLimiterWeightsCollectingRpc(new RemoteMessage<>(
                new LimiterWeightsCollectingRpc(
                        2, idealWeightMap, Collections.singleton(new MemberEndpoint("localhost", 5302))
                ),
                new MemberEndpoint("localhost", 5303))
        );
        assertEquals(1, mockTransporter.getMessageSize());
        MultiLimiterConfigSyncRpc rpc = (MultiLimiterConfigSyncRpc) mockTransporter.getMessage(0).getPayload();
        assertEquals(2, rpc.getRound());
    }

    @Test
    public void testSync1() {
        limiter.syncMultiLimiterConfig();
        assertEquals(1, mockTransporter.getMessageSize());
        MultiLimiterConfigSyncRpc rpc = (MultiLimiterConfigSyncRpc) mockTransporter.getMessage(0).getPayload();
        assertEquals(1, rpc.getRound());
    }

    @Test
    public void testSync2() {
        limiter.onReceiveMultiLimiterConfigSyncRpc(new RemoteMessage<>(
                new MultiLimiterConfigSyncRpc(1),
                new MemberEndpoint("localhost", 5303))
        );
        assertEquals(1, mockTransporter.getMessageSize());
        MultiLimiterConfigSyncResponse response = (MultiLimiterConfigSyncResponse) mockTransporter.getMessage(0).getPayload();
        assertEquals(1, response.getRound());
        assertTrue(response.getConfigMap().isEmpty());
    }

    @Test
    public void testSync3() {
        limiter.onReceiveMultiLimiterConfigSyncRpc(new RemoteMessage<>(
                new MultiLimiterConfigSyncRpc(0),
                new MemberEndpoint("localhost", 5303))
        );
        assertEquals(1, mockTransporter.getMessageSize());
        MultiLimiterConfigSyncResponse response = (MultiLimiterConfigSyncResponse) mockTransporter.getMessage(0).getPayload();
        assertEquals(1, response.getRound());
        assertFalse(response.getConfigMap().isEmpty());
    }

    @Test
    public void testSync4() {
        limiter.onReceiveMultiLimiterConfigSyncResponse(new RemoteMessage<>(
                new MultiLimiterConfigSyncResponse(1),
                new MemberEndpoint("localhost", 5303)
        ));
        assertEquals(0, mockTransporter.getMessageSize());
    }

    @Test
    public void testSync5() {
        limiter.onReceiveMultiLimiterConfigSyncResponse(new RemoteMessage<>(
                new MultiLimiterConfigSyncResponse(0),
                new MemberEndpoint("localhost", 5303)
        ));
        assertEquals(1, mockTransporter.getMessageSize());
        MultiLimiterConfigSyncResponse response = (MultiLimiterConfigSyncResponse) mockTransporter.getMessage(0).getPayload();
        assertEquals(1, response.getRound());
    }

    @Test
    public void testSync6() {
        limiter.onReceiveMultiLimiterConfigSyncResponse(new RemoteMessage<>(
                new MultiLimiterConfigSyncResponse(
                        2,
                        Collections.singletonMap(
                                new MemberEndpoint("localhost", 5302),
                                new TokenBucketRateLimiterConfig(
                                        10, 1, 1000, 0
                                )
                        )
                ),
                new MemberEndpoint("localhost", 5303)
        ));
    }
}