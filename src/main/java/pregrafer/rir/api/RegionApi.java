package pregrafer.rir.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import pregrafer.rir.region.Region;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Main-thread queries. Returned collections are snapshots and loaded region definitions are
 * read-only.
 */
public interface RegionApi {

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

    /** Query committed membership by UUID, without looking up a Bukkit Player. */
    Set<String> getPlayerCurrentRegionIds(UUID playerId);

    /** UUIDs of tracked players currently in this region; empty for unknown/disabled regions. */
    Set<UUID> getRegionPlayers(String regionId);

    /**
     * Get all loaded regions.
     *
     * @return A list of all regions.
     */
    List<Region> getAllRegions();
}
