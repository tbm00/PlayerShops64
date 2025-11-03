

package dev.tbm00.papermc.playershops64.command;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class ExchangeWandCmd implements TabExecutor {
    //private final PlayerShops64 javaPlugin;

    public ExchangeWandCmd(PlayerShops64 javaPlugin) {
        //this.javaPlugin = javaPlugin;
    }

    /**
     * Handles the /exchangesellwand command.
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

        Player player = (Player) sender;
        boolean removed = false;

        if (removeOldInfSellwand(player)) removed = true;
        if (!removed && removeMultiSellwands(player)) removed = true;

        if (removed==false) {
            StaticUtils.sendMessage(sender, "&cError: Couldn't find an old infinite sellwand (or 3 finite sell wands) in your inventory!");
            return true;
        }

        StaticUtils.sendMessage(player, "&aReceived " + 1 + " sell wand!");
        StaticUtils.addToInventoryOrDrop(player, StaticUtils.prepSellWandItemStack(1));
        return true;
    }


    private boolean removeOldInfSellwand(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] storage = inv.getStorageContents();

        for (int i = 0; i < storage.length; i++) {
            ItemStack stack = storage[i];
            if (stack == null) continue;
            if (!stack.getType().equals(Material.SPYGLASS)) continue;

            ItemMeta meta = stack.getItemMeta();
            if (meta==null || !meta.hasLore()) continue;
            
            List<String> lore = meta.getLore();
            if (lore==null || lore.isEmpty()) continue;

            boolean valid = false;
            for (String line : lore) {
                if (line.contains("∞")) {
                    valid = true;
                    break;
                }
            }

            if (valid) {
                int take = Math.min(stack.getAmount(), 1);
                if (take==0) continue;

                int newAmt = stack.getAmount() - take;
                if (newAmt <= 0) inv.setItem(i, null);
                else stack.setAmount(newAmt);
                
                return true;
            }
        }
        return false;
    }

    private boolean removeMultiSellwands(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] storage = inv.getStorageContents();

        int removedCount=0;
        int oldTempWandCount = countOldTempWands(storage);
        if (oldTempWandCount>=3) {
            for (int i = 0; i < storage.length; i++) {
                if (removedCount>=oldTempWandCount) return true;

                ItemStack stack = storage[i];
                if (stack == null) continue;
                if (!stack.getType().equals(Material.SPYGLASS)) continue;

                ItemMeta meta = stack.getItemMeta();
                if (meta==null || !meta.hasLore()) continue;
                
                List<String> lore = meta.getLore();
                if (lore==null || lore.isEmpty()) continue;
                
                boolean isOldWand = false;
                boolean isInfinite = false;
                for (String line : lore) {
                    if (line.contains("uses")) {
                        isOldWand = true;
                    }
                    if (line.contains("∞")) {
                        isInfinite = true;
                    }
                }

                if (isOldWand && !isInfinite) {
                    int take = Math.min(stack.getAmount(), 1);
                    if (take!=1) continue;

                    int newAmt = stack.getAmount() - take;
                    if (newAmt <= 0) inv.setItem(i, null);
                    else stack.setAmount(newAmt);

                    removedCount++;
                    if (removedCount>=oldTempWandCount) return true;
                }
            }
        }
        return false;
    }

    private int countOldTempWands(ItemStack[] storage) {
        int oldTempWandCount = 0;

        for (int i = 0; i < storage.length; i++) {
            ItemStack stack = storage[i];
            if (stack == null) continue;
            if (!stack.getType().equals(Material.SPYGLASS)) continue;

            ItemMeta meta = stack.getItemMeta();
            if (meta==null || !meta.hasLore()) continue;
            
            List<String> lore = meta.getLore();
            if (lore==null || lore.isEmpty()) continue;
            
            boolean isOldWand = false;
            boolean isInfinite = false;
            for (String line : lore) {
                if (line.contains("uses")) {
                    isOldWand = true;
                }
                if (line.contains("∞")) {
                    isInfinite = true;
                }
            }

            if (isOldWand && !isInfinite) {
                oldTempWandCount++;
            }
        }

        return oldTempWandCount;
    }

    /**
     * Handles tab completion for the /exchangesellwand command.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }
}