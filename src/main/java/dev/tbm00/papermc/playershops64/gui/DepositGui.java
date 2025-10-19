package dev.tbm00.papermc.playershops64.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import dev.triumphteam.gui.guis.Gui;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.structure.PriceNode;
import dev.tbm00.papermc.playershops64.data.structure.PriceQueue;
import dev.tbm00.papermc.playershops64.utils.ShopUtils;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class DepositGui {
    private final PlayerShops64 javaPlugin;
    private final Gui gui;
    private final Player player;

    /**
     * Empty gui that try to sell all items inside once closed.
     */
    public DepositGui(PlayerShops64 javaPlugin, Player player) {
        this.javaPlugin = javaPlugin;
        this.gui = new Gui(6, "Deposit Gui");
        this.player = player;

        gui.setCloseGuiAction(event -> {
            Bukkit.getScheduler().runTask(javaPlugin, () -> {
                if (player.isOnline() && !player.isDead()) depositItems(event.getInventory());
                else {
                    for (ItemStack item : event.getInventory().getStorageContents()) {
                        StaticUtils.addToInventoryOrDrop(player, item);
                    }
                }
            });
        });
        gui.enableAllInteractions();
        gui.open(player);
    }

    private void depositItems(Inventory inv) {
        int totalItemsSold = 0;

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
                        int result = ShopUtils.quickDepositToShop(player, node.getUuid(), currentInvAmount);

                        if (result==0) {
                            continue;
                        } else {
                            currentInvAmount -= result;
                            totalItemsSold += result;
                        }
                    } else continue;
                }

                if (currentInvAmount>0) {
                    StaticUtils.addToInventoryOrDrop(player, invItem, currentInvAmount);
                }
            } catch (Exception e) {
                StaticUtils.log(ChatColor.RED, "Caught exception quick depositing in deposit gui: " + e.getMessage());
            }
        }

        if (totalItemsSold<1) StaticUtils.sendMessage(player, "&cCouldn't find any applicable shops for your items!");
        else StaticUtils.sendMessage(player, "&aDepsoited " + totalItemsSold + " items into your shops!");
    }
}