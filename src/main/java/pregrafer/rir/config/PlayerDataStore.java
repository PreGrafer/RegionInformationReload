package pregrafer.rir.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/** Main-thread snapshots, serialized disk writes. UUID visits and preferences survive relogs. */
public final class PlayerDataStore implements AutoCloseable {
    private final YamlConfiguration data;
    private final File file;
    private final Logger logger;
    private final ExecutorService writer =
            Executors.newSingleThreadExecutor(
                    r -> {
                        Thread t = new Thread(r, "RIR-data-writer");
                        t.setDaemon(true);
                        return t;
                    });
    private final Map<UUID, Set<String>> visits = new HashMap<>();
    private Future<?> pending;
    private long revision, savedRevision, pendingRevision;

    public PlayerDataStore(File file, Logger logger) throws Exception {
        this.file = file;
        this.logger = logger;
        data = file.exists() ? RegionRepository.loadYaml(file) : new YamlConfiguration();
    }

    public boolean areNotificationsEnabled(Player p, String type, boolean defaultValue) {
        return data.getBoolean(
                "Players." + p.getUniqueId() + ".notifications." + type, defaultValue);
    }

    public boolean toggleNotifications(Player p, String type, boolean defaultValue) {
        boolean value = !areNotificationsEnabled(p, type, defaultValue);
        data.set("Players." + p.getUniqueId() + ".notifications." + type, value);
        revision++;
        return value;
    }

    public boolean recordFirstVisit(Player player, String region) {
        UUID uuid = player.getUniqueId();
        String path = "Players." + uuid + ".visited";
        Set<String> seen =
                visits.computeIfAbsent(uuid, k -> new HashSet<>(data.getStringList(path)));
        if (!seen.add(region)) return false;
        boolean legacy =
                data.getStringList("LegacyNames." + region)
                        .contains(player.getName().toLowerCase(Locale.ROOT));
        data.set(path, new ArrayList<>(seen));
        revision++;
        return !legacy;
    }

    public boolean hasImportedBetterCommands() {
        return data.getBoolean("Migrations.betterCommands", false);
    }

    public void importLegacyVisits(YamlConfiguration old) {
        ConfigurationSection names = old.getConfigurationSection("Data");
        if (names != null)
            for (String id : names.getKeys(false)) {
                Set<String> merged = new HashSet<>(data.getStringList("LegacyNames." + id));
                for (String name : names.getStringList(id))
                    merged.add(name.toLowerCase(Locale.ROOT));
                data.set("LegacyNames." + id, new ArrayList<>(merged));
            }
        revision++;
    }

    public void markBetterCommandsImported() {
        data.set("Migrations.betterCommands", true);
        revision++;
    }

    public void unloadPlayerCache(UUID uuid) {
        visits.remove(uuid);
    }

    public void saveAsync() {
        if (pending != null) {
            if (!pending.isDone()) return;
            try {
                pending.get();
                savedRevision = pendingRevision;
            } catch (Exception e) {
                logger.severe("玩家数据写入失败，将重试: " + e.getMessage());
            }
            pending = null;
        }
        if (revision == savedRevision) return;
        String snapshot = data.saveToString();
        pendingRevision = revision;
        pending =
                writer.submit(
                        () -> {
                            AtomicFiles.writeAtomically(file.toPath(), snapshot);
                            return null;
                        });
    }

    public void saveSynchronously() throws Exception {
        if (pending != null) {
            try {
                pending.get();
            } catch (ExecutionException failure) {
                // A failed older snapshot must not prevent writing the latest data.
                logger.warning("此前异步保存失败，正在重新保存最新玩家数据: " + failure.getCause());
            } finally {
                if (pending.isDone()) pending = null;
            }
        }
        AtomicFiles.writeAtomically(file.toPath(), data.saveToString());
        savedRevision = revision;
    }

    public void close() {
        try {
            writer.shutdown();
            if (!writer.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.severe("玩家数据保存仍在进行，最新快照未能在关闭前写入");
                return;
            }
            saveSynchronously();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.severe("玩家数据保存被中断");
        } catch (Exception e) {
            logger.severe("关闭时玩家数据保存失败: " + e.getMessage());
        }
    }
}
