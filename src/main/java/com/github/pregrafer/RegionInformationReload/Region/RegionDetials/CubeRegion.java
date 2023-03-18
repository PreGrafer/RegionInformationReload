package com.github.pregrafer.RegionInformationReload.Region.RegionDetials;

import com.github.pregrafer.RegionInformationReload.Region.Region;
import com.github.pregrafer.RegionInformationReload.Tool.Point;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

public class CubeRegion extends Region {
    private final Point point1;
    private final Point point2;

    public CubeRegion(String uniqueId, String regionName, String world, String type, List<String> inInfos, List<String> outInfos, Point point1, Point point2) {
        super(uniqueId, regionName, world, type, inInfos, outInfos);
        this.point1 = new Point(Math.min(point1.getX(), point2.getX()), Math.min(point1.getY(), point2.getY()), Math.min(point1.getZ(), point2.getZ()));
        this.point2 = new Point(Math.max(point1.getX(), point2.getX()), Math.max(point1.getY(), point2.getY()), Math.max(point1.getZ(), point2.getZ()));
    }

    public Point getPoint1() {
        return point1;
    }

    public Point getPoint2() {
        return point2;
    }

    @Override
    public boolean contains(Point point) {
        double x = point.getX();
        double y = point.getY();
        double z = point.getZ();
        return x >= point1.getX() && x <= point2.getX()
                && y >= point1.getY() && y <= point2.getY()
                && z >= point1.getZ() && z <= point2.getZ();
    }

    @Override
    public boolean contains(double x, double y, double z) {
        return x >= point1.getX() && x <= point2.getX()
                && y >= point1.getY() && y <= point2.getY()
                && z >= point1.getZ() && z <= point2.getZ();
    }

    @Override
    public boolean contains(Location location) {
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        return x >= point1.getX() && x <= point2.getX()
                && y >= point1.getY() && y <= point2.getY()
                && z >= point1.getZ() && z <= point2.getZ();
    }

    public double getVolume() {
        double dx = point2.getX() - point1.getX();
        double dy = point2.getY() - point1.getY();
        double dz = point2.getZ() - point1.getZ();
        return dx * dy * dz;
    }

    @Override
    public void draw(Player player) {
        World world = Bukkit.getWorld(this.getWorld());
        Location loc1 = new Location(world, point1.getX(), point2.getY(), point2.getZ());
        Location loc2 = new Location(world, point2.getX(), point1.getY(), point1.getZ());
    }

    @Override
    public String toString() {
        return super.toString() +
                "\nDetails{\n" +
                "Point1: (" + point1.getX() + "," + point1.getY() + "," + point1.getZ() + ")\n" +
                "Point2: (" + point2.getX() + "," + point2.getY() + "," + point2.getZ() + ")\n" +
                '}';
    }
}
