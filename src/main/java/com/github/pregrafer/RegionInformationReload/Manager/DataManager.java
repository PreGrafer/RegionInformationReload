package com.github.pregrafer.RegionInformationReload.Manager;

import com.github.pregrafer.RegionInformationReload.Region.Region;
import com.github.pregrafer.RegionInformationReload.Region.RegionDetials.BallRegion;
import com.github.pregrafer.RegionInformationReload.Region.RegionDetials.CubeRegion;
import com.github.pregrafer.RegionInformationReload.Region.RegionDetials.CylinderRegion;
import com.github.pregrafer.RegionInformationReload.RegionInformationReload;
import com.github.pregrafer.RegionInformationReload.Tool.Point;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 管理所有数据
 */
public class DataManager {
    private static final HashMap<String, String> playerRegionLoc = new HashMap<>(); // 储存玩家所处区域
    private static final HashMap<String, Integer> biomeTasks = new HashMap<>(); // 储存玩家名与生态群系线程号
    private static final HashMap<String, Integer> regionTasks = new HashMap<>(); // 储存玩家名与区域线程号
    private static final HashMap<String, String> biomes = new HashMap<>(); // 储存生态群系ID与自定义名称
    private static final HashMap<String, Region> regions = new HashMap<>(); // 存储区域UID与区域对象 加载插件或reload时初始化
    private static final HashMap<String, String> customMessages = new HashMap<>(); // 储存所有单条自定义信息
    private static final HashMap<String, Point> firstPointList = new HashMap<>(); // 储存建造模式下玩家选取的第一个点
    private static final HashMap<String, Point> secondPointList = new HashMap<>(); // 储存建造模式下玩家选取的第二个点
    private static final List<String> createModeList = new ArrayList<>(); // 缓存建造模式下的玩家
    private static FileConfiguration config = RegionInformationReload.getInstance().getConfig(); // 配置文件对象
    private static String pluginPrefix; // 插件信息前缀 无需操作 存在setter与getter
    private static String tool; // 建造模式选取工具
    private static int biomeSpeed, regionSpeed; // 线程检测速度
    private static List<String> biomeInfos; // 生态群系自定义消息
    private static List<String> kickInfos; // 踢出区域时自定义消息
    private static List<String> helpTips; // help指令下的提示
    private static boolean biomeHighAccuracy; // 生态群系高精度设置
    private static boolean biomeActive; // 初始化开启生态群系
    private static boolean regionActive; // 初始化开启区域
    private static double defaultRedius, defaultHeight; //默认半径与高度

    public static List<String> getKickInfos() {
        return kickInfos;
    }

    public static void setKickInfos(List<String> kickInfos) {
        DataManager.kickInfos = kickInfos;
    }

    public static double getDefaultRedius() {
        return defaultRedius;
    }

    public static void setDefaultRedius(double defaultRedius) {
        DataManager.defaultRedius = defaultRedius;
    }

    public static double getDefaultHeight() {
        return defaultHeight;
    }

    public static void setDefaultHeight(double defaultHeight) {
        DataManager.defaultHeight = defaultHeight;
    }
/*
    下方大部分都是参数的getter与setter
     */

    public static List<String> getHelpTips() {
        return helpTips;
    }

    public static void setHelpTips(List<String> helpTips) {
        DataManager.helpTips = helpTips;
    }

    public static String getTool() {
        return tool;
    }

    public static void setTool(String tool) {
        DataManager.tool = tool;
    }

    public static HashMap<String, Point> getFirstPointList() {
        return firstPointList;
    }

    public static HashMap<String, Point> getSecondPointList() {
        return secondPointList;
    }

    public static List<String> getCreateModeList() {
        return createModeList;
    }

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

    // 清除所有缓存的玩家与区域数据 无用 考虑删除
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

    public static HashMap<String, String> getCustomMessages() {
        return customMessages;
    }

    // 获取所有自定义消息
    public static void setCustomMessages() {
        customMessages.clear();
        ConfigurationSection tips = config.getConfigurationSection("Tips");
        Map<String, Object> tipsMap = tips.getConfigurationSection("Single").getValues(true);
        for (String i : tipsMap.keySet()) {
            if (tipsMap.get(i) != null) {
                customMessages.put(i, String.valueOf(tipsMap.get(i)));
            } else {
                customMessages.put(i, i);
            }
        }
        setBiomeInfos(tips.getStringList("Entry.biomeInfos"));
        setKickInfos(tips.getStringList("Entry.kickInfos"));
        setHelpTips(tips.getStringList("Entry.helpTips"));
    }

    // 重载所有数据 不包括玩家线程
    public static void reload() {
        RegionInformationReload.getInstance().saveDefaultConfig();
        RegionInformationReload.getInstance().reloadConfig();
        config = RegionInformationReload.getInstance().getConfig();
        loadData();
        loadRegionsAndBiomes();
    }

    // 加载自定义设置
    public static void loadData() {
        ConfigurationSection settings = config.getConfigurationSection("Settings");
        setPluginPrefix(settings.getString("pluginPrefix", "&7[&aRegionInfoRe&7]"));
        setBiomeSpeed(settings.getInt("biomeSpeed", 20));
        setRegionSpeed(settings.getInt("regionSpeed", 20));
        setBiomeHighAccuracy(settings.getBoolean("biomeHighAccuracy", false));
        setBiomeActive(settings.getBoolean("activeOnPlayerJoin.biome", true));
        setRegionActive(settings.getBoolean("activeOnPlayerJoin.region", true));
        setTool(settings.getString("tool", "GOLD_HOE"));
        setDefaultRedius(settings.getDouble("defaultRedius", 10.0));
        setDefaultRedius(settings.getDouble("defaultHeight", 10.0));
        setCustomMessages();
    }

    // 加载区域设置与生态群系设置
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
                        regionSection.getString("kickWorld"),
                        new Point(regionSection.getDouble("kickX", 0),
                                regionSection.getDouble("kickY", 0),
                                regionSection.getDouble("kickZ", 0)),
                        new Point(regionSection.getDouble("kickFacePitch", 0),
                                0,
                                regionSection.getDouble("kickFaceYaw", 0)),
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
                        regionSection.getString("kickWorld"),
                        new Point(regionSection.getDouble("kickX", 0),
                                regionSection.getDouble("kickY", 0),
                                regionSection.getDouble("kickZ", 0)),
                        new Point(regionSection.getDouble("kickFacePitch", 0),
                                0,
                                regionSection.getDouble("kickFaceYaw", 0)),
                        new Point(regionSection.getDouble("centerX"),
                                regionSection.getDouble("centerY"),
                                regionSection.getDouble("centerZ")),
                        regionSection.getDouble("radius")
                ));
            } else if ("cylinder".equals(type)) {
                regions.put(regionUniqueId, new CylinderRegion(
                        regionUniqueId,
                        regionSection.getString("name"),
                        regionSection.getString("world"),
                        "cylinder",
                        regionSection.getStringList("inInfos"),
                        regionSection.getStringList("outInfos"),
                        regionSection.getString("kickWorld"),
                        new Point(regionSection.getDouble("kickX", 0),
                                regionSection.getDouble("kickY", 0),
                                regionSection.getDouble("kickZ", 0)),
                        new Point(regionSection.getDouble("kickFacePitch", 0),
                                0,
                                regionSection.getDouble("kickFaceYaw", 0)),
                        new Point(regionSection.getDouble("centerX"),
                                regionSection.getDouble("centerY"),
                                regionSection.getDouble("centerZ")),
                        regionSection.getDouble("radius"),
                        regionSection.getDouble("height")
                ));
            } else {
                regions.put(regionUniqueId, new Region(regionUniqueId,
                        regionSection.getString("name"),
                        regionSection.getString("world"),
                        "ERROR",
                        regionSection.getStringList("inInfos"),
                        regionSection.getStringList("outInfos"),
                        "ERROR",
                        new Point(),
                        new Point()
                ));
            }
        });
        for (String i : biomesMap.keySet()) {
            if (biomesMap.get(i) != null) {
                biomes.put(i, String.valueOf(biomesMap.get(i)));
            } else {
                biomes.put(i, i);
            }
        }
    }

    // 将一个region对象储存到配置文件中
    public static void saveRegion(Region newRegion) {
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
        } else if (newRegion instanceof CylinderRegion) {
            Point center = ((CylinderRegion) newRegion).getCenter();

            data.put("centerX", center.getX());
            data.put("centerY", center.getY());
            data.put("centerZ", center.getZ());
            data.put("radius", ((CylinderRegion) newRegion).getRadius());
            data.put("height", ((CylinderRegion) newRegion).getHeight());
        }

        data.put("inInfos", newRegion.getInInfos());
        data.put("outInfos", newRegion.getOutInfos());
        data.put("kickWorld", newRegion.getKickWorld());
        
        Point kickPoint = newRegion.getKickPoint();
        Point kickFace = newRegion.getKickFace();
        data.put("kickX", kickPoint.getX());
        data.put("kickY", kickPoint.getY());
        data.put("kickZ", kickPoint.getZ());
        data.put("kickFacePitch", kickFace.getX());
        data.put("kickFaceYaw", kickFace.getZ());

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            newSectionOfRegion.set(entry.getKey(), entry.getValue());
        }
        RegionInformationReload.getInstance().saveConfig();
    }
}
