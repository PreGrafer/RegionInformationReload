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


/**
 * 监听玩家交互方块
 * 用于创建模式下选取方块
 */
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
                Point firstPoint = new Point(location.getX(), location.getY(), location.getZ());
                DataManager.getFirstPointList().put(player.getName(), firstPoint);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + DataManager.getCustomMessages().get("firstPoint").replace("%Point%", firstPoint.toString())));
            }
            if (action.equals(Action.RIGHT_CLICK_BLOCK)) {
                Location location = playerInteractEvent.getClickedBlock().getLocation();
                Point secondPoint = new Point(location.getX(), location.getY(), location.getZ());
                DataManager.getSecondPointList().put(player.getName(), secondPoint);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + DataManager.getCustomMessages().get("secondPoint").replace("%Point%", secondPoint.toString())));
            }
            playerInteractEvent.setCancelled(true);
        }
    }
}
