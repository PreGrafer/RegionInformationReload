package com.github.pregrafer.RegionInformationReload.Listener;

import com.github.pregrafer.RegionInformationReload.Manager.DataManager;
import com.github.pregrafer.RegionInformationReload.RegionInformationReload;
import com.github.pregrafer.RegionInformationReload.Task.BiomeTask;
import com.github.pregrafer.RegionInformationReload.Task.RegionTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitScheduler;

/**
 * 插件中只涉及了两处线程操作
 * 这是其中一处 另一处位于MainCommand.java源码
 */
public class PlayerJoinAndQuit implements Listener {
    RegionInformationReload instance = RegionInformationReload.getInstance();

    /**
     * 玩家进入服务器事件处理
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent playerJoinEvent) {
        Player player = playerJoinEvent.getPlayer();
        // 获取群系与区域初始化开启设置
        if (DataManager.isBiomeActive()) {
            // 新建线程 将线程号传入DataManegr储存
            BiomeTask biomeTask = new BiomeTask(player);
            biomeTask.runTaskTimer(instance, DataManager.getBiomeSpeed(), DataManager.getBiomeSpeed());
            DataManager.getBiomeTasks().put(player.getName(), biomeTask.getTaskId());
        }
        if (DataManager.isRegionActive()) {
            RegionTask regionTask = new RegionTask(player);
            regionTask.runTaskTimer(instance, DataManager.getRegionSpeed(), DataManager.getRegionSpeed());
            DataManager.getRegionTasks().put(player.getName(), regionTask.getTaskId());
        }
    }

    /**
     * 玩家离开服务器设置
     * 关闭相应线程
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent playerQuitEvent) {
        Player player = playerQuitEvent.getPlayer();
        BukkitScheduler scheduler = Bukkit.getScheduler();
        if (DataManager.getBiomeTasks().containsKey(player.getName())) {
            scheduler.cancelTask(DataManager.getBiomeTasks().get(player.getName()));
            DataManager.getBiomeTasks().remove(player.getName());
        }
        if (DataManager.getRegionTasks().containsKey(player.getName())) {
            scheduler.cancelTask(DataManager.getRegionTasks().get(player.getName()));
            DataManager.getRegionTasks().remove(player.getName());
        }
    }
}
