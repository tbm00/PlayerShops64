package dev.tbm00.papermc.playershops64.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import net.md_5.bungee.api.chat.TextComponent;

import dev.tbm00.papermc.playershops64.PlayerShops64;

public class StaticUtils {
    private static PlayerShops64 javaPlugin;
    public static final List<String> pendingTeleports = new CopyOnWriteArrayList<>();
    
    public static final String PLAYER_PERM = "playershops64.player";
    public static final String ADMIN_PERM = "playershops64.admin";

    public static NamespacedKey DISPLAY_KEY;
    public static NamespacedKey SHOP_KEY;

    public static void init(PlayerShops64 javaPlugin) {
        StaticUtils.javaPlugin = javaPlugin;
        SHOP_KEY = new NamespacedKey(javaPlugin, "shop-base");
        DISPLAY_KEY = new NamespacedKey(javaPlugin, "display-entity");
    }

    /**
     * Logs one or more messages to the server console with the prefix & specified chat color.
     *
     * @param chatColor the chat color to use for the log messages
     * @param strings one or more message strings to log
     */
    public static void log(ChatColor chatColor, String... strings) {
		for (String s : strings)
            javaPlugin.getServer().getConsoleSender().sendMessage("[DSA64] " + chatColor + s);
	}

    /**
     * Normalizes big decimal to avoid money drift beyond 2 decimals places
     *
     * @param chatColor the chat color to use for the log messages
     * @param strings one or more message strings to log
     */
    public static BigDecimal normalizeBigDecimal(BigDecimal amount) {
        return amount == null ? null : amount.setScale(2, RoundingMode.DOWN);
    }

    /**
     * Formats int to "200,000" style
     * 
     * @param amount the amount to format
     * @return the formatted string
     */
    public static String formatInt(int amount) {
        return NumberFormat.getNumberInstance(Locale.US).format(amount);
    }

    /**
     * Formats double to "200,000" style
     * 
     * @param amount the amount to format
     * @return the formatted string
     */
    public static String formatInt(double amount) {
        return formatInt((int) amount);
    }

    /**
     * Formats material to title case
     * 
     * @param amount the material to format
     * @return the formatted string
     */
    public static String formatMaterial(Material material) {
        if (material == null) return "null";

        StringBuilder builder = new StringBuilder();
        for(String word : material.toString().split("_"))
            builder.append(word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase() + " ");
     
        return builder.toString().trim();
    }

    /**
     * Retrieves a player by their name.
     * 
     * @param arg the name of the player to retrieve
     * @return the Player object, or null if not found
     */
    public static Player getPlayer(String arg) {
        return javaPlugin.getServer().getPlayer(arg);
    }

    /**
     * Checks if the sender has a specific permission.
     * 
     * @param sender the command sender
     * @param perm the permission string
     * @return true if the sender has the permission, false otherwise
     */
    public static boolean hasPermission(CommandSender sender, String perm) {
        if (sender instanceof Player && ((Player)sender).getGameMode()==GameMode.CREATIVE) return false;
        return sender.hasPermission(perm) || sender instanceof ConsoleCommandSender;
    }

    /**
     * Sends a message to a target CommandSender.
     * 
     * @param target the CommandSender to send the message to
     * @param string the message to send
     */
    public static void sendMessage(CommandSender target, String string) {
        if (!string.isBlank())
            target.spigot().sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', javaPlugin.configHandler.getChatPrefix() + string)));
    }

    /**
     * Gives a player an ItemStack.
     * If they have a full inv, it drops on the ground.
     * 
     * @param player the player to give to
     * @param item the item to give
     */
    public static void giveItem(Player player, ItemStack item) {
        if ((player.getInventory().firstEmpty() == -1)) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        } else player.getInventory().addItem(item);
    }

    /**
     * Prepares an base block ItemStack for usage.
     * 
     * @param amount the amount to give
     */
    public static ItemStack prepPlayerShopItemStack(int amount) {
        ItemStack lectern = new ItemStack(Material.LECTERN);
        ItemMeta meta = lectern.getItemMeta();

        meta.getPersistentDataContainer().set(StaticUtils.SHOP_KEY, PersistentDataType.STRING, "true");
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aPlayerShop"));

        lectern.setItemMeta(meta);
        lectern.setAmount(amount);

        return lectern;
    }

    /**
     * Teleports a player to the given world and coordinates after a 5-second delay.
     * If the player moves during the delay, the teleport is cancelled.
     *
     * @param player the player to teleport
     * @param worldName the target world's name
     * @param x target x-coordinate
     * @param y target y-coordinate
     * @param z target z-coordinate
     * @return true if the teleport countdown was started, false if the player was already waiting
     */
    public static boolean teleportPlayer(Player player, String worldName, double x, double y, double z) {
        String playerName = player.getName();
        if (pendingTeleports.contains(playerName)) {
            StaticUtils.sendMessage(player, "&cYou are already waiting for a teleport!");
            return false;
        }
        pendingTeleports.add(playerName);
        StaticUtils.sendMessage(player, "&aTeleporting in 3 seconds -- don't move!");

        // Schedule the teleport to run later
        Bukkit.getScheduler().runTaskLater(javaPlugin, () -> {
            if (pendingTeleports.contains(playerName)) {
                // Remove player from pending list and teleport
                pendingTeleports.remove(playerName);
                World targetWorld = Bukkit.getWorld(worldName);
                if (targetWorld != null) {
                    Location targetLocation = new Location(targetWorld, x, y, z);
                    player.teleport(targetLocation);
                } else {
                    StaticUtils.sendMessage(player, "&cWorld not found!");
                }
            }
        }, 60L);

        return true;
    }

    /**
     * Executes a command as the console.
     * 
     * @param command the command to execute
     * @return true if the command was successfully executed, false otherwise
     */
    public static boolean runCommand(String command) {
        ConsoleCommandSender console = javaPlugin.getServer().getConsoleSender();
        try {
            return Bukkit.dispatchCommand(console, command);
        } catch (Exception e) {
            log(ChatColor.RED, "Caught exception running command " + command + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Executes a command as a specific player.
     * 
     * @param target the player to execute the command as
     * @param command the command to execute
     * @return true if the command was successfully executed, false otherwise
     */
    public static boolean sudoCommand(Player target, String command) {
        try {
            return Bukkit.dispatchCommand(target, command);
        } catch (Exception e) {
            log(ChatColor.RED, "Caught exception sudoing command: " + target.getName() + " : /" + command + ": " + e.getMessage());
            return false;
        }
    }

   /**
     * Executes a command as a specific human entity.
     * 
     * @param target the player to execute the command as
     * @param command the command to execute
     * @return true if the command was successfully executed, false otherwise
     */
    public static boolean sudoCommand(HumanEntity target, String command) {
        try {
            return Bukkit.dispatchCommand(target, command);
        } catch (Exception e) {
            log(ChatColor.RED, "Caught exception sudoing command: " + target.getName() + " : /" + command + ": " + e.getMessage());
            return false;
        }
    }
}