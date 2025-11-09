

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

public class ExchangeShopCmd implements TabExecutor {
    //private final PlayerShops64 javaPlugin;

    public ExchangeShopCmd(PlayerShops64 javaPlugin) {
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
        int removed = removeOldDisplayshops(player);

        if (removed<=0) {
            StaticUtils.sendMessage(sender, "&cError: Couldn't find any old displayshops in your inventory!");
            return true;
        }

        StaticUtils.sendMessage(player, "&aReceived " + removed + " shops!");
        StaticUtils.addToInventoryOrDrop(player, StaticUtils.prepPlayerShopItemStack(1), removed);
        return true;
    }


    private int removeOldDisplayshops(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] storage = inv.getStorageContents();


        int removed = 0;

        for (int i = 0; i < storage.length; i++) {
            ItemStack stack = storage[i];
            if (stack == null) continue;
            if (!stack.getType().equals(Material.LECTERN)) continue;

            ItemMeta meta = stack.getItemMeta();
            if (meta==null || !meta.hasLore()) continue;
            
            List<String> lore = meta.getLore();
            if (lore==null || lore.isEmpty()) continue;

            boolean valid = false;
            for (String line : lore) {
                if (line.contains("empty shop")) {
                    valid = true;
                    break;
                }
            }

            if (valid) {
                int take = stack.getAmount();
                if (take==0) continue;

                int newAmt = stack.getAmount() - take;
                if (newAmt <= 0) inv.setItem(i, null);
                else stack.setAmount(newAmt);

                removed+=take;
            }
        }
        return removed;
    }

    /**
     * Handles tab completion for the /exchangesellwand command.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }
}