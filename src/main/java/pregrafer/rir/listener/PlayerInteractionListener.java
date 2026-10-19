package pregrafer.rir.listener;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import pregrafer.rir.RirPlugin;
import pregrafer.rir.compat.ServerCompatibility;
import pregrafer.rir.config.RegionRepository;
import pregrafer.rir.region.Region;
import pregrafer.rir.spatial.RegionSelection;

public final class PlayerInteractionListener implements Listener {
    private final RirPlugin plugin;

    public PlayerInteractionListener(RirPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        RegionSelection selection = plugin.getSelections().get(p.getUniqueId());
        if (e.getHand() == EquipmentSlot.HAND
                && selection != null
                && selection.isEnabled()
                && p.hasPermission("RIR.admin")
                && e.getItem() != null
                && ServerCompatibility.matchesMaterial(
                        e.getMaterial(), RegionRepository.getSelectionTool())
                && e.getClickedBlock() != null) {
            boolean first = e.getAction() == Action.LEFT_CLICK_BLOCK;
            if (first || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                selection.select(e.getClickedBlock().getLocation(), first);
                e.setCancelled(true);
                p.sendMessage(
                        ChatColor.translateAlternateColorCodes(
                                '&',
                                RegionRepository.getPluginPrefix()
                                        + " &a已选择"
                                        + (first ? "第一" : "第二")
                                        + "点: "
                                        + e.getClickedBlock().getLocation().toVector()));
                return;
            }
        }
        if (e.getItem() == null || p.hasPermission("RIR.bypass.interact")) return;
        String material = ServerCompatibility.normalizeMaterialName(e.getItem().getType().name());
        // Query actual position: the committed membership can lag by one tick after movement.
        for (Region r : RegionRepository.getIndex().findRegionsAt(p.getLocation()))
            if (r.getBlockedInteractionMaterials().contains(material)) {
                e.setCancelled(true);
                return;
            }
    }
}
