package in.xnnyygn.xratelimiter;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class RequestSequenceTest {

    @Test
    public void testAdd() {
        RequestSequence sequence = new RequestSequence(4, 10);
        sequence.add(1, 1);
        sequence.add(2, 2);
        sequence.add(3, 3);
        sequence.add(4, 4);
        // 1, 2, 3, 4
        assertEquals(10, sequence.sum());
        sequence.add(5, 5);
        // 2, 3, 4, 5
        assertEquals(14, sequence.sum());
        sequence.add(13, 13);
        // 4, 5, 13
        assertEquals(22, sequence.sum());
        sequence.add(15, 15);
        // 13, 15
        assertEquals(28, sequence.sum());
    }

    @Test
    @Ignore
    public void testFindFirst() {
        assertEquals(-1, findFirstNotBefore(new int[]{1}, 1));
        assertEquals(-1, findFirstNotBefore(new int[]{1}, 0));
        assertEquals(0, findFirstNotBefore(new int[]{1}, 2));
        assertEquals(1, findFirstNotBefore(new int[]{1, 2}, 3));
        assertEquals(0, findFirstNotBefore(new int[]{1, 2}, 2));
        assertEquals(-1, findFirstNotBefore(new int[]{1, 2}, 1));
        assertEquals(-1, findFirstNotBefore(new int[]{1, 2}, 0));
    }

    private int findFirstNotBefore(int[] array, int v) {
        assert array.length > 0;
        int low = 0;
        int high = array.length - 1;
        int middle;
        while (low <= high) {
            middle = (low + high) / 2;
            if (array[middle] >= v) {
                high = middle - 1;
            } else {
                low = middle + 1;
            }
        }
        return high;
    }

    @Test
    public void testAverage() {
        RequestSequence sequence = new RequestSequence(4, 10);
        assertEquals(0, sequence.average().getSum());

        sequence.add(1, 1);
        assertEquals(1, sequence.average(0).getSum());

        sequence.add(2, 1);
        sequence.add(3, 1);

        sequence.add(4, 1);
        sequence.add(5, 1);
        sequence.add(5, 2);
        sequence.add(6, 1);

        // (1 + 1 + 2 + 1) / (6 - 4) => 2.5
        assertEquals(5, sequence.average(0).getSum());
    }

    @Test
    public void testMax() {
        RequestSequence sequence = new RequestSequence(4, 10);
        assertEquals(0, sequence.max(10).getSum());
        sequence.add(1, 1);
        RequestSequence.Range range = sequence.max(10);
        assertEquals(1, range.getSum());
    }

    @Test
    public void testMax2() {
        RequestSequence sequence = new RequestSequence(16, 100);
        sequence.add(1, 1);
        sequence.add(2, 1);
        sequence.add(3, 1);
        sequence.add(7, 1);
        sequence.add(8, 1);
        sequence.add(9, 1);
        sequence.add(10, 1);
        sequence.add(11, 1);
        RequestSequence.Range range = sequence.max(5);
        assertEquals(7, range.getStartTime());
        assertEquals(11, range.getEndTime());
        assertEquals(5, range.getSum());
    }
}