package pregrafer.rir.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import pregrafer.rir.RirPlugin;
import pregrafer.rir.config.RegionRepository;
import pregrafer.rir.region.Region;
import pregrafer.rir.spatial.RegionIndex;
import pregrafer.rir.tracking.PlayerTracker;

import java.util.*;

public class DefaultRegionApi implements RegionApi {
    private final PlayerTracker tracker;

    public DefaultRegionApi(PlayerTracker tracker) {
        this.tracker = tracker;
    }

    public DefaultRegionApi() {
        this(RirPlugin.getInstance().getTracker());
    }

    public Region getRegion(String id) {
        return RegionRepository.getRegions().get(id);
    }

    public List<Region> getRegions(Location location) {
        return Collections.unmodifiableList(RegionRepository.getIndex().findRegionsAt(location));
    }

    public Set<String> getPlayerCurrentRegionIds(Player player) {
        if (player == null) return Collections.emptySet();
        return getPlayerCurrentRegionIds(player.getUniqueId());
    }

    public Set<String> getPlayerCurrentRegionIds(UUID playerId) {
        return tracker.getPlayerRegionIds(playerId);
    }

    public Set<UUID> getRegionPlayers(String regionId) {
        return tracker.getRegionPlayers(regionId);
    }

    public List<Region> getAllRegions() {
        List<Region> list = new ArrayList<>(RegionRepository.getRegions().values());
        list.sort(RegionIndex.ORDER);
        return Collections.unmodifiableList(list);
    }
}
