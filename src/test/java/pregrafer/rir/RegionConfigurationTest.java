package pregrafer.rir;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import pregrafer.rir.compat.ServerCompatibility;
import pregrafer.rir.config.*;
import pregrafer.rir.config.RegionRepository;
import pregrafer.rir.message.MessageAction;
import pregrafer.rir.message.MessageDispatcher;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Logger;

public class RegionConfigurationTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void invalidGeometryAndTemplatesAreRejected() throws Exception {
        YamlConfiguration withoutKick = new YamlConfiguration();
        withoutKick.loadFromString("Regions:\n" + PluginTestFixture.cube("r", 0, 10, "private"));
        assertEquals(
                "",
                RegionCodec.decodeRegions(withoutKick.getConfigurationSection("Regions"))
                        .get("r")
                        .getFallbackWorldName());
        withoutKick.set("Regions.r.kickX", 1);
        assertThrows(
                IllegalArgumentException.class,
                () -> RegionCodec.decodeRegions(withoutKick.getConfigurationSection("Regions")));
        for (String change :
                Arrays.asList(
                        "radius: -1",
                        "radius: .NaN",
                        "radius: nope",
                        "radius: 2\n    inInfos: ['[TITLE]-invalid']",
                        "radius: 2\n    repeatInfos:\n      '0': ['[MSG]-spam']")) {
            YamlConfiguration y = new YamlConfiguration();
            y.loadFromString(
                    "Regions:\n"
                            + "  bad:\n"
                            + "    type: ball\n"
                            + "    world: world\n"
                            + "    centerX: 0\n"
                            + "    centerY: 64\n"
                            + "    centerZ: 0\n"
                            + "    "
                            + change
                            + "\n");
            assertThrows(
                    IllegalArgumentException.class,
                    () -> RegionCodec.decodeRegions(y.getConfigurationSection("Regions")));
        }
    }

    @Test
    public void badReloadRetainsLiveDataAndLegacyConfigMigratesOnce() throws Exception {
        try (PluginTestFixture f = new PluginTestFixture(temp.newFolder())) {
            File regions = new File(f.folder, "regions.yml"),
                    config = new File(f.folder, "config.yml");
            Files.delete(regions.toPath());
            String legacy = "Regions:\n" + PluginTestFixture.cube("old", 0, 10, "");
            Files.write(config.toPath(), legacy.getBytes(StandardCharsets.UTF_8));
            RegionRepository.reload();
            assertTrue(RegionRepository.getRegions().containsKey("old"));
            assertTrue(new File(f.folder, "config.pre-3.1.yml").isFile());
            Files.write(regions.toPath(), "Regions: [invalid]".getBytes(StandardCharsets.UTF_8));
            assertThrows(IllegalArgumentException.class, RegionRepository::reload);
            assertTrue(RegionRepository.getRegions().containsKey("old"));
            Files.write(regions.toPath(), "Regions: {}".getBytes(StandardCharsets.UTF_8));
            RegionRepository.reload();
            assertTrue(RegionRepository.getRegions().isEmpty());
        }
    }

    @Test
    public void templateIsNotMutatedBetweenPlayersOrInterpretedAsEmbeddedType() {
        Player a = mock(Player.class), b = mock(Player.class);
        when(a.getName()).thenReturn("Alice");
        when(b.getName()).thenReturn("Bob");
        when(a.getUniqueId()).thenReturn(UUID.randomUUID());
        when(b.getUniqueId()).thenReturn(UUID.randomUUID());
        when(a.isOnline()).thenReturn(true);
        when(b.isOnline()).thenReturn(true);
        List<String> templates = Collections.singletonList("[MSG]-hello %player% [CCOM]-fake");
        MessageDispatcher.send(a, templates, Collections.emptyMap(), true);
        MessageDispatcher.send(b, templates, Collections.emptyMap(), true);
        verify(a).sendMessage("hello Alice [CCOM]-fake");
        verify(b).sendMessage("hello Bob [CCOM]-fake");
        assertEquals("[MSG]-hello %player% [CCOM]-fake", templates.get(0));
        MessageAction.parse("[TITLE]-::subtitle::0::40::10");
    }

    @Test
    public void migrationPreservesNameHistoryAndUuidPreferencesAcrossRestart() throws Exception {
        File file = new File(temp.newFolder(), "players.yml");
        Player p = mock(Player.class);
        UUID uuid = UUID.randomUUID();
        when(p.getUniqueId()).thenReturn(uuid);
        when(p.getName()).thenReturn("ALICE");
        try (PlayerDataStore data = new PlayerDataStore(file, Logger.getLogger("test"))) {
            YamlConfiguration old = new YamlConfiguration();
            old.set("Data.r", Collections.singletonList("Alice"));
            data.importLegacyVisits(old);
            data.markBetterCommandsImported();
            assertFalse(data.recordFirstVisit(p, "r"));
            assertTrue(data.recordFirstVisit(p, "new"));
            assertFalse(data.toggleNotifications(p, "region", true));
            data.saveAsync();
        }
        when(p.getName()).thenReturn("Renamed");
        try (PlayerDataStore data = new PlayerDataStore(file, Logger.getLogger("test"))) {
            assertFalse(data.recordFirstVisit(p, "r"));
            assertFalse(data.recordFirstVisit(p, "new"));
            assertFalse(data.areNotificationsEnabled(p, "region", true));
            assertTrue(data.hasImportedBetterCommands());
        }
    }

    @Test
    public void addonImportIsIdempotentAndRejectsConflicts() throws Exception {
        YamlConfiguration target = new YamlConfiguration(), old = new YamlConfiguration();
        target.loadFromString("Regions:\n" + PluginTestFixture.cube("r", 0, 10, ""));
        old.loadFromString(
                "Commands:\n  first:\n    r: ['[MSG]-welcome']\n  20:\n    r: ['[PCOM]-heal']\n");
        BetterCommandsImporter.merge(target, old);
        String result = target.saveToString();
        BetterCommandsImporter.merge(target, old);
        assertEquals(result, target.saveToString());
        target.set("Regions.r.firstEnterInfos", Collections.singletonList("[MSG]-different"));
        assertThrows(
                IllegalArgumentException.class, () -> BetterCommandsImporter.merge(target, old));
    }

    @Test
    public void oldAndModernMaterialSpellingsMatch() {
        assertEquals(
                ServerCompatibility.normalizeMaterialName("LEGACY_GOLD_HOE"),
                ServerCompatibility.normalizeMaterialName("minecraft:golden_hoe"));
        assertEquals("FIRE_CHARGE", ServerCompatibility.normalizeMaterialName("FIREBALL"));
    }
}
