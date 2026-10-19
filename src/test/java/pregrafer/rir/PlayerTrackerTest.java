package pregrafer.rir;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import pregrafer.rir.api.DefaultRegionApi;
import pregrafer.rir.config.RegionRepository;
import pregrafer.rir.event.*;

import java.util.*;

public class PlayerTrackerTest {
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

    private Set<String> ids() {
        return new DefaultRegionApi().getPlayerCurrentRegionIds(f.player);
    }

    @Test
    public void denialOfOverlappingRegionDoesNotCommitOrRewardOtherRegion() throws Exception {
        f.load(
                "Regions:\n"
                        + PluginTestFixture.cube("public", 0, 10, "")
                        + "    inInfos: ['[MSG]-entered']\n"
                        + PluginTestFixture.cube("private", 0, 10, "private.access"));
        f.tracker.trackPlayer(f.player);
        f.tracker.run();
        clearInvocations(f.player, f.events);
        f.enter(5);
        assertTrue(ids().isEmpty());
        verify(f.player, never()).sendMessage("entered");
        verify(f.events, never()).callEvent(isA(RegionEnteredEvent.class));
    }

    @Test
    public void cancelledHookAndCancelledOuterMoveDoNotCommit() throws Exception {
        f.load("Regions:\n" + PluginTestFixture.cube("public", 0, 10, ""));
        f.tracker.trackPlayer(f.player);
        f.tracker.run();
        doAnswer(
                        i -> {
                            Event e = i.getArgument(0);
                            if (e instanceof RegionEnterAttemptEvent)
                                ((RegionEnterAttemptEvent) e).setCancelled(true);
                            return null;
                        })
                .when(f.events)
                .callEvent(any(Event.class));
        f.enter(5);
        assertTrue(ids().isEmpty());
        reset(f.events);
        assertTrue(
                f.tracker.canMoveTo(f.player, f.position.get(), new Location(f.world, 5, 64, 0)));
        // Another plugin cancels the outer movement: actual position remains unchanged.
        f.tracker.markPositionDirty(f.player);
        f.tracker.run();
        assertTrue(ids().isEmpty());
    }

    @Test
    public void successfulTransitionsCommitBeforeSuccessEventAndFirstVisitRunsOnce()
            throws Exception {
        f.load(
                "Regions:\n"
                        + PluginTestFixture.cube("public", 0, 10, "")
                        + "    firstEnterInfos: ['[MSG]-first %player%']\n");
        f.tracker.trackPlayer(f.player);
        f.tracker.run();
        doAnswer(
                        i -> {
                            if (i.getArgument(0) instanceof RegionEnteredEvent)
                                assertEquals(Collections.singleton("public"), ids());
                            return null;
                        })
                .when(f.events)
                .callEvent(any(Event.class));
        f.enter(5);
        f.enter(-5);
        f.enter(5);
        verify(f.player, times(1)).sendMessage("first Alice");
        assertEquals(Collections.singleton("public"), ids());
        f.tracker.untrackPlayer(f.player);
        assertTrue(ids().isEmpty());
        assertTrue(new DefaultRegionApi().getRegionPlayers("public").isEmpty());
    }

    @Test
    public void hintsOffStillTracksAndRunsCommandsAndRepeatsStopAfterExit() throws Exception {
        f.load(
                "Regions:\n"
                        + PluginTestFixture.cube("public", 0, 10, "")
                        + "    inInfos: ['[MSG]-tip', '[PCOM]-entry']\n"
                        + "    repeatInfos:\n"
                        + "      '20': ['[PCOM]-stay']\n");
        f.data.toggleNotifications(f.player, "region", true);
        f.tracker.trackPlayer(f.player);
        f.tracker.run();
        f.enter(5);
        assertEquals(Collections.singleton("public"), ids());
        verify(f.player, never()).sendMessage("tip");
        verify(f.player).performCommand("entry");
        for (int i = 0; i < 40; i++) f.tracker.run();
        verify(f.player, times(2)).performCommand("stay");
        f.enter(-5);
        for (int i = 0; i < 60; i++) f.tracker.run();
        verify(f.player, times(2)).performCommand("stay");
    }

    @Test
    public void fractionalMovementAndSameCoordinatesInDifferentWorldsAreDetected()
            throws Exception {
        f.load("Regions:\n" + PluginTestFixture.cube("public", 0, 10, ""));
        f.position.set(new Location(f.world, -0.1, 64, 0));
        f.tracker.trackPlayer(f.player);
        f.tracker.run();
        f.enter(0.1);
        assertFalse(ids().isEmpty());
        Location to = new Location(f.otherWorld, 0.1, 64, 0);
        assertTrue(f.tracker.canMoveTo(f.player, f.position.get(), to));
        f.position.set(to);
        f.tracker.markPositionDirty(f.player);
        f.tracker.run();
        assertTrue(ids().isEmpty());
    }

    @Test
    public void stationaryPlayersDoNotRecheckGeometryAndReloadDoesNotReplayActions()
            throws Exception {
        f.load(
                "Regions:\n"
                        + PluginTestFixture.cube("public", 0, 10, "")
                        + "    inInfos: ['[PCOM]-entry']\n");
        f.tracker.trackPlayer(f.player);
        f.tracker.run();
        f.enter(5);
        long checks = f.tracker.getPositionQueryCount();
        for (int i = 0; i < 100; i++) f.tracker.run();
        assertEquals(checks, f.tracker.getPositionQueryCount());
        RegionRepository.reload();
        f.tracker.rebuildPlayerStates();
        for (int i = 0; i < 40; i++) f.tracker.run();
        verify(f.player, times(1)).performCommand("entry");
        assertEquals(Collections.singleton("public"), ids());
    }

    @Test
    public void pendingTeleportUsesActualFinalDestination() throws Exception {
        f.load("Regions:\n" + PluginTestFixture.cube("public", 0, 10, ""));
        f.tracker.trackPlayer(f.player);
        f.tracker.run();
        assertTrue(
                f.tracker.canMoveTo(f.player, f.position.get(), new Location(f.world, 5, 64, 0)));
        f.position.set(new Location(f.otherWorld, 5, 64, 0));
        f.tracker.markPositionDirty(f.player);
        f.tracker.run();
        assertTrue(ids().isEmpty());
    }

    @Test
    public void commandTeleportStopsRemainingActionsAndDoesNotConsumeFirstVisit() throws Exception {
        f.load(
                "Regions:\n"
                        + PluginTestFixture.cube("public", 0, 10, "")
                        + "    inInfos: ['[PCOM]-warp', '[PCOM]-wrong-place']\n"
                        + "    firstEnterInfos: ['[MSG]-first']\n");
        when(f.player.performCommand("warp"))
                .thenAnswer(
                        i -> {
                            f.position.set(new Location(f.world, -5, 64, 0));
                            return true;
                        });
        f.tracker.trackPlayer(f.player);
        f.tracker.run();
        f.enter(5);
        verify(f.player, never()).performCommand("wrong-place");
        verify(f.player, never()).sendMessage("first");
        assertTrue(f.data.recordFirstVisit(f.player, "public"));
    }

    @Test
    public void placeholderReadsPublishedSnapshotAndQuitClearsIt() throws Exception {
        f.load("Regions:\n" + PluginTestFixture.cube("public", 0, 10, ""));
        f.tracker.trackPlayer(f.player);
        f.tracker.run();
        f.enter(5);
        assertEquals("public", f.tracker.getPlaceholderValue(f.player.getUniqueId(), "region_ids"));
        clearInvocations(f.player);
        assertEquals(
                "1",
                f.tracker.getPlaceholderValue(
                        java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"),
                        "region_count"));
        verifyNoInteractions(f.player);
        f.tracker.untrackPlayer(f.player);
        assertEquals("", f.tracker.getPlaceholderValue(f.player.getUniqueId(), "region"));
    }

    @Test
    public void nonRoundRepeatIntervalRunsAtItsExactDueTick() throws Exception {
        f.load(
                "Regions:\n"
                        + PluginTestFixture.cube("public", 0, 10, "")
                        + "    repeatInfos:\n      '25': ['[PCOM]-stay']\n");
        f.tracker.trackPlayer(f.player);
        f.tracker.run();
        f.enter(5);
        for (int i = 0; i < 24; i++) f.tracker.run();
        verify(f.player, never()).performCommand("stay");
        f.tracker.run();
        verify(f.player, times(1)).performCommand("stay");
    }
}
