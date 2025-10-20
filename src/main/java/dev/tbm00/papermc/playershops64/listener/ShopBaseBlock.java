package dev.tbm00.papermc.playershops64.listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.structure.Shop;
import dev.tbm00.papermc.playershops64.data.enums.AdjustType;
import dev.tbm00.papermc.playershops64.gui.ShopManageGui;
import dev.tbm00.papermc.playershops64.gui.ShopTransactionGui;
import dev.tbm00.papermc.playershops64.utils.ShopUtils;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class ShopBaseBlock implements Listener {
    private final PlayerShops64 javaPlugin;

    public ShopBaseBlock(PlayerShops64 javaPlugin) {
        this.javaPlugin = javaPlugin;
    }

    @EventHandler
    public void onShopBaseBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (!block.getType().equals(Material.LECTERN)) return;

        ItemStack heldItem = event.getItemInHand();
        ItemMeta heldItemMeta = heldItem.getItemMeta();
        if (heldItemMeta == null) return;

        // Verify held item has base block PDC key
        PersistentDataContainer itemDataContainer = heldItemMeta.getPersistentDataContainer();
        if (!itemDataContainer.has(StaticUtils.SHOP_KEY, PersistentDataType.STRING)) return;
        
        // Prevent duplicate shops at tsame location
        Location location = block.getLocation();
        if (javaPlugin.getShopHandler().hasShopAtBlock(location)) {
            event.setCancelled(true);
            StaticUtils.sendMessage(event.getPlayer(), "&cThere is already a PlayerShop at this block.");
            return;
        }

        // Apply PDC key to new block's tileState
        BlockState blockState = block.getState();
        if (blockState instanceof TileState tileState) {
            tileState.getPersistentDataContainer().set(StaticUtils.SHOP_KEY, PersistentDataType.STRING, "true");
            tileState.update(true, false); // apply to world
        }

        Player owner = event.getPlayer();
        World world = block.getWorld();

        ShopUtils.createShop(owner, world, location);

        StaticUtils.sendMessage(owner, "&aCreated a new PlayerShop! Click to manage it, or sneak-left-click to start selling your held item");
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockClick(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if (!isProtectedShopBlock(block)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        Shop shop = javaPlugin.getShopHandler().getShopAtBlock(block.getLocation());
        if (shop == null) {
            StaticUtils.sendMessage(player, "&cShop data not found for this block. Try relogging or contact staff.");
            return;
        }

        boolean isManager = player.getUniqueId().equals(shop.getOwnerUuid()) || shop.isAssistant(player.getUniqueId());
        boolean isSneaking = player.isSneaking();
        Action action = event.getAction();

        if (isManager) {
            if (isSneaking) {
                if (action==Action.LEFT_CLICK_BLOCK) {
                    /*if (shop.getItemStack()==null || shop.getItemStock()<1) {
                        ShopUtils.deleteShop(player, shop.getUuid(), block);
                        return;
                    } else {*/
                        ShopUtils.adjustStock(player, shop.getUuid(), AdjustType.ADD, 1);
                        return;
                    //}
                } else if (action==Action.RIGHT_CLICK_BLOCK) {
                    if (shop.getItemStack()==null) {
                        ShopUtils.setShopItem(player, shop.getUuid());
                        return;
                    } else {
                        ShopUtils.adjustStock(player, shop.getUuid(), AdjustType.REMOVE, 1);
                        return;
                    }
                } else return;
            } else {
                if (action==Action.LEFT_CLICK_BLOCK || action==Action.RIGHT_CLICK_BLOCK) {
                    new ShopManageGui(javaPlugin, player, false, shop.getUuid());
                    return;
                } else return;
            }
        } else {
            if (shop.getItemStack()==null) {
                StaticUtils.sendMessage(player, "&cThis shop does not have a sale item set up!");
                return;
            }
            if (shop.getBuyPrice()==null && shop.getSellPrice()==null) {
                StaticUtils.sendMessage(player, "&cThis shop has buying and selling both disabled!");
                return;
            }
            new ShopTransactionGui(javaPlugin, player, false, shop.getUuid(), 1, true);
            return;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (isProtectedShopBlock(block)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(this::isProtectedShopBlock);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(this::isProtectedShopBlock);
    }

    private boolean isProtectedShopBlock(Block b) {
        BlockState blockState = b.getState();
        if (!(blockState instanceof TileState tileState)) return false;
        return tileState.getPersistentDataContainer().has(StaticUtils.SHOP_KEY, PersistentDataType.STRING);
    }
}