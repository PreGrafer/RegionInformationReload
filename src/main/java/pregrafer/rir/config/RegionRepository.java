package pregrafer.rir.config;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.*;

import pregrafer.rir.RirPlugin;
import pregrafer.rir.message.MessageDispatcher;
import pregrafer.rir.region.Region;
import pregrafer.rir.spatial.RegionIndex;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Loads, validates and stores region configuration. Player sessions belong to PlayerTracker. */
public final class RegionRepository {
    private static FileConfiguration config = new YamlConfiguration();
    private static YamlConfiguration regionConfig = new YamlConfiguration();
    private static final HashMap<String, Region> regions = new LinkedHashMap<>();
    private static final Map<String, String> biomes = new HashMap<>(), messages = new HashMap<>();
    private static final Map<String, Region> regionView = Collections.unmodifiableMap(regions);
    private static RegionIndex index = new RegionIndex(Collections.emptyList(), 4096);
    private static int generation;

    private RegionRepository() {}

    public static YamlConfiguration loadYaml(File file)
            throws IOException, InvalidConfigurationException {
        YamlConfiguration value = new YamlConfiguration();
        value.load(file);
        return value;
    }

    public static void reload() {
        RirPlugin plugin = RirPlugin.getInstance();
        plugin.saveDefaultConfig();
        try {
            File configFile = new File(plugin.getDataFolder(), "config.yml"),
                    regionsFile = new File(plugin.getDataFolder(), "regions.yml");
            YamlConfiguration next = loadYaml(configFile);
            YamlConfiguration nextRegions = readRegionFile(regionsFile, next);
            validateSettings(next);
            Map<String, Region> loaded = decodeRegionConfig(nextRegions);
            RegionIndex nextIndex =
                    new RegionIndex(
                            loaded.values(),
                            next.getInt("Settings.index.maxChunksPerRegion", 4096));
            if (!regionsFile.exists()) {
                if (next.isConfigurationSection("Regions")) {
                    Path backup = new File(plugin.getDataFolder(), "config.pre-3.1.yml").toPath();
                    if (!Files.exists(backup)) Files.copy(configFile.toPath(), backup);
                }
                AtomicFiles.writeAtomically(regionsFile.toPath(), nextRegions.saveToString());
            }
            config = next;
            regionConfig = nextRegions;
            applyRegions(loaded, nextIndex);
            biomes.clear();
            ConfigurationSection b = config.getConfigurationSection("Biomes");
            if (b != null) for (String k : b.getKeys(false)) biomes.put(k, b.getString(k, k));
            messages.clear();
            ConfigurationSection tips = config.getConfigurationSection("Tips.Single");
            if (tips != null)
                for (String k : tips.getKeys(false)) messages.put(k, tips.getString(k, k));
            MessageDispatcher.clearCache();
        } catch (IOException | InvalidConfigurationException | IllegalArgumentException e) {
            throw new IllegalArgumentException("配置未应用，保留此前有效数据: " + e.getMessage(), e);
        }
    }

    private static void validateSettings(ConfigurationSection c) {
        for (String path :
                Arrays.asList(
                        "Settings",
                        "Settings.activeOnPlayerJoin",
                        "Settings.index",
                        "Settings.preview",
                        "Tips",
                        "Tips.Entry",
                        "Tips.Single",
                        "Biomes")) ConfigValidation.section(c, path);
        for (String path :
                Arrays.asList(
                        "Settings.enforceEntryPermissions",
                        "Settings.biomeHighAccuracy",
                        "Settings.activeOnPlayerJoin.biome",
                        "Settings.activeOnPlayerJoin.region")) ConfigValidation.bool(c, path);
        for (String path : Arrays.asList("Settings.pluginPrefix", "Settings.tool"))
            ConfigValidation.string(c, path);
        for (String path : Arrays.asList("Biomes", "Tips.Single")) {
            ConfigurationSection values = c.getConfigurationSection(path);
            if (values != null)
                for (String key : values.getKeys(false))
                    ConfigValidation.string(c, path + "." + key);
        }
        ConfigValidation.integer(c, "Settings.deniedMessageCooldown", 1, 72000);
        ConfigValidation.integer(c, "Settings.preview.maxParticles", 24, 1200);
        for (String path :
                new String[] {
                    "Settings.biomeSpeed",
                    "Settings.regionSpeed",
                    "Settings.index.maxChunksPerRegion"
                }) {
            ConfigValidation.integer(c, path, 1, 72000);
        }
        for (String p :
                new String[] {
                    "Settings.defaultRadius", "Settings.defaultRedius", "Settings.defaultHeight"
                }) {
            if (c.contains(p)
                    && (!(c.get(p) instanceof Number)
                            || !Double.isFinite(c.getDouble(p))
                            || c.getDouble(p) <= 0
                            || c.getDouble(p) > 30000000))
                throw new IllegalArgumentException(
                        p + " must be positive and finite, at most 30000000");
        }
        for (String path : new String[] {"Tips.Entry.biomeInfos", "Tips.Entry.kickInfos"})
            RegionCodec.readActions(c, path);
    }

    private static YamlConfiguration readRegionFile(File file, YamlConfiguration settings)
            throws IOException, InvalidConfigurationException {
        if (file.exists()) return loadYaml(file);
        ConfigValidation.section(settings, "Regions");
        YamlConfiguration regions = new YamlConfiguration();
        ConfigurationSection legacy = settings.getConfigurationSection("Regions");
        if (legacy == null) regions.createSection("Regions");
        else regions.createSection("Regions", legacy.getValues(true));
        return regions;
    }

    /** Read and validate disk configuration without writing files or replacing live state. */
    public static ConfigurationReport validateConfiguration() {
        File folder = RirPlugin.getInstance().getDataFolder();
        try {
            YamlConfiguration settings = loadYaml(new File(folder, "config.yml"));
            validateSettings(settings);
            Map<String, Region> loaded =
                    decodeRegionConfig(readRegionFile(new File(folder, "regions.yml"), settings));
            RegionIndex candidateIndex =
                    new RegionIndex(
                            loaded.values(),
                            settings.getInt("Settings.index.maxChunksPerRegion", 4096));
            List<String> warnings = new ArrayList<>();
            int active = 0;
            for (Region region : loaded.values()) {
                if (!region.isEnabled()) continue;
                active++;
                if (Bukkit.getWorld(region.getWorldName()) == null)
                    warnings.add(
                            "区域 " + region.getUniqueId() + " 的世界未加载: " + region.getWorldName());
            }
            return new ConfigurationReport(
                    loaded.size(), active, candidateIndex.getEntryCount(), warnings);
        } catch (IOException | InvalidConfigurationException e) {
            throw new IllegalArgumentException("配置检查失败: " + e.getMessage(), e);
        }
    }

    private static Map<String, Region> decodeRegionConfig(YamlConfiguration y) {
        if (y.contains("Regions") && !y.isConfigurationSection("Regions"))
            throw new IllegalArgumentException("Regions must be a mapping");
        return RegionCodec.decodeRegions(y.getConfigurationSection("Regions"));
    }

    private static void applyRegions(Map<String, Region> loaded, RegionIndex nextIndex) {
        for (Region region : loaded.values()) region.freeze();
        regions.clear();
        regions.putAll(loaded);
        index = nextIndex;
        generation++;
    }

    public static YamlConfiguration copyRegionConfig() {
        YamlConfiguration copy = new YamlConfiguration();
        try {
            copy.loadFromString(regionConfig.saveToString());
        } catch (InvalidConfigurationException e) {
            throw new IllegalStateException(e);
        }
        return copy;
    }

    public static void saveRegions(YamlConfiguration next) {
        Map<String, Region> loaded = decodeRegionConfig(next);
        RegionIndex nextIndex =
                new RegionIndex(
                        loaded.values(), config.getInt("Settings.index.maxChunksPerRegion", 4096));
        try {
            AtomicFiles.writeAtomically(
                    new File(RirPlugin.getInstance().getDataFolder(), "regions.yml").toPath(),
                    next.saveToString());
        } catch (IOException e) {
            throw new IllegalArgumentException("区域保存失败: " + e.getMessage(), e);
        }
        regionConfig = next;
        applyRegions(loaded, nextIndex);
        RirPlugin.getInstance().rebuildPlayerStates();
    }

    public static void saveRegion(Region region) {
        if (!RegionCodec.isValidId(region.getUniqueId())
                || regions.containsKey(region.getUniqueId()))
            throw new IllegalArgumentException("区域 ID 无效或已存在");
        YamlConfiguration next = copyRegionConfig();
        RegionCodec.encodeRegion(next.createSection("Regions." + region.getUniqueId()), region);
        saveRegions(next);
    }

    public static void deleteRegion(String id) {
        YamlConfiguration next = copyRegionConfig();
        next.set("Regions." + id, null);
        saveRegions(next);
    }

    public static RegionIndex getIndex() {
        return index;
    }

    public static int getGeneration() {
        return generation;
    }

    public static FileConfiguration getConfig() {
        return config;
    }

    public static Map<String, Region> getRegions() {
        return regionView;
    }

    public static List<Region> getRegionsByWorld(String world) {
        return index.getWorldRegions(world);
    }

    public static Map<String, String> getBiomes() {
        return biomes;
    }

    public static Map<String, String> getMessages() {
        return messages;
    }

    public static String getMessage(String key, String fallback) {
        return messages.getOrDefault(key, fallback);
    }

    public static String getPluginPrefix() {
        return config.getString("Settings.pluginPrefix", "&7[&aRegionInfoRe&7]");
    }

    public static String getSelectionTool() {
        return config.getString("Settings.tool", "GOLDEN_HOE");
    }

    public static int getBiomeCheckIntervalTicks() {
        return config.getInt("Settings.biomeSpeed", 100);
    }

    public static int getRegionCheckIntervalTicks() {
        return config.getInt("Settings.regionSpeed", 20);
    }

    public static boolean isBiomeNotificationEnabledByDefault() {
        return config.getBoolean("Settings.activeOnPlayerJoin.biome", true);
    }

    public static boolean isRegionNotificationEnabledByDefault() {
        return config.getBoolean("Settings.activeOnPlayerJoin.region", true);
    }

    public static boolean isBiomeHighAccuracyEnabled() {
        return config.getBoolean("Settings.biomeHighAccuracy", true);
    }

    public static double getDefaultRadius() {
        return config.getDouble(
                "Settings.defaultRadius", config.getDouble("Settings.defaultRedius", 10));
    }

    public static double getDefaultHeight() {
        return config.getDouble("Settings.defaultHeight", 10);
    }

    public static List<String> getBiomeActions() {
        return config.getStringList("Tips.Entry.biomeInfos");
    }

    public static List<String> getEntryDeniedActions() {
        return config.getStringList("Tips.Entry.kickInfos");
    }

    public static void clear() {
        regions.clear();
        biomes.clear();
        messages.clear();
        index = new RegionIndex(Collections.emptyList(), 4096);
        MessageDispatcher.clearCache();
    }
}
