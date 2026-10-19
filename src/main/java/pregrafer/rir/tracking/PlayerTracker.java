package pregrafer.rir.tracking;

import org.bukkit.*;
import org.bukkit.entity.Player;

import pregrafer.rir.RirPlugin;
import pregrafer.rir.compat.ServerCompatibility;
import pregrafer.rir.config.PlayerDataStore;
import pregrafer.rir.config.RegionRepository;
import pregrafer.rir.event.*;
import pregrafer.rir.message.MessageDispatcher;
import pregrafer.rir.region.Region;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/** All Bukkit access and transitions run on the server thread. One task services all players. */
public final class PlayerTracker implements Runnable {
    private final RirPlugin plugin;
    private final PlayerDataStore data;
    private final Map<UUID, PlayerState> states = new HashMap<>();
    private final Map<String, Set<UUID>> regionPlayers = new HashMap<>();
    private final Map<UUID, Map<String, String>> display = new ConcurrentHashMap<>();
    private final Set<UUID> evaluating = new HashSet<>();
    private Set<PlayerState> pendingPositions = new LinkedHashSet<>();
    private Set<PlayerState> positionBatch = new LinkedHashSet<>();
    private final TickQueue<PlayerState> scheduledPlayers = new TickQueue<>();
    private long tick, positionQueryCount, candidateTestCount;
    private long nextCalibrationSlot, playerUpdateCount, calibrationCount;
    private boolean stopped;

    private static final class PlayerState {
        final Player player;
        final UUID playerId;
        LinkedHashMap<String, Region> regions = new LinkedHashMap<>();
        final Map<String, Map<Long, Long>> repeats = new HashMap<>();
        List<Region> candidates = Collections.emptyList();
        String candidateWorld;
        int chunkX, chunkZ, generation = -1;
        Location approvedPosition, lastPosition, lastAllowedPosition, lastBiomePosition;
        String biome;
        int consecutiveFailures;
        long retryAfterTick, nextErrorLogTick;
        long lastDeniedMessageTick = -1000,
                nextCalibrationTick,
                nextBiomeCheckTick,
                nextRepeatTick = Long.MAX_VALUE;

        PlayerState(Player player) {
            this.player = player;
            this.playerId = player.getUniqueId();
        }
    }

    public PlayerTracker(RirPlugin plugin, PlayerDataStore data) {
        this.plugin = plugin;
        this.data = data;
    }

    private PlayerState getOrCreateState(Player player) {
        PlayerState state = states.get(player.getUniqueId());
        if (state == null) {
            state = new PlayerState(player);
            states.put(state.playerId, state);
            long period = RegionRepository.getRegionCheckIntervalTicks();
            long phase = nextCalibrationSlot++ % period;
            long delay = Math.floorMod(phase - tick % period, period);
            state.nextCalibrationTick = tick + (delay == 0 ? period : delay);
            state.nextBiomeCheckTick = tick + 1;
            schedulePlayer(state);
        }
        return state;
    }

    public void trackPlayer(Player player) {
        pendingPositions.add(getOrCreateState(player));
    }

    public void untrackPlayer(Player player) {
        display.remove(player.getUniqueId());
        PlayerState state = states.remove(player.getUniqueId());
        if (state != null) {
            pendingPositions.remove(state);
            scheduledPlayers.cancel(state);
            for (Region region : state.regions.values())
                removeMembership(region.getUniqueId(), state.playerId);
        }

        data.unloadPlayerCache(player.getUniqueId());
    }

    public void rebuildPlayerStates() {
        regionPlayers.clear();
        states.clear();
        pendingPositions.clear();
        scheduledPlayers.clear();
        nextCalibrationSlot = 0;
        display.clear();
        // Rebuild silently: reload/create/delete must not replay entry rewards or commands.
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerState state = getOrCreateState(player);
            Location location = player.getLocation();
            state.regions = findRegions(state, location);
            state.lastPosition = location.clone();
            state.lastBiomePosition = location.clone();
            state.biome = ServerCompatibility.getBiomeName(location);
            updateMemberships(state);
            scheduleRepeatingActions(state);
            state.nextBiomeCheckTick = tick + RegionRepository.getBiomeCheckIntervalTicks();
            schedulePlayer(state);
            if (findDeniedRegion(player, state.regions.values()) == null)
                state.lastAllowedPosition = location.clone();
        }
    }

    /** Invalidate retained API/session references when the plugin is disabled. */
    public void stop() {
        stopped = true;
        states.clear();
        regionPlayers.clear();
        pendingPositions.clear();
        scheduledPlayers.clear();
        display.clear();
    }

    private LinkedHashMap<String, Region> findRegions(PlayerState state, Location location) {
        String world = location.getWorld().getName();
        int x = location.getBlockX() >> 4, z = location.getBlockZ() >> 4;
        if (state.generation != RegionRepository.getGeneration()
                || !world.equals(state.candidateWorld)
                || x != state.chunkX
                || z != state.chunkZ) {
            state.candidates =
                    RegionRepository.getIndex()
                            .findCandidates(world, location.getX(), location.getZ());
            state.candidateWorld = world;
            state.chunkX = x;
            state.chunkZ = z;
            state.generation = RegionRepository.getGeneration();
        }
        positionQueryCount++;
        candidateTestCount += state.candidates.size();
        LinkedHashMap<String, Region> result = new LinkedHashMap<>();
        for (Region region : state.candidates)
            if (region.contains(location)) result.put(region.getUniqueId(), region);
        return result;
    }

    public static boolean hasSamePosition(Location a, Location b) {
        return a != null
                && b != null
                && Objects.equals(a.getWorld(), b.getWorld())
                && a.getX() == b.getX()
                && a.getY() == b.getY()
                && a.getZ() == b.getZ();
    }

    private boolean canEnter(Player player, Region region) {
        return !RegionRepository.getConfig().getBoolean("Settings.enforceEntryPermissions", true)
                || region.getEnterPermission().isEmpty()
                || player.hasPermission("RIR.bypass.enter")
                || player.hasPermission(region.getEnterPermission());
    }

    private Region findDeniedRegion(Player player, Collection<Region> regions) {
        for (Region region : regions) if (!canEnter(player, region)) return region;
        return null;
    }

    public boolean canMoveTo(Player player, Location from, Location to) {
        if (to == null || to.getWorld() == null) return false;
        PlayerState state = getOrCreateState(player);
        int generation = RegionRepository.getGeneration();
        if (!evaluating.add(player.getUniqueId())) return false;
        try {
            LinkedHashMap<String, Region> next = findRegions(state, to);
            // Check all permissions before exposing cancellable hooks to addons.
            for (Region region : next.values())
                if (!region.contains(from) && !canEnter(player, region)) {
                    sendEntryDeniedMessage(state, region);
                    state.approvedPosition = null;
                    return false;
                }
            for (Region region : next.values())
                if (!state.regions.containsKey(region.getUniqueId()) && !region.contains(from)) {
                    RegionEnterAttemptEvent event = new RegionEnterAttemptEvent(player, region);
                    Bukkit.getPluginManager().callEvent(event);
                    if (!isTracked(state) || RegionRepository.getGeneration() != generation)
                        return false;
                    if (event.isCancelled()) {
                        state.approvedPosition = null;
                        return false;
                    }
                }
            state.approvedPosition = to.clone();
            return true;
        } finally {
            evaluating.remove(player.getUniqueId());
        }
    }

    public void markPositionDirty(Player player) {
        PlayerState state = states.get(player.getUniqueId());
        if (state != null) pendingPositions.add(state);
    }

    private void sendEntryDeniedMessage(PlayerState state, Region region) {
        if (tick - state.lastDeniedMessageTick
                < Math.max(
                        1,
                        RegionRepository.getConfig().getInt("Settings.deniedMessageCooldown", 40)))
            return;
        state.lastDeniedMessageTick = tick;
        MessageDispatcher.send(
                state.player,
                RegionRepository.getEntryDeniedActions(),
                createRegionPlaceholders(region),
                true);
    }

    private Map<String, String> createRegionPlaceholders(Region region) {
        Map<String, String> values = new HashMap<>();
        values.put("%name%", region.getName());
        values.put("%id%", region.getUniqueId());
        return values;
    }

    private void updateMemberships(PlayerState state) {
        for (Region region : state.regions.values())
            regionPlayers
                    .computeIfAbsent(region.getUniqueId(), id -> new LinkedHashSet<>())
                    .add(state.playerId);
        publishPlaceholders(state);
    }

    private void removeMembership(String regionId, UUID playerId) {
        Set<UUID> members = regionPlayers.get(regionId);
        if (members == null) return;
        members.remove(playerId);
        if (members.isEmpty()) regionPlayers.remove(regionId);
    }

    /** Main-thread snapshots; callers cannot modify tracked membership. */
    public Set<String> getPlayerRegionIds(UUID playerId) {
        PlayerState state = states.get(playerId);
        return state == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new LinkedHashSet<>(state.regions.keySet()));
    }

    public Set<UUID> getRegionPlayers(String regionId) {
        Set<UUID> members = regionPlayers.get(regionId);
        return members == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new LinkedHashSet<>(members));
    }

    private void publishPlaceholders(PlayerState state) {
        Map<String, String> values = new HashMap<>();
        List<String> names = new ArrayList<>();
        for (Region region : state.regions.values()) names.add(region.getName());
        String none = RegionRepository.getMessage("noRegion", "野外");
        values.put(
                "region",
                ChatColor.translateAlternateColorCodes(
                        '&', names.isEmpty() ? none : String.join(" ", names)));
        values.put(
                "region_primary",
                ChatColor.translateAlternateColorCodes('&', names.isEmpty() ? none : names.get(0)));
        values.put("region_ids", String.join(" ", state.regions.keySet()));
        values.put("region_count", String.valueOf(names.size()));
        values.put(
                "biome",
                state.biome == null
                        ? ""
                        : RegionRepository.getBiomes().getOrDefault(state.biome, state.biome));
        display.put(state.player.getUniqueId(), Collections.unmodifiableMap(values));
    }

    /** Safe for asynchronous PlaceholderAPI consumers; no live Bukkit calls. */
    public String getPlaceholderValue(UUID player, String key) {
        switch (key) {
            case "region":
            case "region_primary":
            case "region_ids":
            case "region_count":
            case "biome":
                Map<String, String> values = display.get(player);
                return values == null ? "" : values.get(key);
            default:
                return null;
        }
    }

    private void scheduleRepeatingActions(PlayerState state) {
        state.repeats.keySet().retainAll(state.regions.keySet());
        state.nextRepeatTick = Long.MAX_VALUE;
        for (Region region : state.regions.values()) {
            if (region.getRepeatingActions().isEmpty()) continue;
            Map<Long, Long> due =
                    state.repeats.computeIfAbsent(region.getUniqueId(), k -> new HashMap<>());
            for (Long period : region.getRepeatingActions().keySet())
                due.putIfAbsent(period, tick + period);
            for (long when : due.values())
                state.nextRepeatTick = Math.min(state.nextRepeatTick, when);
        }
    }

    private boolean isStateCurrent(PlayerState state, Location location, int generation) {
        return isTracked(state)
                && state.player.isOnline()
                && RegionRepository.getGeneration() == generation
                && hasSamePosition(state.player.getLocation(), location);
    }

    private void reconcilePosition(PlayerState state) {
        Player player = state.player;
        int generation = RegionRepository.getGeneration();
        Location location = player.getLocation();
        if (hasSamePosition(location, state.lastPosition)) return;
        LinkedHashMap<String, Region> next = findRegions(state, location);
        boolean approvedPosition = hasSamePosition(location, state.approvedPosition);
        state.approvedPosition = null;
        Region denied = null;
        for (Region region : next.values())
            if (!state.regions.containsKey(region.getUniqueId()) && !canEnter(player, region)) {
                denied = region;
                break;
            }
        if (denied != null) {
            sendEntryDeniedMessage(state, denied);
            if (isTracked(state) && RegionRepository.getGeneration() == generation)
                moveToAllowedLocation(state, denied);
            return;
        }
        if (!approvedPosition) {
            for (Region region : next.values())
                if (!state.regions.containsKey(region.getUniqueId())) {
                    RegionEnterAttemptEvent e = new RegionEnterAttemptEvent(player, region);
                    Bukkit.getPluginManager().callEvent(e);
                    if (!isTracked(state) || RegionRepository.getGeneration() != generation) return;
                    if (e.isCancelled()) {
                        moveToAllowedLocation(state, region);
                        return;
                    }
                    if (!hasSamePosition(player.getLocation(), location)) {
                        pendingPositions.add(state);
                        return;
                    }
                }
        }
        LinkedHashMap<String, Region> old = state.regions;
        if (old.keySet().equals(next.keySet())) {
            state.lastPosition = location.clone();
            state.lastAllowedPosition = location.clone();
            return;
        }
        // Commit every membership before firing notifications, commands or success events.
        for (Region region : old.values())
            if (!next.containsKey(region.getUniqueId()))
                removeMembership(region.getUniqueId(), state.playerId);
        state.regions = next;
        state.lastPosition = location.clone();
        state.lastAllowedPosition = location.clone();
        updateMemberships(state);
        scheduleRepeatingActions(state);
        boolean tips =
                data.areNotificationsEnabled(
                        player, "region", RegionRepository.isRegionNotificationEnabledByDefault());
        for (Region region : old.values())
            if (!next.containsKey(region.getUniqueId())) {
                Bukkit.getPluginManager().callEvent(new RegionLeftEvent(player, region));
                if (!isStateCurrent(state, location, generation)) return;
                MessageDispatcher.send(
                        player,
                        region.getLeaveActions(),
                        createRegionPlaceholders(region),
                        tips,
                        () -> isStateCurrent(state, location, generation));
                if (!isStateCurrent(state, location, generation)) return;
            }
        for (Region region : next.values())
            if (!old.containsKey(region.getUniqueId())) {
                Bukkit.getPluginManager().callEvent(new RegionEnteredEvent(player, region));
                if (!isStateCurrent(state, location, generation)) return;
                MessageDispatcher.send(
                        player,
                        region.getEnterActions(),
                        createRegionPlaceholders(region),
                        tips,
                        () -> isStateCurrent(state, location, generation));
                if (!isStateCurrent(state, location, generation)) return;
                boolean first =
                        !region.getFirstEnterActions().isEmpty()
                                && data.recordFirstVisit(player, region.getUniqueId());
                if (first)
                    MessageDispatcher.send(
                            player,
                            region.getFirstEnterActions(),
                            createRegionPlaceholders(region),
                            tips,
                            () -> isStateCurrent(state, location, generation));
                if (!isStateCurrent(state, location, generation)) return;
            }
    }

    private void moveToAllowedLocation(PlayerState state, Region denied) {
        Location target = state.lastAllowedPosition;
        if (target == null || !isAllowedLocation(state.player, target)) {
            World world = Bukkit.getWorld(denied.getFallbackWorldName());
            if (world != null) {
                target =
                        new Location(
                                world,
                                denied.getFallbackPosition().getX(),
                                denied.getFallbackPosition().getY(),
                                denied.getFallbackPosition().getZ(),
                                (float) denied.getFallbackRotation().getZ(),
                                (float) denied.getFallbackRotation().getX());
            }
            if (target == null
                    || denied.contains(target)
                    || !isAllowedLocation(state.player, target))
                target = state.player.getWorld().getSpawnLocation();
        }
        if (target != null && !denied.contains(target) && isAllowedLocation(state.player, target)) {
            if (state.player.isInsideVehicle()) state.player.leaveVehicle();
            state.player.teleport(target);
            pendingPositions.add(state);
        }
    }

    private boolean isAllowedLocation(Player player, Location location) {
        return location != null
                && location.getWorld() != null
                && findDeniedRegion(player, RegionRepository.getIndex().findRegionsAt(location))
                        == null;
    }

    private void updateBiome(PlayerState state) {
        if (tick < state.nextBiomeCheckTick) return;
        state.nextBiomeCheckTick = tick + RegionRepository.getBiomeCheckIntervalTicks();
        Location location = state.player.getLocation();
        if (state.lastBiomePosition != null
                && Objects.equals(location.getWorld(), state.lastBiomePosition.getWorld())
                && location.getBlockX() == state.lastBiomePosition.getBlockX()
                && location.getBlockY() == state.lastBiomePosition.getBlockY()
                && location.getBlockZ() == state.lastBiomePosition.getBlockZ()) return;
        state.lastBiomePosition = location;
        String next = ServerCompatibility.getBiomeName(location), old = state.biome;
        state.biome = next;
        publishPlaceholders(state);
        if (old == null
                || old.equals(next)
                || !data.areNotificationsEnabled(
                        state.player,
                        "biome",
                        RegionRepository.isBiomeNotificationEnabledByDefault())) return;
        String oldName = RegionRepository.getBiomes().getOrDefault(old, old),
                newName = RegionRepository.getBiomes().getOrDefault(next, next);
        if (!RegionRepository.isBiomeHighAccuracyEnabled()
                && (oldName.equals(newName) || old.contains(next) || next.contains(old))) return;
        Map<String, String> values = new HashMap<>();
        values.put("%oldBiome%", oldName);
        values.put("%newBiome%", newName);
        MessageDispatcher.send(state.player, RegionRepository.getBiomeActions(), values, true);
    }

    private void runRepeatingActions(PlayerState state) {
        Location location = state.player.getLocation();
        int generation = RegionRepository.getGeneration();
        state.nextRepeatTick = Long.MAX_VALUE;
        try {
            for (Region region : new ArrayList<>(state.regions.values())) {
                Map<Long, Long> due = state.repeats.get(region.getUniqueId());
                if (due == null) continue;
                for (Map.Entry<Long, List<String>> plan : region.getRepeatingActions().entrySet()) {
                    long when = due.getOrDefault(plan.getKey(), Long.MAX_VALUE);
                    if (tick < when) {
                        state.nextRepeatTick = Math.min(state.nextRepeatTick, when);
                        continue;
                    }
                    due.put(plan.getKey(), tick + plan.getKey());
                    state.nextRepeatTick = Math.min(state.nextRepeatTick, tick + plan.getKey());
                    if (!region.contains(location) || !canEnter(state.player, region)) continue;
                    MessageDispatcher.send(
                            state.player,
                            plan.getValue(),
                            createRegionPlaceholders(region),
                            data.areNotificationsEnabled(
                                    state.player,
                                    "region",
                                    RegionRepository.isRegionNotificationEnabledByDefault()),
                            () -> isStateCurrent(state, location, generation));
                    if (!isStateCurrent(state, location, generation)) return;
                }
            }
        } finally {
            // A command may move within the same region before later plans are visited.
            // Preserve every remaining deadline, including those skipped by an early return.
            if (isTracked(state)) scheduleRepeatingActions(state);
        }
    }

    private boolean isTracked(PlayerState state) {
        return states.get(state.playerId) == state;
    }

    private void schedulePlayer(PlayerState state) {
        if (isTracked(state))
            scheduledPlayers.schedule(
                    state,
                    // An interrupted update must never run twice in the same tick.
                    Math.max(
                            Math.max(tick + 1, state.retryAfterTick),
                            Math.min(
                                    state.nextCalibrationTick,
                                    Math.min(state.nextBiomeCheckTick, state.nextRepeatTick))));
    }

    private void updatePlayer(PlayerState state, boolean positionDirty) {
        // A callback may have quit/rejoined a player or rebuilt every state in this tick.
        if (!isTracked(state)) return;
        if (tick < state.retryAfterTick) {
            schedulePlayer(state);
            return;
        }
        try {
            if (!state.player.isOnline()) {
                untrackPlayer(state.player);
                return;
            }
            playerUpdateCount++;
            boolean calibration = tick >= state.nextCalibrationTick;
            if (calibration) {
                calibrationCount++;
                state.nextCalibrationTick = tick + RegionRepository.getRegionCheckIntervalTicks();
                Region denied = findDeniedRegion(state.player, state.regions.values());
                if (denied != null) {
                    sendEntryDeniedMessage(state, denied);
                    if (!isTracked(state)) return;
                    moveToAllowedLocation(state, denied);
                }
            }
            if (!isTracked(state)) return;
            if (calibration || positionDirty) reconcilePosition(state);
            if (!isTracked(state)) return;
            updateBiome(state);
            if (!isTracked(state)) return;
            if (tick >= state.nextRepeatTick) runRepeatingActions(state);
            state.consecutiveFailures = 0;
            state.retryAfterTick = 0;
        } catch (RuntimeException error) {
            if (isTracked(state)) {
                state.consecutiveFailures = Math.min(8, state.consecutiveFailures + 1);
                state.retryAfterTick = tick + Math.min(100, 1L << (state.consecutiveFailures - 1));
                pendingPositions.add(state);
                state.nextBiomeCheckTick = Math.min(state.nextBiomeCheckTick, state.retryAfterTick);
                scheduleRepeatingActions(state);
            }
            if (tick >= state.nextErrorLogTick) {
                state.nextErrorLogTick = tick + 200;
                plugin.getLogger()
                        .log(
                                Level.WARNING,
                                "Cannot update region state for "
                                        + state.playerId
                                        + "; retrying with backoff",
                                error);
            }
        } finally {
            // Also retain future work if a Bukkit API throws; removed states stay removed.
            schedulePlayer(state);
        }
    }

    @Override
    public void run() {
        if (stopped) return;
        tick++;
        // Swap reusable buffers: changes produced by callbacks belong to the next tick.
        Set<PlayerState> batch = pendingPositions;
        pendingPositions = positionBatch;
        positionBatch = batch;
        try {
            for (PlayerState state : batch) updatePlayer(state, true);
        } finally {
            batch.clear();
        }
        PlayerState state;
        while ((state = scheduledPlayers.pollDue(tick)) != null) updatePlayer(state, false);
        if (!stopped && tick % 1200 == 0) data.saveAsync();
    }

    public String getBiomeDisplayName(Player player) {
        PlayerState state = states.get(player.getUniqueId());
        String biome =
                state == null || state.biome == null
                        ? ServerCompatibility.getBiomeName(player.getLocation())
                        : state.biome;
        return RegionRepository.getBiomes().getOrDefault(biome, biome);
    }

    public int getTrackedPlayerCount() {
        return states.size();
    }

    public long getPositionQueryCount() {
        return positionQueryCount;
    }

    public long getCandidateTestCount() {
        return candidateTestCount;
    }

    public int getScheduledPlayerCount() {
        return scheduledPlayers.size();
    }

    public long getPlayerUpdateCount() {
        return playerUpdateCount;
    }

    public long getCalibrationCount() {
        return calibrationCount;
    }
}
