package com.github.pregrafer.RegionInformationReload.Manager;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

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
        if (params.equalsIgnoreCase("region")) {
            if (DataManager.getPlayerRegionLoc().containsKey(player.getName())) {
                return DataManager.getRegions().get(DataManager.getPlayerRegionLoc().get(player.getName())).getRegionName();
            } else {
                return DataManager.getCustomMessages().get("noRegion");
            }
        }
        if (params.equalsIgnoreCase("biome")) {
            return DataManager.getBiomes().get(((Player) player).getLocation().getBlock().getBiome().name());
        }
        return null;
    }
}
