package pregrafer.rir.tracking;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TickQueueTest {
    @Test
    public void deadlinesOrderTiesAndReschedulingHaveNoDuplicates() {
        TickQueue<String> queue = new TickQueue<>();
        queue.schedule("later", 37);
        queue.schedule("first", 25);
        queue.schedule("second", 25);
        queue.schedule("first", 25);
        assertNull(queue.pollDue(24));
        assertEquals("first", queue.pollDue(25));
        queue.schedule("later", 20);
        assertEquals("later", queue.pollDue(25));
        queue.schedule("second", 50);
        assertNull(queue.pollDue(49));
        assertEquals("second", queue.pollDue(50));
        assertEquals(0, queue.size());
        assertNull(queue.pollDue(Long.MAX_VALUE));
    }

    @Test
    public void cancellationAndClearRemoveFarFutureWorkImmediately() {
        TickQueue<String> queue = new TickQueue<>();
        for (int tick = 0; tick < 10000; tick++) {
            queue.schedule("player", Integer.MAX_VALUE + (long) tick);
            assertEquals(1, queue.size());
        }
        queue.cancel("player");
        queue.cancel("missing");
        assertEquals(0, queue.size());
        assertNull(queue.pollDue(Long.MAX_VALUE));
        queue.schedule("old-session", 1);
        queue.clear();
        queue.schedule("new-session", 2);
        assertNull(queue.pollDue(1));
        assertEquals("new-session", queue.pollDue(2));
    }

    @Test
    public void randomUpdatesMatchEarliestLiveDeadline() {
        TickQueue<Integer> queue = new TickQueue<>();
        Map<Integer, Long> expected = new HashMap<>();
        Random random = new Random(1709);
        for (int tick = 0; tick < 3000; tick++) {
            for (int op = 0; op < 5; op++) {
                int value = random.nextInt(50);
                if (random.nextInt(4) == 0) {
                    expected.remove(value);
                    queue.cancel(value);
                } else {
                    long due = tick + random.nextInt(100);
                    expected.put(value, due);
                    queue.schedule(value, due);
                }
            }
            while (true) {
                long earliest =
                        expected.values().stream()
                                .mapToLong(Long::longValue)
                                .min()
                                .orElse(Long.MAX_VALUE);
                Integer actual = queue.pollDue(tick);
                if (earliest > tick) {
                    assertNull(actual);
                    break;
                }
                assertNotNull(actual);
                assertEquals(Long.valueOf(earliest), expected.remove(actual));
            }
            assertEquals(expected.size(), queue.size());
        }
    }
}
