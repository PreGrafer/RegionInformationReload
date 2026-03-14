package com.github.pregrafer.RegionInformationReload.Listener;

import com.github.pregrafer.RegionInformationReload.Event.PlayerEnterRegionEvent;
import com.github.pregrafer.RegionInformationReload.Event.PlayerLeaveRegionEvent;
import com.github.pregrafer.RegionInformationReload.Manager.DataManager;
import com.github.pregrafer.RegionInformationReload.Manager.InfoManager;
import com.github.pregrafer.RegionInformationReload.Region.Region;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.*;

import static org.bukkit.Bukkit.getPluginManager;

public class PlayerMove implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location location = event.getTo();
        if (location == null) {
            return;
        }
        if (location.getBlockX() != from.getBlockX() || location.getBlockY() != from.getBlockY() || location.getBlockZ() != from.getBlockZ()) {
            Player player = event.getPlayer();
            if (DataManager.isRegionActive()) {
                Map<String, Set<String>> playerRegionLoc = DataManager.getPlayerRegionLoc();
                DataManager.getPlayerRegionLoc().computeIfAbsent(player.getName(), k -> new HashSet<>());
                Set<String> newRegionId = new HashSet<>();
                Set<String> oldRegionId = new HashSet<>(playerRegionLoc.get(player.getName()));

                for (Region region : DataManager.getRegionsByWorld(location.getWorld().getName())) {
                    if (region.contains(location)) {
                        newRegionId.add(region.getUniqueId());
                    }
                }
                for (String regionId : newRegionId) {
                    if (!oldRegionId.contains(regionId)) {
                        Region newRegion = DataManager.getRegions().get(regionId);
                        PlayerEnterRegionEvent playerEnterRegionEvent = new PlayerEnterRegionEvent(player, newRegion);
                        getPluginManager().callEvent(playerEnterRegionEvent);
                        if (playerEnterRegionEvent.isCancelled()) {
                            event.setCancelled(true);
                        }
                    }
                }
                for (String regionId : oldRegionId) {
                    if (!newRegionId.contains(regionId)) {
                        Region oldRegion = DataManager.getRegions().get(regionId);
                        PlayerLeaveRegionEvent playerLeaveRegionEvent = new PlayerLeaveRegionEvent(player, oldRegion);
                        getPluginManager().callEvent(playerLeaveRegionEvent);
                    }
                }
            }
            if (DataManager.isBiomeActive()) {
                Map<String, String> biomes = DataManager.getBiomes();
                String newBiome = location.getBlock().getBiome().name();
                String oldBiome = DataManager.getOldBiomes().put(player.getName(), newBiome);
                if (oldBiome == null) {
                    return;
                }
                String oldBiomeCustomName = biomes.getOrDefault(oldBiome, oldBiome);
                String biomeCustomName = biomes.getOrDefault(newBiome, newBiome);
                if (newBiome.equals(oldBiome)) {
                    return;
                }
                if ((newBiome.contains(oldBiome) || oldBiome.contains(newBiome)) && (!DataManager.isBiomeHighAccuracy())) {
                    return;
                }
                if (biomes.containsKey(newBiome)) {
                    List<String> biomeInfos = new ArrayList<>(DataManager.getBiomeInfos());
                    biomeInfos.replaceAll(s -> ChatColor.translateAlternateColorCodes('&',
                            s.replace("%newBiome%", biomeCustomName).replace("%oldBiome%", oldBiomeCustomName)));
                    new InfoManager(player, biomeInfos).sendInfos();
                }
            }
        }
    }
}
