package pregrafer.rir.spatial;

import org.bukkit.entity.Player;

import pregrafer.rir.compat.ServerCompatibility;
import pregrafer.rir.config.RegionRepository;
import pregrafer.rir.geometry.Point3D;
import pregrafer.rir.region.*;
import pregrafer.rir.region.Region;

/** Fixed work/packet budget even for world-sized regions; only the requesting player sees it. */
public final class RegionPreview {
    private RegionPreview() {}

    public static void draw(Region r, Player p) {
        if (!r.getWorldName().equals(p.getWorld().getName())) return;
        int budget = RegionRepository.getConfig().getInt("Settings.preview.maxParticles", 240);
        budget = Math.max(24, Math.min(1200, budget));
        if (r instanceof CuboidRegion) {
            double[] b = r.getBounds();
            int steps = Math.max(2, budget / 12);
            for (int axis = 0; axis < 3; axis++)
                for (int a = 0; a < 2; a++)
                    for (int c = 0; c < 2; c++)
                        for (int i = 0; i < steps; i++) {
                            double[] v = {b[0], b[1], b[2]};
                            int other = (axis + 1) % 3, last = (axis + 2) % 3;
                            v[axis] = b[axis] + (b[axis + 3] - b[axis]) * i / (steps - 1.0);
                            v[other] = b[other + 3 * a];
                            v[last] = b[last + 3 * c];
                            ServerCompatibility.spawnPreviewParticle(p, v[0], v[1], v[2]);
                        }
        } else {
            boolean ball = r instanceof SphereRegion;
            if (!ball && !(r instanceof CylinderRegion)) return;
            Point3D center =
                    ball ? ((SphereRegion) r).getCenter() : ((CylinderRegion) r).getCenter();
            double radius =
                    ball ? ((SphereRegion) r).getRadius() : ((CylinderRegion) r).getRadius();
            int steps = budget / 3;
            for (int i = 0; i < steps; i++) {
                double angle = 2 * Math.PI * i / steps,
                        x = Math.cos(angle) * radius,
                        z = Math.sin(angle) * radius;
                ServerCompatibility.spawnPreviewParticle(
                        p, center.getX() + x, center.getY(), center.getZ() + z);
                if (ball) {
                    ServerCompatibility.spawnPreviewParticle(
                            p, center.getX() + x, center.getY() + z, center.getZ());
                    ServerCompatibility.spawnPreviewParticle(
                            p, center.getX(), center.getY() + x, center.getZ() + z);
                } else {
                    double height = ((CylinderRegion) r).getHeight();
                    ServerCompatibility.spawnPreviewParticle(
                            p, center.getX() + x, center.getY() + height, center.getZ() + z);
                    int side = i % 4;
                    double a = side * Math.PI / 2;
                    ServerCompatibility.spawnPreviewParticle(
                            p,
                            center.getX() + Math.cos(a) * radius,
                            center.getY() + height * i / (steps - 1.0),
                            center.getZ() + Math.sin(a) * radius);
                }
            }
        }
    }
}
