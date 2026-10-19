package pregrafer.rir.tracking;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Main-thread deadline queue with one cancellable entry per value and no stale entries. */
final class TickQueue<T> {
    private final TreeMap<Long, LinkedHashSet<T>> deadlines = new TreeMap<>();
    private final Map<T, Long> scheduled = new HashMap<>();

    void schedule(T value, long tick) {
        Objects.requireNonNull(value, "value");
        Long previous = scheduled.get(value);
        if (previous != null && previous == tick) return;
        cancel(value);
        scheduled.put(value, tick);
        deadlines.computeIfAbsent(tick, ignored -> new LinkedHashSet<>()).add(value);
    }

    void cancel(T value) {
        Long tick = scheduled.remove(value);
        if (tick == null) return;
        LinkedHashSet<T> bucket = deadlines.get(tick);
        bucket.remove(value);
        if (bucket.isEmpty()) deadlines.remove(tick);
    }

    T pollDue(long tick) {
        Map.Entry<Long, LinkedHashSet<T>> first = deadlines.firstEntry();
        if (first == null || first.getKey() > tick) return null;
        Iterator<T> values = first.getValue().iterator();
        T value = values.next();
        values.remove();
        if (first.getValue().isEmpty()) deadlines.pollFirstEntry();
        scheduled.remove(value);
        return value;
    }

    int size() {
        return scheduled.size();
    }

    void clear() {
        scheduled.clear();
        deadlines.clear();
    }
}
