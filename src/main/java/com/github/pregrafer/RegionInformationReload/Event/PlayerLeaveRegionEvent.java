package com.github.pregrafer.RegionInformationReload.Event;

import com.github.pregrafer.RegionInformationReload.Region.Region;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import javax.annotation.Nonnull;

public class PlayerLeaveRegionEvent extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();
    private final Region region;

    public PlayerLeaveRegionEvent(Player player, Region region) {
        super(player);
        this.region = region;
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
}
