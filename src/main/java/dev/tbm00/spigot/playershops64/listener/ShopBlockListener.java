package dev.tbm00.spigot.playershops64.listener;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
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

import dev.tbm00.spigot.playershops64.PlayerShops64;

public class ShopBlockListener implements Listener {

    private final PlayerShops64 javaPlugin;

    public ShopBlockListener(PlayerShops64 javaPlugin) {
        this.javaPlugin = javaPlugin;
    }

    @EventHandler
    public void onShopBaseBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (!block.getType().equals(Material.LECTERN)) return;

        ItemStack item = event.getItemInHand();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey key = new NamespacedKey(javaPlugin, "base-shop-block");
        PersistentDataContainer itemDataContainer = meta.getPersistentDataContainer();

        if (!itemDataContainer.has(key, PersistentDataType.STRING)) {
            event.getPlayer().sendMessage("ItemStack didn't have the PDC key");
            return;
        }

        String value = itemDataContainer.get(key, PersistentDataType.STRING);
        event.getPlayer().sendMessage("The itemStack has the PDC key with value: " + value);

        // Write PDC key to new block's tileState
        BlockState state = event.getBlockPlaced().getState();
        if (state instanceof TileState tileState) {
            tileState.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
            boolean updatedState = tileState.update(true, false); // apply to world
            event.getPlayer().sendMessage("Wrote key to block PDC: " + value + " (updated ==" + updatedState + ")");
        } else {
            event.getPlayer().sendMessage("Placed block doesn't have a TileState");
        }
    }

    private boolean isProtected(Block b) {
        BlockState s = b.getState();
        if (!(s instanceof TileState ts)) return false;
        NamespacedKey key = new NamespacedKey(javaPlugin, "base-shop-block");
        return ts.getPersistentDataContainer().has(key, PersistentDataType.STRING);
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