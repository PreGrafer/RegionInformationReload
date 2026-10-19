package pregrafer.rir.message;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import pregrafer.rir.compat.ServerCompatibility;

import java.util.Map;

/** Immutable parsed template; replacements always produce a fresh string. */
public final class MessageAction {
    private final String type, text;
    private final String[] title;
    private final int in, stay, out;

    private MessageAction(String type, String text, String[] title, int in, int stay, int out) {
        this.type = type;
        this.text = text;
        this.title = title;
        this.in = in;
        this.stay = stay;
        this.out = out;
    }

    public static MessageAction parse(String line) {
        int end = line.indexOf("]-");
        if (!line.startsWith("[") || end < 2)
            throw new IllegalArgumentException(
                    "Message must start with [MSG]-, [TITLE]-, [ACB]-, [PCOM]- or [CCOM]-: "
                            + line);
        String type = line.substring(1, end), text = line.substring(end + 2);
        if (!type.matches("MSG|TITLE|ACB|PCOM|CCOM"))
            throw new IllegalArgumentException("Unknown message type: " + type);
        String[] title = null;
        int in = 10, stay = 40, out = 10;
        if (type.equals("TITLE")) {
            title = text.split("::", -1);
            if (title.length != 2 && title.length != 5)
                throw new IllegalArgumentException(
                        "TITLE requires title::subtitle[::fadeIn::stay::fadeOut]");
            if (title.length == 5) {
                in = ticks(title[2]);
                stay = ticks(title[3]);
                out = ticks(title[4]);
            }
        }
        return new MessageAction(type, text, title, in, stay, out);
    }

    private static int ticks(String text) {
        int n = Integer.parseInt(text.trim());
        if (n < 0 || n > 72000)
            throw new IllegalArgumentException("Title timing must be 0..72000 ticks");
        return n;
    }

    public static String render(String text, Player player, Map<String, String> values) {
        String result =
                text.replace("%player%", player.getName())
                        .replace("%uuid%", player.getUniqueId().toString());
        for (Map.Entry<String, String> value : values.entrySet())
            result = result.replace(value.getKey(), value.getValue());
        return ChatColor.translateAlternateColorCodes('&', result);
    }

    public boolean isCommand() {
        return type.equals("PCOM") || type.equals("CCOM");
    }

    public void send(Player player, Map<String, String> values) {
        String result = render(text, player, values);
        switch (type) {
            case "MSG":
                player.sendMessage(result);
                break;
            case "TITLE":
                ServerCompatibility.sendTitle(
                        player,
                        render(title[0], player, values),
                        render(title[1], player, values),
                        in,
                        stay,
                        out);
                break;
            case "ACB":
                ServerCompatibility.sendActionBar(player, result);
                break;
            case "PCOM":
                player.performCommand(result.startsWith("/") ? result.substring(1) : result);
                break;
            case "CCOM":
                Bukkit.dispatchCommand(
                        Bukkit.getConsoleSender(),
                        result.startsWith("/") ? result.substring(1) : result);
                break;
            default:
                throw new IllegalStateException(type);
        }
    }
}
