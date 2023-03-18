package com.github.pregrafer.RegionInformationReload.Task;

import com.github.pregrafer.RegionInformationReload.Manager.DataManager;
import com.github.pregrafer.RegionInformationReload.Manager.InfoManager;
import com.github.pregrafer.RegionInformationReload.Region.Region;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;


public class RegionTask extends BukkitRunnable {
    Player player;
    Location location;
    String world;
    String oldRegionId = null;


    public RegionTask(Player player) {
        this.player = player;
        this.location = player.getLocation();
        this.world = location.getWorld().getName();
    }

    private void updateLocation() {
        this.location = player.getLocation();
        this.world = location.getWorld().getName();
    }

    @Override
    public void run() {
        updateLocation();
        HashMap<String, String> playerRegionLoc = DataManager.getPlayerRegionLoc();
        if (playerRegionLoc.containsKey(player.getName())) {
            oldRegionId = playerRegionLoc.get(player.getName());
        }
        for (Region region : DataManager.getRegions()) {
            if (region.getWorld().equalsIgnoreCase(world) && region.contains(location)) {
                String uniqueId = region.getUniqueId();
                if (uniqueId.equals(oldRegionId)) {
                    return;
                }
                playerRegionLoc.put(player.getName(), uniqueId);
                if (!uniqueId.isEmpty()) {
                    List<String> inInfos = new ArrayList<>(region.getInInfos());
                    inInfos.replaceAll(s -> ChatColor.translateAlternateColorCodes('&', s.replace("%name%", region.getRegionName())));
                    new InfoManager(player, inInfos).sendInfos();
                }
                return;
            }
        }
        if (!Objects.isNull(oldRegionId)) {
            Region oldRegion = DataManager.getRegion(oldRegionId);
            oldRegionId = null;
            List<String> outInfos = new ArrayList<>(oldRegion.getOutInfos());
            outInfos.replaceAll(s -> ChatColor.translateAlternateColorCodes('&', s.replace("%name%", oldRegion.getRegionName())));
            new InfoManager(player, outInfos).sendInfos();
            playerRegionLoc.remove(player.getName());
        }
    }
}

