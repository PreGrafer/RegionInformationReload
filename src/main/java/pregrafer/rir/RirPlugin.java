package pregrafer.rir;

import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import pregrafer.rir.api.*;
import pregrafer.rir.command.*;
import pregrafer.rir.config.PlayerDataStore;
import pregrafer.rir.config.RegionRepository;
import pregrafer.rir.integration.RegionExpansion;
import pregrafer.rir.listener.*;
import pregrafer.rir.spatial.RegionSelection;
import pregrafer.rir.tracking.PlayerTracker;

import java.io.File;
import java.util.*;

public class RirPlugin extends JavaPlugin {
    private static volatile RirPlugin instance;
    private volatile PlayerTracker tracker;
    private PlayerDataStore playerDataStore;
    private RegionExpansion placeholders;
    private final Map<UUID, RegionSelection> selections = new HashMap<>();

    public static RirPlugin getInstance() {
        return instance;
    }

    public PlayerTracker getTracker() {
        return tracker;
    }

    public PlayerDataStore getPlayerDataStore() {
        return playerDataStore;
    }

    public Map<UUID, RegionSelection> getSelections() {
        return selections;
    }

    public void rebuildPlayerStates() {
        if (tracker != null) tracker.rebuildPlayerStates();
    }

    public void reloadPlugin() {
        RegionRepository.reload();
        selections.clear();
        rebuildPlayerStates();
    }

    @Override
    public void onEnable() {
        instance = this;
        try {
            RegionRepository.reload();
            playerDataStore =
                    new PlayerDataStore(new File(getDataFolder(), "players.yml"), getLogger());
            tracker = new PlayerTracker(this, playerDataStore);
            RegionCommand command = new RegionCommand(this);
            getCommand("regioninformationreload").setExecutor(command);
            getCommand("regioninformationreload").setTabCompleter(command);
            getCommand("itemtype").setExecutor(new ItemTypeCommand());
            Bukkit.getPluginManager().registerEvents(new PlayerMovementListener(this), this);
            Bukkit.getPluginManager().registerEvents(new PlayerInteractionListener(this), this);
            Bukkit.getServicesManager()
                    .register(
                            RegionApi.class,
                            new DefaultRegionApi(tracker),
                            this,
                            ServicePriority.Normal);
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                placeholders = new RegionExpansion();
                placeholders.register();
            }
            for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers())
                tracker.trackPlayer(player);
            Bukkit.getScheduler().runTaskTimer(this, tracker, 1, 1);
            getLogger()
                    .info(
                            "Loaded "
                                    + RegionRepository.getRegions().size()
                                    + " regions; one shared tracker task.");
            if (Bukkit.getPluginManager().getPlugin("RIRBetterCommands") != null)
                getLogger()
                        .warning(
                                "RIRBetterCommands 已安装；迁移后请移除旧附属，避免重复指令。使用 /rir"
                                        + " importbettercommands。");
        } catch (Exception | LinkageError e) {
            getLogger().severe("启动失败: " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        Bukkit.getServicesManager().unregisterAll(this);
        if (tracker != null) tracker.stop();
        if (placeholders != null) placeholders.unregister();
        if (playerDataStore != null) playerDataStore.close();
        selections.clear();
        RegionRepository.clear();
        tracker = null;
        instance = null;
    }
}
