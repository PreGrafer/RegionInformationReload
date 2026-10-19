package pregrafer.rir;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.Test;

import pregrafer.rir.geometry.Point3D;
import pregrafer.rir.region.*;
import pregrafer.rir.region.Region;
import pregrafer.rir.spatial.RegionIndex;

import java.util.*;

public class RegionIndexTest {
    private Region shape(String id, int type, String world, double x, double z, double size) {
        if (type == 0)
            return new CuboidRegion(
                    id,
                    id,
                    world,
                    "cube",
                    Collections.emptyList(),
                    Collections.emptyList(),
                    world,
                    new Point3D(),
                    new Point3D(),
                    Collections.emptySet(),
                    new Point3D(x, 10, z),
                    new Point3D(x + size, 30, z + size));
        if (type == 1)
            return new SphereRegion(
                    id,
                    id,
                    world,
                    "ball",
                    Collections.emptyList(),
                    Collections.emptyList(),
                    world,
                    new Point3D(),
                    new Point3D(),
                    Collections.emptySet(),
                    new Point3D(x, 20, z),
                    size);
        return new CylinderRegion(
                id,
                id,
                world,
                "cylinder",
                Collections.emptyList(),
                Collections.emptyList(),
                world,
                new Point3D(),
                new Point3D(),
                Collections.emptySet(),
                new Point3D(x, 10, z),
                size,
                20);
    }

    @Test
    public void randomizedIndexMatchesFullScanIncludingNegativeChunksAndHugeRegions() {
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        Random random = new Random(918);
        List<Region> regions = new ArrayList<>();
        for (int i = 0; i < 900; i++)
            regions.add(
                    shape(
                            "r" + i,
                            i % 3,
                            i % 5 == 0 ? "other" : "world",
                            random.nextInt(2048) - 1024,
                            random.nextInt(2048) - 1024,
                            random.nextInt(64) + 1));
        regions.add(shape("huge", 0, "world", -30000000, -30000000, 60000000));
        RegionIndex index = new RegionIndex(regions, 64);
        for (int n = 0; n < 2000; n++) {
            Location at =
                    new Location(
                            world,
                            random.nextDouble() * 2048 - 1024,
                            random.nextDouble() * 60,
                            random.nextDouble() * 2048 - 1024);
            List<Region> expected = new ArrayList<>();
            for (Region r : regions) if (r.contains(at)) expected.add(r);
            expected.sort(RegionIndex.ORDER);
            assertEquals(expected, index.findRegionsAt(at));
        }
        assertTrue(index.getEntryCount() < 900 * 64);
    }

    @Test
    public void sparseTenThousandRegionsNeedOnlyOneCandidate() {
        List<Region> regions = new ArrayList<>();
        for (int x = 0; x < 100; x++)
            for (int z = 0; z < 100; z++)
                regions.add(shape(x + "_" + z, 0, "world", x * 64, z * 64, 8));
        RegionIndex index = new RegionIndex(regions, 64);
        assertEquals(10000, index.getEntryCount());
        assertEquals(1, index.findCandidates("world", 64 * 50 + 4, 64 * 50 + 4).size());
    }

    @Test
    public void boundaryPrecisionWorldAndDefensiveGeometry() {
        World world = mock(World.class), other = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(other.getName()).thenReturn("other");
        Region sphere = shape("ball", 1, "world", -16, 0, 0.2);
        assertTrue(sphere.contains(-16.1, 20, 0));
        assertFalse(sphere.contains(-16.3, 20, 0));
        assertFalse(sphere.contains(new Location(other, -16, 20, 0)));
        assertFalse(sphere.contains((Location) null));
        ((SphereRegion) sphere).getCenter().setX(100);
        assertTrue(sphere.contains(-16, 20, 0));
        RegionIndex index = new RegionIndex(Collections.singleton(sphere), 16);
        assertEquals(1, index.findRegionsAt(new Location(world, -16.1, 20, 0)).size());
        assertEquals(1, index.findRegionsAt(new Location(world, -15.9, 20, 0)).size());
        assertTrue(shape("c", 2, "world", 0, 0, 2).contains(0, 30, 0));
        assertFalse(shape("c", 2, "world", 0, 0, 2).contains(0, 30.01, 0));
    }

    @Test
    public void priorityAndDisabledRegions() {
        Region a = shape("a", 0, "world", 0, 0, 16),
                b = shape("b", 0, "world", 0, 0, 16),
                disabled = shape("c", 0, "world", 0, 0, 16);
        b.configure(10, true, "", Collections.emptyList(), Collections.emptyMap());
        disabled.configure(99, false, "", Collections.emptyList(), Collections.emptyMap());
        assertEquals(
                Arrays.asList(b, a),
                new RegionIndex(Arrays.asList(a, b, disabled), 16).findCandidates("world", 16, 16));
    }
}
