package com.github.pregrafer.RegionInformationReload.Region.RegionDetials;

import com.github.pregrafer.RegionInformationReload.Region.Region;
import com.github.pregrafer.RegionInformationReload.Tool.Point;
import org.bukkit.Location;

import java.util.List;

public class BallRegion extends Region {
    private final Point center;
    private final double radius;

    public BallRegion(String uniqueId, String regionName, String world, String type, List<String> inInfos, List<String> outInfos, Point center, double radius) {
        super(uniqueId, regionName, world, type, inInfos, outInfos);
        this.center = center;
        this.radius = radius;
    }

    public Point getCenter() {
        return center;
    }


    public double getRadius() {
        return radius;
    }

    @Override
    public boolean contains(Point point) {
        return point.distance(center) <= radius;
    }

    @Override
    public boolean contains(double x, double y, double z) {
        return center.distance(x, y, z) <= radius;
    }

    @Override
    public boolean contains(Location location) {
        return center.distance(location.getX(), location.getY(), location.getZ()) <= radius;
    }

    @Override
    public String toString() {
        return super.toString() +
                "\nDetails{\n" +
                "Center: (" + center.getX() + "," + center.getY() + "," + center.getZ() + ")\n" +
                "radius: " + radius + '\n' +
                '}';
    }

}
