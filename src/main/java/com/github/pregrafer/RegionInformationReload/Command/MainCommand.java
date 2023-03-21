package com.github.pregrafer.RegionInformationReload.Command;

import com.github.pregrafer.RegionInformationReload.Manager.DataManager;
import com.github.pregrafer.RegionInformationReload.Region.RegionDetials.BallRegion;
import com.github.pregrafer.RegionInformationReload.Region.RegionDetials.CubeRegion;
import com.github.pregrafer.RegionInformationReload.RegionInformationReload;
import com.github.pregrafer.RegionInformationReload.Task.BiomeTask;
import com.github.pregrafer.RegionInformationReload.Task.RegionTask;
import com.github.pregrafer.RegionInformationReload.Tool.Point;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.ArrayList;
import java.util.List;

public class MainCommand implements CommandExecutor, TabExecutor {
    RegionInformationReload instance = RegionInformationReload.getInstance();

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (strings.length == 1) {
            if (strings[0].equalsIgnoreCase("reload")) {
                if (commandSender.hasPermission("RIR.admin")) {
                    DataManager.reload();
                    commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + DataManager.getCustomMessages().get("reload")));
                    return true;
                } else {
                    commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + DataManager.getCustomMessages().get("noPermission")));
                }
            } else if (strings[0].equalsIgnoreCase("createMode") && commandSender instanceof Player) {
                Player player = (Player) commandSender;
                if (player.hasPermission("RIR.admin")) {
                    if (DataManager.getCreateModeList().contains(player.getName())) {
                        DataManager.getCreateModeList().remove(player.getName());
                        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + DataManager.getCustomMessages().get("createModeSwitch").replace("%Action%", "关闭")));
                    } else {
                        DataManager.getCreateModeList().add(player.getName());
                        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + DataManager.getCustomMessages().get("createModeSwitch").replace("%Action%", "开启")));
                    }
                } else {
                    commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + DataManager.getCustomMessages().get("noPermission")));
                }
            } else if (strings[0].equalsIgnoreCase("help") && commandSender.hasPermission("RIR.help")) {
                DataManager.getHelpTips().forEach(tip -> {
                    commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', tip));
                });
            } else {
                commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + DataManager.getCustomMessages().get("wrongUsage")));
            }
        } else if (strings.length == 2) {
            if (strings[0].equalsIgnoreCase("check") && DataManager.getRegions().containsKey(strings[1])) {
                commandSender.sendMessage(DataManager.getRegions().get(strings[1]).toString());
            } else if (strings[0].equalsIgnoreCase("switch") && commandSender instanceof Player) {
                Player player = (Player) commandSender;
                BukkitScheduler scheduler = Bukkit.getScheduler();
                if (strings[1].equalsIgnoreCase("biome")) {
                    if (player.hasPermission("RIR.switch.biome")) {
                        if (DataManager.getBiomeTasks().containsKey(player.getName())) {
                            scheduler.cancelTask(DataManager.getBiomeTasks().get(player.getName()));
                            DataManager.getBiomeTasks().remove(player.getName());
                            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + DataManager.getCustomMessages().get("biomeSwitch").replace("%Action%", "关闭")));
                        } else {
                            BiomeTask biomeTask = new BiomeTask(player);
                            biomeTask.runTaskTimer(instance, DataManager.getBiomeSpeed(), DataManager.getBiomeSpeed());
                            DataManager.getBiomeTasks().put(player.getName(), biomeTask.getTaskId());
                            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + DataManager.getCustomMessages().get("biomeSwitch").replace("%Action%", "开启")));
                        }
                    } else {
                        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + DataManager.getCustomMessages().get("noPermission")));
                    }
                } else if (strings[1].equalsIgnoreCase("region")) {
                    if (player.hasPermission("RIR.switch.region")) {
                        if (DataManager.getRegionTasks().containsKey(player.getName())) {
                            scheduler.cancelTask(DataManager.getRegionTasks().get(player.getName()));
                            DataManager.getRegionTasks().remove(player.getName());
                            DataManager.getPlayerRegionLoc().remove(player.getName());
                            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + DataManager.getCustomMessages().get("regionSwitch").replace("%Action%", "关闭")));
                        } else {
                            RegionTask regionTask = new RegionTask(player);
                            regionTask.runTaskTimer(instance, DataManager.getRegionSpeed(), DataManager.getRegionSpeed());
                            DataManager.getRegionTasks().put(player.getName(), regionTask.getTaskId());
                            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + DataManager.getCustomMessages().get("regionSwitch").replace("%Action%", "开启")));
                        }
                    } else {
                        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + DataManager.getCustomMessages().get("noPermission")));
                    }
                } else {
                    commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + DataManager.getCustomMessages().get("wrongUsage")));
                }
            } else {
                commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + DataManager.getCustomMessages().get("wrongUsage")));
            }
        } else if (strings.length == 3) {
            if (strings[0].equalsIgnoreCase("create") && commandSender instanceof Player) {
                Player player = (Player) commandSender;
                if (player.hasPermission("RIR.admin")) {
                    if (strings[1].equalsIgnoreCase("ball")) {
                        if (DataManager.getFirstPointList().containsKey(player.getName())) {
                            String regionUniqueId = String.valueOf(strings[2]);
                            Point centerPoint = DataManager.getFirstPointList().get(player.getName());
                            BallRegion ballRegion = new BallRegion(
                                    regionUniqueId,
                                    regionUniqueId,
                                    player.getWorld().getName(),
                                    "ball",
                                    new ArrayList<>(),
                                    new ArrayList<>(),
                                    new Point(centerPoint.getX() + 0.5, centerPoint.getY() + 0.5, centerPoint.getZ() + 0.5),
                                    10.0);
                            DataManager.saveRegion(ballRegion);
                            DataManager.getRegions().put(regionUniqueId, ballRegion);
                            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + DataManager.getCustomMessages().get("createSuccessfully")));
                        } else {
                            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + DataManager.getCustomMessages().get("pointNotEnough")));
                        }
                    } else if (strings[1].equalsIgnoreCase("cube")) {
                        if (DataManager.getFirstPointList().containsKey(player.getName()) && DataManager.getSecondPointList().containsKey(player.getName())) {
                            String regionUniqueId = String.valueOf(strings[2]);

                            Point firstPoint = DataManager.getFirstPointList().get(player.getName());
                            Point secondPoint = DataManager.getSecondPointList().get(player.getName());

                            CubeRegion cubeRegion = new CubeRegion(
                                    regionUniqueId,
                                    regionUniqueId,
                                    player.getWorld().getName(),
                                    "cube",
                                    new ArrayList<>(),
                                    new ArrayList<>(),
                                    new Point(Math.min(firstPoint.getX(), secondPoint.getX()), Math.min(firstPoint.getY(), secondPoint.getY()), Math.min(firstPoint.getZ(), secondPoint.getZ())),
                                    new Point(Math.max(firstPoint.getX(), secondPoint.getX()) + 1, Math.max(firstPoint.getY(), secondPoint.getY()) + 1, Math.max(firstPoint.getZ(), secondPoint.getZ()) + 1));
                            DataManager.saveRegion(cubeRegion);
                            DataManager.getRegions().put(regionUniqueId, cubeRegion);
                            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + DataManager.getCustomMessages().get("createSuccessfully")));
                        } else {
                            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + DataManager.getCustomMessages().get("pointNotEnough")));
                        }
                    }
                } else {
                    commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + DataManager.getCustomMessages().get("noPermission")));
                }
            } else {
                commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + DataManager.getCustomMessages().get("wrongUsage")));
            }
        } else {
            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + DataManager.getCustomMessages().get("unknowCommand")));
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        List<String> tabHelper = new ArrayList<>();
        if (commandSender.hasPermission("RIR.admin")) {
            if (strings.length == 1) {
                tabHelper.add("check");
                tabHelper.add("create");
                tabHelper.add("createMode");
                tabHelper.add("help");
                tabHelper.add("reload");
                tabHelper.add("switch");
                return tabHelper;
            } else if (strings.length == 2) {
                if (strings[0].equalsIgnoreCase("check")) {
                    for (String regionUid : DataManager.getRegions().keySet()) {
                        tabHelper.add(regionUid);
                    }
                    return tabHelper;
                } else if (strings[0].equalsIgnoreCase("create")) {
                    tabHelper.add("ball");
                    tabHelper.add("cube");
                } else if (strings[0].equalsIgnoreCase("switch")) {
                    tabHelper.add("biome");
                    tabHelper.add("region");
                }
            }
        }
        return tabHelper;
    }
}
