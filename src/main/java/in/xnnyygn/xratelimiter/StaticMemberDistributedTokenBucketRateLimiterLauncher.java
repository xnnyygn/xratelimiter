package in.xnnyygn.xratelimiter;

import com.google.common.collect.ImmutableSet;
import in.xnnyygn.xratelimiter.gossip.MemberEndpoint;
import in.xnnyygn.xratelimiter.rpc.DefaultTransporter;
import in.xnnyygn.xratelimiter.schedule.DefaultScheduler;

public class StaticMemberDistributedTokenBucketRateLimiterLauncher {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("usage <port>");
            return;
        }

        MemberEndpoint endpoint = new MemberEndpoint("localhost", Integer.parseInt(args[0]));
        DistributedTokenBucketRateLimiterArguments limiterArguments = new DistributedTokenBucketRateLimiterArguments(
                endpoint,
                ImmutableSet.of(
                        new MemberEndpoint("localhost", 5302),
                        new MemberEndpoint("localhost", 5303),
                        new MemberEndpoint("localhost", 5304)
                ),
                new TokenBucketRateLimiterConfig(60, 60, 1000, 0)
        );
        limiterArguments.setTransporter(new DefaultTransporter(endpoint, limiterArguments.getMessageDispatcher()));
        limiterArguments.setScheduler(new DefaultScheduler());

        StaticMemberDistributedTokenBucketRateLimiter limiter = new StaticMemberDistributedTokenBucketRateLimiter(limiterArguments);
        limiter.initialize();
        SimpleService service = new SimpleService(limiter, endpoint.getPort() + 1000);
        service.start();
        System.in.read();
        service.stop();
        limiter.shutdown();
    }

}
