package dev.tbm00.papermc.playershops64.listener;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class PlayerCoupon implements Listener {
    private final PlayerShops64 javaPlugin;

    private final Location adminShopLocation;
    private final int nearbyRadius;

    public PlayerCoupon(PlayerShops64 javaPlugin) {
        this.javaPlugin = javaPlugin;
        adminShopLocation = StaticUtils.parseLocation(javaPlugin.getConfigHandler().getAdminShopLocation());
        nearbyRadius = javaPlugin.getConfigHandler().getAdminShopNearByRadius();
        javaPlugin.getLogger().info("player coupon listener initialized");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerUseEmptyMap(PlayerInteractEvent event) {
        if (event.getItem()==null || event.getItem().getType()!=Material.MAP) return;

        ItemStack item = event.getItem().clone();
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) return;

        // Verify held item has base block PDC key
        PersistentDataContainer itemDataContainer = itemMeta.getPersistentDataContainer();
        if (!itemDataContainer.has(StaticUtils.COUPON_HALF_OFF_KEY, PersistentDataType.STRING)) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        if (javaPlugin.getShopHandler().activeCoupons.contains(playerUuid)) {
            StaticUtils.sendMessage(player, "&cYou are already using a coupon... &eYour next purchase at &o/adminshop &r&ewill discounted!");
            return;
        }

        if (!player.getLocation().getWorld().equals(adminShopLocation.getWorld())) {
            StaticUtils.sendMessage(player, "&cYou must be at the AdminShop to use the coupon! Teleport there with &o/adminshop&r&c!");
            return;
        }

        if (adminShopLocation.distance(player.getLocation()) > nearbyRadius) {
            StaticUtils.sendMessage(player, "&cYou must be at the AdminShop to use the coupon! Teleport there with &o/adminshop&r&c!");
            return;
        }

        if (!StaticUtils.removeSpecificHeldItem(player, item, 1)) {
            StaticUtils.sendMessage(player, "&cFailed to remove the coupon from your hand!");
            return;
        }

        StaticUtils.sendMessage(event.getPlayer(), "&aRedeemed a coupon -- your next purchase at &o/adminshop&r&a will discounted!");
        StaticUtils.sendMessage(event.getPlayer(), "&2(be sure to make your purchase before the day ends)");
        javaPlugin.getShopHandler().activeCoupons.add(playerUuid);
    }
}