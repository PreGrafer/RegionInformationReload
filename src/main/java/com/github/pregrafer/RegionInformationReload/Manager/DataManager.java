package com.github.pregrafer.RegionInformationReload.Manager;

import com.github.pregrafer.RegionInformationReload.Region.Region;
import com.github.pregrafer.RegionInformationReload.Region.RegionDetials.BallRegion;
import com.github.pregrafer.RegionInformationReload.Region.RegionDetials.CubeRegion;
import com.github.pregrafer.RegionInformationReload.RegionInformationReload;
import com.github.pregrafer.RegionInformationReload.Tool.Point;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class DataManager {
    private static final HashMap<String, String> playerRegionLoc = new HashMap<>();
    private static final HashMap<String, Integer> biomeTasks = new HashMap<>();
    private static final HashMap<String, Integer> regionTasks = new HashMap<>();
    private static final HashMap<String, String> biomes = new HashMap<>();
    private static final Set<Region> regions = new HashSet<>();
    private static final FileConfiguration config = RegionInformationReload.getInstance().getConfig();
    private static String pluginPrefix;
    private static String biomeSwitch;
    private static String regionSwitch;
    private static int biomeSpeed, regionSpeed;
    private static List<String> biomeInfos;
    private static boolean biomeHighAccuracy;

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

    public static Region getRegion(String regionId) {
        for (Region region : regions) {
            if (region.getUniqueId().equalsIgnoreCase(regionId)) {
                return region;
            }
        }
        return null;
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

    public static Set<Region> getRegions() {
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
        ConfigurationSection regions = config.getConfigurationSection("Regions");
        regions.getKeys(false).stream().forEach(regionUniqueId -> {
            ConfigurationSection region = regions.getConfigurationSection(regionUniqueId);
            String type = region.getString("type");
            if ("cube".equals(type)) {
                DataManager.getRegions().add(new CubeRegion(
                        regionUniqueId,
                        region.getString("name"),
                        region.getString("world"),
                        "cube",
                        region.getStringList("inInfos"),
                        region.getStringList("outInfos"),
                        new Point(region.getDouble("X1"),
                                region.getDouble("Y1"),
                                region.getDouble("Z1")),
                        new Point(region.getDouble("X2"),
                                region.getDouble("Y2"),
                                region.getDouble("Z2"))
                ));
            } else if ("ball".equals(type)) {
                DataManager.getRegions().add(new BallRegion(
                        regionUniqueId,
                        region.getString("name"),
                        region.getString("world"),
                        "ball",
                        region.getStringList("inInfos"),
                        region.getStringList("outInfos"),
                        new Point(region.getDouble("centerX"),
                                region.getDouble("centerY"),
                                region.getDouble("centerZ")),
                        region.getDouble("radius")
                ));
            } else {
                DataManager.getRegions().add(new Region(region.getString("name"),
                        region.getString("world"),
                        "ERROR",
                        regionUniqueId,
                        region.getStringList("inInfos"),
                        region.getStringList("outInfos")
                ));
            }
        });
        for (String i : biomes.keySet()) {
            if (biomes.get(i) != null) {
                DataManager.getBiomes().put(i, String.valueOf(biomes.get(i)));
            } else {
                DataManager.getBiomes().put(i, " ");
            }
        }
    }


    private static void saveRegion(Region newRegion) {
        ConfigurationSection regions = config.getConfigurationSection("Regions");
        regions.createSection(newRegion.getUniqueId());
        ConfigurationSection newSectionOfRegion = regions.getConfigurationSection(newRegion.getUniqueId());

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
    }
}
