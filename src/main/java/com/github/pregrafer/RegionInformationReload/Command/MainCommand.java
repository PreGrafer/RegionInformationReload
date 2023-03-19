package com.github.pregrafer.RegionInformationReload.Command;

import com.github.pregrafer.RegionInformationReload.Manager.DataManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.List;

public class MainCommand implements CommandExecutor, TabExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!commandSender.hasPermission("RIR.admin")) {
            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + DataManager.getCustomMessages().get("noPermission")));
            return false;
        }
        if (strings.length == 1) {
            if (strings[0].equalsIgnoreCase("reload")) {
                DataManager.reload();
                commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + DataManager.getCustomMessages().get("reload")));
                return true;
            } else if (strings[0].equalsIgnoreCase("check")) {
                commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + DataManager.getCustomMessages().get("wrongUsage")));
            } else {
                commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', DataManager.getPluginPrefix() + DataManager.getCustomMessages().get("unknowCommand")));
            }
        } else if (strings.length == 2) {
            if (strings[0].equalsIgnoreCase("check") && DataManager.getRegions().containsKey(strings[1])) {
                commandSender.sendMessage(DataManager.getRegions().get(strings[1]).toString());
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
                tabHelper.add("reload");
                return tabHelper;
            } else if (strings.length == 2) {
                for (String regionUid : DataManager.getRegions().keySet()) {
                    tabHelper.add(regionUid);
                }
                return tabHelper;
            }
        }
        return tabHelper;
    }
}
