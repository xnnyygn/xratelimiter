package in.xnnyygn.xratelimiter;

import com.google.common.primitives.Ints;
import in.xnnyygn.xratelimiter.rpc.ParserException;
import in.xnnyygn.xratelimiter.rpc.TransporterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class SimpleService {

    private static final Logger logger = LoggerFactory.getLogger(SimpleService.class);
    private final TokenBucketRateLimiter limiter;
    private final int port;

    private DatagramSocket datagramSocket;
    private Thread serverThread;
    private volatile boolean running;

    public SimpleService(TokenBucketRateLimiter limiter, int port) {
        this.limiter = limiter;
        this.port = port;
    }

    public void start() {
        logger.info("start service at port {}", port);
        try {
            datagramSocket = new DatagramSocket(port);
        } catch (SocketException e) {
            throw new TransporterException(e);
        }
        running = true;
        serverThread = new Thread(this::serve, "service");
        serverThread.start();
    }

    private void serve() {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (running) {
            try {
                datagramSocket.receive(packet);
                process(packet);
            } catch (SocketException ignored) {
                // socket is closed
                break;
            } catch (IOException | ParserException e) {
                logger.warn("failed to receive to parse packet", e);
            }
        }
    }

    private void process(DatagramPacket packet) {
        if (packet.getLength() < 4) {
            return;
        }
        int n = Ints.fromByteArray(packet.getData());
        if (limiter.take(n)) {
            System.out.println("take " + n + " ok");
        } else {
            System.out.println("take " + n + " failed");
        }
    }

    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        datagramSocket.close();
        try {
            serverThread.join();
        } catch (InterruptedException ignored) {
        }
    }

}
