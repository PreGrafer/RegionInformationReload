package com.github.pregrafer.RegionInformationReload.Manager;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * 用于信息发送
 */
public class InfoManager {

    private Player player;
    private List<String> infos;

    public InfoManager(Player player, List<String> infos) {
        this.player = player;
        this.infos = infos;
    }

    // 更新InfoManager属性 现未使用
    public void updatePlayerInfo(Player player, List<String> infos) {
        this.player = player;
        this.infos = infos;
    }

    public void sendInfos() {
        // 对每一条info匹配输出格式并调用方法
        infos.replaceAll(info -> info.replace("%player%", player.getName()));
        for (String info : infos) {
            MessageType type = getMessageType(info);
            if (type != null) {
                switch (type) {
                    case MSG:
                        sendMessage(info);
                        break;
                    case TITLE:
                        sendTitle(info);
                        break;
                    case ACTION_BAR:
                        sendActionBar(info);
                        break;
                    case PCOM:
                        sendPlayerCommand(info);
                        break;
                    case CCOM:
                        sendConsoleCommand(info);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    //消息分类
    private MessageType getMessageType(String info) {
        if (info.contains("[MSG]")) {
            return MessageType.MSG;
        } else if (info.contains("[TITLE]")) {
            return MessageType.TITLE;
        } else if (info.contains("[ACB]")) {
            return MessageType.ACTION_BAR;
        } else if (info.contains("[PCOM]")) {
            return MessageType.PCOM;
        } else if (info.contains("[CCOM]")) {
            return MessageType.CCOM;
        }
        return null;
    }

    /*
    下方为发送信息的具体实现与消息分类的枚举类
     */
    private void sendMessage(String info) {
        String message = info.split("-", 2)[1];
        player.sendMessage(message);
    }

    private void sendTitle(String info) {
        String message = info.split("-", 2)[1];
        String[] split = message.split("\\:\\:", 5);
        String title = split[0];
        String subTitle = split[1];
        int in = Integer.parseInt(split[2].trim());
        int stay = Integer.parseInt(split[3].trim());
        int out = Integer.parseInt(split[4].trim());
        player.sendTitle(title, subTitle, in, stay, out);
    }

    private void sendActionBar(String info) {
        String message = info.split("-", 2)[1];
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }

    private void sendPlayerCommand(String info) {
        player.performCommand(info.split("-", 2)[1]);
    }

    private void sendConsoleCommand(String info) {
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), info.split("-", 2)[1]);
    }

    private enum MessageType {
        // 消息
        MSG,
        // 标题
        TITLE,
        // 活动栏
        ACTION_BAR,
        // 玩家指令
        PCOM,
        // 后台指令
        CCOM
    }
}
