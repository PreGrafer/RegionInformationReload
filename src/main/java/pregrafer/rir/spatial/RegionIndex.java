package pregrafer.rir.spatial;

import org.bukkit.Location;

import pregrafer.rir.region.Region;

import java.util.*;

/**
 * Immutable after construction. Large regions use one world entry instead of millions of chunk
 * entries.
 */
public final class RegionIndex {
    public static final Comparator<Region> ORDER =
            Comparator.comparingInt(Region::getPriority)
                    .reversed()
                    .thenComparing(Region::getUniqueId);
    private final Map<String, Map<Long, List<Region>>> chunks = new HashMap<>();
    private final Map<String, List<Region>> large = new HashMap<>();
    private final Map<String, List<Region>> worlds = new HashMap<>();
    private int entries;

    public RegionIndex(Collection<Region> regions, int maxChunks) {
        if (maxChunks < 1) throw new IllegalArgumentException("maxChunks must be positive");
        for (Region region : regions) {
            if (!region.isEnabled()) continue;
            worlds.computeIfAbsent(region.getWorldName(), k -> new ArrayList<>()).add(region);
            double[] b = region.getBounds();
            int x1 = toChunkCoordinate(b[0]),
                    z1 = toChunkCoordinate(b[2]),
                    x2 = toChunkCoordinate(b[3]),
                    z2 = toChunkCoordinate(b[5]);
            long width = (long) x2 - x1 + 1, depth = (long) z2 - z1 + 1;
            // Also cap total references across many medium-sized regions.
            if (width > maxChunks
                    || depth > maxChunks
                    || width * depth > maxChunks
                    || entries + width * depth > 262144) {
                large.computeIfAbsent(region.getWorldName(), k -> new ArrayList<>()).add(region);
                continue;
            }
            Map<Long, List<Region>> map =
                    chunks.computeIfAbsent(region.getWorldName(), k -> new HashMap<>());
            for (long x = x1; x <= x2; x++)
                for (long z = z1; z <= z2; z++) {
                    map.computeIfAbsent(packChunkKey((int) x, (int) z), k -> new ArrayList<>())
                            .add(region);
                    entries++;
                }
        }
        worlds.replaceAll(
                (k, v) -> {
                    v.sort(ORDER);
                    return Collections.unmodifiableList(v);
                });
        large.replaceAll((world, regionsInWorld) -> sortedSnapshot(regionsInWorld));
        for (Map<Long, List<Region>> map : chunks.values())
            map.replaceAll((chunk, regionsInChunk) -> sortedSnapshot(regionsInChunk));
    }

    private static List<Region> sortedSnapshot(List<Region> regions) {
        regions.sort(ORDER);
        return Collections.unmodifiableList(regions);
    }

    private static int toChunkCoordinate(double n) {
        return (int) Math.floor(n / 16.0);
    }

    private static long packChunkKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    public List<Region> findCandidates(String world, double x, double z) {
        Map<Long, List<Region>> map = chunks.get(world);
        List<Region> local =
                map == null
                        ? Collections.emptyList()
                        : map.getOrDefault(
                                packChunkKey(toChunkCoordinate(x), toChunkCoordinate(z)),
                                Collections.emptyList());
        List<Region> global = large.getOrDefault(world, Collections.emptyList());
        if (local.isEmpty()) return global;
        if (global.isEmpty()) return local;
        List<Region> result = new ArrayList<>(local.size() + global.size());
        int localIndex = 0, globalIndex = 0;
        while (localIndex < local.size() && globalIndex < global.size()) {
            if (ORDER.compare(local.get(localIndex), global.get(globalIndex)) <= 0)
                result.add(local.get(localIndex++));
            else result.add(global.get(globalIndex++));
        }
        while (localIndex < local.size()) result.add(local.get(localIndex++));
        while (globalIndex < global.size()) result.add(global.get(globalIndex++));
        return Collections.unmodifiableList(result);
    }

    public List<Region> findRegionsAt(Location at) {
        if (at == null || at.getWorld() == null) return Collections.emptyList();
        List<Region> result = new ArrayList<>();
        for (Region r : findCandidates(at.getWorld().getName(), at.getX(), at.getZ()))
            if (r.contains(at)) result.add(r);
        return result;
    }

    public List<Region> getWorldRegions(String name) {
        return worlds.getOrDefault(name, Collections.emptyList());
    }

    public int getEntryCount() {
        return entries;
    }
}
