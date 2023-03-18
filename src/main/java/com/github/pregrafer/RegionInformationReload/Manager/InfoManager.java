package com.github.pregrafer.RegionInformationReload.Manager;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.List;

public class InfoManager {

    private Player player;
    private List<String> infos;

    public InfoManager(Player player, List<String> infos) {
        this.player = player;
        this.infos = infos;
    }

    public void updatePlayerInfo(Player player, List<String> infos) {
        this.player = player;
        this.infos = infos;
    }

    public void sendInfos() {
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
                }
            }
        }
    }

    private MessageType getMessageType(String info) {
        if (info.contains("[MSG]")) {
            return MessageType.MSG;
        } else if (info.contains("[TITLE]")) {
            return MessageType.TITLE;
        } else if (info.contains("[ACB]")) {
            return MessageType.ACTION_BAR;
        }
        return null;
    }

    private void sendMessage(String info) {
        String message = info.split("-", 2)[1];
        player.sendMessage(message);
    }

    private void sendTitle(String info) {
        String message = info.split("-", 2)[1];
        String[] split = message.split("\\:\\:", 5);
        String title = split[0];
        String subTitle = split[1];
        int in = Integer.valueOf(split[2].trim());
        int stay = Integer.valueOf(split[3].trim());
        int out = Integer.valueOf(split[4].trim());
        player.sendTitle(title, subTitle, in, stay, out);
    }

    private void sendActionBar(String info) {
        String message = info.split("-", 2)[1];
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }

    private enum MessageType {
        MSG,
        TITLE,
        ACTION_BAR
    }
}
