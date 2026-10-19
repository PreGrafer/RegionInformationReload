package pregrafer.rir.spatial;

import org.bukkit.Location;

public final class RegionSelection {
    private Location first, second;
    private boolean enabled;

    public boolean toggle() {
        enabled = !enabled;
        if (!enabled) {
            first = null;
            second = null;
        }
        return enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void select(Location point, boolean primary) {
        if (first != null && !first.getWorld().equals(point.getWorld())
                || second != null && !second.getWorld().equals(point.getWorld())) {
            first = null;
            second = null;
        }
        if (primary) first = point.clone();
        else second = point.clone();
    }

    public Location getFirst() {
        return first == null ? null : first.clone();
    }

    public Location getSecond() {
        return second == null ? null : second.clone();
    }
}
