package pregrafer.rir.listener;

import org.bukkit.event.*;
import org.bukkit.event.player.*;

import pregrafer.rir.RirPlugin;
import pregrafer.rir.tracking.PlayerTracker;

public final class PlayerMovementListener implements Listener {
    private final RirPlugin plugin;

    public PlayerMovementListener(RirPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void checkMovement(PlayerMoveEvent e) {
        if (e instanceof PlayerTeleportEvent
                || PlayerTracker.hasSamePosition(e.getFrom(), e.getTo())) return;
        if (!plugin.getTracker().canMoveTo(e.getPlayer(), e.getFrom(), e.getTo()))
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void checkTeleport(PlayerTeleportEvent e) {
        if (!plugin.getTracker().canMoveTo(e.getPlayer(), e.getFrom(), e.getTo()))
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent e) {
        if (!PlayerTracker.hasSamePosition(e.getFrom(), e.getTo()))
            plugin.getTracker().markPositionDirty(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        plugin.getTracker().markPositionDirty(e.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        plugin.getTracker().trackPlayer(e.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        plugin.getTracker().untrackPlayer(e.getPlayer());
        plugin.getSelections().remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        plugin.getTracker().markPositionDirty(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangeWorld(PlayerChangedWorldEvent e) {
        plugin.getTracker().markPositionDirty(e.getPlayer());
    }
}
