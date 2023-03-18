package com.github.pregrafer.RegionInformationReload.Manager;

import com.github.pregrafer.RegionInformationReload.Region.Region;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DataManager {
    private static final HashMap<String, String> playerRegionLoc = new HashMap<>();
    private static final HashMap<String, Integer> biomeTasks = new HashMap<>();
    private static final HashMap<String, Integer> regionTasks = new HashMap<>();
    private static final HashMap<String, String> biomes = new HashMap<>();
    private static final Set<Region> regions = new HashSet<>();
    private static String pluginPrefix;
    private static String biomeSwitch;
    private static String regionSwitch;
    private static int biomeSpeed, regionSpeed;
    private static AreaManager areaManager;
    private static List<String> biomeInfos;
    private static boolean biomeHighAccuracy;

    public static AreaManager getAreaManager() {
        return areaManager;
    }

    public static void setAreaManager(AreaManager areaManager) {
        DataManager.areaManager = areaManager;
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

    public static void clean() {
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
}
