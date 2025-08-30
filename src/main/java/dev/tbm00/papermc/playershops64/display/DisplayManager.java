package dev.tbm00.papermc.playershops64.display;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.Shop;

public class DisplayManager {
    private final PlayerShops64 javaPlugin;
    private final Map<UUID, ShopDisplay> displays = new ConcurrentHashMap<>();

    public DisplayManager(PlayerShops64 javaPlugin) {
        this.plugin = plugin;
    }

    public ShopDisplay getOrCreate(UUID shopId, Shop shop) {
        if (shopId == null || shop == null) return null;
        return displays.computeIfAbsent(shopId, id -> new ShopDisplay(plugin, shop));
    }

    public ShopDisplay get(UUID shopId) {
        return displays.get(shopId);
    }

    public boolean shouldSee(UUID shopId, Player player, int viewDistance) {
        ShopDisplay d = displays.get(shopId);
        return d != null && d.shouldSee(player, viewDistance);
    }

    public void delete(UUID shopId) {
        ShopDisplay d = displays.remove(shopId);
        if (d != null) d.clear();
    }

    public void clearAll() {
        displays.values().forEach(ShopDisplay::clear);
        displays.clear();
    }

    public Map<UUID, ShopDisplay> getAll() {
        return displays;
    }
}
