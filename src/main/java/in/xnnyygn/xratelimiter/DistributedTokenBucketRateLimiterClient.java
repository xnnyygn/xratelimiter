package in.xnnyygn.xratelimiter;

import com.google.common.primitives.Ints;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class DistributedTokenBucketRateLimiterClient {

    private final int port;
    private volatile boolean running = false;
    private volatile int rate = 1;

    public DistributedTokenBucketRateLimiterClient(int port) {
        this.port = port;
    }

    public void run() throws Exception {
        Thread thread = new Thread(this::send, "client");
        thread.start();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("rate >");
            int n = scanner.nextInt();
            if (n < 0) {
                running = false;
                break;
            }
            System.out.println(n);
            rate = n;
        }
        thread.join();
    }

    private void send() {
        running = true;
        try (DatagramSocket datagramSocket = new DatagramSocket()) {
            while (running) {
                if (rate > 0) {
                    datagramSocket.send(new DatagramPacket(Ints.toByteArray(rate), 4, InetAddress.getLoopbackAddress(), port));
                }
                Thread.sleep(1000L);
            }
        } catch (Exception ignored) {
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("usage <port>");
            return;
        }
        DistributedTokenBucketRateLimiterClient client = new DistributedTokenBucketRateLimiterClient(Integer.parseInt(args[0]));
        client.run();
    }
}
