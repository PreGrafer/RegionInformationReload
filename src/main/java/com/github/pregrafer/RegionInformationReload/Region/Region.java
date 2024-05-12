package com.github.pregrafer.RegionInformationReload.Region;

import com.github.pregrafer.RegionInformationReload.Tool.Point;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Region {
    private final Point kickPoint;
    private final Point kickFace;
    private final String kickWorld;
    private final Set<String> playersInRegion = new HashSet<>();
    private final String uniqueId;
    private final String regionName;
    private final String world;
    private final List<String> inInfos;
    private final List<String> outInfos;
    private final String type;
    private final Set<String> banInteractItems;

    public Region(String uniqueId, String regionName, String world, String type, List<String> inInfos, List<String> outInfos, String kickWorld, Point kickPoint, Point kickFace, Set<String> banInteractItems) {
        this.uniqueId = uniqueId;
        this.regionName = regionName;
        this.world = world;
        this.type = type;
        this.inInfos = inInfos;
        this.outInfos = outInfos;
        this.kickWorld = kickWorld;
        this.kickPoint = kickPoint;
        this.kickFace = kickFace;
        this.banInteractItems = banInteractItems;
    }

    public Set<String> getBanInteractItems() {
        return banInteractItems;
    }

    public Set<String> getPlayersInRegion() {
        return playersInRegion;
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

    public String getRegionName() {
        return this.regionName;
    }

    public String getWorld() {
        return this.world;
    }

    public String getType() {
        return this.type;
    }

    public List<String> getInInfos() {
        return this.inInfos;
    }

    public List<String> getOutInfos() {
        return this.outInfos;
    }

    public String toString() {
        String players = "";
        for (String player : playersInRegion) {
            players = players + player + ' ';
        }
        return "Region{\n" +
                "uniqueId: " + this.uniqueId + '\n' +
                "regionName: " + this.regionName + '\n' +
                "world: " + this.world + '\n' +
                "type: " + this.type + '\n' +
                "inInfos: " + this.inInfos + '\n' +
                "outInfos: " + this.outInfos + '\n' +
                "kickWorld: " + this.kickWorld + '\n' +
                "kickPoint: (" + this.kickPoint.getX() + "," + this.kickPoint.getY() + "," + this.kickPoint.getZ() + ")\n" +
                "kickFace: (" + this.kickFace.getX() + "," + this.kickPoint.getZ() + ")\n" +
                "playersInRegion: (" + players.trim() + ")\n" +
                '}';
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
