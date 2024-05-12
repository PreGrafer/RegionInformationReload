package com.github.pregrafer.RegionInformationReload.Task;

import com.github.pregrafer.RegionInformationReload.Event.PlayerEnterRegionEvent;
import com.github.pregrafer.RegionInformationReload.Event.PlayerLeaveRegionEvent;
import com.github.pregrafer.RegionInformationReload.Manager.DataManager;
import com.github.pregrafer.RegionInformationReload.Region.Region;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.bukkit.Bukkit.getPluginManager;


public class RegionTask extends BukkitRunnable {
    final Player player;
    Location location;
    String world;
    Set<String> oldRegionId = new HashSet<>();


    public RegionTask(Player player) {
        this.player = player;
        this.location = player.getLocation();
        this.world = location.getWorld().getName();
        DataManager.getPlayerRegionLoc().put(player.getName(), new HashSet<>());
    }

    private void updateLocation() {
        this.location = player.getLocation();
        this.world = location.getWorld().getName();
    }

    @Override
    public void run() {
        updateLocation();
        HashMap<String, Set<String>> playerRegionLoc = DataManager.getPlayerRegionLoc();
        Set<String> newRegionId = new HashSet<>();
        if (playerRegionLoc.containsKey(player.getName())) {
            oldRegionId = playerRegionLoc.get(player.getName());
        }
        for (Region region : DataManager.getRegions().values()) {
            if (region.getWorld().equalsIgnoreCase(world) && region.contains(location)) {
                newRegionId.add(region.getUniqueId());
            }
        }
        for (String regionId : newRegionId) {
            if (!oldRegionId.contains(regionId)) {
                Region newRegion = DataManager.getRegions().get(regionId);
                getPluginManager().callEvent(new PlayerEnterRegionEvent(player, newRegion));
            }
        }
        for (String regionId : oldRegionId) {
            if (!newRegionId.contains(regionId)) {
                Region oldRegion = DataManager.getRegions().get(regionId);
                getPluginManager().callEvent(new PlayerLeaveRegionEvent(player, oldRegion));
            }
        }
    }
}

