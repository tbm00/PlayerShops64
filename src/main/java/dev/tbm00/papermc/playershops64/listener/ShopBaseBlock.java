package dev.tbm00.papermc.playershops64.listener;

import java.math.BigDecimal;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.Shop;
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
        if (!itemDataContainer.has(StaticUtils.SHOP_BASE_PDC_KEY, PersistentDataType.STRING)) return;
        
        // Prevent duplicate shops at tsame location
        Location location = block.getLocation();
        if (javaPlugin.shopHandler.hasShopAtBlock(location)) {
            event.setCancelled(true);
            StaticUtils.sendMessage(event.getPlayer(), "&cThere is already a PlayerShop at this block.");
            return;
        }

        // Apply PDC key to new block's tileState
        BlockState blockState = block.getState();
        if (blockState instanceof TileState tileState) {
            tileState.getPersistentDataContainer().set(StaticUtils.SHOP_BASE_PDC_KEY, PersistentDataType.STRING, "true");
            tileState.update(true, false); // apply to world
        }

        Player owner = event.getPlayer();
        World world = block.getWorld();

        Shop shop = new Shop(UUID.randomUUID(),
                            (UUID) owner.getUniqueId(),
                            owner.getName(),
                            world,
                            location,
                            null,
                            1,
                            0,
                            0,
                            100,
                            50,
                            null,
                            false,
                            false);
        
    }

    private boolean isProtected(Block b) {
        BlockState blockState = b.getState();
        if (!(blockState instanceof TileState tileState)) return false;
        return tileState.getPersistentDataContainer().has(StaticUtils.SHOP_BASE_PDC_KEY, PersistentDataType.STRING);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (isProtected(block)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(this::isProtected);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(this::isProtected);
    }
}