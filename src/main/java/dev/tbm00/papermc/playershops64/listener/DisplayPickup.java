package dev.tbm00.papermc.playershops64.listener;

import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.persistence.PersistentDataType;

import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class DisplayPickup implements Listener {

    private static boolean isShopDisplay(Item it) {
        if (it == null || it.getPersistentDataContainer() == null) return false;
        String tag = it.getPersistentDataContainer().get(StaticUtils.DISPLAY_KEY, PersistentDataType.STRING);
        return "item".equals(tag);
    }

    // Block players, mobs, any living entity
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityPickup(EntityPickupItemEvent e) {
        if (isShopDisplay(e.getItem())) {
            e.setCancelled(true);
        }
    }

    // Block hoppers and hopper minecarts (all inventory suction)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryPickup(InventoryPickupItemEvent e) {
        if (isShopDisplay(e.getItem())) {
            e.setCancelled(true);
        }
    }
}
