package com.github.pregrafer.RegionInformationReload;

import com.github.pregrafer.RegionInformationReload.Command.MainCommand;
import com.github.pregrafer.RegionInformationReload.Listener.PlayerEnterAndLeaveRegion;
import com.github.pregrafer.RegionInformationReload.Listener.PlayerInteractBlock;
import com.github.pregrafer.RegionInformationReload.Listener.PlayerJoinAndQuit;
import com.github.pregrafer.RegionInformationReload.Manager.DataManager;
import com.github.pregrafer.RegionInformationReload.Manager.PlaceHolderManager;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import static org.bukkit.Bukkit.getPluginManager;


/**
 * @author Kerwin
 * JDK 1.8_361
 */

public class RegionInformationReload extends JavaPlugin {

    private static RegionInformationReload instance;


    public static RegionInformationReload getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        DataManager.reload();
        register();
        getLogger().info(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + " &bLoading RegionInformationRe..."));
    }

    @Override
    public void onDisable() {
        DataManager.cleanAllData();
        getLogger().info(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + " &bUnloading RegionInformationRe..."));
    }

    // 注册监听器与命令 依赖
    private void register() {
        if (getCommand("regioninformationreload") != null) {
            getCommand("regioninformationreload").setExecutor(new MainCommand());
        }
        getPluginManager().registerEvents(new PlayerEnterAndLeaveRegion(), instance);
        getPluginManager().registerEvents(new PlayerInteractBlock(), instance);
        getPluginManager().registerEvents(new PlayerJoinAndQuit(), instance);
        if (getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceHolderManager().register();
        }
    }
}
