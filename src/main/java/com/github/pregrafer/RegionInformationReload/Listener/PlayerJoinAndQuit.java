package com.github.pregrafer.RegionInformationReload.Listener;

import com.github.pregrafer.RegionInformationReload.Manager.DataManager;
import com.github.pregrafer.RegionInformationReload.RegionInformationReload;
import com.github.pregrafer.RegionInformationReload.Task.BiomeTask;
import com.github.pregrafer.RegionInformationReload.Task.RegionTask;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitScheduler;

public class PlayerJoinAndQuit implements Listener {
    RegionInformationReload instance = RegionInformationReload.getInstance();
    ConfigurationSection settings = instance.getConfig().getConfigurationSection("Settings");

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent playerJoinEvent) {
        Player player = playerJoinEvent.getPlayer();
        if (settings.getConfigurationSection("activeOnPlayerJoin").getBoolean("biomes")) {
            BiomeTask biomeTask = new BiomeTask(player);
            biomeTask.runTaskTimerAsynchronously(instance, DataManager.getBiomeSpeed(), DataManager.getBiomeSpeed());
            DataManager.getBiomeTasks().put(player.getName(), biomeTask.getTaskId());
        }
        if (settings.getConfigurationSection("activeOnPlayerJoin").getBoolean("regions")) {
            RegionTask locTask = new RegionTask(player);
            locTask.runTaskTimerAsynchronously(instance, DataManager.getRegionSpeed(), DataManager.getRegionSpeed());
            DataManager.getRegionTasks().put(player.getName(), locTask.getTaskId());
        }
    }

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
