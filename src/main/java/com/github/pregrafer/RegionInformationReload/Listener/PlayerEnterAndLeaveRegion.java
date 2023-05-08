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

/**
 * 玩家与区域相关事件处理
 * 新建InfoManager对象处理信息
 */
public class PlayerEnterAndLeaveRegion implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEnterRegion(PlayerEnterRegionEvent playerEnterRegionEvent) {
        HashMap<String, String> playerRegionLoc = DataManager.getPlayerRegionLoc();
        Player player = playerEnterRegionEvent.getPlayer();
        Region region = playerEnterRegionEvent.getRegion();
        playerRegionLoc.put(player.getName(), region.getUniqueId());
        if (!region.getUniqueId().isEmpty()) {
            List<String> inInfos = new ArrayList<>(region.getInInfos());
            // 处理颜色字符和占位符
            inInfos.replaceAll(s -> ChatColor.translateAlternateColorCodes('&', s.replace("%name%", region.getRegionName())));
            // 初始化对象传入信息并发送至玩家
            new InfoManager(player, inInfos).sendInfos();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLeaveRegion(PlayerLeaveRegionEvent playerLeaveRegionEvent) {
        HashMap<String, String> playerRegionLoc = DataManager.getPlayerRegionLoc();
        Player player = playerLeaveRegionEvent.getPlayer();
        Region region = playerLeaveRegionEvent.getRegion();
        List<String> outInfos = new ArrayList<>(region.getOutInfos());
        outInfos.replaceAll(s -> ChatColor.translateAlternateColorCodes('&', s.replace("%name%", region.getRegionName())));
        new InfoManager(player, outInfos).sendInfos();
        playerRegionLoc.remove(player.getName());
    }
}
