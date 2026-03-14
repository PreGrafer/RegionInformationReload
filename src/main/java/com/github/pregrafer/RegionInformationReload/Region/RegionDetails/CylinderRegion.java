package com.github.pregrafer.RegionInformationReload.Region.RegionDetails;

import com.github.pregrafer.RegionInformationReload.Region.Region;
import com.github.pregrafer.RegionInformationReload.Tool.Point;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;

public class CylinderRegion extends Region {
    private static final double PARTICLE_SPACING = 0.8D;
    private final Point center;
    private final double radius;
    private final double height;

    public CylinderRegion(String uniqueId, String regionName, String world, String type, List<String> inInfos, List<String> outInfos, String kickWorld, Point kickPoint, Point kickFace, Set<String> banInteractItems, Point center, double radius, double height) {
        super(uniqueId, regionName, world, type, inInfos, outInfos, kickWorld, kickPoint, kickFace, banInteractItems);
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

    @Override
    public void draw(Player player) {
        World world = Bukkit.getWorld(this.getWorld());
        if (world == null || !player.getWorld().equals(world) || radius <= 0 || height <= 0) {
            return;
        }

        int circleSteps = Math.max(20, (int) Math.ceil(2.0D * Math.PI * radius / PARTICLE_SPACING));
        int heightSteps = Math.max(1, (int) Math.ceil(height / PARTICLE_SPACING));
        double baseY = center.getY();
        double topY = center.getY() + height;

        for (int i = 0; i <= circleSteps; i++) {
            double angle = (2.0D * Math.PI * i) / (double) circleSteps;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double x = center.getX() + offsetX;
            double z = center.getZ() + offsetZ;

            player.spawnParticle(Particle.VILLAGER_HAPPY, new Location(world, x, baseY, z), 1, 0.0, 0.0, 0.0, 0.0);
            player.spawnParticle(Particle.VILLAGER_HAPPY, new Location(world, x, topY, z), 1, 0.0, 0.0, 0.0, 0.0);

            if (i % Math.max(1, circleSteps / 12) == 0) {
                for (int h = 0; h <= heightSteps; h++) {
                    double y = baseY + ((double) h / (double) heightSteps) * height;
                    player.spawnParticle(Particle.VILLAGER_HAPPY, new Location(world, x, y, z), 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        }
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
