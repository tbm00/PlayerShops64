package dev.tbm00.papermc.playershops64.display;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.ShopHandler;
import dev.tbm00.papermc.playershops64.data.structure.Shop;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class VisualTask extends BukkitRunnable {
    private final PlayerShops64 javaPlugin;
    private final ShopHandler shopHandler;

    private int viewDistance;
    private int viewDistanceSquared;

    public final Map<UUID, UUID> prevFocusedShop = new HashMap<>();
    public final Map<UUID, Set<UUID>> prevLoadedBases = new HashMap<>();
    public final Map<UUID, Set<UUID>> workingBases = new HashMap<>();

    public VisualTask(PlayerShops64 javaPlugin, ShopHandler shopHandler) {
        this.javaPlugin = javaPlugin;
        this.shopHandler = shopHandler;
        this.viewDistance = javaPlugin.getConfigHandler().getDisplayViewDistance();
        this.viewDistanceSquared = viewDistance * viewDistance;
        StaticUtils.log(ChatColor.GREEN, "VisualTask initialized.");
    }

    @Override
    public void run() {
        if (shopHandler.isShopMapEmpty()) return;

        for (Player player : javaPlugin.getServer().getOnlinePlayers()) {
            World world = player.getWorld();
            Location center = player.getLocation();
            DisplayManager displayManager = javaPlugin.getShopHandler().getDisplayManager();
            UUID playerUuid = player.getUniqueId();
            Set<UUID> prevBases = prevLoadedBases.computeIfAbsent(playerUuid, k -> new HashSet<>());
            Set<UUID> currBases = workingBases.computeIfAbsent(playerUuid, k -> new HashSet<>());
            currBases.clear();
        
            // Load - nearby shop bases
            int cx = center.getBlockX() >> 4, cz = center.getBlockZ() >> 4;
            int blockRadius = Math.max(16, viewDistance);
            int chunkRadius = Math.max(1, (blockRadius >> 4) + 1);
            for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
                for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                    int x = cx + dx, z = cz + dz;
                    if (!world.isChunkLoaded(x, z)) continue;

                    for (UUID shopUuid : javaPlugin.getShopHandler().getShopsInChunk(world, x, z)) {
                        Shop shop = javaPlugin.getShopHandler().getShop(shopUuid);
                        if (shop == null || shop.getLocation() == null) continue;

                        if (shop.getLocation().distanceSquared(center) <= (viewDistanceSquared)) {
                            displayManager.ensureLoadedFor(player, shop, false);
                            currBases.add(shopUuid);
                        }
                    }
                }
            }

            // Unload - previously loaded bases that are no longer nearby
            for (UUID shopUuid : prevBases) {
                if (currBases.contains(shopUuid)) continue;

                Shop shop = javaPlugin.getShopHandler().getShop(shopUuid);
                if (shop == null || shop.getLocation() == null) continue;

                displayManager.ensureUnloadedFor(player, shop);
            }

            // Refresh - previously loaded bases
            prevBases.clear();
            prevBases.addAll(currBases);

            // Load - focused shop text
            Shop focusedShop = javaPlugin.getShopHandler().getShopInFocus(player);
            UUID focusedUuid = (focusedShop == null) ? null : focusedShop.getUuid();
            if (focusedShop != null) {
                displayManager.ensureLoadedFor(player, focusedShop, true);
            }

            // Unload - previously focused shop text
            UUID prevUuid = prevFocusedShop.get(player.getUniqueId());
            if (prevUuid!=null && (focusedUuid==null || !prevUuid.equals(focusedUuid))) {
                Shop prevShop = javaPlugin.getShopHandler().getShop(prevUuid);
                ShopDisplay prevDisplay = (prevShop == null) ? null : javaPlugin.getShopHandler().getDisplayManager().get(prevUuid);
                if (prevDisplay != null) prevDisplay.hideText(player);
            }

            // Refresh - previously focused shop text
            prevFocusedShop.put(player.getUniqueId(), focusedUuid);
        }
    }
}
