package dev.tbm00.papermc.playershops64.display;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.structure.Shop;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class DisplayManager {
    private final PlayerShops64 javaPlugin;
    private final Map<UUID, ShopDisplay> displays = new ConcurrentHashMap<>();

    public DisplayManager(PlayerShops64 javaPlugin) {
        this.javaPlugin = javaPlugin;
        StaticUtils.log(ChatColor.GREEN, "DisplayManager initialized.");
    }

    public ShopDisplay upsertDisplay(Shop shop) {
        ShopDisplay shopDisplay = getOrCreate(shop.getUuid());
        if (shopDisplay!=null) shopDisplay.update();
        return shopDisplay;
    }

    public ShopDisplay getOrCreate(UUID shopUuid) {
        if (shopUuid == null) return null;
        return displays.computeIfAbsent(shopUuid, id -> new ShopDisplay(javaPlugin, shopUuid));
    }

    public ShopDisplay get(UUID shopUuid) {
        return displays.get(shopUuid);
    }

    public Map<UUID, ShopDisplay> getAll() {
        return displays;
    }

    public void delete(UUID shopUuid) {
        ShopDisplay shopDisplay = displays.remove(shopUuid);
        if (shopDisplay != null) shopDisplay.clear();
    }

    public void deleteAll() {
        displays.values().forEach(ShopDisplay::clear);
        displays.clear();
    }

    public void ensureLoadedFor(Player player, Shop shop, boolean focused) {
        if (player == null || shop == null) return;
        if (!player.getWorld().equals(shop.getWorld())) return;
        if (!shop.getWorld().isChunkLoaded(shop.getLocation().getBlockX() >> 4, shop.getLocation().getBlockZ() >> 4)) return;

        ShopDisplay shopDisplay = getOrCreate(shop.getUuid());
        shopDisplay.show(player, focused);
    }

    public void ensureUnloadedFor(Player player, Shop shop) {
        if (player == null || shop == null) return;

        ShopDisplay shopDisplay = get(shop.getUuid());
        if (shopDisplay != null) shopDisplay.hide(player, true);
    }

    public void invalidateDisplay(UUID shopUuid) {
        ShopDisplay d = displays.get(shopUuid);
        if (d != null) d.clear();
    }

    public void invalidateDisplays(Set<UUID> shopUuids) {
        if (shopUuids == null) return;
        for (UUID shopUuid : shopUuids) invalidateDisplay(shopUuid);
    }

    public void purgeViewer(UUID playerUuid) {
        for (ShopDisplay sd : displays.values()) sd.purgeViewer(playerUuid);
    }
}
