package com.github.pregrafer.RegionInformationReload.Task;

import com.github.pregrafer.RegionInformationReload.Event.PlayerEnterRegionEvent;
import com.github.pregrafer.RegionInformationReload.Event.PlayerLeaveRegionEvent;
import com.github.pregrafer.RegionInformationReload.Manager.DataManager;
import com.github.pregrafer.RegionInformationReload.Region.Region;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Objects;

import static org.bukkit.Bukkit.getPluginManager;


public class RegionTask extends BukkitRunnable {
    final Player player;
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
        for (Region region : DataManager.getRegions().values()) {
            if (region.getWorld().equalsIgnoreCase(world) && region.contains(location)) {
                if (region.getUniqueId().equals(oldRegionId)) {
                    return;
                }
                if (!Objects.isNull(oldRegionId)) {
                    Region oldRegion = DataManager.getRegions().get(oldRegionId);
                    getPluginManager().callEvent(new PlayerLeaveRegionEvent(player, oldRegion));
                }
                getPluginManager().callEvent(new PlayerEnterRegionEvent(player, region));
                return;
            }
        }
        if (!Objects.isNull(oldRegionId)) {
            Region oldRegion = DataManager.getRegions().get(oldRegionId);
            getPluginManager().callEvent(new PlayerLeaveRegionEvent(player, oldRegion));
            oldRegionId = null;
        }
    }
}

