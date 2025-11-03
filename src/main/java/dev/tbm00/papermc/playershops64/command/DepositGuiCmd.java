

package dev.tbm00.papermc.playershops64.command;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.gui.DepositGui;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class DepositGuiCmd implements TabExecutor {
    private final PlayerShops64 javaPlugin;

    public DepositGuiCmd(PlayerShops64 javaPlugin) {
        this.javaPlugin = javaPlugin;
    }

    /**
     * Handles the /depositgui command.
     * 
     * @param player the command sender
     * @param consoleCommand the command being executed
     * @param alias the alias used for the command
     * @param args the arguments passed to the command
     * @return true when the command was handled
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (sender instanceof ConsoleCommandSender) {
            StaticUtils.sendMessage(sender, "&cThis command cannot be run through the console!");
            return true;
        } else if (!StaticUtils.hasPermission(sender, StaticUtils.PLAYER_PERM)) {
            StaticUtils.sendMessage(sender, "&cNo permission!");
            return true;
        }

        new DepositGui(javaPlugin, (Player) sender);
        return true;
    }

    /**
     * Handles tab completion for the /depositgui command.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }
}