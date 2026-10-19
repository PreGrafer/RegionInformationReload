package pregrafer.rir.message;

import org.bukkit.entity.Player;

import pregrafer.rir.RirPlugin;

import java.util.*;
import java.util.function.BooleanSupplier;

/** Dispatches messages and commands without modifying caller-owned templates. */
public final class MessageDispatcher {
    private static final Map<String, MessageAction> CACHE =
            new LinkedHashMap<String, MessageAction>(256, 0.75f, true) {
                protected boolean removeEldestEntry(Map.Entry<String, MessageAction> e) {
                    return size() > 2048;
                }
            };

    private MessageDispatcher() {}

    public static void clearCache() {
        CACHE.clear();
    }

    public static void send(
            Player player,
            List<String> templates,
            Map<String, String> placeholders,
            boolean areNotificationsEnabled) {
        send(player, templates, placeholders, areNotificationsEnabled, () -> true);
    }

    public static void send(
            Player player,
            List<String> templates,
            Map<String, String> placeholders,
            boolean areNotificationsEnabled,
            BooleanSupplier continueSending) {
        if (player == null || !player.isOnline() || templates == null) return;
        for (String template : templates) {
            if (!player.isOnline() || !continueSending.getAsBoolean()) return;
            try {
                MessageAction action = CACHE.computeIfAbsent(template, MessageAction::parse);
                // Disabling tips must never disable configured region commands or access checks.
                if (areNotificationsEnabled || action.isCommand())
                    action.send(player, placeholders);
            } catch (RuntimeException e) {
                RirPlugin plugin = RirPlugin.getInstance();
                if (plugin != null)
                    plugin.getLogger().warning("Cannot execute region message: " + e.getMessage());
            }
        }
    }
}
