

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

import dev.tbm00.papermc.playershops64.utils.*;

public class AdminCmd implements TabExecutor {

    public AdminCmd() {}

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
            default: {
                StaticUtils.sendMessage(sender, "&cNo applicable argument provided!");
                return true;
            }
        }
    }
    
    /**
     * Handles the sub command for the help menu.
     * 
     * @param player the command sender
     * @return true after displaying help menu
     */
    private boolean handleHelpCmd(Player player) {
        player.sendMessage(ChatColor.DARK_PURPLE + "--- " + ChatColor.LIGHT_PURPLE + "Admin Shop Commands" + ChatColor.DARK_PURPLE + " ---\n"
            + ChatColor.WHITE + "/testshopadmin" + ChatColor.GRAY + " Base admin command\n"
        );
        return true;
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