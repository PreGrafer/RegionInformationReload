package com.github.pregrafer.RegionInformationReload.Manager;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

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
        // %RIR_region%返回玩家所在的区域
        if (params.equalsIgnoreCase("region")) {
            if (DataManager.getPlayerRegionLoc().containsKey(player.getName())) {
                return DataManager.getRegions().get(DataManager.getPlayerRegionLoc().get(player.getName())).getRegionName();
            } else {
                return DataManager.getCustomMessages().get("noRegion");
            }
        }
        // %RIR_region%返回玩家所在的生态群系的自定义名称
        if (params.equalsIgnoreCase("biome")) {
            return DataManager.getBiomes().get(((Player) player).getLocation().getBlock().getBiome().name());
        }
        return null;
    }
}
