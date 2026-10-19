package pregrafer.rir.config;

import org.bukkit.configuration.ConfigurationSection;

import pregrafer.rir.compat.ServerCompatibility;
import pregrafer.rir.geometry.Point3D;
import pregrafer.rir.message.MessageAction;
import pregrafer.rir.region.*;
import pregrafer.rir.region.Region;

import java.util.*;

public final class RegionCodec {
    private RegionCodec() {}

    public static boolean isValidId(String id) {
        return id != null && id.matches("[A-Za-z0-9_-]{1,64}");
    }

    public static List<String> readActions(ConfigurationSection section, String path) {
        List<String> list = ConfigValidation.strings(section, path);
        for (String line : list) MessageAction.parse(line);
        return list;
    }

    private static double readCoordinate(ConfigurationSection s, String key) {
        Object raw = s.get(key);
        if (!(raw instanceof Number))
            throw new IllegalArgumentException("Missing or nonnumeric " + key);
        double n = ((Number) raw).doubleValue();
        if (!Double.isFinite(n) || Math.abs(n) > 30000000)
            throw new IllegalArgumentException(key + " must be finite and within +/-30000000");
        return n;
    }

    public static Map<String, Region> decodeRegions(ConfigurationSection root) {
        Map<String, Region> result = new LinkedHashMap<>();
        if (root == null) return result;
        for (String id : root.getKeys(false)) {
            try {
                if (!isValidId(id))
                    throw new IllegalArgumentException(
                            "ID must contain only letters, digits, _ or - (1..64)");
                ConfigurationSection s = root.getConfigurationSection(id);
                if (s == null) throw new IllegalArgumentException("Region must be a mapping");
                for (String key :
                        Arrays.asList("type", "name", "world", "kickWorld", "enterPermission"))
                    ConfigValidation.string(s, key);
                ConfigValidation.bool(s, "enabled");
                ConfigValidation.integer(s, "priority", Integer.MIN_VALUE, Integer.MAX_VALUE);
                String type = s.getString("type", "").toLowerCase(Locale.ROOT),
                        name = s.getString("name", id),
                        world = s.getString("world", "");
                if (world.trim().isEmpty()) throw new IllegalArgumentException("world is required");
                List<String> in = readActions(s, "inInfos"), out = readActions(s, "outInfos");
                Set<String> banned = new HashSet<>();
                for (String item : ConfigValidation.strings(s, "banInteractItems"))
                    banned.add(ServerCompatibility.normalizeMaterialName(item));
                boolean hasKick = s.contains("kickX") || s.contains("kickY") || s.contains("kickZ");
                String kickWorld = hasKick ? s.getString("kickWorld", world) : "";
                Point3D kick =
                        hasKick
                                ? new Point3D(
                                        readCoordinate(s, "kickX"),
                                        readCoordinate(s, "kickY"),
                                        readCoordinate(s, "kickZ"))
                                : new Point3D();
                Point3D face =
                        new Point3D(
                                s.contains("kickFacePitch")
                                        ? readCoordinate(s, "kickFacePitch")
                                        : 0,
                                0,
                                s.contains("kickFaceYaw") ? readCoordinate(s, "kickFaceYaw") : 0);
                Region r;
                if (type.equals("cube")) {
                    r =
                            new CuboidRegion(
                                    id,
                                    name,
                                    world,
                                    type,
                                    in,
                                    out,
                                    kickWorld,
                                    kick,
                                    face,
                                    banned,
                                    new Point3D(
                                            readCoordinate(s, "X1"),
                                            readCoordinate(s, "Y1"),
                                            readCoordinate(s, "Z1")),
                                    new Point3D(
                                            readCoordinate(s, "X2"),
                                            readCoordinate(s, "Y2"),
                                            readCoordinate(s, "Z2")));
                } else {
                    Point3D center =
                            new Point3D(
                                    readCoordinate(s, "centerX"),
                                    readCoordinate(s, "centerY"),
                                    readCoordinate(s, "centerZ"));
                    double radius = readCoordinate(s, "radius");
                    if (type.equals("ball"))
                        r =
                                new SphereRegion(
                                        id, name, world, type, in, out, kickWorld, kick, face,
                                        banned, center, radius);
                    else if (type.equals("cylinder"))
                        r =
                                new CylinderRegion(
                                        id,
                                        name,
                                        world,
                                        type,
                                        in,
                                        out,
                                        kickWorld,
                                        kick,
                                        face,
                                        banned,
                                        center,
                                        radius,
                                        readCoordinate(s, "height"));
                    else throw new IllegalArgumentException("Unknown region type: " + type);
                }
                Map<Long, List<String>> repeats = new LinkedHashMap<>();
                if (s.contains("repeatInfos") && !s.isConfigurationSection("repeatInfos"))
                    throw new IllegalArgumentException("repeatInfos must be a mapping");
                ConfigurationSection repeat = s.getConfigurationSection("repeatInfos");
                if (repeat != null)
                    for (String period : repeat.getKeys(false)) {
                        long ticks = Long.parseLong(period);
                        if (ticks < 20 || ticks > Integer.MAX_VALUE)
                            throw new IllegalArgumentException(
                                    "Repeat interval must be 20..2147483647 ticks");
                        if (repeats.containsKey(ticks))
                            throw new IllegalArgumentException(
                                    "Duplicate repeat interval: " + period);
                        repeats.put(
                                ticks, Collections.unmodifiableList(readActions(repeat, period)));
                    }
                r.configure(
                        s.getInt("priority", 0),
                        s.getBoolean("enabled", true),
                        s.getString("enterPermission", "RIR.enter." + id),
                        readActions(s, "firstEnterInfos"),
                        repeats);
                result.put(id, r);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Regions." + id + ": " + e.getMessage(), e);
            }
        }
        return result;
    }

    public static void encodeRegion(ConfigurationSection s, Region r) {
        s.set("name", r.getName());
        s.set("world", r.getWorldName());
        s.set("type", r.getType());
        s.set("enabled", r.isEnabled());
        s.set("priority", r.getPriority());
        s.set("enterPermission", r.getEnterPermission());
        s.set("inInfos", r.getEnterActions());
        s.set("outInfos", r.getLeaveActions());
        s.set("firstEnterInfos", r.getFirstEnterActions());
        s.set("banInteractItems", new ArrayList<>(r.getBlockedInteractionMaterials()));
        s.set("kickWorld", r.getFallbackWorldName());
        s.set("kickX", r.getFallbackPosition().getX());
        s.set("kickY", r.getFallbackPosition().getY());
        s.set("kickZ", r.getFallbackPosition().getZ());
        s.set("kickFacePitch", r.getFallbackRotation().getX());
        s.set("kickFaceYaw", r.getFallbackRotation().getZ());
        for (Map.Entry<Long, List<String>> e : r.getRepeatingActions().entrySet())
            s.set("repeatInfos." + e.getKey(), e.getValue());
        if (r instanceof CuboidRegion) {
            double[] b = r.getBounds();
            String[] keys = {"X1", "Y1", "Z1", "X2", "Y2", "Z2"};
            for (int i = 0; i < 6; i++) s.set(keys[i], b[i]);
        } else {
            Point3D center =
                    r instanceof SphereRegion
                            ? ((SphereRegion) r).getCenter()
                            : ((CylinderRegion) r).getCenter();
            s.set("centerX", center.getX());
            s.set("centerY", center.getY());
            s.set("centerZ", center.getZ());
            s.set(
                    "radius",
                    r instanceof SphereRegion
                            ? ((SphereRegion) r).getRadius()
                            : ((CylinderRegion) r).getRadius());
            if (r instanceof CylinderRegion) s.set("height", ((CylinderRegion) r).getHeight());
        }
    }
}
