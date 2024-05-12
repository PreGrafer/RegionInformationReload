package com.github.pregrafer.RegionInformationReload.Event;

import com.github.pregrafer.RegionInformationReload.Region.Region;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import javax.annotation.Nonnull;

/**
 * 玩家进入区域事件
 */
public class PlayerEnterRegionEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Region region;
    private boolean cancelled;

    public PlayerEnterRegionEvent(Player player, Region region) {
        super(player);
        this.region = region;
        this.cancelled = false;
    }

    public static HandlerList getHandlerList() {
        // 事件类的「获取处理器」方法
        return handlers;
    }

    public Region getRegion() {
        return region;
    }

    @Override
    @Nonnull
    public HandlerList getHandlers() {
        // 事件对象的「获取处理器」方法
        return handlers;
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
