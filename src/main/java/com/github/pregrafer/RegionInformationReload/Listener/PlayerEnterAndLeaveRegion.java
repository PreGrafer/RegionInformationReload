package com.github.pregrafer.RegionInformationReload.Listener;

import com.github.pregrafer.RegionInformationReload.Event.PlayerEnterRegionEvent;
import com.github.pregrafer.RegionInformationReload.Event.PlayerLeaveRegionEvent;
import com.github.pregrafer.RegionInformationReload.Manager.DataManager;
import com.github.pregrafer.RegionInformationReload.Manager.InfoManager;
import com.github.pregrafer.RegionInformationReload.Region.Region;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * 处理玩家进入与离开区域监听器
 */
public class PlayerEnterAndLeaveRegion implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEnterRegion(PlayerEnterRegionEvent playerEnterRegionEvent) {
        HashMap<String, Set<String>> playerRegionLoc = DataManager.getPlayerRegionLoc();
        Player player = playerEnterRegionEvent.getPlayer();
        Region region = playerEnterRegionEvent.getRegion();
        if (player.hasPermission("RIR.enter." + region.getUniqueId())) {
            playerRegionLoc.get(player.getName()).add(region.getUniqueId());
            region.getPlayersInRegion().add(player.getName());
            if (!region.getUniqueId().isEmpty()) {
                List<String> inInfos = new ArrayList<>(region.getInInfos());
                inInfos.replaceAll(s -> ChatColor.translateAlternateColorCodes('&', s.replace("%name%", region.getRegionName())));
                (new InfoManager(player, inInfos)).sendInfos();
            }
        } else {
            playerEnterRegionEvent.setCancelled(true);
//            Point kickPoint = region.getKickPoint();
//            Point kickFace = region.getKickFace();
//            World kickWorld = Bukkit.getWorld(region.getKickWorld());
//            if (kickWorld == null || !Bukkit.getWorlds().contains(kickWorld)) {
//                kickWorld = player.getWorld();
//            }
//            player.teleport(new Location(kickWorld, kickPoint.getX(), kickPoint.getY(), kickPoint.getZ(), (float) kickFace.getX(), (float) kickFace.getZ()));
            List<String> kickInfos = new ArrayList<>(DataManager.getKickInfos());
            kickInfos.replaceAll(s -> ChatColor.translateAlternateColorCodes('&', s.replace("%name%", region.getRegionName())));
            (new InfoManager(player, kickInfos)).sendInfos();
        }

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLeaveRegion(PlayerLeaveRegionEvent playerLeaveRegionEvent) {
        HashMap<String, Set<String>> playerRegionLoc = DataManager.getPlayerRegionLoc();
        Player player = playerLeaveRegionEvent.getPlayer();
        Region region = playerLeaveRegionEvent.getRegion();
        List<String> outInfos = new ArrayList<>(region.getOutInfos());
        outInfos.replaceAll(s -> ChatColor.translateAlternateColorCodes('&', s.replace("%name%", region.getRegionName())));
        (new InfoManager(player, outInfos)).sendInfos();
        region.getPlayersInRegion().remove(player.getName());
        playerRegionLoc.get(player.getName()).remove(region.getUniqueId());
    }
}
