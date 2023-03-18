package com.github.pregrafer.RegionInformationReload;

import com.github.pregrafer.RegionInformationReload.Listener.PlayerJoinAndQuit;
import com.github.pregrafer.RegionInformationReload.Manager.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;


/**
 * @author Kerwin
 * JDK 1.8_351
 */

public class RegionInformationReload extends JavaPlugin {

    private static RegionInformationReload instance;
    FileConfiguration config;


    public static RegionInformationReload getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        register();
        DataManager.loadData();
        DataManager.loadRegionsAndBiomes();
        Bukkit.getLogger().info(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + " &bLoading RegionInformationRe..."));
    }

    @Override
    public void onDisable() {
        DataManager.cleanAllData();
        Bukkit.getLogger().info(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + " &bUnloading RegionInformationRe..."));
    }

    private void register() {
        Bukkit.getPluginManager().registerEvents(new PlayerJoinAndQuit(), instance);
    }

}
