package pregrafer.rir;

import static org.mockito.Mockito.*;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

import pregrafer.rir.config.PlayerDataStore;
import pregrafer.rir.config.RegionRepository;
import pregrafer.rir.tracking.PlayerTracker;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public final class PluginTestFixture implements AutoCloseable {
    public final Server server = mock(Server.class);
    public final PluginManager events = mock(PluginManager.class);
    public final RirPlugin plugin = mock(RirPlugin.class);
    public final World world = mock(World.class), otherWorld = mock(World.class);
    public final Player player = mock(Player.class);
    public final AtomicReference<Location> position = new AtomicReference<>();
    public final PlayerDataStore data;
    public final PlayerTracker tracker;
    public final File folder;

    public PluginTestFixture(File folder) throws Exception {
        this.folder = folder;
        Field f = Bukkit.class.getDeclaredField("server");
        f.setAccessible(true);
        f.set(null, server);
        f = RirPlugin.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, plugin);
        when(server.getPluginManager()).thenReturn(events);
        when(server.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(folder);
        when(world.getName()).thenReturn("world");
        when(otherWorld.getName()).thenReturn("other");
        when(server.getWorld("world")).thenReturn(world);
        when(server.getWorld("other")).thenReturn(otherWorld);
        when(world.getSpawnLocation()).thenReturn(new Location(world, -10, 64, 0));
        Block block = mock(Block.class);
        when(block.getBiome()).thenReturn(Biome.PLAINS);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(block);
        when(otherWorld.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(block);
        when(world.getBlockAt(any(Location.class))).thenReturn(block);
        when(otherWorld.getBlockAt(any(Location.class))).thenReturn(block);
        when(player.getUniqueId())
                .thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        when(player.getName()).thenReturn("Alice");
        when(player.isOnline()).thenReturn(true);
        position.set(new Location(world, -5, 64, 0));
        when(player.getLocation()).thenAnswer(i -> position.get().clone());
        when(player.getWorld()).thenAnswer(i -> position.get().getWorld());
        when(player.teleport(any(Location.class)))
                .thenAnswer(
                        i -> {
                            position.set(((Location) i.getArgument(0)).clone());
                            return true;
                        });
        doReturn(Collections.singleton(player)).when(server).getOnlinePlayers();
        new YamlConfiguration().save(new File(folder, "config.yml"));
        new YamlConfiguration().save(new File(folder, "regions.yml"));
        RegionRepository.reload();
        data = new PlayerDataStore(new File(folder, "players.yml"), Logger.getLogger("test"));
        tracker = new PlayerTracker(plugin, data);
        when(plugin.getPlayerDataStore()).thenReturn(data);
        when(plugin.getTracker()).thenReturn(tracker);
        doAnswer(
                        i -> {
                            tracker.rebuildPlayerStates();
                            return null;
                        })
                .when(plugin)
                .rebuildPlayerStates();
    }

    public void load(String yaml) throws Exception {
        YamlConfiguration y = new YamlConfiguration();
        y.loadFromString(yaml);
        y.save(new File(folder, "regions.yml"));
        RegionRepository.reload();
    }

    public static String cube(String id, int x1, int x2, String permission) {
        return "  "
                + id
                + ":\n    type: cube\n    world: world\n    X1: "
                + x1
                + "\n    Y1: 60\n    Z1: -5\n    X2: "
                + x2
                + "\n    Y2: 70\n    Z2: 5\n    enterPermission: '"
                + permission
                + "'\n";
    }

    public void enter(double x) {
        Location from = position.get(), to = new Location(world, x, 64, 0);
        if (tracker.canMoveTo(player, from, to)) {
            position.set(to);
            tracker.markPositionDirty(player);
        }
        tracker.run();
    }

    public void close() {
        data.close();
        RegionRepository.clear();
    }
}
