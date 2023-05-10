package com.github.pregrafer.RegionInformationReload.Region;

import com.github.pregrafer.RegionInformationReload.Tool.Point;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class Region {
    private final Point kickPoint;
    private final Point kickFace;
    private final String kickWorld;
    private String uniqueId;
    private String regionName;
    private String world;
    private List<String> inInfos;
    private List<String> outInfos;
    private String type;
    public Region(String uniqueId, String regionName, String world, String type, List<String> inInfos, List<String> outInfos, String kickWorld, Point kickPoint, Point kickFace) {
        this.uniqueId = uniqueId;
        this.regionName = regionName;
        this.world = world;
        this.type = type;
        this.inInfos = inInfos;
        this.outInfos = outInfos;
        this.kickWorld = kickWorld;
        this.kickPoint = kickPoint;
        this.kickFace = kickFace;
    }

    public String getKickWorld() {
        return kickWorld;
    }

    public Point getKickFace() {
        return kickFace;
    }

    public Point getKickPoint() {
        return this.kickPoint;
    }

    public String getUniqueId() {
        return this.uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getRegionName() {
        return this.regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getWorld() {
        return this.world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getInInfos() {
        return this.inInfos;
    }

    public void setInInfos(List<String> inInfos) {
        this.inInfos = inInfos;
    }

    public List<String> getOutInfos() {
        return this.outInfos;
    }

    public void setOutInfos(List<String> outInfos) {
        this.outInfos = outInfos;
    }

    public String toString() {
        return "Region{\n" +
                "uniqueId: " + this.uniqueId + '\n' +
                "regionName: " + this.regionName + '\n' +
                "world: " + this.world + '\n' +
                "type: " + this.type + '\n' +
                "inInfos: " + this.inInfos + '\n' +
                "outInfos: " + this.outInfos + '\n' +
                "kickPoint: (" + this.kickPoint.getX() + "," + this.kickPoint.getY() + "," + this.kickPoint.getZ() + ")\n" + '}';
    }

    public void draw(Player player) {
        player.sendMessage("Draw the region.");
    }

    public boolean contains(Point point) {
        return false;
    }

    public boolean contains(double x, double y, double z) {
        return false;
    }

    public boolean contains(Location location) {
        return false;
    }
}
