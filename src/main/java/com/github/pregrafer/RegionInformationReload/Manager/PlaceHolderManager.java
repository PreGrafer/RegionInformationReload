package com.github.pregrafer.RegionInformationReload.Manager;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

/**
 * 注册Papi变量
 */

public class PlaceHolderManager extends PlaceholderExpansion {
    @Override
    public String getIdentifier() {
        return "RIR";
    }

    @Override
    public String getAuthor() {
        return "pop2wo";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null || player.getName() == null) {
            return "";
        }
        // %RIR_region%返回玩家所在的区域
        if (params.equalsIgnoreCase("region")) {
            if (DataManager.getPlayerRegionLoc().containsKey(player.getName())) {
                Set<String> strings = DataManager.getPlayerRegionLoc().get(player.getName());
                if (!strings.isEmpty()) {
                    Set<String> regionNames = new HashSet<>();
                    for (String regionID : strings) {
                        regionNames.add(DataManager.getRegions().get(regionID).getRegionName());
                    }
                    return String.join(" ", regionNames);
                }

            }
            return DataManager.getCustomMessages().get("noRegion");
        }
        // %RIR_region%返回玩家所在的生态群系的自定义名称
        if (params.equalsIgnoreCase("biome")) {
            if (!(player instanceof Player) || !((Player) player).isOnline()) {
                return "";
            }
            return DataManager.getBiomes().get(((Player) player).getLocation().getBlock().getBiome().name());
        }
        return null;
    }
}
