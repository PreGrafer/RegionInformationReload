package pregrafer.rir.region;

import pregrafer.rir.geometry.Point3D;

import java.util.List;
import java.util.Set;

public class CuboidRegion extends Region {
    public CuboidRegion(
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
            Point3D point1,
            Point3D point2) {
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

        this.point1 =
                new Point3D(
                        Math.min(point1.getX(), point2.getX()),
                        Math.min(point1.getY(), point2.getY()),
                        Math.min(point1.getZ(), point2.getZ()));
        this.point2 =
                new Point3D(
                        Math.max(point1.getX(), point2.getX()),
                        Math.max(point1.getY(), point2.getY()),
                        Math.max(point1.getZ(), point2.getZ()));
    }

    private final Point3D point1, point2;

    public Point3D getPoint1() {
        return copy(point1);
    }

    public Point3D getPoint2() {
        return copy(point2);
    }

    public boolean contains(double x, double y, double z) {
        return x >= point1.getX()
                && x <= point2.getX()
                && y >= point1.getY()
                && y <= point2.getY()
                && z >= point1.getZ()
                && z <= point2.getZ();
    }

    public double getVolume() {
        return (point2.getX() - point1.getX())
                * (point2.getY() - point1.getY())
                * (point2.getZ() - point1.getZ());
    }

    public double[] getBounds() {
        return new double[] {
            point1.getX(), point1.getY(), point1.getZ(), point2.getX(), point2.getY(), point2.getZ()
        };
    }

    public boolean contains(Point3D point) {
        return contains(point.getX(), point.getY(), point.getZ());
    }

    private static Point3D copy(Point3D p) {
        return new Point3D(p.getX(), p.getY(), p.getZ());
    }
}
