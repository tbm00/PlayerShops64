

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
import dev.tbm00.papermc.playershops64.data.enums.QueryType;
import dev.tbm00.papermc.playershops64.data.enums.SortType;
import dev.tbm00.papermc.playershops64.gui.ListShopsGui;
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
            case "menu":
                return handleMenuCmd(player);
            case "own":
                return handleOwnCmd(player);
            default: {
                StaticUtils.sendMessage(sender, "&cNo applicable argument provided!");
                return true;
            }
        }
    }
    
    private boolean handleHelpCmd(Player player) {
        player.sendMessage(ChatColor.DARK_PURPLE + "--- " + ChatColor.LIGHT_PURPLE + "Player Shop Commands" + ChatColor.DARK_PURPLE + " ---\n"
            + ChatColor.WHITE + "/testshop" + ChatColor.GRAY + " Base player command\n"
        );
        return true;
    }

    private boolean handleMenuCmd(Player player) {
        new ListShopsGui(javaPlugin, javaPlugin.getShopHandler().getShopView(), player, SortType.MATERIAL, QueryType.NO_QUERY, null, false);
        return true;
    }

    private boolean handleOwnCmd(Player player) {
        new ListShopsGui(javaPlugin, javaPlugin.getShopHandler().getShopView(), player, SortType.MATERIAL, QueryType.PLAYER_UUID, player.getUniqueId().toString(), false);
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