package in.xnnyygn.xratelimiter.schedule;

import java.util.concurrent.ScheduledFuture;

public interface Scheduler {

    ScheduledFuture<?> schedule(Runnable command, long delay);

    ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay);

    void shutdown();

}
