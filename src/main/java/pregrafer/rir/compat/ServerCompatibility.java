package pregrafer.rir.compat;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Locale;

/** Resolve optional APIs once; no CraftBukkit/NMS package dependencies. */
public final class ServerCompatibility {
    private static final Method TITLE =
            findMethod(
                    Player.class,
                    "sendTitle",
                    String.class,
                    String.class,
                    int.class,
                    int.class,
                    int.class);
    private static final Method ACTION_BAR =
            findMethod(Player.class, "sendActionBar", String.class);
    private static final Method SPIGOT_ACTION_BAR = findSpigotActionBarMethod();
    private static final Method LEGACY_COMPONENTS =
            findNamedMethod(
                    "net.md_5.bungee.api.chat.TextComponent", "fromLegacyText", String.class);
    private static final Object ACTION_BAR_TYPE = resolveActionBarType();
    private static final Method BIOME_KEY = findNamedMethod("org.bukkit.Keyed", "getKey");
    private static final Particle PREVIEW = resolvePreviewParticle();

    private ServerCompatibility() {}

    private static Method findNamedMethod(String type, String name, Class<?>... args) {
        try {
            return findMethod(Class.forName(type), name, args);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static Object resolveActionBarType() {
        if (SPIGOT_ACTION_BAR == null) return null;
        try {
            return SPIGOT_ACTION_BAR.getParameterTypes()[0].getField("ACTION_BAR").get(null);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... args) {
        try {
            return type.getMethod(name, args);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    public static void sendTitle(
            Player player, String title, String subtitle, int in, int stay, int out) {
        if (TITLE != null) {
            try {
                TITLE.invoke(player, title, subtitle, in, stay, out);
                return;
            } catch (ReflectiveOperationException ignored) {
            }
        }
        player.sendTitle(title, subtitle);
    }

    public static void sendActionBar(Player player, String text) {
        if (ACTION_BAR != null) {
            try {
                ACTION_BAR.invoke(player, text);
                return;
            } catch (ReflectiveOperationException ignored) {
            }
        }
        if (SPIGOT_ACTION_BAR != null && ACTION_BAR_TYPE != null && LEGACY_COMPONENTS != null) {
            try {
                Object components = LEGACY_COMPONENTS.invoke(null, text);
                SPIGOT_ACTION_BAR.invoke(player.spigot(), ACTION_BAR_TYPE, components);
                return;
            } catch (ReflectiveOperationException ignored) {
            }
        }
        // Spigot 1.9 has no public action-bar method. A chat fallback keeps the message usable.
        player.sendMessage(text);
    }

    private static Method findSpigotActionBarMethod() {
        try {
            return Player.Spigot.class.getMethod(
                    "sendMessage",
                    Class.forName("net.md_5.bungee.api.ChatMessageType"),
                    Class.forName("[Lnet.md_5.bungee.api.chat.BaseComponent;"));
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static Particle resolvePreviewParticle() {
        for (String name : new String[] {"HAPPY_VILLAGER", "VILLAGER_HAPPY", "FLAME"}) {
            try {
                return (Particle) Particle.class.getField(name).get(null);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    public static void spawnPreviewParticle(Player player, double x, double y, double z) {
        if (PREVIEW != null) player.spawnParticle(PREVIEW, x, y, z, 1, 0, 0, 0, 0);
    }

    public static String normalizeMaterialName(String name) {
        String value = name.trim().toUpperCase(Locale.ROOT).replace("MINECRAFT:", "");
        if (value.startsWith("LEGACY_")) value = value.substring(7);
        if (value.startsWith("GOLD_")) value = "GOLDEN_" + value.substring(5);
        if (value.startsWith("WOOD_")) value = "WOODEN_" + value.substring(5);
        if (value.equals("FIREBALL")) value = "FIRE_CHARGE";
        if (value.equals("SULPHUR")) value = "GUNPOWDER";
        return value;
    }

    public static boolean matchesMaterial(Material actual, String configured) {
        return normalizeMaterialName(actual.name()).equals(normalizeMaterialName(configured));
    }

    public static String getBiomeName(Location location) {
        // Biome changed from an enum to a registry interface in modern Bukkit.
        Object biome = location.getBlock().getBiome();
        if (biome instanceof Enum<?>) return ((Enum<?>) biome).name();
        if (BIOME_KEY == null) return biome.toString();
        try {
            Object key = BIOME_KEY.invoke(biome);
            String name = key.toString();
            return name.startsWith("minecraft:")
                    ? name.substring(10).toUpperCase(Locale.ROOT)
                    : name;
        } catch (ReflectiveOperationException e) {
            return biome.toString();
        }
    }
}
