package in.xnnyygn.xratelimiter;

import com.google.common.collect.ImmutableSet;
import in.xnnyygn.xratelimiter.gossip.MemberEndpoint;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

public class DefaultLimiterWeightCollectorTest {

    private static final TokenBucketRateLimiterConfig globalConfig = new TokenBucketRateLimiterConfig(60, 1, 1000, 0);

    @Test
    public void testAddDifferentRound() {
        DefaultLimiterWeightCollector collector = new DefaultLimiterWeightCollector(
                2, globalConfig, Collections.singleton(new MemberEndpoint("localhost", 5302))
        );
        assertNull(collector.add(3, new MemberEndpoint("localhost", 5302), 0));
    }

    @Test
    public void testAddUnexpectedEndpoint() {
        DefaultLimiterWeightCollector collector = new DefaultLimiterWeightCollector(
                2, globalConfig, Collections.singleton(new MemberEndpoint("localhost", 5302))
        );
        assertNull(collector.add(2, new MemberEndpoint("localhost", 5303), 0));
    }

    @Test
    public void testAddIdealWeightZero() {
        MemberEndpoint endpoint = new MemberEndpoint("localhost", 5302);
        DefaultLimiterWeightCollector collector = new DefaultLimiterWeightCollector(
                2, globalConfig, Collections.singleton(endpoint)
        );
        MultiLimiterConfig multiLimiterConfig = collector.add(2, endpoint, 0);
        assertNotNull(multiLimiterConfig);
        assertEquals(0, multiLimiterConfig.getQuota(endpoint).intValue());
    }

    @Test
    public void testAdd() {
        MemberEndpoint endpoint1 = new MemberEndpoint("localhost", 5302);
        MemberEndpoint endpoint2 = new MemberEndpoint("localhost", 5303);
        DefaultLimiterWeightCollector collector = new DefaultLimiterWeightCollector(
                2, globalConfig, ImmutableSet.of(endpoint1, endpoint2)
        );
        assertNull(collector.add(2, endpoint1, 1));
        MultiLimiterConfig multiLimiterConfig = collector.add(2, endpoint2, 1);
        assertNotNull(multiLimiterConfig);
        assertEquals(30, multiLimiterConfig.getQuota(endpoint1).intValue());
        assertEquals(30, multiLimiterConfig.getQuota(endpoint2).intValue());
    }

}