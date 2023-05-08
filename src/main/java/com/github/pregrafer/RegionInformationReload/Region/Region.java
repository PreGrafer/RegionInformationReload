package com.github.pregrafer.RegionInformationReload.Region;

import com.github.pregrafer.RegionInformationReload.Tool.Point;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * 区域的基类
 * 也用于处理未分类的错误区域
 */
public class Region {
    private String uniqueId; //唯一ID
    private String regionName; //自定义名称
    private String world; //所处世界
    private List<String> inInfos;
    private List<String> outInfos;
    private String type;

    public Region(String uniqueId, String regionName, String world, String type, List<String> inInfos, List<String> outInfos) {
        this.uniqueId = uniqueId;
        this.regionName = regionName;
        this.world = world;
        this.type = type;
        this.inInfos = inInfos;
        this.outInfos = outInfos;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getInInfos() {
        return inInfos;
    }

    public void setInInfos(List<String> inInfos) {
        this.inInfos = inInfos;
    }

    public List<String> getOutInfos() {
        return outInfos;
    }

    public void setOutInfos(List<String> outInfos) {
        this.outInfos = outInfos;
    }

    // 重写toString方法
    @Override
    public String toString() {
        return "Region{\n" +
                "uniqueId: " + uniqueId + '\n' +
                "regionName: " + regionName + '\n' +
                "world: " + world + '\n' +
                "type: " + type + '\n' +
                "inInfos: " + inInfos + '\n' +
                "outInfos: " + outInfos + '\n' +
                '}';
    }

    // 区域呈现 考虑使用粒子效果 未完成
    public void draw(Player player) {
        player.sendMessage("Draw the region.");
    }

    public boolean contains(Point point) {
        return false;
    }

    public boolean contains(double x, double y, double z) {
        return false;
    }

    public boolean contains(Location location) {
        return false;
    }
}
