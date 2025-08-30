package dev.tbm00.spigot.playershops64;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import dev.tbm00.spigot.playershops64.data.ConfigHandler;
import dev.tbm00.spigot.playershops64.data.MySQLConnection;
import dev.tbm00.spigot.playershops64.data.Shop;
import dev.tbm00.spigot.playershops64.data.ShopDAO;
import dev.tbm00.spigot.playershops64.display.DisplayManager;
import dev.tbm00.spigot.playershops64.display.VisualTask;
import dev.tbm00.spigot.playershops64.hook.VaultHook;

public class ShopHandler {
    private final PlayerShops64 javaPlugin;
    public final ConfigHandler configHandler;
    private final ShopDAO dao;
    private final VaultHook economy;
    private final DisplayManager displayManager;
    private final Map<UUID, Shop> shops = new LinkedHashMap<>(); 

    // running task to show shop entities
    private VisualTask visualTask;

    public ShopHandler(PlayerShops64 javaPlugin, ConfigHandler configHandler, MySQLConnection db, VaultHook economy) {
        this.javaPlugin = javaPlugin;
        this.configHandler = configHandler;
        this.dao = new ShopDAO(db);
        this.economy = economy;
        this.displayManager = new DisplayManager(javaPlugin);
        this.visualTask = new VisualTask(javaPlugin, this);

        this.visualTask.runTaskTimer(javaPlugin, 20L, Math.max(1L, 10L));
    }

    public void shutdown() {
        if (visualTask != null) {
            visualTask.cancel();
            visualTask = null;
        }
        displayManager.clearAll();
    }

    public DisplayManager getDisplayManager() {
        return displayManager;
    }

    public Map<UUID, Shop> getShopMap() {
        return Collections.unmodifiableMap(shops);
    }

    public Shop getShop(UUID uuid) {
        return shops.get(uuid);
    }

    public void addOrUpdateShop(Shop shop) {
        if (shop == null || shop.getUuid() == null) {
            return;
        }
        shops.put(shop.getUuid(), shop);
    }

    public void removeShop(UUID uuid) {
        shops.remove(uuid);
        displayManager.delete(uuid);
    }

    public String formatShopText(Shop shop) {
        String item = (shop.getItemStack()==null) ? "null" : shop.getItemStack().getType().name();
        String stackSize = (!((1<=shop.getStackSize())&&(shop.getStackSize()<=64))) ? "error" : shop.getStackSize() + "";
        String buy = (shop.getBuyPrice()==null) ? "null" : (shop.getBuyPrice().compareTo(BigDecimal.valueOf(-1.0))==0) ? "disabled" : shop.getBuyPrice().toPlainString();
        String sell = (shop.getSellPrice()==null) ? "null" : (shop.getSellPrice().compareTo(BigDecimal.valueOf(-1.0))==0) ? "disabled" : shop.getSellPrice().toPlainString();
        String stock = (shop.getItemStock()==-1) ? "infinite" : shop.getItemStock() + "";
        String balance = (shop.getMoneyStock()==null) ? "null" : (shop.getMoneyStock().compareTo(BigDecimal.valueOf(-1.0))==0) ? "infinite" : shop.getMoneyStock().toPlainString();
        String owner = (shop.getOwnerName()==null) ? "null" : shop.getOwnerName();
        return item + " x " + stackSize
                    + "\n" + shop.getItemStack().getItemMeta().getLore().get(0)
                    + "\nBuy for $" + buy
                    + "\nSell for $" + sell
                    + "\nStock: " + stock + ", Balance: " + balance
                    + "\nOwner: " + owner;
    }

    public boolean isLookingAtShop(Player player, Shop shop, double maxDistance) {
        if (player == null || shop == null || shop.getLocation() == null) return false;
        if (!player.getWorld().equals(shop.getWorld())) return false;

        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();

        // Raytrace to a small AABB around the shop base block
        Location shopLoc = shop.getLocation().clone().add(0.5, 0.5, 0.5);
        double aabb = 0.6; // half-size

        RayTraceResult rtr = player.getWorld().rayTrace(eye, dir, maxDistance, FluidCollisionMode.NEVER, true, 0.1, entity -> false);

        if (rtr == null) return false;
        Location hit = rtr.getHitPosition().toLocation(player.getWorld());

        return Math.abs(hit.getX() - shopLoc.getX()) <= aabb &&
               Math.abs(hit.getY() - shopLoc.getY()) <= aabb &&
               Math.abs(hit.getZ() - shopLoc.getZ()) <= aabb;
    }
}
