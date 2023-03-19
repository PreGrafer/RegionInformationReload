package com.github.pregrafer.RegionInformationReload.Manager;

import com.github.pregrafer.RegionInformationReload.Region.Region;
import com.github.pregrafer.RegionInformationReload.Region.RegionDetials.BallRegion;
import com.github.pregrafer.RegionInformationReload.Region.RegionDetials.CubeRegion;
import com.github.pregrafer.RegionInformationReload.RegionInformationReload;
import com.github.pregrafer.RegionInformationReload.Tool.Point;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataManager {
    private static final HashMap<String, String> playerRegionLoc = new HashMap<>();
    private static final HashMap<String, Integer> biomeTasks = new HashMap<>();
    private static final HashMap<String, Integer> regionTasks = new HashMap<>();
    private static final HashMap<String, String> biomes = new HashMap<>();
    private static final HashMap<String, Region> regions = new HashMap<>();
    private static final FileConfiguration config = RegionInformationReload.getInstance().getConfig();
    private static String pluginPrefix;
    private static String biomeSwitch;
    private static String regionSwitch;
    private static int biomeSpeed, regionSpeed;
    private static List<String> biomeInfos;
    private static boolean biomeHighAccuracy;
    private static boolean biomeActive;
    private static boolean regionActive;

    public static boolean isBiomeActive() {
        return biomeActive;
    }

    public static void setBiomeActive(boolean biomeActive) {
        DataManager.biomeActive = biomeActive;
    }

    public static boolean isRegionActive() {
        return regionActive;
    }

    public static void setRegionActive(boolean regionActive) {
        DataManager.regionActive = regionActive;
    }

    public static String getBiomeSwitch() {
        return biomeSwitch;
    }

    public static void setBiomeSwitch(String biomeSwitch) {
        DataManager.biomeSwitch = biomeSwitch;
    }

    public static String getRegionSwitch() {
        return regionSwitch;
    }

    public static void setRegionSwitch(String regionSwitch) {
        DataManager.regionSwitch = regionSwitch;
    }

    public static List<String> getBiomeInfos() {
        return biomeInfos;
    }

    public static void setBiomeInfos(List<String> biomeInfos) {
        DataManager.biomeInfos = biomeInfos;
    }

    public static boolean isBiomeHighAccuracy() {
        return biomeHighAccuracy;
    }

    public static void setBiomeHighAccuracy(boolean biomeHighAccuracy) {
        DataManager.biomeHighAccuracy = biomeHighAccuracy;
    }

    public static void cleanAllData() {
        playerRegionLoc.clear();
        biomeTasks.clear();
        regionTasks.clear();
        regions.clear();
        biomes.clear();
    }


    public static String getPluginPrefix() {
        return pluginPrefix;
    }

    public static void setPluginPrefix(String pluginPrefix) {
        DataManager.pluginPrefix = pluginPrefix;
    }

    public static int getBiomeSpeed() {
        return biomeSpeed;
    }

    public static void setBiomeSpeed(int biomeSpeed) {
        DataManager.biomeSpeed = biomeSpeed;
    }

    public static int getRegionSpeed() {
        return regionSpeed;
    }

    public static void setRegionSpeed(int regionSpeed) {
        DataManager.regionSpeed = regionSpeed;
    }

    public static HashMap<String, String> getPlayerRegionLoc() {
        return playerRegionLoc;
    }

    public static HashMap<String, Region> getRegions() {
        return regions;
    }

    public static HashMap<String, String> getBiomes() {
        return biomes;
    }

    public static HashMap<String, Integer> getBiomeTasks() {
        return biomeTasks;
    }

    public static HashMap<String, Integer> getRegionTasks() {
        return regionTasks;
    }

    public static void loadData() {
        ConfigurationSection settings = config.getConfigurationSection("Settings");
        ConfigurationSection tips = config.getConfigurationSection("Tips");
        setPluginPrefix(settings.getString("pluginPrefix", "&7[&aRegionInfoRe&7]"));
        setBiomeSpeed(settings.getInt("biomeSpeed", 20));
        setRegionSpeed(settings.getInt("regionSpeed", 20));
        setBiomeHighAccuracy(settings.getBoolean("biomeHighAccuracy", false));
        setBiomeActive(settings.getBoolean("activeOnPlayerJoin.biome", true));
        setRegionActive(settings.getBoolean("activeOnPlayerJoin.region", true));
        setBiomeInfos(tips.getStringList("biomeInfos"));
        setBiomeSwitch(tips.getString("biomeSwitch", " 生物群系提示已%Action%!"));
        setRegionSwitch(tips.getString("regionSwitch", " 区域提示已%Action%!"));
    }

    public static void reload() {
        RegionInformationReload.getInstance().reloadConfig();
        loadData();
        loadRegionsAndBiomes();
    }

    public static void loadRegionsAndBiomes() {
        regions.clear();
        biomes.clear();
        ConfigurationSection regionsSection = config.getConfigurationSection("Regions");
        Map<String, Object> biomesMap = config.getConfigurationSection("Biomes").getValues(true);
        regionsSection.getKeys(false).stream().forEach(regionUniqueId -> {
            ConfigurationSection regionSection = regionsSection.getConfigurationSection(regionUniqueId);
            String type = regionSection.getString("type");
            if ("cube".equals(type)) {
                regions.put(regionUniqueId, new CubeRegion(
                        regionUniqueId,
                        regionSection.getString("name"),
                        regionSection.getString("world"),
                        "cube",
                        regionSection.getStringList("inInfos"),
                        regionSection.getStringList("outInfos"),
                        new Point(regionSection.getDouble("X1"),
                                regionSection.getDouble("Y1"),
                                regionSection.getDouble("Z1")),
                        new Point(regionSection.getDouble("X2"),
                                regionSection.getDouble("Y2"),
                                regionSection.getDouble("Z2"))
                ));
            } else if ("ball".equals(type)) {
                regions.put(regionUniqueId, new BallRegion(
                        regionUniqueId,
                        regionSection.getString("name"),
                        regionSection.getString("world"),
                        "ball",
                        regionSection.getStringList("inInfos"),
                        regionSection.getStringList("outInfos"),
                        new Point(regionSection.getDouble("centerX"),
                                regionSection.getDouble("centerY"),
                                regionSection.getDouble("centerZ")),
                        regionSection.getDouble("radius")
                ));
            } else {
                regions.put(regionUniqueId, new Region(regionUniqueId,
                        regionSection.getString("name"),
                        regionSection.getString("world"),
                        "ERROR",
                        regionSection.getStringList("inInfos"),
                        regionSection.getStringList("outInfos")
                ));
            }
        });
        for (String i : biomesMap.keySet()) {
            if (biomesMap.get(i) != null) {
                biomes.put(i, String.valueOf(biomesMap.get(i)));
            } else {
                biomes.put(i, " ");
            }
        }
    }


    private static void saveRegion(Region newRegion) {
        ConfigurationSection regionsSection = config.getConfigurationSection("Regions");
        regionsSection.createSection(newRegion.getUniqueId());
        ConfigurationSection newSectionOfRegion = regionsSection.getConfigurationSection(newRegion.getUniqueId());

        Map<String, Object> data = new HashMap<>();
        data.put("name", newRegion.getRegionName());
        data.put("world", newRegion.getWorld());
        data.put("type", newRegion.getType());

        if (newRegion instanceof CubeRegion) {
            Point point1 = ((CubeRegion) newRegion).getPoint1();
            Point point2 = ((CubeRegion) newRegion).getPoint2();

            data.put("X1", point1.getX());
            data.put("Y1", point1.getY());
            data.put("Z1", point1.getZ());
            data.put("X2", point2.getX());
            data.put("Y2", point2.getY());
            data.put("Z2", point2.getZ());
        } else if (newRegion instanceof BallRegion) {
            Point center = ((BallRegion) newRegion).getCenter();

            data.put("centerX", center.getX());
            data.put("centerY", center.getY());
            data.put("centerZ", center.getZ());
            data.put("radius", ((BallRegion) newRegion).getRadius());
        }

        data.put("inInfos", newRegion.getInInfos());
        data.put("outInfos", newRegion.getOutInfos());

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            newSectionOfRegion.set(entry.getKey(), entry.getValue());
        }
        RegionInformationReload.getInstance().saveConfig();
    }
}
