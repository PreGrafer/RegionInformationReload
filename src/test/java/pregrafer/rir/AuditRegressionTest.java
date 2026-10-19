package pregrafer.rir;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import pregrafer.rir.api.DefaultRegionApi;
import pregrafer.rir.command.RegionCommand;
import pregrafer.rir.config.ConfigurationReport;
import pregrafer.rir.config.PlayerDataStore;
import pregrafer.rir.config.RegionCodec;
import pregrafer.rir.config.RegionRepository;
import pregrafer.rir.integration.RegionExpansion;
import pregrafer.rir.region.Region;
import pregrafer.rir.spatial.RegionIndex;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuditRegressionTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void placeholderRequestSurvivesTrackerBeingClearedDuringShutdown() throws Exception {
        try (PluginTestFixture f = new PluginTestFixture(temp.newFolder())) {
            f.tracker.trackPlayer(f.player);
            f.tracker.run();
            // Simulate disable clearing the plugin's tracker between successive reads.
            when(f.plugin.getTracker()).thenReturn(f.tracker, null);
            RegionExpansion expansion = new RegionExpansion();
            assertEquals("0", expansion.onRequest(f.player, "region_count"));
            assertEquals("", expansion.onRequest(f.player, "region_count"));
        }
    }

    @Test
    public void teleportWithinSameRegionDoesNotDelayOtherRepeatPeriods() throws Exception {
        try (PluginTestFixture f = new PluginTestFixture(temp.newFolder())) {
            f.load(
                    "Regions:\n"
                            + PluginTestFixture.cube("r", 0, 10, "")
                            + "    repeatInfos:\n      '20': ['[PCOM]-warp']\n"
                            + "      '25': ['[PCOM]-other']\n");
            f.position.set(new Location(f.world, 5, 64, 0));
            when(f.player.performCommand("warp"))
                    .thenAnswer(
                            ignored -> {
                                f.position.set(new Location(f.world, 6, 64, 0));
                                f.tracker.markPositionDirty(f.player);
                                return true;
                            });
            f.tracker.trackPlayer(f.player);
            for (int tick = 1; tick <= 25; tick++) f.tracker.run();
            verify(f.player, times(1)).performCommand("warp");
            verify(f.player, never()).performCommand("other");
            f.tracker.run();
            verify(f.player, times(1)).performCommand("other");
        }
    }

    private PlayerDataStore failedSave(File file, PluginTestFixture f) throws Exception {
        PlayerDataStore data = new PlayerDataStore(file, mock(Logger.class));
        data.recordFirstVisit(f.player, "first");
        Files.write(file.getParentFile().toPath(), new byte[] {1});
        data.saveAsync();
        Field field = PlayerDataStore.class.getDeclaredField("pending");
        field.setAccessible(true);
        Future<?> write = (Future<?>) field.get(data);
        assertThrows(ExecutionException.class, () -> write.get(5, TimeUnit.SECONDS));
        Files.delete(file.getParentFile().toPath());
        data.recordFirstVisit(f.player, "latest");
        return data;
    }

    @Test
    public void synchronousSaveRecoversFromEarlierAsyncFailure() throws Exception {
        try (PluginTestFixture f = new PluginTestFixture(temp.newFolder())) {
            File file = new File(temp.newFolder(), "store/players.yml");
            try (PlayerDataStore data = failedSave(file, f)) {
                data.saveSynchronously();
                assertEquals(
                        2,
                        YamlConfiguration.loadConfiguration(file)
                                .getStringList("Players." + f.player.getUniqueId() + ".visited")
                                .size());
            }
        }
    }

    @Test
    public void shutdownRetriesLatestSnapshotAfterAsyncFailure() throws Exception {
        try (PluginTestFixture f = new PluginTestFixture(temp.newFolder())) {
            File file = new File(temp.newFolder(), "store/players.yml");
            failedSave(file, f).close();
            assertTrue(file.isFile());
            assertTrue(
                    YamlConfiguration.loadConfiguration(file)
                            .getStringList("Players." + f.player.getUniqueId() + ".visited")
                            .contains("latest"));
        }
    }

    @Test
    public void invalidRegionFieldTypesAreRejectedRatherThanSilentlyDefaulted() throws Exception {
        for (Object[] invalid :
                Arrays.asList(
                        new Object[] {"enabled", "false"},
                        new Object[] {"priority", 1.5},
                        new Object[] {"priority", 2147483648L},
                        new Object[] {"enterPermission", Collections.singletonList("vip")},
                        new Object[] {"world", 123},
                        new Object[] {"banInteractItems", "LAVA_BUCKET"},
                        new Object[] {"banInteractItems", Arrays.asList("LAVA_BUCKET", 123)})) {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.loadFromString("Regions:\n" + PluginTestFixture.cube("r", 0, 10, ""));
            yaml.set("Regions.r." + invalid[0], invalid[1]);
            assertThrows(
                    invalid[0].toString(),
                    IllegalArgumentException.class,
                    () -> RegionCodec.decodeRegions(yaml.getConfigurationSection("Regions")));
        }
    }

    @Test
    public void apiMembershipUsesUuidAndReturnsSnapshotsWithReadOnlyDefinitions() throws Exception {
        try (PluginTestFixture f = new PluginTestFixture(temp.newFolder())) {
            f.load("Regions:\n" + PluginTestFixture.cube("r", 0, 10, ""));
            DefaultRegionApi api = new DefaultRegionApi(f.tracker);
            f.position.set(new Location(f.world, 5, 64, 0));
            f.tracker.trackPlayer(f.player);
            f.tracker.run();
            Set<UUID> members = api.getRegionPlayers("r");
            Set<String> ids = api.getPlayerCurrentRegionIds(f.player.getUniqueId());
            assertEquals(Collections.singleton(f.player.getUniqueId()), members);
            assertThrows(UnsupportedOperationException.class, members::clear);
            assertThrows(UnsupportedOperationException.class, ids::clear);
            assertThrows(
                    UnsupportedOperationException.class,
                    () -> RegionRepository.getRegions().clear());
            Region region = api.getRegion("r");
            assertThrows(
                    IllegalStateException.class,
                    () ->
                            region.configure(
                                    9, false, "", Collections.emptyList(), Collections.emptyMap()));
            when(f.player.getName()).thenReturn("Renamed");
            assertEquals(Collections.singleton("r"), api.getPlayerCurrentRegionIds(f.player));
            f.enter(-5);
            assertTrue(api.getRegionPlayers("r").isEmpty());
            assertTrue(api.getPlayerCurrentRegionIds(f.player).isEmpty());
            assertEquals(1, members.size());
            assertEquals(Collections.singleton("r"), ids);
            f.tracker.stop();
            assertTrue(api.getRegionPlayers("r").isEmpty());
            assertEquals(0, f.tracker.getScheduledPlayerCount());
            clearInvocations(f.player);
            f.tracker.run();
            verifyNoInteractions(f.player);
        }
    }

    @Test
    public void candidateListsAreReusedAndMergedInPriorityOrder() throws Exception {
        try (PluginTestFixture f = new PluginTestFixture(temp.newFolder())) {
            f.load("Regions:\n" + PluginTestFixture.cube("local", 0, 10, ""));
            RegionIndex index = RegionRepository.getIndex();
            assertSame(index.findCandidates("world", 1, 1), index.findCandidates("world", 2, 2));
            assertThrows(
                    UnsupportedOperationException.class,
                    () -> index.findCandidates("world", 1, 1).clear());
            f.load(
                    "Regions:\n"
                            + PluginTestFixture.cube("local", 0, 10, "")
                            + PluginTestFixture.cube("big", -100, 100, "")
                            + "    priority: 20\n");
            RegionIndex mixed = new RegionIndex(RegionRepository.getRegions().values(), 4);
            assertEquals("big", mixed.findCandidates("world", 1, 1).get(0).getUniqueId());
            assertEquals("local", mixed.findCandidates("world", 1, 1).get(1).getUniqueId());
            assertSame(
                    mixed.findCandidates("world", 200, 1), mixed.findCandidates("world", 300, 1));
        }
    }

    @Test
    public void validationCommandChecksSavedFilesWithoutApplyingOrWritingThem() throws Exception {
        try (PluginTestFixture f = new PluginTestFixture(temp.newFolder())) {
            f.load("Regions:\n" + PluginTestFixture.cube("live", 0, 10, ""));
            int generation = RegionRepository.getGeneration();
            File regions = new File(f.folder, "regions.yml");
            byte[] saved =
                    ("Regions:\n"
                                    + PluginTestFixture.cube("saved", 0, 10, "")
                                            .replace("world: world", "world: unloaded"))
                            .getBytes(StandardCharsets.UTF_8);
            Files.write(regions.toPath(), saved);
            ConfigurationReport result = RegionRepository.validateConfiguration();
            assertEquals(1, result.getRegionCount());
            assertEquals(1, result.getWarnings().size());
            assertTrue(result.getWarnings().get(0).contains("unloaded"));
            when(f.player.hasPermission("RIR.admin")).thenReturn(true);
            RegionCommand command = new RegionCommand(f.plugin);
            Command bukkit = mock(Command.class);
            command.onCommand(f.player, bukkit, "rir", new String[] {"validate"});
            verify(f.player).sendMessage(contains("未应用"));
            assertEquals(
                    Collections.singletonList("validate"),
                    command.onTabComplete(f.player, bukkit, "rir", new String[] {"val"}));
            assertEquals(generation, RegionRepository.getGeneration());
            assertTrue(RegionRepository.getRegions().containsKey("live"));
            assertArrayEquals(saved, Files.readAllBytes(regions.toPath()));
            Files.write(regions.toPath(), "Regions: [bad]".getBytes(StandardCharsets.UTF_8));
            assertThrows(IllegalArgumentException.class, RegionRepository::validateConfiguration);
            assertEquals(generation, RegionRepository.getGeneration());
        }
    }

    @Test
    public void validationOfLegacyFilesDoesNotPerformMigration() throws Exception {
        try (PluginTestFixture f = new PluginTestFixture(temp.newFolder())) {
            File regions = new File(f.folder, "regions.yml");
            Files.delete(regions.toPath());
            File config = new File(f.folder, "config.yml");
            Files.write(
                    config.toPath(),
                    ("Regions:\n" + PluginTestFixture.cube("old", 0, 10, ""))
                            .getBytes(StandardCharsets.UTF_8));
            assertEquals(1, RegionRepository.validateConfiguration().getRegionCount());
            assertFalse(regions.exists());
            assertFalse(new File(f.folder, "config.pre-3.1.yml").exists());
            Files.write(config.toPath(), "Regions: [bad]".getBytes(StandardCharsets.UTF_8));
            assertThrows(IllegalArgumentException.class, RegionRepository::reload);
            assertFalse(regions.exists());
        }
    }

    @Test
    public void invalidSettingsAndDuplicateNormalizedPeriodsAreRejected() throws Exception {
        try (PluginTestFixture f = new PluginTestFixture(temp.newFolder())) {
            for (String yaml :
                    Arrays.asList(
                            "Settings: [bad]",
                            "Settings:\n  enforceEntryPermissions: 'false'",
                            "Settings:\n  regionSpeed: 1.5",
                            "Settings:\n  preview:\n    maxParticles: -1")) {
                Files.write(
                        new File(f.folder, "config.yml").toPath(),
                        yaml.getBytes(StandardCharsets.UTF_8));
                int generation = RegionRepository.getGeneration();
                assertThrows(IllegalArgumentException.class, RegionRepository::reload);
                assertEquals(generation, RegionRepository.getGeneration());
            }
        }
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(
                "Regions:\n"
                        + PluginTestFixture.cube("r", 0, 10, "")
                        + "    repeatInfos:\n      '20': ['[MSG]-a']\n      '020': ['[MSG]-b']\n");
        assertThrows(
                IllegalArgumentException.class,
                () -> RegionCodec.decodeRegions(yaml.getConfigurationSection("Regions")));
    }

    @Test
    public void persistentPlayerErrorsBackOffAndDoNotSpamLogsOrBlockOtherPlayers()
            throws Exception {
        try (PluginTestFixture f = new PluginTestFixture(temp.newFolder())) {
            Logger logger = mock(Logger.class);
            when(f.plugin.getLogger()).thenReturn(logger);
            when(f.player.getLocation()).thenThrow(new IllegalStateException("unavailable"));
            f.tracker.trackPlayer(f.player);
            for (int tick = 0; tick < 20; tick++) {
                f.tracker.markPositionDirty(f.player);
                f.tracker.run();
            }
            assertEquals(5, f.tracker.getPlayerUpdateCount()); // 1, 2, 4, 8, 16.
            verify(logger, times(1)).log(eq(Level.WARNING), anyString(), any(Throwable.class));
            assertEquals(1, f.tracker.getScheduledPlayerCount());
            doAnswer(ignored -> f.position.get().clone()).when(f.player).getLocation();
            for (int tick = 20; tick < 32; tick++) f.tracker.run();
            assertEquals(6, f.tracker.getPlayerUpdateCount());
        }
    }
}
