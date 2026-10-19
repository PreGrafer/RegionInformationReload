package pregrafer.rir.region;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import pregrafer.rir.geometry.Point3D;
import pregrafer.rir.spatial.RegionPreview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class Region {
    private final Point3D fallbackPosition;
    private final Point3D fallbackRotation;
    private final String fallbackWorldName;
    private final String uniqueId;
    private final String name;
    private final String worldName;
    private final List<String> enterActions;
    private final List<String> leaveActions;
    private final String type;
    private final Set<String> blockedInteractionMaterials;

    private int priority;
    private boolean frozen;
    private boolean enabled = true;
    private String enterPermission;
    private List<String> firstEnterActions = Collections.emptyList();
    private Map<Long, List<String>> repeatingActions = Collections.emptyMap();

    public void configure(
            int priority,
            boolean enabled,
            String permission,
            List<String> first,
            Map<Long, List<String>> repeat) {
        if (frozen)
            throw new IllegalStateException(
                    "Loaded regions are read-only; save configuration and rebuild the index");
        this.priority = priority;
        this.enabled = enabled;
        this.enterPermission = permission;
        this.firstEnterActions = Collections.unmodifiableList(new ArrayList<>(first));
        Map<Long, List<String>> plans = new LinkedHashMap<>();
        repeat.forEach(
                (period, messages) ->
                        plans.put(period, Collections.unmodifiableList(new ArrayList<>(messages))));
        this.repeatingActions = Collections.unmodifiableMap(plans);
    }

    public int getPriority() {
        return priority;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getEnterPermission() {
        return enterPermission == null ? "RIR.enter." + uniqueId : enterPermission;
    }

    public List<String> getFirstEnterActions() {
        return firstEnterActions;
    }

    public Map<Long, List<String>> getRepeatingActions() {
        return repeatingActions;
    }

    /** Called by the repository before publishing this definition to API consumers. */
    public final void freeze() {
        frozen = true;
    }

    public abstract double[] getBounds();

    public Region(
            String uniqueId,
            String name,
            String worldName,
            String type,
            List<String> enterActions,
            List<String> leaveActions,
            String fallbackWorldName,
            Point3D fallbackPosition,
            Point3D fallbackRotation,
            Set<String> blockedInteractionMaterials) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.worldName = worldName;
        this.type = type;
        this.enterActions = Collections.unmodifiableList(new ArrayList<>(enterActions));
        this.leaveActions = Collections.unmodifiableList(new ArrayList<>(leaveActions));
        this.fallbackWorldName = fallbackWorldName;
        this.fallbackPosition =
                new Point3D(
                        fallbackPosition.getX(), fallbackPosition.getY(), fallbackPosition.getZ());
        this.fallbackRotation =
                new Point3D(
                        fallbackRotation.getX(), fallbackRotation.getY(), fallbackRotation.getZ());
        this.blockedInteractionMaterials =
                Collections.unmodifiableSet(new HashSet<>(blockedInteractionMaterials));
    }

    public Set<String> getBlockedInteractionMaterials() {
        return blockedInteractionMaterials;
    }

    public String getFallbackWorldName() {
        return fallbackWorldName;
    }

    public Point3D getFallbackRotation() {
        return new Point3D(
                fallbackRotation.getX(), fallbackRotation.getY(), fallbackRotation.getZ());
    }

    public Point3D getFallbackPosition() {
        return new Point3D(
                fallbackPosition.getX(), fallbackPosition.getY(), fallbackPosition.getZ());
    }

    public String getUniqueId() {
        return this.uniqueId;
    }

    public String getName() {
        return this.name;
    }

    public String getWorldName() {
        return this.worldName;
    }

    public String getType() {
        return this.type;
    }

    public List<String> getEnterActions() {
        return this.enterActions;
    }

    public List<String> getLeaveActions() {
        return this.leaveActions;
    }

    public String toString() {
        return "Region{\n"
                + "uniqueId: "
                + this.uniqueId
                + '\n'
                + "regionName: "
                + this.name
                + '\n'
                + "world: "
                + this.worldName
                + '\n'
                + "type: "
                + this.type
                + '\n'
                + "inInfos: "
                + this.enterActions
                + '\n'
                + "outInfos: "
                + this.leaveActions
                + '\n'
                + "kickWorld: "
                + this.fallbackWorldName
                + '\n'
                + "kickPoint: ("
                + this.fallbackPosition.getX()
                + ","
                + this.fallbackPosition.getY()
                + ","
                + this.fallbackPosition.getZ()
                + ")\n"
                + "kickFace: ("
                + this.fallbackRotation.getX()
                + ","
                + this.fallbackRotation.getZ()
                + ")\n"
                + '}';
    }

    public void draw(Player player) {
        RegionPreview.draw(this, player);
    }

    public boolean contains(Point3D point) {
        return contains(point.getX(), point.getY(), point.getZ());
    }

    public abstract boolean contains(double x, double y, double z);

    public boolean contains(Location location) {
        return location != null
                && location.getWorld() != null
                && worldName.equals(location.getWorld().getName())
                && contains(location.getX(), location.getY(), location.getZ());
    }
}
