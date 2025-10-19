

package dev.tbm00.papermc.playershops64.command;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.enums.QueryType;
import dev.tbm00.papermc.playershops64.data.enums.SortType;
import dev.tbm00.papermc.playershops64.gui.DepositGui;
import dev.tbm00.papermc.playershops64.gui.ListShopsGui;
import dev.tbm00.papermc.playershops64.gui.SellGui;
import dev.tbm00.papermc.playershops64.utils.*;

public class AdminCmd implements TabExecutor {
    private final PlayerShops64 javaPlugin;

    public AdminCmd(PlayerShops64 javaPlugin) {
        this.javaPlugin = javaPlugin;
    }

    /**
     * Handles the /testshopadmin command.
     * 
     * @param player the command sender
     * @param consoleCommand the command being executed
     * @param alias the alias used for the command
     * @param args the arguments passed to the command
     * @return true if the command was handled successfully, false otherwise
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!StaticUtils.hasPermission(sender, StaticUtils.ADMIN_PERM)) {
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
            case "menu":
            case "gui":
                return handleMenuCmd(player);
            case "sellgui":
                return handleSellGuiCmd(player);
            case "depositgui":
                return handleDepositGuiCmd(player);
            case "give":
                return handleGiveCmd(player, args);
            default: {
                StaticUtils.sendMessage(sender, "&cNo applicable argument provided!");
                return true;
            }
        }
    }
    
    private boolean handleHelpCmd(Player player) {
        player.sendMessage(ChatColor.DARK_PURPLE + "--- " + ChatColor.LIGHT_PURPLE + "Admin Shop Commands" + ChatColor.DARK_PURPLE + " ---\n"
            + ChatColor.WHITE + "/testshopadmin" + ChatColor.GRAY + " Base admin command\n"
        );
        return true;
    }

    private boolean handleSellGuiCmd(Player player) {
        new SellGui(javaPlugin, player);
        return true;
    }

    private boolean handleDepositGuiCmd(Player player) {
        new DepositGui(javaPlugin, player);
        return true;
    }

    private boolean handleBuyCmd(Player player, String[] args) {
        Integer amount = 1;
        if (args.length>1) {
            amount = Integer.parseInt(args[1]);
        }

        StaticUtils.addToInventoryOrDrop(player, StaticUtils.prepPlayerShopItemStack(amount));
        player.sendMessage(ChatColor.GREEN + "You should've received "+amount+" lectern(s) with the PDC key!");
        return true;
    }

    private boolean handleMenuCmd(Player player) {
        new ListShopsGui(javaPlugin, javaPlugin.getShopHandler().getShopView(), player, true, SortType.MATERIAL, QueryType.NO_QUERY, null);
        return true;
    }

    private boolean handleGiveCmd(CommandSender sender, String[] args) {
        String argument = args.length >= 2 ? args[1] : null; // should be player
        String argument2 = args.length >= 3 ? args[2] : null; // should be item type
        String argument3 = args.length >= 4 ? args[3] : null; // should be amount

        int amount = 1;
        Integer j = Integer.parseInt(argument3);
        if (j!=null) amount = j;

        Player player = getPlayerFromCommand(sender, argument);
        if (player == null) {
            sender.sendMessage(ChatColor.RED + "Could not find target player!");
            return false;
        }

        if (argument2==null || argument2.isBlank()) {
            sender.sendMessage(ChatColor.RED + "Can't give nothing!");
            return false;
        } argument2.replace("_","");

        if (argument2.equalsIgnoreCase("SELLWAND")) {
            StaticUtils.sendMessage(player, "&aReceived " + amount + " sell wand!");
            StaticUtils.addToInventoryOrDrop(player, StaticUtils.prepSellWandItemStack(amount));
        } else if (argument2.equalsIgnoreCase("DEPOSITWAND")) {
            StaticUtils.sendMessage(player, "&aReceived " + amount + " deposit wand!");
            StaticUtils.addToInventoryOrDrop(player, StaticUtils.prepDepositWandItemStack(amount));
        } else if (argument2.equalsIgnoreCase("SHOP") || argument2.equalsIgnoreCase("SHOPBLOCK") || argument2.equalsIgnoreCase("BASEBLOCK")) {
            StaticUtils.sendMessage(player, "&aReceived " + amount + " player shops!");
            StaticUtils.addToInventoryOrDrop(player, StaticUtils.prepPlayerShopItemStack(amount));
        } else {
            sender.sendMessage(ChatColor.RED + "'"+argument2+"' is not defined!");
            return false;
        }

        return true;
    }

    private Player getPlayerFromCommand(CommandSender sender, String arg) {
        if (arg == null) {
            sender.sendMessage(ChatColor.RED + "Could not find target player!");
            return null;
        } else {
            Player player = javaPlugin.getServer().getPlayer(arg);
            if (player == null) {
            }
            return player;
        }
    }

    /**
     * Handles tab completion for the /testshopadmin command.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.clear();
            String[] subCmds = new String[]{"<item>","<player>","transfer","pos1","pos2","copy","paste"};
            for (String n : subCmds) {
                if (n!=null && n.startsWith(args[0])) 
                    list.add(n);
            }
            Bukkit.getOnlinePlayers().forEach(player -> {
                if (player.getName().startsWith(args[0])&&args[0].length()>0)
                    list.add(player.getName());
            });
            for (Material mat : Material.values()) {
                if (mat.name().toLowerCase().startsWith(args[0].toLowerCase())&&args[0].length()>1)
                    list.add(mat.name().toLowerCase());
            }
        } else if (args.length == 2) {
            if (args[0].equals("transfer")) {
                Bukkit.getOnlinePlayers().forEach(player -> {
                    if (player.getName().startsWith(args[1]))
                        list.add(player.getName());
                });
            }
        } else if (args.length == 3) {
            if (args[0].equals("transfer")) {
                Bukkit.getOnlinePlayers().forEach(player -> {
                    if (player.getName().startsWith(args[1]))
                        list.add(player.getName());
                });
            }
        }
        return list;
    }
}