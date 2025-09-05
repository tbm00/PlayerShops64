

package dev.tbm00.papermc.playershops64.command;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.utils.*;

public class ShopCmd implements TabExecutor {
    private final PlayerShops64 javaPlugin;

    public ShopCmd(PlayerShops64 javaPlugin) {
        this.javaPlugin = javaPlugin;
    }

    /**
     * Handles the /testshop command.
     * 
     * @param player the command sender
     * @param consoleCommand the command being executed
     * @param alias the alias used for the command
     * @param args the arguments passed to the command
     * @return true if the command was handled successfully, false otherwise
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (sender instanceof ConsoleCommandSender) {
            StaticUtils.sendMessage(sender, "&cThis command cannot be run through the console!");
            return true;
        } else if (!StaticUtils.hasPermission(sender, StaticUtils.PLAYER_PERM)) {
            StaticUtils.sendMessage(sender, "&cNo permission!");
            return true;
        } else if (args.length == 0) {
            StaticUtils.sendMessage(sender, "&cNo argument provided!");
            return true;
        }

        Player player = (Player) sender;
        String subCmd = args[0].toLowerCase();
        switch (subCmd) {
            case "help":
                return handleHelpCmd(player);
            case "buy":
                return handleBuyCmd(player, args);
            default: {
                StaticUtils.sendMessage(sender, "&cNo applicable argument provided!");
                return true;
            }
        }
    }

    private boolean handleBuyCmd(Player player, String[] args) {
        Integer amount = 1;
        if (args.length>1) {
            amount = Integer.parseInt(args[1]);
        }

        StaticUtils.giveItem(player, StaticUtils.prepPlayerShopItemStack(amount));
        player.sendMessage(ChatColor.GREEN + "You should've received "+amount+" lectern(s) with the PDC key!");
        return true;
    }
    
    /**
     * Handles the sub command for the help menu.
     * 
     * @param player the command sender
     * @return true after displaying help menu
     */
    private boolean handleHelpCmd(Player player) {
        player.sendMessage(ChatColor.DARK_PURPLE + "--- " + ChatColor.LIGHT_PURPLE + "Player Shop Commands" + ChatColor.DARK_PURPLE + " ---\n"
            + ChatColor.WHITE + "/testshop" + ChatColor.GRAY + " Base player command\n"
        );
        return true;
    }

    /**
     * Handles tab completion for the /testshop command.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        return list;
    }
}