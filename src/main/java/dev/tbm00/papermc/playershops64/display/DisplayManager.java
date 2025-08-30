package dev.tbm00.papermc.playershops64.display;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.Shop;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class DisplayManager {
    private final PlayerShops64 javaPlugin;
    private final Map<UUID, ShopDisplay> displays = new ConcurrentHashMap<>();

    public DisplayManager(PlayerShops64 javaPlugin) {
        StaticUtils.log(ChatColor.YELLOW, "DisplayManager starting initialization.");
        this.javaPlugin = javaPlugin;
        StaticUtils.log(ChatColor.YELLOW, "DisplayManager class initialized.");
    }

    public ShopDisplay getOrCreate(UUID shopId, Shop shop) {
        if (shopId == null || shop == null) return null;
        return displays.computeIfAbsent(shopId, id -> new ShopDisplay(javaPlugin, shop));
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
