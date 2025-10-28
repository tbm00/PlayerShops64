package dev.tbm00.papermc.playershops64.display;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.ChatColor;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class DisplayManager {
    private final PlayerShops64 javaPlugin;
    private final Map<UUID, ShopDisplay> displays = new ConcurrentHashMap<>();

    public DisplayManager(PlayerShops64 javaPlugin) {
        this.javaPlugin = javaPlugin;
        StaticUtils.log(ChatColor.GREEN, "DisplayManager initialized.");
    }

    public ShopDisplay getOrCreate(UUID shopUuid) {
        if (shopUuid == null) return null;
        return displays.computeIfAbsent(shopUuid, id -> new ShopDisplay(javaPlugin, shopUuid));
    }

    public ShopDisplay get(UUID shopUuid) {
        return displays.get(shopUuid);
    }

    public void delete(UUID shopUuid) {
        ShopDisplay shopDisplay = displays.remove(shopUuid);
        if (shopDisplay != null) shopDisplay.clear();
    }

    public void clearAll() {
        displays.values().forEach(ShopDisplay::clear);
        displays.clear();
    }

    public Map<UUID, ShopDisplay> getAll() {
        return displays;
    }
}
