package pregrafer.rir.integration;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import org.bukkit.OfflinePlayer;

import pregrafer.rir.RirPlugin;
import pregrafer.rir.tracking.PlayerTracker;

import java.util.*;

public class RegionExpansion extends PlaceholderExpansion {
    public String getIdentifier() {
        return "RIR";
    }

    public String getAuthor() {
        return "pop2wo";
    }

    public String getVersion() {
        return RirPlugin.getInstance().getDescription().getVersion();
    }

    public boolean persist() {
        return true;
    }

    public String onRequest(OfflinePlayer offline, String params) {
        if (offline == null) return "";
        RirPlugin plugin = RirPlugin.getInstance();
        PlayerTracker tracker = plugin == null ? null : plugin.getTracker();
        return tracker == null
                ? ""
                : tracker.getPlaceholderValue(
                        offline.getUniqueId(), params.toLowerCase(Locale.ROOT));
    }
}
