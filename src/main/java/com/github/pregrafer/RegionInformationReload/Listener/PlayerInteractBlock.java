package com.github.pregrafer.RegionInformationReload.Listener;

import com.github.pregrafer.RegionInformationReload.Manager.DataManager;
import com.github.pregrafer.RegionInformationReload.Tool.Point;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class PlayerInteractBlock implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent playerInteractEvent) {
        Player player = playerInteractEvent.getPlayer();
        if (DataManager.getCreateModeList().contains(player.getName()) &&
                playerInteractEvent.getItem() != null &&
                playerInteractEvent.getMaterial().name().equalsIgnoreCase(DataManager.getTool())) {
            Action action = playerInteractEvent.getAction();
            if (action.equals(Action.LEFT_CLICK_BLOCK)) {
                Location location = playerInteractEvent.getClickedBlock().getLocation();
                Point firstPoint = new Point(location.getX() + 0.5, location.getY() + 0.5, location.getZ() + 0.5);
                DataManager.getFirstPointList().put(player.getName(), firstPoint);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + DataManager.getCustomMessages().get("firstPoint").replace("%Point%", firstPoint.toString())));
            }
            if (action.equals(Action.RIGHT_CLICK_BLOCK)) {
                Location location = playerInteractEvent.getClickedBlock().getLocation();
                Point secondPoint = new Point(location.getX() + 0.5, location.getY() + 0.5, location.getZ() + 0.5);
                DataManager.getSecondPointList().put(player.getName(), secondPoint);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + DataManager.getCustomMessages().get("secondPoint").replace("%Point%", secondPoint.toString())));
            }
            playerInteractEvent.setCancelled(true);
        }
    }
}
