package com.github.pregrafer.RegionInformationReload.Region.RegionDetials;

import com.github.pregrafer.RegionInformationReload.Region.Region;
import com.github.pregrafer.RegionInformationReload.Tool.Point;
import org.bukkit.Location;

import java.util.List;

public class BallRegion extends Region {
    private final Point center;
    private final double radius;

    public BallRegion(String uniqueId, String regionName, String world, String type, List<String> inInfos, List<String> outInfos, Point kickPoint, Point center, double radius) {
        super(uniqueId, regionName, world, type, inInfos, outInfos, kickPoint);
        this.center = center;
        this.radius = radius;
    }

    public Point getCenter() {
        return this.center;
    }

    public double getRadius() {
        return this.radius;
    }

    public boolean contains(Point point) {
        return point.distance(this.center) <= this.radius;
    }

    public boolean contains(double x, double y, double z) {
        return this.center.distance(x, y, z) <= this.radius;
    }

    public boolean contains(Location location) {
        return this.center.distance(location.getX(), location.getY(), location.getZ()) <= this.radius;
    }

    public String toString() {
        return super.toString() + "\nDetails{\nCenter: (" + this.center.getX() + "," + this.center.getY() + "," + this.center.getZ() + ")\nradius: " + this.radius + '\n' + '}';
    }
}
