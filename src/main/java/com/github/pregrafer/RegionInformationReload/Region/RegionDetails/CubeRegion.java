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

public class CubeRegion extends Region {
    private static final double PARTICLE_SPACING = 0.9D;
    private final Point point1;
    private final Point point2;

    public CubeRegion(String uniqueId, String regionName, String world, String type, List<String> inInfos, List<String> outInfos, String kickWorld, Point kickPoint, Point kickFace, Set<String> banInteractItems, Point point1, Point point2) {
        super(uniqueId, regionName, world, type, inInfos, outInfos, kickWorld, kickPoint, kickFace, banInteractItems);
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
        if (world == null || !player.getWorld().equals(world)) {
            return;
        }

        double minX = point1.getX();
        double minY = point1.getY();
        double minZ = point1.getZ();
        double maxX = point2.getX();
        double maxY = point2.getY();
        double maxZ = point2.getZ();

        drawEdge(player, world, minX, minY, minZ, maxX, minY, minZ);
        drawEdge(player, world, minX, minY, maxZ, maxX, minY, maxZ);
        drawEdge(player, world, minX, maxY, minZ, maxX, maxY, minZ);
        drawEdge(player, world, minX, maxY, maxZ, maxX, maxY, maxZ);

        drawEdge(player, world, minX, minY, minZ, minX, maxY, minZ);
        drawEdge(player, world, maxX, minY, minZ, maxX, maxY, minZ);
        drawEdge(player, world, minX, minY, maxZ, minX, maxY, maxZ);
        drawEdge(player, world, maxX, minY, maxZ, maxX, maxY, maxZ);

        drawEdge(player, world, minX, minY, minZ, minX, minY, maxZ);
        drawEdge(player, world, maxX, minY, minZ, maxX, minY, maxZ);
        drawEdge(player, world, minX, maxY, minZ, minX, maxY, maxZ);
        drawEdge(player, world, maxX, maxY, minZ, maxX, maxY, maxZ);
    }

    private void drawEdge(Player player, World world, double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int steps = Math.max(1, (int) Math.ceil(length / PARTICLE_SPACING));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / (double) steps;
            double x = x1 + dx * t;
            double y = y1 + dy * t;
            double z = z1 + dz * t;
            player.spawnParticle(Particle.VILLAGER_HAPPY, new Location(world, x, y, z), 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    public String toString() {
        return super.toString() +
                "\nDetails{\n" +
                "Point1: (" + this.point1.getX() + "," + this.point1.getY() + "," + this.point1.getZ() + ")\n" +
                "Point2: (" + this.point2.getX() + "," + this.point2.getY() + "," + this.point2.getZ() + ")\n" +
                '}';
    }
}
