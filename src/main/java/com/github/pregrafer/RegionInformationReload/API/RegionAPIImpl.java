package com.github.pregrafer.RegionInformationReload.API;

import com.github.pregrafer.RegionInformationReload.Manager.DataManager;
import com.github.pregrafer.RegionInformationReload.Region.Region;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RegionAPIImpl implements RegionAPI {

    @Override
    public Region getRegion(String id) {
        return DataManager.getRegions().get(id);
    }

    @Override
    public List<Region> getRegions(Location location) {
        List<Region> result = new ArrayList<>();
        // Use optimized lookup if available, otherwise iterate all
        // For now, we will rely on DataManager. We will optimize DataManager next.
        // Once DataManager has regionsByWorld, we should use it here too.
        
        List<Region> worldRegions = DataManager.getRegionsByWorld(location.getWorld().getName());
        if (worldRegions == null) {
            return result;
        }
        
        for (Region region : worldRegions) {
            if (region.contains(location)) {
                result.add(region);
            }
        }
        return result;
    }

    @Override
    public Set<String> getPlayerCurrentRegionIds(Player player) {
        return DataManager.getPlayerRegionLoc().get(player.getName());
    }

    @Override
    public List<Region> getAllRegions() {
        return new ArrayList<>(DataManager.getRegions().values());
    }
}
