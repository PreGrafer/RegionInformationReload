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

public class BallRegion extends Region {
    private static final double PARTICLE_SPACING = 0.8D;
    private final Point center;
    private final double radius;

    public BallRegion(String uniqueId, String regionName, String world, String type, List<String> inInfos, List<String> outInfos, String kickWorld, Point kickPoint, Point kickFace, Set<String> banInteractItems, Point center, double radius) {
        super(uniqueId, regionName, world, type, inInfos, outInfos, kickWorld, kickPoint, kickFace, banInteractItems);
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

    @Override
    public void draw(Player player) {
        World world = Bukkit.getWorld(this.getWorld());
        if (world == null || !player.getWorld().equals(world) || radius <= 0) {
            return;
        }
        int steps = Math.max(24, (int) Math.ceil(2.0D * Math.PI * radius / PARTICLE_SPACING));
        for (int i = 0; i <= steps; i++) {
            double angle = (2.0D * Math.PI * i) / (double) steps;
            double cos = Math.cos(angle) * radius;
            double sin = Math.sin(angle) * radius;
            player.spawnParticle(Particle.VILLAGER_HAPPY, center.getX() + cos, center.getY() + sin, center.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            player.spawnParticle(Particle.VILLAGER_HAPPY, center.getX() + cos, center.getY(), center.getZ() + sin, 1, 0.0, 0.0, 0.0, 0.0);
            player.spawnParticle(Particle.VILLAGER_HAPPY, center.getX(), center.getY() + cos, center.getZ() + sin, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    public String toString() {
        return super.toString() +
                "\nDetails{\n" +
                "Center: (" + this.center.getX() + "," + this.center.getY() + "," + this.center.getZ() + ")\n" +
                "radius: " + this.radius + '\n' +
                '}';
    }
}
