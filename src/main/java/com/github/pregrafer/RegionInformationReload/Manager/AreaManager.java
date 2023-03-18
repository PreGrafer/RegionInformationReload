package com.github.pregrafer.RegionInformationReload.Manager;

import com.github.pregrafer.RegionInformationReload.Region.Region;
import com.github.pregrafer.RegionInformationReload.Region.RegionDetials.BallRegion;
import com.github.pregrafer.RegionInformationReload.Region.RegionDetials.CubeRegion;
import com.github.pregrafer.RegionInformationReload.RegionInformationReload;
import com.github.pregrafer.RegionInformationReload.Tool.Point;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

public class AreaManager {
    RegionInformationReload instance;
    ConfigurationSection regions;
    Map<String, Object> biomes;

    public AreaManager() {
        this.instance = RegionInformationReload.getInstance();
        this.regions = instance.getConfig().getConfigurationSection("Regions");
        this.biomes = instance.getConfig().getConfigurationSection("Biomes").getValues(true);
    }

    public void loadRegionsAndBiomes() {
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

    private void reloadRegionsAndBiomes() {
        DataManager.getRegions().clear();
        loadRegionsAndBiomes();
    }

    private void saveRegion(Region newRegion) {
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
