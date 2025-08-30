package dev.tbm00.papermc.playershops64.display;

import java.util.Map;
import java.util.UUID;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.ShopHandler;
import dev.tbm00.papermc.playershops64.data.Shop;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;

public class VisualTask extends BukkitRunnable {
    private final PlayerShops64 javaPlugin;
    private final ShopHandler shopHandler;

    public int tickCycle;
    public int viewDistance;
    public int focusDistance;
    public float scale;
    public static double OFFX = 0.0, OFFY = 0.0, OFFZ = 0.0;

    // simple spin state (radians) per item display entity UUID — optional
    // you can extend this to smoother interpolation if desired
    // Map<UUID, Float> spinAngles = new HashMap<>();
    public VisualTask(PlayerShops64 javaPlugin, ShopHandler shopHandler) {
        this.javaPlugin = javaPlugin;
        this.shopHandler = shopHandler;
        this.tickCycle = javaPlugin.configHandler.getDisplayTickCycle();
        this.viewDistance = javaPlugin.configHandler.getDisplayViewDistance();
        this.focusDistance = javaPlugin.configHandler.getDisplayFocusDistance();
        this.scale = (float) javaPlugin.configHandler.getDisplayGlassScale();
    }

    @Override
    public void run() {
        Map<UUID, Shop> shops = shopHandler.getShopMap();
        if (shops == null || shops.isEmpty()) return;

        for (Shop shop : shops.values()) {
            // Guards
            if (shop == null || shop.getLocation() == null || shop.getWorld() == null) continue;
            World world = shop.getWorld();
            if (!world.isChunkLoaded(shop.getLocation().getBlockX() >> 4, shop.getLocation().getBlockZ() >> 4)) continue;

            // Get/build display
            UUID id = shop.getUuid();
            ShopDisplay shopDisplay = shopHandler.getDisplayManager().getOrCreate(id, shop);
            if (shopDisplay == null) continue;

            // Build text
            String text = shopHandler.formatShopText(shop);

            // Update entities
            shopDisplay.update(world, text, scale, OFFX, OFFY, OFFZ);

            // Per-player show/hide
            for (Player player : javaPlugin.getServer().getOnlinePlayers()) {
                if (!player.isOnline()) continue;

                boolean inRange = shopDisplay.shouldSee(player, viewDistance);
                if (!inRange) {
                    shopDisplay.hide(player, false);
                    continue;
                }
                boolean focused = shopHandler.isLookingAtShop(player, shop, focusDistance);
                shopDisplay.show(player, focused);
            }
        }
    }

    // isNearbyShop(player, shop, radius / 2.0);
    private boolean isNearbyShop(Player p, Shop shop, double halfRadius) {
        if (!p.getWorld().equals(shop.getWorld())) return false;
        double dx = Math.abs(p.getLocation().getX() - shop.getLocation().getX());
        double dy = Math.abs(p.getLocation().getY() - shop.getLocation().getY());
        double dz = Math.abs(p.getLocation().getZ() - shop.getLocation().getZ());
        return dx <= halfRadius && dy <= halfRadius && dz <= halfRadius;
    }
}
