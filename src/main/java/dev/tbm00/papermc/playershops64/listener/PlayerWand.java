package dev.tbm00.papermc.playershops64.listener;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.structure.Shop;
import dev.tbm00.papermc.playershops64.data.structure.ShopPriceNode;
import dev.tbm00.papermc.playershops64.data.structure.ShopPriceQueue;
import dev.tbm00.papermc.playershops64.engine.QuickSellEngine;
import dev.tbm00.papermc.playershops64.gui.SellConfirmGui;
import dev.tbm00.papermc.playershops64.utils.ShopUtils;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class PlayerWand implements Listener {
    private final PlayerShops64 javaPlugin;

    public PlayerWand(PlayerShops64 javaPlugin) {
        this.javaPlugin = javaPlugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack heldItem = event.getItem();
        Block block = event.getClickedBlock();
        if (heldItem == null || player == null || block == null) return;

        ItemMeta heldItemMeta = heldItem.getItemMeta();
        if (heldItemMeta == null) return;

        if (!player.isSneaking()) return;

        if (!StaticUtils.CONTAINER_MATERIALS.contains(block.getType())) return;
        BlockState state = block.getState();
        if (!(state instanceof InventoryHolder holder)) return;
        Inventory blocksInv = holder.getInventory();

        PersistentDataContainer itemDataContainer = heldItemMeta.getPersistentDataContainer();
        if (itemDataContainer.has(StaticUtils.DESPOIT_WAND_KEY, PersistentDataType.STRING)) {
            event.setCancelled(true);
            depositItemsFromContainer(blocksInv, player);
        } else if (itemDataContainer.has(StaticUtils.SELL_WAND_KEY, PersistentDataType.STRING)) {
            event.setCancelled(true);
            sellItemsFromContainer(blocksInv, player);
        } else return;
    }

    private void sellItemsFromContainer(Inventory inv, Player player) {
        if (!player.isOnline() || player.isDead()) {
            for (ItemStack item : inv.getStorageContents()) {
                StaticUtils.addToInventoryOrDrop(player, item);
            } return;
        }

        QuickSellEngine engine = new QuickSellEngine(javaPlugin, player, inv);
        engine.computePlans(inv, player.getUniqueId());
        
        if (engine.plans.sellPlan.totalItems<=0) {
            StaticUtils.sendMessage(player, "&cCouldn't find any applicable shops for your items!");
            return;
        }

        new SellConfirmGui(javaPlugin, player, engine);
    }

    private void depositItemsFromContainer(Inventory inv, Player player) {
        int totalItemsDeposited = 0;

        for (int slot = 0; slot < inv.getSize(); slot++) {
            ItemStack invItem = inv.getItem(slot);
            if (invItem == null) continue;

            Material material = invItem.getType();
            int currentInvAmount = invItem.getAmount();
            boolean modified = false;

            if (currentInvAmount <= 0) continue;

            try {
                ShopPriceQueue queue = javaPlugin.getShopHandler().getShopPriceQueue(material);
                if (queue == null || queue.isEmpty()) continue;

                for (ShopPriceNode node : queue) {
                    if (currentInvAmount <= 0) break;

                    Shop shop = javaPlugin.getShopHandler().getShop(node.getUuid());
                    if (shop.getOwnerUuid()!=null && !shop.getOwnerUuid().equals(player.getUniqueId())) continue;
                    if (!shop.getItemStack().isSimilar(invItem)) continue;

                    int deposited = ShopUtils.quickDepositToShop(player, node.getUuid(), currentInvAmount);
                    if (deposited <= 0) continue;

                    modified = true;
                    currentInvAmount -= deposited;
                    totalItemsDeposited += deposited;
                }

                // update stack
                if (modified) {
                    if (currentInvAmount <= 0) {
                        inv.clear(slot);
                    } else {
                        invItem.setAmount(currentInvAmount);
                    }
                }
            } catch (Exception e) {
                StaticUtils.log(ChatColor.RED, "Caught exception quick depositing from container: " + e.getMessage());
            }
        }

        if (totalItemsDeposited<1) StaticUtils.sendMessage(player, "&cCouldn't find any applicable shops for your items!");
        else StaticUtils.sendMessage(player, "&aDeposited " + totalItemsDeposited + " items into your shops!");
    }
}
