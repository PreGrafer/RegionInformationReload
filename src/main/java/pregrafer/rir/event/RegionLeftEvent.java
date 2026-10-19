package pregrafer.rir.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import pregrafer.rir.region.Region;

/** 玩家离开区域事件 */
public class RegionLeftEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Region region;

    public RegionLeftEvent(Player player, Region region) {
        super(player);
        this.region = region;
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
}
