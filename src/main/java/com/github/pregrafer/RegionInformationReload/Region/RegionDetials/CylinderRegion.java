package com.github.pregrafer.RegionInformationReload.Region.RegionDetials;

import com.github.pregrafer.RegionInformationReload.Region.Region;
import com.github.pregrafer.RegionInformationReload.Tool.Point;
import org.bukkit.Location;

import java.util.List;

public class CylinderRegion extends Region {
    private final Point center;
    private final double radius;
    private final double height;

    public CylinderRegion(String uniqueId, String regionName, String world, String type, List<String> inInfos, List<String> outInfos, Point kickPoint, Point kickFace, Point center, double radius, double height) {
        super(uniqueId, regionName, world, type, inInfos, outInfos, kickPoint, kickFace);
        this.center = center;
        this.radius = radius;
        this.height = height;
    }

    public double getHeight() {
        return this.height;
    }

    public Point getCenter() {
        return this.center;
    }

    public double getRadius() {
        return this.radius;
    }

    public boolean contains(Point point) {
        return this.center.distance(point.getX(), this.center.getY(), point.getZ()) <= this.radius && this.center.getY() <= point.getY() && point.getY() <= this.center.getY() + this.height;
    }

    public boolean contains(double x, double y, double z) {
        return this.center.distance(x, this.center.getY(), z) <= this.radius && this.center.getY() <= y && y <= this.center.getY() + this.height;
    }

    public boolean contains(Location location) {
        return this.center.distance(location.getX(), this.center.getY(), location.getZ()) <= this.radius && this.center.getY() <= location.getY() && location.getY() <= this.center.getY() + this.height;
    }

    public String toString() {
        return super.toString() +
                "\nDetails{\n" +
                "Center: (" + this.center.getX() + "," + this.center.getY() + "," + this.center.getZ() + ")\n" +
                "radius: " + this.radius + '\n' +
                "height: " + this.height + '\n' +
                '}';
    }
}
