package pregrafer.rir.config;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import pregrafer.rir.RirPlugin;

import java.io.File;
import java.nio.file.*;
import java.util.*;

public final class BetterCommandsImporter {
    private BetterCommandsImporter() {}

    public static void merge(YamlConfiguration target, YamlConfiguration old) {
        ConfigurationSection commands = old.getConfigurationSection("Commands");
        if (commands == null) throw new IllegalArgumentException("旧配置缺少 Commands 节点");
        for (String group : commands.getKeys(false)) {
            boolean first = group.equals("first");
            if (!first) {
                long period = Long.parseLong(group);
                if (period < 20 || period > Integer.MAX_VALUE)
                    throw new IllegalArgumentException("旧周期 " + group + " 无效，至少为 20 tick");
            }
            ConfigurationSection section = commands.getConfigurationSection(group);
            if (section == null) throw new IllegalArgumentException("Commands." + group + " 必须是映射");
            for (String id : section.getKeys(false)) {
                if (!RegionCodec.isValidId(id) || !target.isConfigurationSection("Regions." + id))
                    throw new IllegalArgumentException("旧附属引用的区域不存在: " + id);
                String path =
                        "Regions."
                                + id
                                + "."
                                + (first ? "firstEnterInfos" : "repeatInfos." + group);
                List<String> messages = RegionCodec.readActions(section, id);
                List<String> existing = target.getStringList(path);
                if (!existing.isEmpty() && !existing.equals(messages))
                    throw new IllegalArgumentException("配置冲突，未覆盖: " + path);
                target.set(path, messages);
            }
        }
        RegionCodec.decodeRegions(target.getConfigurationSection("Regions"));
    }

    public static void run(RirPlugin plugin) throws Exception {
        if (Bukkit.getPluginManager().isPluginEnabled("RIRBetterCommands"))
            throw new IllegalArgumentException(
                    "请先停服移除旧 RIRBetterCommands.jar，保留其数据目录，重启后再迁移，避免重复执行");
        if (plugin.getPlayerDataStore().hasImportedBetterCommands())
            throw new IllegalArgumentException("BetterCommands 已迁移，无需重复导入");
        File source =
                new File(plugin.getDataFolder().getParentFile(), "RIRBetterCommands/config.yml");
        if (!source.isFile()) throw new IllegalArgumentException("未找到 " + source.getPath());
        YamlConfiguration old = RegionRepository.loadYaml(source),
                next = RegionRepository.copyRegionConfig();
        merge(next, old);
        String stamp = String.valueOf(System.currentTimeMillis());
        File regions = new File(plugin.getDataFolder(), "regions.yml"),
                players = new File(plugin.getDataFolder(), "players.yml");
        plugin.getPlayerDataStore().saveSynchronously();
        Files.copy(
                regions.toPath(),
                new File(plugin.getDataFolder(), "regions.before-bettercommands-" + stamp + ".yml")
                        .toPath());
        Files.copy(
                players.toPath(),
                new File(plugin.getDataFolder(), "players.before-bettercommands-" + stamp + ".yml")
                        .toPath());
        // Persist legacy visit history first, before enabling any first-visit commands.
        plugin.getPlayerDataStore().importLegacyVisits(old);
        plugin.getPlayerDataStore().saveSynchronously();
        RegionRepository.saveRegions(next);
        // The last step marks completion. Re-running after an interrupted migration is safe.
        plugin.getPlayerDataStore().markBetterCommandsImported();
        plugin.getPlayerDataStore().saveSynchronously();
    }
}
