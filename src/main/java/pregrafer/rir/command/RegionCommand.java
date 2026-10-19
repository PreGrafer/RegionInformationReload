package pregrafer.rir.command;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import pregrafer.rir.RirPlugin;
import pregrafer.rir.config.*;
import pregrafer.rir.config.RegionRepository;
import pregrafer.rir.geometry.Point3D;
import pregrafer.rir.region.*;
import pregrafer.rir.region.Region;
import pregrafer.rir.spatial.RegionSelection;

import java.util.*;

public final class RegionCommand implements TabExecutor {
    private final RirPlugin plugin;
    private final Map<UUID, Long> previewCooldown = new HashMap<>();

    public RegionCommand(RirPlugin plugin) {
        this.plugin = plugin;
    }

    public RegionCommand() {
        this(RirPlugin.getInstance());
    }

    private void say(CommandSender sender, String text) {
        sender.sendMessage(
                ChatColor.translateAlternateColorCodes(
                        '&', RegionRepository.getPluginPrefix() + " " + text));
    }

    private Player player(CommandSender sender) {
        if (!(sender instanceof Player)) throw new IllegalArgumentException("此命令只能由玩家执行");
        return (Player) sender;
    }

    private void permission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission))
            throw new IllegalArgumentException(
                    RegionRepository.getMessage("noPermission", "你没有权限"));
    }

    private void usage(boolean valid, String usage) {
        if (!valid) throw new IllegalArgumentException("用法: /rir " + usage);
    }

    private Region region(String id) {
        Region r = RegionRepository.getRegions().get(id);
        if (r == null) throw new IllegalArgumentException("区域不存在: " + id);
        return r;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        try {
            if (sub.equals("help")) {
                permission(sender, "RIR.help");
                say(sender, "&a/rir here &7当前位置 | &a/rir switch <biome|region> &7提示开关");
                if (sender.hasPermission("RIR.admin")) {
                    say(sender, "&a/rir createMode &7选点 | &a/rir pos1 &7/ &a/rir pos2 &7当前位置选点");
                    say(sender, "&a/rir create <cube|ball|cylinder> <id> [radius] [height]");
                    say(sender, "&a/rir list [page] &7| &a/rir check <id> &7| &a/rir draw <id>");
                    say(sender, "&a/rir set <id> <name|priority|enabled|permission> <value>");
                    say(
                            sender,
                            "&a/rir delete <id> confirm &7| &a/rir reload &7| &a/rir stats &7|"
                                    + " &a/rir validate &7| &a/rir importbettercommands");
                }
                return true;
            }
            if (sub.equals("switch")) {
                usage(
                        args.length == 2
                                && (args[1].equalsIgnoreCase("biome")
                                        || args[1].equalsIgnoreCase("region")),
                        "switch <biome|region>");
                String type = args[1].toLowerCase(Locale.ROOT);
                permission(sender, "RIR.switch." + type);
                Player p = player(sender);
                boolean enabled =
                        plugin.getPlayerDataStore()
                                .toggleNotifications(
                                        p,
                                        type,
                                        type.equals("biome")
                                                ? RegionRepository
                                                        .isBiomeNotificationEnabledByDefault()
                                                : RegionRepository
                                                        .isRegionNotificationEnabledByDefault());
                say(
                        sender,
                        (type.equals("biome") ? "生物群系" : "区域") + "提示已" + (enabled ? "开启" : "关闭"));
                return true;
            }
            if (sub.equals("here")) {
                permission(sender, "RIR.help");
                Player p = player(sender);
                List<Region> list = RegionRepository.getIndex().findRegionsAt(p.getLocation());
                say(
                        sender,
                        "&b世界: "
                                + p.getWorld().getName()
                                + " &b群系: "
                                + plugin.getTracker().getBiomeDisplayName(p));
                if (list.isEmpty()) say(sender, RegionRepository.getMessage("noRegion", "野外"));
                for (Region r : list)
                    say(
                            sender,
                            r.getUniqueId()
                                    + " &7("
                                    + r.getName()
                                    + "&7), priority="
                                    + r.getPriority());
                return true;
            }
            permission(sender, "RIR.admin");
            switch (sub) {
                case "validate":
                    usage(args.length == 1, "validate");
                    ConfigurationReport report = RegionRepository.validateConfiguration();
                    say(
                            sender,
                            "&a配置检查通过（未应用），区域="
                                    + report.getRegionCount()
                                    + ", 启用="
                                    + report.getActiveRegionCount()
                                    + ", 索引条目="
                                    + report.getIndexEntryCount());
                    for (int i = 0; i < Math.min(10, report.getWarnings().size()); i++)
                        say(sender, "&e" + report.getWarnings().get(i));
                    if (report.getWarnings().size() > 10)
                        say(sender, "&e另有 " + (report.getWarnings().size() - 10) + " 条世界未加载提示");
                    break;
                case "reload":
                    usage(args.length == 1, "reload");
                    plugin.reloadPlugin();
                    say(sender, "&a重载成功，区域数: " + RegionRepository.getRegions().size());
                    break;
                case "createmode":
                    {
                        usage(args.length == 1, "createMode");
                        Player p = player(sender);
                        boolean enabled =
                                plugin.getSelections()
                                        .computeIfAbsent(
                                                p.getUniqueId(), id -> new RegionSelection())
                                        .toggle();
                        say(
                                sender,
                                "选点模式已"
                                        + (enabled
                                                ? "开启，工具: " + RegionRepository.getSelectionTool()
                                                : "关闭"));
                        break;
                    }
                case "pos1":
                case "pos2":
                    {
                        usage(args.length == 1, sub);
                        Player p = player(sender);
                        RegionSelection s =
                                plugin.getSelections()
                                        .computeIfAbsent(
                                                p.getUniqueId(), id -> new RegionSelection());
                        s.select(p.getLocation().getBlock().getLocation(), sub.equals("pos1"));
                        say(sender, "&a已选择 " + sub + "，世界: " + p.getWorld().getName());
                        break;
                    }
                case "create":
                    create(player(sender), args);
                    break;
                case "list":
                    {
                        usage(args.length <= 2, "list [page]");
                        int page = args.length == 2 ? Integer.parseInt(args[1]) : 1;
                        List<String> ids = new ArrayList<>(RegionRepository.getRegions().keySet());
                        Collections.sort(ids);
                        int pages = Math.max(1, (ids.size() + 9) / 10);
                        if (page < 1 || page > pages)
                            throw new IllegalArgumentException("页码应为 1.." + pages);
                        say(sender, "&a区域列表 " + page + "/" + pages + " (共 " + ids.size() + ")");
                        for (int i = (page - 1) * 10; i < Math.min(page * 10, ids.size()); i++) {
                            Region r = region(ids.get(i));
                            say(
                                    sender,
                                    r.getUniqueId()
                                            + " &7| "
                                            + r.getWorldName()
                                            + " | "
                                            + r.getType()
                                            + " | "
                                            + (r.isEnabled() ? "启用" : "禁用"));
                        }
                        break;
                    }
                case "check":
                    {
                        usage(args.length == 2, "check <id>");
                        Region r = region(args[1]);
                        say(sender, "&a" + r.getUniqueId() + " &7(" + r.getName() + "&7)");
                        say(
                                sender,
                                "world="
                                        + r.getWorldName()
                                        + ", type="
                                        + r.getType()
                                        + ", enabled="
                                        + r.isEnabled()
                                        + ", priority="
                                        + r.getPriority());
                        say(
                                sender,
                                "bounds="
                                        + Arrays.toString(r.getBounds())
                                        + ", players="
                                        + plugin.getTracker()
                                                .getRegionPlayers(r.getUniqueId())
                                                .size());
                        say(
                                sender,
                                "permission="
                                        + (r.getEnterPermission().isEmpty()
                                                ? "公开"
                                                : r.getEnterPermission())
                                        + ", bannedItems="
                                        + r.getBlockedInteractionMaterials());
                        say(
                                sender,
                                "firstEnterInfos="
                                        + r.getFirstEnterActions().size()
                                        + ", repeatTicks="
                                        + r.getRepeatingActions().keySet());
                        break;
                    }
                case "draw":
                    {
                        usage(args.length == 2, "draw <id>");
                        Player p = player(sender);
                        Region r = region(args[1]);
                        if (!p.getWorld().getName().equals(r.getWorldName()))
                            throw new IllegalArgumentException("请进入区域所在世界: " + r.getWorldName());
                        long now = System.currentTimeMillis(),
                                last = previewCooldown.getOrDefault(p.getUniqueId(), 0L);
                        if (now - last < 2000) throw new IllegalArgumentException("请等待 2 秒再预览");
                        previewCooldown.entrySet().removeIf(e -> now - e.getValue() > 2000);
                        previewCooldown.put(p.getUniqueId(), now);
                        r.draw(p);
                        say(sender, "&a已显示区域边界: " + r.getUniqueId());
                        break;
                    }
                case "delete":
                    usage(
                            args.length == 3 && args[2].equalsIgnoreCase("confirm"),
                            "delete <id> confirm");
                    region(args[1]);
                    RegionRepository.deleteRegion(args[1]);
                    say(sender, "&a已删除区域: " + args[1]);
                    break;
                case "set":
                    set(sender, args);
                    break;
                case "stats":
                    usage(args.length == 1, "stats");
                    say(
                            sender,
                            "regions="
                                    + RegionRepository.getRegions().size()
                                    + ", indexEntries="
                                    + RegionRepository.getIndex().getEntryCount()
                                    + ", players="
                                    + plugin.getTracker().getTrackedPlayerCount());
                    say(
                            sender,
                            "queries="
                                    + plugin.getTracker().getPositionQueryCount()
                                    + ", candidateTests="
                                    + plugin.getTracker().getCandidateTestCount()
                                    + ", sharedTasks=1");
                    say(
                            sender,
                            "scheduledPlayers="
                                    + plugin.getTracker().getScheduledPlayerCount()
                                    + ", playerUpdates="
                                    + plugin.getTracker().getPlayerUpdateCount()
                                    + ", calibrations="
                                    + plugin.getTracker().getCalibrationCount());
                    break;
                case "importbettercommands":
                    usage(args.length == 1, "importbettercommands");
                    BetterCommandsImporter.run(plugin);
                    say(sender, "&a迁移完成，原配置保留，迁移前备份已生成");
                    break;
                default:
                    throw new IllegalArgumentException("未知命令，请使用 /rir help");
            }
        } catch (Exception e) {
            say(
                    sender,
                    "&c"
                            + (e.getMessage() == null
                                    ? e.getClass().getSimpleName()
                                    : e.getMessage()));
        }
        return true;
    }

    private void create(Player p, String[] args) {
        usage(
                args.length >= 3 && args.length <= 5,
                "create <cube|ball|cylinder> <id> [radius] [height]");
        String type = args[1].toLowerCase(Locale.ROOT), id = args[2];
        if (!RegionCodec.isValidId(id) || RegionRepository.getRegions().containsKey(id))
            throw new IllegalArgumentException("ID 无效或已存在；只能使用字母、数字、_、-，最多 64 字符");
        if (!Arrays.asList("cube", "ball", "cylinder").contains(type))
            throw new IllegalArgumentException("未知类型: " + type);
        usage(
                (type.equals("cube") && args.length == 3)
                        || (type.equals("ball") && args.length <= 4)
                        || type.equals("cylinder"),
                "create <cube|ball|cylinder> <id> [radius] [height]");
        RegionSelection selection = plugin.getSelections().get(p.getUniqueId());
        Location a = selection == null ? null : selection.getFirst(),
                b = selection == null ? null : selection.getSecond();
        if (a == null || type.equals("cube") && b == null)
            throw new IllegalArgumentException("选点不足，使用 /rir pos1、/rir pos2 或选点工具");
        if (!p.getWorld().equals(a.getWorld()) || b != null && !a.getWorld().equals(b.getWorld()))
            throw new IllegalArgumentException("选点必须位于同一世界且玩家在该世界");
        double
                radius =
                        args.length >= 4
                                ? Double.parseDouble(args[3])
                                : RegionRepository.getDefaultRadius(),
                height =
                        args.length == 5
                                ? Double.parseDouble(args[4])
                                : RegionRepository.getDefaultHeight();
        String world = a.getWorld().getName();
        Point3D center = new Point3D(a.getBlockX() + 0.5, a.getBlockY() + 0.5, a.getBlockZ() + 0.5);
        Location safe = p.getWorld().getSpawnLocation();
        Point3D kick = new Point3D(safe.getX(), safe.getY(), safe.getZ());
        List<String> in = Collections.singletonList("[ACB]-&a进入 %name%"),
                out = Collections.singletonList("[ACB]-&7离开 %name%");
        Region r;
        if (type.equals("cube"))
            r =
                    new CuboidRegion(
                            id,
                            id,
                            world,
                            type,
                            in,
                            out,
                            world,
                            kick,
                            new Point3D(),
                            Collections.emptySet(),
                            new Point3D(
                                    Math.min(a.getBlockX(), b.getBlockX()),
                                    Math.min(a.getBlockY(), b.getBlockY()),
                                    Math.min(a.getBlockZ(), b.getBlockZ())),
                            new Point3D(
                                    Math.max(a.getBlockX(), b.getBlockX()) + 1,
                                    Math.max(a.getBlockY(), b.getBlockY()) + 1,
                                    Math.max(a.getBlockZ(), b.getBlockZ()) + 1));
        else if (type.equals("ball"))
            r =
                    new SphereRegion(
                            id,
                            id,
                            world,
                            type,
                            in,
                            out,
                            world,
                            kick,
                            new Point3D(),
                            Collections.emptySet(),
                            center,
                            radius);
        else
            r =
                    new CylinderRegion(
                            id,
                            id,
                            world,
                            type,
                            in,
                            out,
                            world,
                            kick,
                            new Point3D(),
                            Collections.emptySet(),
                            center,
                            radius,
                            height);
        // New informational regions are public; legacy regions retain their original permission
        // gate.
        r.configure(0, true, "", Collections.emptyList(), Collections.emptyMap());
        RegionRepository.saveRegion(r);
        say(p, "&a已创建区域 " + id + "，立即生效；使用 /rir set 或编辑 regions.yml 定制");
    }

    private void set(CommandSender sender, String[] args) {
        usage(args.length >= 4, "set <id> <name|priority|enabled|permission> <value>");
        region(args[1]);
        String field = args[2].toLowerCase(Locale.ROOT),
                value = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        Object parsed;
        switch (field) {
            case "name":
                parsed = value;
                break;
            case "priority":
                parsed = Integer.parseInt(value);
                break;
            case "enabled":
                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false"))
                    throw new IllegalArgumentException("enabled 只能为 true/false");
                parsed = Boolean.parseBoolean(value);
                break;
            case "permission":
                field = "enterPermission";
                parsed = value.equalsIgnoreCase("public") ? "" : value;
                break;
            default:
                throw new IllegalArgumentException(
                        "可修改: name, priority, enabled, permission（public 表示公开）");
        }
        YamlConfiguration next = RegionRepository.copyRegionConfig();
        next.set("Regions." + args[1] + "." + field, parsed);
        RegionRepository.saveRegions(next);
        say(sender, "&a已更新 " + args[1] + "." + field);
    }

    public List<String> onTabComplete(
            CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) return Collections.emptyList();
        List<String> choices = new ArrayList<>();
        boolean admin = sender.hasPermission("RIR.admin");
        if (args.length == 1) {
            if (sender.hasPermission("RIR.help")) choices.addAll(Arrays.asList("help", "here"));
            if (sender.hasPermission("RIR.switch.biome")
                    || sender.hasPermission("RIR.switch.region")) choices.add("switch");
            if (admin)
                choices.addAll(
                        Arrays.asList(
                                "reload",
                                "validate",
                                "createMode",
                                "pos1",
                                "pos2",
                                "create",
                                "list",
                                "check",
                                "draw",
                                "delete",
                                "set",
                                "stats",
                                "importbettercommands"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("switch")) {
                for (String type : Arrays.asList("biome", "region"))
                    if (sender.hasPermission("RIR.switch." + type)) choices.add(type);
            }
            if (admin && args[0].equalsIgnoreCase("create"))
                choices.addAll(Arrays.asList("cube", "ball", "cylinder"));
            if (admin
                    && Arrays.asList("check", "draw", "delete", "set")
                            .contains(args[0].toLowerCase(Locale.ROOT)))
                choices.addAll(RegionRepository.getRegions().keySet());
        } else if (admin && args.length == 3) {
            if (args[0].equalsIgnoreCase("delete")) choices.add("confirm");
            if (args[0].equalsIgnoreCase("set"))
                choices.addAll(Arrays.asList("name", "priority", "enabled", "permission"));
        } else if (admin && args.length == 4 && args[0].equalsIgnoreCase("set")) {
            if (args[2].equalsIgnoreCase("enabled")) choices.addAll(Arrays.asList("true", "false"));
            if (args[2].equalsIgnoreCase("permission")) choices.add("public");
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        choices.removeIf(value -> !value.toLowerCase(Locale.ROOT).startsWith(prefix));
        Collections.sort(choices);
        return choices;
    }
}
