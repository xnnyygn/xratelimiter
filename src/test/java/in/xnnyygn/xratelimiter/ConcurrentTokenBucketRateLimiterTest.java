package in.xnnyygn.xratelimiter;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

public class ConcurrentTokenBucketRateLimiterTest {

    @Test
    @Ignore
    public void testTake() throws IOException {
        ConcurrentTokenBucketRateLimiter limiter = new ConcurrentTokenBucketRateLimiter(new TokenBucketRateLimiterConfig(
                60, 60, 100, 0
        ));
        Thread thread1 = new Thread(() -> {
            consumeTokens(limiter);
        });
        Thread thread2 = new Thread(() -> {
            consumeTokens(limiter);
        });
        Thread thread3 = new Thread(() -> {
            consumeTokens(limiter);
        });
        thread1.start();
        thread2.start();
        thread3.start();
        System.in.read();
        thread1.interrupt();
        thread2.interrupt();
        thread3.interrupt();
    }

    private void consumeTokens(ConcurrentTokenBucketRateLimiter limiter) {
        while (!Thread.currentThread().isInterrupted()) {
            limiter.take(1);
        }
    }

}