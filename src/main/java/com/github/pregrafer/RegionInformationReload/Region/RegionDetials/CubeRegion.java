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

    public CubeRegion(String uniqueId, String regionName, String world, String type, List<String> inInfos, List<String> outInfos, Point kickPoint, Point kickFace, Point point1, Point point2) {
        super(uniqueId, regionName, world, type, inInfos, outInfos, kickPoint, kickFace);
        this.point1 = new Point(Math.min(point1.getX(), point2.getX()), Math.min(point1.getY(), point2.getY()), Math.min(point1.getZ(), point2.getZ()));
        this.point2 = new Point(Math.max(point1.getX(), point2.getX()), Math.max(point1.getY(), point2.getY()), Math.max(point1.getZ(), point2.getZ()));
    }

    public Point getPoint1() {
        return this.point1;
    }

    public Point getPoint2() {
        return this.point2;
    }

    public boolean contains(Point point) {
        double x = point.getX();
        double y = point.getY();
        double z = point.getZ();
        return x >= this.point1.getX() && x <= this.point2.getX() && y >= this.point1.getY() && y <= this.point2.getY() && z >= this.point1.getZ() && z <= this.point2.getZ();
    }

    public boolean contains(double x, double y, double z) {
        return x >= this.point1.getX() && x <= this.point2.getX() && y >= this.point1.getY() && y <= this.point2.getY() && z >= this.point1.getZ() && z <= this.point2.getZ();
    }

    public boolean contains(Location location) {
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        return x >= this.point1.getX() && x <= this.point2.getX() && y >= this.point1.getY() && y <= this.point2.getY() && z >= this.point1.getZ() && z <= this.point2.getZ();
    }

    public double getVolume() {
        double dx = this.point2.getX() - this.point1.getX();
        double dy = this.point2.getY() - this.point1.getY();
        double dz = this.point2.getZ() - this.point1.getZ();
        return dx * dy * dz;
    }

    public void draw(Player player) {
        World world = Bukkit.getWorld(this.getWorld());
        new Location(world, this.point1.getX(), this.point2.getY(), this.point2.getZ());
        new Location(world, this.point2.getX(), this.point1.getY(), this.point1.getZ());
    }

    public String toString() {
        return super.toString() +
                "\nDetails{\n" +
                "Point1: (" + this.point1.getX() + "," + this.point1.getY() + "," + this.point1.getZ() + ")\n" +
                "Point2: (" + this.point2.getX() + "," + this.point2.getY() + "," + this.point2.getZ() + ")\n" +
                '}';
    }
}
