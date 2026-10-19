package pregrafer.rir.region;

import pregrafer.rir.geometry.Point3D;

import java.util.List;
import java.util.Set;

public class SphereRegion extends Region {
    public SphereRegion(
            String uniqueId,
            String name,
            String worldName,
            String type,
            List<String> enterActions,
            List<String> leaveActions,
            String fallbackWorldName,
            Point3D fallbackPosition,
            Point3D fallbackRotation,
            Set<String> blockedInteractionMaterials,
            Point3D center,
            double radius) {
        super(
                uniqueId,
                name,
                worldName,
                type,
                enterActions,
                leaveActions,
                fallbackWorldName,
                fallbackPosition,
                fallbackRotation,
                blockedInteractionMaterials);

        if (!Double.isFinite(radius) || radius <= 0)
            throw new IllegalArgumentException("radius must be positive and finite");
        this.center = copy(center);
        this.radius = radius;
        this.radiusSquared = radius * radius;
    }

    private final Point3D center;
    private final double radius, radiusSquared;

    public Point3D getCenter() {
        return copy(center);
    }

    public double getRadius() {
        return radius;
    }

    public boolean contains(double x, double y, double z) {
        double dx = x - center.getX(), dy = y - center.getY(), dz = z - center.getZ();
        return dx * dx + dy * dy + dz * dz <= radiusSquared;
    }

    public double[] getBounds() {
        return new double[] {
            center.getX() - radius,
            center.getY() - radius,
            center.getZ() - radius,
            center.getX() + radius,
            center.getY() + radius,
            center.getZ() + radius
        };
    }

    public boolean contains(Point3D point) {
        return contains(point.getX(), point.getY(), point.getZ());
    }

    private static Point3D copy(Point3D p) {
        return new Point3D(p.getX(), p.getY(), p.getZ());
    }
}
