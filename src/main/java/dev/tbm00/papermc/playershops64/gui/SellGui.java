package dev.tbm00.papermc.playershops64.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.structure.PriceNode;
import dev.tbm00.papermc.playershops64.data.structure.PriceQueue;
import dev.tbm00.papermc.playershops64.data.structure.Shop;
import dev.tbm00.papermc.playershops64.utils.ShopUtils;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.PaginatedGui;

public class SellGui {
    private final PlayerShops64 javaPlugin;
    private final Gui gui;
    private final Player player;

    /**
     * Empty gui that try to sell all items inside once closed.
     */
    public SellGui(PlayerShops64 javaPlugin, Player player) {
        this.javaPlugin = javaPlugin;
        this.gui = new Gui(6, "Sell Gui");
        this.player = player;

        gui.setCloseGuiAction(event -> {
            BukkitScheduler scheduler = Bukkit.getScheduler();
            scheduler.runTask(javaPlugin, () -> {
                if (player.isOnline() && !player.isDead()) sellItems(player, null);
            });
        });
        gui.enableAllInteractions();
        gui.open(player);
    }

    private void sellItems(Player player, Inventory inv) {
        int totalEarned = 0, totalItemsSold = 0;

        invItemFor:
        for (ItemStack invItem : inv.getContents()) {
            if (invItem == null) continue;

            Material material = invItem.getType();
            int currentInvAmount = invItem.getAmount();
            
            
            try {
                PriceQueue queue = javaPlugin.getShopHandler().getSellPriceQueue(material);
                if (queue==null || queue.isEmpty()) continue invItemFor;
                for (PriceNode node : queue) {
                    if (currentInvAmount<=0) continue invItemFor;
                    if (javaPlugin.getShopHandler().getShop(node.getUuid()).getItemStack().isSimilar(invItem)) {
                        int[] results = ShopUtils.quickSellToShop(player, node.getUuid(), currentInvAmount);

                        if (results[0]==0) {
                            continue;
                        } else {
                            currentInvAmount -= results[0];
                            totalItemsSold += results[0];
                            totalEarned += results[1];
                        }
                    } else continue;
                }

                if (currentInvAmount>0) {
                    StaticUtils.addToInventoryOrDrop(player, invItem, currentInvAmount);
                }
            } catch (Exception e) {
                StaticUtils.log(ChatColor.RED, "Caught exception quick selling in sell gui: " + e.getMessage());
            }
        }

        if (totalItemsSold<1) StaticUtils.sendMessage(player, "&cCouldn't find any applicable shops for your items!");
        else StaticUtils.sendMessage(player, "&aSold " + totalItemsSold + " items for a total of $" + StaticUtils.formatIntUS(totalEarned));
    }
}