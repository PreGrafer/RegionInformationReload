package pregrafer.rir.command;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class ItemTypeCommand implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("RIR.itemtype")) {
            sender.sendMessage("你没有权限");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("此命令只能由玩家执行");
            return true;
        }
        sender.sendMessage(((Player) sender).getInventory().getItemInMainHand().getType().name());
        return true;
    }
}
