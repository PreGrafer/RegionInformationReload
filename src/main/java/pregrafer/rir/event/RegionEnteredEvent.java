package pregrafer.rir.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import pregrafer.rir.region.Region;

/** Fired after actual position and all memberships are committed. Use this for rewards. */
public final class RegionEnteredEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Region region;

    public RegionEnteredEvent(Player player, Region region) {
        super(player);
        this.region = region;
    }

    public Region getRegion() {
        return region;
    }

    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
