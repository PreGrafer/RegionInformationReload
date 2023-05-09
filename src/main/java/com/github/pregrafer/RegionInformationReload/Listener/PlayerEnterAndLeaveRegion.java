package com.github.pregrafer.RegionInformationReload.Listener;

import com.github.pregrafer.RegionInformationReload.Event.PlayerEnterRegionEvent;
import com.github.pregrafer.RegionInformationReload.Event.PlayerLeaveRegionEvent;
import com.github.pregrafer.RegionInformationReload.Manager.DataManager;
import com.github.pregrafer.RegionInformationReload.Manager.InfoManager;
import com.github.pregrafer.RegionInformationReload.Region.Region;
import com.github.pregrafer.RegionInformationReload.Tool.Point;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 处理玩家进入与离开区域监听器
 */
public class PlayerEnterAndLeaveRegion implements Listener {
    public PlayerEnterAndLeaveRegion() {
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEnterRegion(PlayerEnterRegionEvent playerEnterRegionEvent) {
        HashMap<String, String> playerRegionLoc = DataManager.getPlayerRegionLoc();
        Player player = playerEnterRegionEvent.getPlayer();
        Region region = playerEnterRegionEvent.getRegion();
        if (player.hasPermission("RIR.enter." + region.getUniqueId())) {
            playerRegionLoc.put(player.getName(), region.getUniqueId());
            if (!region.getUniqueId().isEmpty()) {
                List<String> inInfos = new ArrayList<>(region.getInInfos());
                inInfos.replaceAll(s -> ChatColor.translateAlternateColorCodes('&', s.replace("%name%", region.getRegionName())));
                (new InfoManager(player, inInfos)).sendInfos();
            }
        } else {
            Point kickPoint = region.getKickPoint();
            player.teleport(new Location(player.getWorld(), kickPoint.getX(), kickPoint.getY(), kickPoint.getZ()));
            List<String> kickInfos = new ArrayList<>(DataManager.getKickInfos());
            kickInfos.replaceAll(s -> ChatColor.translateAlternateColorCodes('&', s.replace("%name%", region.getRegionName())));
            (new InfoManager(player, kickInfos)).sendInfos();
        }

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLeaveRegion(PlayerLeaveRegionEvent playerLeaveRegionEvent) {
        HashMap<String, String> playerRegionLoc = DataManager.getPlayerRegionLoc();
        Player player = playerLeaveRegionEvent.getPlayer();
        Region region = playerLeaveRegionEvent.getRegion();
        List<String> outInfos = new ArrayList<>(region.getOutInfos());
        outInfos.replaceAll(s -> ChatColor.translateAlternateColorCodes('&', s.replace("%name%", region.getRegionName())));
        (new InfoManager(player, outInfos)).sendInfos();
        playerRegionLoc.remove(player.getName());
    }
}
