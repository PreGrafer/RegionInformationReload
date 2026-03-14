package com.github.pregrafer.RegionInformationReload.API;

import com.github.pregrafer.RegionInformationReload.Region.Region;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;

/**
 * RegionInformationReload API interface.
 */
public interface RegionAPI {

    /**
     * Get a region by its unique ID.
     *
     * @param id The unique ID of the region.
     * @return The Region object, or null if not found.
     */
    Region getRegion(String id);

    /**
     * Get all regions at a specific location.
     *
     * @param location The location to check.
     * @return A list of regions containing the location.
     */
    List<Region> getRegions(Location location);

    /**
     * Get all regions a player is currently inside.
     *
     * @param player The player to check.
     * @return A set of region IDs the player is currently in.
     */
    Set<String> getPlayerCurrentRegionIds(Player player);

    /**
     * Get all loaded regions.
     *
     * @return A list of all regions.
     */
    List<Region> getAllRegions();
}
