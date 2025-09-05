package dev.tbm00.papermc.playershops64;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import dev.tbm00.papermc.playershops64.data.MySQLConnection;
import dev.tbm00.papermc.playershops64.data.Shop;
import dev.tbm00.papermc.playershops64.data.ShopDAO;
import dev.tbm00.papermc.playershops64.display.DisplayManager;
import dev.tbm00.papermc.playershops64.display.ShopDisplay;
import dev.tbm00.papermc.playershops64.display.VisualTask;
import dev.tbm00.papermc.playershops64.hook.VaultHook;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class ShopHandler {
    private final PlayerShops64 javaPlugin;
    private final ShopDAO dao;
    private final VaultHook economy;
    private final DisplayManager displayManager;
    private final Map<UUID, Shop> shops = new LinkedHashMap<>(); 

    private VisualTask visualTask;

    public ShopHandler(PlayerShops64 javaPlugin, MySQLConnection db, VaultHook economy) {
        StaticUtils.log(ChatColor.YELLOW, "ShopHandler starting initialization...");
        this.javaPlugin = javaPlugin;
        this.dao = new ShopDAO(db);
        this.economy = economy;
        
        this.displayManager = new DisplayManager(javaPlugin);
        this.visualTask = new VisualTask(javaPlugin, this);

        this.visualTask.runTaskTimer(javaPlugin, 20L, Math.max(1L, javaPlugin.configHandler.getDisplayTickCycle()));
        StaticUtils.log(ChatColor.YELLOW, "ShopHandler class initialized.");
        loadShops();
    }

    public void loadShops() {
        StaticUtils.log(ChatColor.YELLOW, "ShopHandler loading shops from DB...");
        int loaded = 0, skippedNullShop = 0, skippedNullWorld = 0, skippedNullLoc = 0;

        for (Shop shop : dao.getAllShops()) {
            if (shop == null) {
                skippedNullShop++;
                StaticUtils.log(ChatColor.RED, "Skipping null shop");
                continue;
            }

            if (shop.getWorld() == null) {
                skippedNullWorld++;
                StaticUtils.log(ChatColor.RED, "Skipping " + shop.getOwnerName() + "'s shop: " + shop.getUuid() + ", world is not loaded");
                continue;
            }
            if (shop.getLocation() == null) {
                skippedNullLoc++;
                StaticUtils.log(ChatColor.RED, "Skipping " + shop.getOwnerName() + "'s shop: " + shop.getUuid() + ", location is null");
                continue;
            }

            shops.put(shop.getUuid(), shop);
            loaded++;
        }


        StaticUtils.log(ChatColor.GREEN, "Loaded " + loaded + " shops, " +
                "\nskippedNullShop: " + skippedNullShop +
                ", skippedNullWorld:" + skippedNullWorld +
                ", skippedNullLocation: " + skippedNullLoc);
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

    public void upsertShop(Shop shop) {
        if (shop == null || shop.getUuid() == null) {
            return;
        }

        // run DB operations async
        javaPlugin.getServer().getScheduler().runTaskAsynchronously(javaPlugin, () -> {
            boolean ok = dao.upsertShop(shop);
            if (!ok) {
                StaticUtils.log(ChatColor.RED, "DB upsert failed for shop " + shop.getUuid());
                return;
            } else {
                StaticUtils.log(ChatColor.GREEN, "DB upsert passed for shop " + shop.getUuid());
            }

            // go back to main for memory + visuals
            javaPlugin.getServer().getScheduler().runTask(javaPlugin, () -> {
                shops.put(shop.getUuid(), shop);

                // instantly refresh this shop's display
                ShopDisplay shopDisplay = displayManager.getOrCreate(shop.getUuid(), shop);
                if (shopDisplay != null) shopDisplay.update(shop.getWorld(), formatShopText(shop));
            });
        });
    }

    public void removeShop(UUID uuid) {
        // run DB operations async
        javaPlugin.getServer().getScheduler().runTaskAsynchronously(javaPlugin, () -> {
            boolean ok = dao.deleteShop(uuid);
            if (!ok) {
                StaticUtils.log(ChatColor.RED, "DB delete failed for shop " + uuid);
                return;
            }

            // go back to main for memory + visuals
            javaPlugin.getServer().getScheduler().runTask(javaPlugin, () -> {
                shops.remove(uuid);
                displayManager.delete(uuid);
            });
        });
    }

    public String formatShopText(Shop shop) {
        ItemStack item = shop.getItemStack();
        String itemname = (item==null) ? "null" : item.getType().name();
        String stackSize = (!((1<=shop.getStackSize())&&(shop.getStackSize()<=64))) ? "error" : shop.getStackSize() + "";
        String buy = (shop.getBuyPrice()==null) ? "null" : (shop.getBuyPrice().compareTo(BigDecimal.valueOf(-1.0))==0) ? "disabled" : shop.getBuyPrice().toPlainString();
        String sell = (shop.getSellPrice()==null) ? "null" : (shop.getSellPrice().compareTo(BigDecimal.valueOf(-1.0))==0) ? "disabled" : shop.getSellPrice().toPlainString();
        String stock = (shop.getItemStock()==-1) ? "infinite" : shop.getItemStock() + "";
        String balance = (shop.getMoneyStock()==null) ? "null" : (shop.getMoneyStock().compareTo(BigDecimal.valueOf(-1.0))==0) ? "infinite" : shop.getMoneyStock().toPlainString();
        String owner = (shop.getOwnerName()==null) ? "null" : shop.getOwnerName();
        String lore0 = "";
        try {
            if (item!=null) {
                ItemMeta meta = item.getItemMeta();
                if (meta.hasLore() && meta.getLore()!=null && !meta.getLore().isEmpty()) {
                    lore0 = String.valueOf(meta.getLore().get(0));
                }
            }
        } catch (Exception e) {e.printStackTrace();}
        if (lore0.isBlank()) {
            return itemname + " &7x &f" + stackSize
                        + "\n&7Buy for &a$" + buy
                        + "\n&7Sell for &c$" + sell
                        + "\n&7Stock: &e" + stock + "&7, Balance: &e$" + balance
                        + "\n&7Owner: &e" + owner;
        } else {
            return itemname + " &7x &f" + stackSize
                        + "\n" + lore0
                        + "\n&7Buy for &a$" + buy
                        + "\n&7Sell for &c$" + sell
                        + "\n&7Stock: &e" + stock + "&7, Balance: &e$" + balance
                        + "\n&7Owner: &e" + owner;
        }
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

    public boolean hasShopAtBlock(Location blockLocation) {
        if (blockLocation == null) return false;
        World world = blockLocation.getWorld();
        int bx = blockLocation.getBlockX(), by = blockLocation.getBlockY(), bz = blockLocation.getBlockZ();
        for (Shop shop : shops.values()) {
            if (shop.getWorld() == null || shop.getLocation() == null) continue;
            if (!shop.getWorld().equals(world)) continue;
            Location shopLocation = shop.getLocation();
            if (shopLocation.getBlockX() == bx && shopLocation.getBlockY() == by && shopLocation.getBlockZ() == bz) return true;
        }
        return false;
    }
}
