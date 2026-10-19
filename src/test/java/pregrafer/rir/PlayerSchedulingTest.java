package pregrafer.rir;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import pregrafer.rir.api.DefaultRegionApi;
import pregrafer.rir.config.RegionRepository;
import pregrafer.rir.event.RegionEnterAttemptEvent;
import pregrafer.rir.event.RegionEnteredEvent;

import java.util.*;

public class PlayerSchedulingTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    private PluginTestFixture f;

    @Before
    public void before() throws Exception {
        f = new PluginTestFixture(temp.newFolder());
    }

    @After
    public void after() {
        f.close();
    }

    private void ticks(int count) {
        for (int i = 0; i < count; i++) f.tracker.run();
    }

    private Player player(int id, double x) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(new UUID(0, id));
        when(player.getName()).thenReturn("Player" + id);
        when(player.isOnline()).thenReturn(true);
        when(player.getLocation()).thenAnswer(ignored -> new Location(f.world, x, 64, 0));
        when(player.getWorld()).thenReturn(f.world);
        return player;
    }

    @Test
    public void idleTicksDoNotTouchPlayersBeforeTheirDeadline() {
        f.tracker.trackPlayer(f.player);
        ticks(1);
        long updates = f.tracker.getPlayerUpdateCount();
        long queries = f.tracker.getPositionQueryCount();
        clearInvocations(f.player);
        ticks(18);
        verifyNoInteractions(f.player);
        assertEquals(updates, f.tracker.getPlayerUpdateCount());
        ticks(1);
        assertEquals(updates + 1, f.tracker.getPlayerUpdateCount());
        assertEquals(queries, f.tracker.getPositionQueryCount());
    }

    @Test
    public void dirtyMovementOnlyProcessesThatPlayerAndCoalescesDuplicateMarks() throws Exception {
        f.load("Regions:\n" + PluginTestFixture.cube("public", 0, 10, ""));
        RegionRepository.getConfig().set("Settings.regionSpeed", 1000);
        Player bob = player(2, -5), carol = player(3, -5);
        for (Player player : Arrays.asList(f.player, bob, carol)) f.tracker.trackPlayer(player);
        ticks(5);
        clearInvocations(bob, carol);
        long updates = f.tracker.getPlayerUpdateCount();
        long queries = f.tracker.getPositionQueryCount();
        f.position.set(new Location(f.world, 5, 64, 0));
        for (int i = 0; i < 100; i++) f.tracker.markPositionDirty(f.player);
        ticks(1);
        assertEquals(updates + 1, f.tracker.getPlayerUpdateCount());
        assertEquals(queries + 1, f.tracker.getPositionQueryCount());
        assertEquals(
                Collections.singleton("public"),
                new DefaultRegionApi().getPlayerCurrentRegionIds(f.player));
        verifyNoInteractions(bob, carol);
        assertEquals(3, f.tracker.getScheduledPlayerCount());
    }

    @Test
    public void fortyPlayersCalibrateTwoPerTickAcrossTwentyTicks() {
        for (int i = 1; i <= 40; i++) f.tracker.trackPlayer(player(i, -5));
        ticks(1); // Initial position and biome publication is intentionally immediate.
        for (int tick = 0; tick < 20; tick++) {
            long updates = f.tracker.getPlayerUpdateCount();
            long calibrations = f.tracker.getCalibrationCount();
            ticks(1);
            assertEquals(2, f.tracker.getPlayerUpdateCount() - updates);
            assertEquals(2, f.tracker.getCalibrationCount() - calibrations);
            assertEquals(40, f.tracker.getScheduledPlayerCount());
        }
    }

    @Test
    public void calibrationFindsUnmarkedMovementAndRevokedPermissionWithinOnePeriod()
            throws Exception {
        f.load("Regions:\n" + PluginTestFixture.cube("private", 0, 10, "access"));
        when(f.player.hasPermission("access")).thenReturn(true);
        f.tracker.trackPlayer(f.player);
        ticks(1);
        f.position.set(new Location(f.world, 5, 64, 0)); // No PlayerMoveEvent, like a vehicle path.
        ticks(19);
        assertEquals(
                Collections.singleton("private"),
                new DefaultRegionApi().getPlayerCurrentRegionIds(f.player));
        when(f.player.hasPermission("access")).thenReturn(false);
        ticks(20);
        assertTrue(f.position.get().getX() < 0);
        assertTrue(new DefaultRegionApi().getPlayerCurrentRegionIds(f.player).isEmpty());
    }

    @Test
    public void biomeAndRepeatDeadlinesRemainIndependentWhenTheyCoincide() throws Exception {
        f.load(
                "Regions:\n"
                        + PluginTestFixture.cube("public", 0, 10, "")
                        + "    repeatInfos:\n      '25': ['[PCOM]-stay']\n");
        RegionRepository.getConfig().set("Settings.biomeSpeed", 25);
        RegionRepository.getConfig()
                .set("Tips.Entry.biomeInfos", Collections.singletonList("[MSG]-%newBiome%"));
        f.position.set(new Location(f.world, 5, 64, 0));
        f.tracker.trackPlayer(f.player);
        ticks(1); // Both clocks start at tick 1, next due tick 26.
        Block forest = mock(Block.class);
        when(forest.getBiome()).thenReturn(Biome.FOREST);
        when(f.world.getBlockAt(any(Location.class))).thenReturn(forest);
        f.position.set(new Location(f.world, 6, 64, 0));
        f.tracker.markPositionDirty(f.player);
        ticks(24);
        verify(f.player, never()).performCommand("stay");
        verify(f.player, never()).sendMessage("FOREST");
        ticks(1);
        verify(f.player, times(1)).performCommand("stay");
        verify(f.player, times(1)).sendMessage("FOREST");
        assertEquals("FOREST", f.tracker.getPlaceholderValue(f.player.getUniqueId(), "biome"));
    }

    @Test
    public void changingOverlapDoesNotResetUnchangedRegionsRepeatClock() throws Exception {
        f.load(
                "Regions:\n"
                        + PluginTestFixture.cube("a", 0, 10, "")
                        + "    repeatInfos:\n      '25': ['[PCOM]-a']\n"
                        + PluginTestFixture.cube("b", 5, 15, "")
                        + "    repeatInfos:\n      '37': ['[PCOM]-b']\n");
        f.tracker.trackPlayer(f.player);
        ticks(1);
        f.enter(2); // A entered at tick 2, due 27.
        ticks(9);
        f.enter(7); // B entered at tick 12, due 49.
        ticks(14);
        verify(f.player, never()).performCommand("a");
        ticks(1);
        verify(f.player, times(1)).performCommand("a");
        f.enter(12); // A leaves, B's original deadline survives.
        ticks(20);
        verify(f.player, never()).performCommand("b");
        ticks(1);
        verify(f.player, times(1)).performCommand("b");
        ticks(20);
        verify(f.player, times(1)).performCommand("a");
        assertEquals(1, f.tracker.getScheduledPlayerCount());
    }

    @Test
    public void commandMovementIsConfirmedNextTickWithoutLosingTheDirtyMark() throws Exception {
        f.load(
                "Regions:\n"
                        + PluginTestFixture.cube("a", 0, 10, "")
                        + "    inInfos: ['[PCOM]-warp']\n"
                        + PluginTestFixture.cube("b", 20, 30, ""));
        when(f.player.performCommand("warp"))
                .thenAnswer(
                        ignored -> {
                            f.position.set(new Location(f.world, 25, 64, 0));
                            f.tracker.markPositionDirty(f.player);
                            return true;
                        });
        f.tracker.trackPlayer(f.player);
        ticks(1);
        f.enter(5);
        assertEquals(
                Collections.singleton("a"),
                new DefaultRegionApi().getPlayerCurrentRegionIds(f.player));
        ticks(1);
        assertEquals(
                Collections.singleton("b"),
                new DefaultRegionApi().getPlayerCurrentRegionIds(f.player));
    }

    @Test
    public void reloadInsideDueCommandCancelsOldWorkAndRestartsTheNewClock() throws Exception {
        f.load(
                "Regions:\n"
                        + PluginTestFixture.cube("public", 0, 10, "")
                        + "    repeatInfos:\n      '25': ['[PCOM]-reload', '[PCOM]-stale']\n");
        when(f.player.performCommand("reload"))
                .thenAnswer(
                        ignored -> {
                            RegionRepository.reload();
                            f.tracker.rebuildPlayerStates();
                            return true;
                        });
        f.position.set(new Location(f.world, 5, 64, 0));
        f.tracker.trackPlayer(f.player);
        ticks(26);
        verify(f.player, times(1)).performCommand("reload");
        verify(f.player, never()).performCommand("stale");
        assertEquals(1, f.tracker.getScheduledPlayerCount());
        ticks(24);
        verify(f.player, times(1)).performCommand("reload");
        ticks(1);
        verify(f.player, times(2)).performCommand("reload");
        verify(f.player, never()).performCommand("stale");
    }

    @Test
    public void reloadInsideEntryCheckDiscardsOldStateBeforeCommit() throws Exception {
        f.load(
                "Regions:\n"
                        + PluginTestFixture.cube("public", 0, 10, "")
                        + "    inInfos: ['[PCOM]-stale']\n");
        f.tracker.trackPlayer(f.player);
        ticks(1);
        doAnswer(
                        invocation -> {
                            if (invocation.getArgument(0) instanceof RegionEnterAttemptEvent) {
                                RegionRepository.reload();
                                f.tracker.rebuildPlayerStates();
                            }
                            return null;
                        })
                .when(f.events)
                .callEvent(any(Event.class));
        f.position.set(new Location(f.world, 5, 64, 0));
        f.tracker.markPositionDirty(f.player);
        ticks(1);
        verify(f.player, never()).performCommand("stale");
        verify(f.events, never()).callEvent(isA(RegionEnteredEvent.class));
        assertEquals(1, f.tracker.getScheduledPlayerCount());
        assertEquals(
                Collections.singleton("public"),
                new DefaultRegionApi().getPlayerCurrentRegionIds(f.player));
    }

    @Test
    public void quitAndSameUuidRejoinDuringDirtyBatchDoNotProcessOldSession() throws Exception {
        f.load(
                "Regions:\n"
                        + PluginTestFixture.cube("public", 0, 10, "")
                        + "    inInfos: ['[PCOM]-entry']\n");
        Player bob = player(2, 5), replacement = player(2, 5);
        f.position.set(new Location(f.world, 5, 64, 0));
        f.tracker.trackPlayer(f.player);
        f.tracker.trackPlayer(bob);
        doAnswer(
                        invocation -> {
                            Event event = invocation.getArgument(0);
                            if (event instanceof RegionEnteredEvent
                                    && ((RegionEnteredEvent) event).getPlayer() == f.player) {
                                f.tracker.untrackPlayer(bob);
                                f.tracker.trackPlayer(replacement);
                            }
                            return null;
                        })
                .when(f.events)
                .callEvent(any(Event.class));
        ticks(1);
        verify(bob, never()).performCommand("entry");
        verify(replacement, never()).performCommand("entry");
        assertEquals(2, f.tracker.getScheduledPlayerCount());
        ticks(1);
        verify(replacement, times(1)).performCommand("entry");
        f.tracker.untrackPlayer(f.player);
        f.tracker.untrackPlayer(replacement);
        assertEquals(0, f.tracker.getScheduledPlayerCount());
        clearInvocations(bob, replacement);
        ticks(150);
        verifyNoInteractions(bob, replacement);
    }

    @Test
    public void oneFailedPlayerDoesNotBlockOthersOrSpinUntilTheNextTick() throws Exception {
        f.load(
                "Regions:\n"
                        + PluginTestFixture.cube("public", 0, 10, "")
                        + "    inInfos: ['[PCOM]-entry']\n");
        f.position.set(new Location(f.world, 5, 64, 0));
        when(f.player.getLocation())
                .thenThrow(new IllegalStateException("temporary failure"))
                .thenAnswer(ignored -> f.position.get().clone());
        when(f.plugin.getLogger()).thenReturn(mock(java.util.logging.Logger.class));
        Player bob = player(2, 5);
        f.tracker.trackPlayer(f.player);
        f.tracker.trackPlayer(bob);
        ticks(1);
        verify(f.player, never()).performCommand("entry");
        verify(bob, times(1)).performCommand("entry");
        assertEquals(2, f.tracker.getScheduledPlayerCount());
        ticks(1);
        verify(f.player, times(1)).performCommand("entry");
        verify(bob, times(1)).performCommand("entry");
    }
}
