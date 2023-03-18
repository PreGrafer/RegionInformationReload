package com.github.pregrafer.RegionInformationReload;

import com.github.pregrafer.RegionInformationReload.Listener.PlayerJoinAndQuit;
import com.github.pregrafer.RegionInformationReload.Manager.AreaManager;
import com.github.pregrafer.RegionInformationReload.Manager.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
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
        loadCS();
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

    private void loadCS() {
        config = getConfig();
        ConfigurationSection settings = config.getConfigurationSection("Settings");
        ConfigurationSection tips = config.getConfigurationSection("Tips");
        DataManager.setPluginPrefix(settings.getString("pluginPrefix", "&7[&aRegionInfoRe&7]"));
        DataManager.setBiomeSpeed(settings.getInt("biomeSpeed", 20));
        DataManager.setRegionSpeed(settings.getInt("regionSpeed", 20));
        DataManager.setBiomeHighAccuracy(settings.getBoolean("biomeHighAccuracy", false));
        DataManager.setBiomeInfos(tips.getStringList("biomeInfos"));
        DataManager.setBiomeSwitch(tips.getString("biomeSwitch", " 生物群系提示已%Action%!"));
        DataManager.setRegionSwitch(tips.getString("regionSwitch", " 区域提示已%Action%!"));
        DataManager.setAreaManager(new AreaManager());
        DataManager.getAreaManager().loadRegionsAndBiomes();
    }
}
