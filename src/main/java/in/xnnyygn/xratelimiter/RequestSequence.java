package in.xnnyygn.xratelimiter;

public class RequestSequence {

    private final Request[] requests;
    private final int capacity;
    private final int capacity1;
    private final long duration;
    private int first = 0;
    private int last = -1;

    public RequestSequence(int capacity, long duration) {
        if (capacity <= 0 || (capacity & (capacity - 1)) != 0) {
            throw new IllegalArgumentException("capacity <= 0 or not power of 2");
        }
        if (duration < 0) {
            throw new IllegalArgumentException("duration < 0");
        }
        this.requests = new Request[capacity];
        this.capacity = capacity;
        this.capacity1 = capacity - 1;
        this.duration = duration;
    }

    public void add(int n) {
        add(System.currentTimeMillis(), n);
    }

    void add(long timestamp, int n) {
        // assert timestamp > timestamp of last request
        last++;
        if (last - first == capacity) {
            first++;
        }
        requests[last & capacity1] = new Request(timestamp, n);
        first = findFirstBefore(first, last, timestamp - duration + 1) + 1;
    }

    private Request request(int position) {
        return requests[position & capacity1];
    }

    private int findFirstBefore(int from, int to, long timestamp) {
        int low = from;
        int high = to;
        int middle;
        while (low <= high) {
            middle = (low + high) / 2;
            if (request(middle).timestamp >= timestamp) {
                high = middle - 1;
            } else {
                low = middle + 1;
            }
        }
        return high;
    }

    public long sum() {
        if (first > last) {
            return 0;
        }
        if (first == last) {
            return request(first).n;
        }
        return doSum(first, last);
    }

    private long doSum(int from, int to) {
        long sum = 0;
        for (int i = from; i <= to; i++) {
            sum += request(i).n;
        }
        return sum;
    }

    public Range average() {
        return average(System.currentTimeMillis() - duration + 1);
    }

    public Range average(long startTime) {
        if (first > last) {
            return RANGE_ZERO;
        }
        int from = findFirstBefore(first, last, startTime) + 1;
        if (from > last) {
            return RANGE_ZERO;
        }
        return new Range(request(from).timestamp, request(last).timestamp, doSum(from, last));
    }


    public Range max(long window) {
        if (first > last || window <= 0) {
            return RANGE_ZERO;
        }
        Request firstRequest = request(first);
        long max = firstRequest.n;
        long sum = firstRequest.n;
        int low = first;
        long startTime = firstRequest.timestamp;
        long endTime = startTime;
        int newLow, j;
        for (int i = first + 1; i <= last; i++) {
            Request request = request(i);
            sum += request.n;
            newLow = findFirstBefore(low, i - 1, request.timestamp - window + 1) + 1;
            if (newLow > low) {
                for (j = low; j < newLow; j++) {
                    sum -= request(j).n;
                }
                low = newLow;
                startTime = request(newLow).timestamp;
            }
            if (sum > max) {
                max = sum;
                endTime = request.timestamp;
            }
        }
        return new Range(startTime, endTime, max);
    }

    public static class Range {

        private final long startTime;
        private final long endTime;
        private final long sum;

        Range(long startTime, long endTime, long sum) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.sum = sum;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public long getSum() {
            return sum;
        }

        public boolean isValid() {
            return startTime < endTime;
        }

        @Override
        public String toString() {
            return "Range{" +
                    "startTime=" + startTime +
                    ", endTime=" + endTime +
                    ", sum=" + sum +
                    '}';
        }

    }

    private static final Range RANGE_ZERO = new Range(0, 0, 0);

    private static class Request {

        final long timestamp;
        final int n;

        Request(long timestamp, int n) {
            this.timestamp = timestamp;
            this.n = n;
        }

        @Override
        public String toString() {
            return "Request{" +
                    "timestamp=" + timestamp +
                    ", n=" + n +
                    '}';
        }

    }

}
