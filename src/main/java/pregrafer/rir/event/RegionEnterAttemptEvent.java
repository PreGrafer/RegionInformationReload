package pregrafer.rir.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import pregrafer.rir.region.Region;

/** 玩家进入区域事件 */
public class RegionEnterAttemptEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Region region;
    private boolean cancelled;

    public RegionEnterAttemptEvent(Player player, Region region) {
        super(player);
        this.region = region;
        this.cancelled = false;
    }

    public static HandlerList getHandlerList() {
        // 事件类的「获取处理器」方法
        return HANDLERS;
    }

    public Region getRegion() {
        return region;
    }

    @Override
    public HandlerList getHandlers() {
        // 事件对象的「获取处理器」方法
        return HANDLERS;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
