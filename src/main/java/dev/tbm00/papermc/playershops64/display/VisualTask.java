package dev.tbm00.papermc.playershops64.display;

import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.ShopHandler;
import dev.tbm00.papermc.playershops64.data.structure.Shop;
import dev.tbm00.papermc.playershops64.utils.ShopUtils;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class VisualTask extends BukkitRunnable {
    private final PlayerShops64 javaPlugin;
    private final ShopHandler shopHandler;

    private int viewDistance;
    private int focusDistance;

    public VisualTask(PlayerShops64 javaPlugin, ShopHandler shopHandler) {
        this.javaPlugin = javaPlugin;
        this.shopHandler = shopHandler;
        this.viewDistance = javaPlugin.getConfigHandler().getDisplayViewDistance();
        this.focusDistance = javaPlugin.getConfigHandler().getDisplayFocusDistance();
        StaticUtils.log(ChatColor.GREEN, "VisualTask initialized.");
    }

    @Override
    public void run() {
        //StaticUtils.log(ChatColor.YELLOW, "VisualTask.run(): start");
        Map<UUID, Shop> shops = shopHandler.getShopView();
        if (shops == null || shops.isEmpty()) {
            //StaticUtils.log(ChatColor.YELLOW, "VisualTask.run(): shop map empty");
            return;
        }

        for (Shop shop : shops.values()) {
            // Guards
            if (shop == null || shop.getLocation() == null || shop.getWorld() == null) {
                //StaticUtils.log(ChatColor.YELLOW, "VisualTask.run(): shop, location, or world is null");
                continue;
            }
            World world = shop.getWorld();
            if (!world.isChunkLoaded(shop.getLocation().getBlockX() >> 4, shop.getLocation().getBlockZ() >> 4)) {
                //StaticUtils.log(ChatColor.YELLOW, "VisualTask.run(): EXIT - chunk not loaded");
                continue;
            }

            // Get/build display
            UUID shopUuid = shop.getUuid();
            ShopDisplay shopDisplay = shopHandler.getDisplayManager().getOrCreate(shopUuid);
            if (shopDisplay == null) {
                StaticUtils.log(ChatColor.YELLOW, "VisualTask.run(): EXIT - shopDisplay null");
                continue;
            }

            // Build text
            String text = ShopUtils.formatHologramText(shop);

            // Update entities
            shopDisplay.update(world, text);

            // Per-player show/hide
            //StaticUtils.log(ChatColor.YELLOW, "VisualTask.run().forEachPlayer(): ");
            for (Player player : javaPlugin.getServer().getOnlinePlayers()) {
                if (!player.isOnline()) continue;
                //StaticUtils.log(ChatColor.YELLOW, "VisualTask.run().forEachPlayer(): "+player.getName());

                boolean inRange = shopDisplay.shouldSee(player, viewDistance);
                if (!inRange) {
                    //StaticUtils.log(ChatColor.YELLOW, "VisualTask.run().forEachPlayer(): "+player.getName()+" not in range -> hide");
                    shopDisplay.hide(player, true);
                    continue;
                }
                boolean focused = shopHandler.isLookingAtShop(player, shop, focusDistance);
                shopDisplay.show(player, focused);
                //StaticUtils.log(ChatColor.YELLOW, "VisualTask.run().forEachPlayer(): "+player.getName()+" in range -> show, focused: "+focused);
            }
        }
    }
}
