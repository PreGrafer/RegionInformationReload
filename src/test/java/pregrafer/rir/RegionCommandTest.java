package pregrafer.rir;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import pregrafer.rir.command.RegionCommand;
import pregrafer.rir.config.RegionRepository;
import pregrafer.rir.region.Region;
import pregrafer.rir.spatial.RegionSelection;

import java.util.*;

public class RegionCommandTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void createIsImmediatelyIndexedAndDuplicatesAndWrongWorldCannotOverwrite()
            throws Exception {
        try (PluginTestFixture f = new PluginTestFixture(temp.newFolder())) {
            when(f.player.hasPermission("RIR.admin")).thenReturn(true);
            RegionSelection selection = new RegionSelection();
            selection.select(new Location(f.world, 0, 64, 0), true);
            selection.select(new Location(f.world, 3, 67, 3), false);
            Map<UUID, RegionSelection> selections = new HashMap<>();
            selections.put(f.player.getUniqueId(), selection);
            when(f.plugin.getSelections()).thenReturn(selections);
            RegionCommand command = new RegionCommand(f.plugin);
            Command bukkit = mock(Command.class);
            assertTrue(
                    command.onCommand(
                            f.player, bukkit, "rir", new String[] {"create", "cube", "test"}));
            assertEquals(
                    1,
                    RegionRepository.getIndex()
                            .findRegionsAt(new Location(f.world, 1, 65, 1))
                            .size());
            Region original = RegionRepository.getRegions().get("test");
            assertEquals("", original.getEnterPermission());
            command.onCommand(f.player, bukkit, "rir", new String[] {"create", "ball", "test"});
            assertSame(original, RegionRepository.getRegions().get("test"));
            f.position.set(new Location(f.otherWorld, 0, 64, 0));
            command.onCommand(f.player, bukkit, "rir", new String[] {"create", "ball", "other"});
            assertFalse(RegionRepository.getRegions().containsKey("other"));
            command.onCommand(f.player, bukkit, "rir", new String[] {"delete", "test"});
            assertTrue(RegionRepository.getRegions().containsKey("test"));
            command.onCommand(f.player, bukkit, "rir", new String[] {"delete", "test", "confirm"});
            assertTrue(
                    RegionRepository.getIndex()
                            .findRegionsAt(new Location(f.world, 1, 65, 1))
                            .isEmpty());
        }
    }

    @Test
    public void tabCompletionFiltersBothPermissionAndPrefix() throws Exception {
        try (PluginTestFixture f = new PluginTestFixture(temp.newFolder())) {
            RegionCommand command = new RegionCommand(f.plugin);
            Command bukkit = mock(Command.class);
            assertTrue(command.onTabComplete(f.player, bukkit, "rir", new String[] {""}).isEmpty());
            when(f.player.hasPermission("RIR.admin")).thenReturn(true);
            assertEquals(
                    Arrays.asList("create", "createMode"),
                    command.onTabComplete(f.player, bukkit, "rir", new String[] {"cre"}));
            assertEquals(
                    Collections.singletonList("cylinder"),
                    command.onTabComplete(f.player, bukkit, "rir", new String[] {"create", "cy"}));
        }
    }
}
