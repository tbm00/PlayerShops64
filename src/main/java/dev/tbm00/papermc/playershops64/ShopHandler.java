package dev.tbm00.papermc.playershops64;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
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
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class ShopHandler {
    private final PlayerShops64 javaPlugin;
    private final ShopDAO dao;
    private final DisplayManager displayManager;
    private final Map<UUID, Shop> shops = new LinkedHashMap<>();
    private final Map<UUID, Map<Long, UUID>> shopIndex = new HashMap<>(); 
               // Map<WorldUID, Map<PackedBlockPos, ShopUUID>>

    private VisualTask visualTask;

    public ShopHandler(PlayerShops64 javaPlugin, MySQLConnection db) {
        this.javaPlugin = javaPlugin;
        this.dao = new ShopDAO(db);
        
        this.displayManager = new DisplayManager(javaPlugin);
        this.visualTask = new VisualTask(javaPlugin, this);

        this.visualTask.runTaskTimer(javaPlugin, 20L, Math.max(1L, javaPlugin.getConfigHandler().getDisplayTickCycle()));
        StaticUtils.log(ChatColor.GREEN, "ShopHandler initialized.");
        loadShops();
    }

    public void loadShops() {
        StaticUtils.log(ChatColor.YELLOW, "ShopHandler loading shops from DB...");
        int loaded = 0, skippedNullShop = 0, skippedNullWorld = 0, skippedNullLoc = 0;

        shopIndex.clear();
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
            indexShop(shop);
            loaded++;
        }


        StaticUtils.log(ChatColor.GREEN, "ShopHandler loaded " + loaded + " shops, " +
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

    public Map<UUID, Shop> getShopView() {
        return Collections.unmodifiableMap(shops);
    }

    public Shop getShop(UUID uuid) {
        return copyOf(shops.get(uuid));
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
                Shop prev = shops.put(shop.getUuid(), shop);
                if (prev != null) deindexShop(prev);
                indexShop(shop);

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
                Shop removed = shops.remove(uuid);
                if (removed != null) deindexShop(removed);
                displayManager.delete(uuid);
            });
        });
    }

    public String formatShopText(Shop shop) {
        ItemStack item = shop.getItemStack();
        String name = (item==null) ? "null" : item.getType().name();
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
                if (meta!=null) {
                    if (meta.hasDisplayName()) name = meta.getDisplayName();
                    if (name.isBlank()) {
                        name = meta.getItemName();
                        if (name.isBlank()) {
                            name = WordUtils.capitalizeFully(item.getType().name().toLowerCase().replace("_", " "));
                        }
                    }

                    if (meta.hasLore() && meta.getLore()!=null && !meta.getLore().isEmpty()) {
                        lore0 = String.valueOf(meta.getLore().get(0));
                    }
                }
            }
        } catch (Exception e) {e.printStackTrace();}
        if (lore0.isBlank()) {
            return name + " &7x &f" + stackSize
                        + "\n&7Buy for &a$" + buy
                        + "\n&7Sell for &c$" + sell
                        + "\n&7Stock: &e" + stock + "&7, Balance: &e$" + balance
                        + "\n&7Owner: &e" + owner;
        } else {
            return name + " &7x &f" + stackSize
                        + "\n" + lore0
                        + "\n&7Buy for &a$" + buy
                        + "\n&7Sell for &c$" + sell
                        + "\n&7Stock: &e" + stock + "&7, Balance: &e$" + balance
                        + "\n&7Owner: &e" + owner;
        }
    }

    public Shop getShopInFocus(Player player) {
        if (player == null) return null;
        final double maxDistance = (double) javaPlugin.getConfigHandler().getDisplayFocusDistance();

        // 1) Fast path: target block
        Block target = player.getTargetBlockExact((int) Math.ceil(maxDistance), FluidCollisionMode.NEVER);
        if (target != null) {
            return getIndexedShop(target.getWorld(), target.getX(), target.getY(), target.getZ());
        }

        // 2) Fallback: a single ray trace, still O(1) thanks to the index
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        RayTraceResult result = player.getWorld().rayTrace(
                eye, direction, maxDistance,
                FluidCollisionMode.NEVER,
                true,      // ignorePassableBlocks
                0.1,       // ray size
                entity -> false // ignore entities
        );

        if (result == null) return null;

        Block hitBlock = result.getHitBlock();
        return getShopAtBlock(hitBlock != null ? hitBlock.getLocation()
                                               : result.getHitPosition().toLocation(player.getWorld()));
    }

    public boolean isLookingAtShop(Player player, Shop shop, double maxDistance) {
        if (player == null || shop == null || shop.getLocation() == null || shop.getWorld() == null) return false;
        if (!player.getWorld().equals(shop.getWorld())) return false;

        Shop focused = getShopInFocus(player);
        return focused != null && focused.getUuid().equals(shop.getUuid());
    }

    public boolean hasShopAtBlock(Location location) {
        if (location == null || location.getWorld() == null) return false;
        return getIndexedShop(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ()) != null;
    }

    public Shop getShopAtBlock(Location location) {
        if (location == null || location.getWorld() == null) {
            StaticUtils.log(ChatColor.RED, "location==null or world==null");
            return null;
        }
        Shop shop = getIndexedShop(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        if (shop == null) {
            StaticUtils.log(ChatColor.RED, "location not found in shopIndex");
        }
        return shop;
    }

    private static long packBlockPos(int x, int y, int z) {
        // Same idea as Mojang’s BlockPos long packing
        return ((x & 0x3FFFFFFL) << 38) | ((z & 0x3FFFFFFL) << 12) | (y & 0xFFFL);
    }

    private void indexShop(Shop shop) {
        if (shop == null || shop.getWorld() == null || shop.getLocation() == null) return;
        UUID worldId = shop.getWorld().getUID();
        Map<Long, UUID> byPos = shopIndex.computeIfAbsent(worldId, k -> new HashMap<>());
        long key = packBlockPos(shop.getLocation().getBlockX(), shop.getLocation().getBlockY(), shop.getLocation().getBlockZ());

        UUID old = byPos.put(key, shop.getUuid());
        if (old != null && !old.equals(shop.getUuid())) {
            StaticUtils.log(ChatColor.RED, "Seems there are two shops at same location: " + shop.getWorld().getName() +" @ " + shop.getLocation().getX() + ", " +shop.getLocation().getY()+ ", " + shop.getLocation().getZ() + "\n" +
                            "Old shop: " + old + "\n",
                            "New shop: " + shop.getUuid());
        }
    }

    private void deindexShop(Shop shop) {
        if (shop == null || shop.getWorld() == null || shop.getLocation() == null) return;
        UUID worldId = shop.getWorld().getUID();
        Map<Long, UUID> byPos = shopIndex.get(worldId);
        if (byPos == null) return;

        long key = packBlockPos(shop.getLocation().getBlockX(), shop.getLocation().getBlockY(), shop.getLocation().getBlockZ());
        byPos.remove(key);
        if (byPos.isEmpty()) shopIndex.remove(worldId);
    }

    private Shop getIndexedShop(World world, int bx, int by, int bz) {
        UUID id = getIndexedShopUuid(world, bx, by, bz);
        Shop live = (id == null) ? null : shops.get(id);
        return copyOf(live);
    }

    private UUID getIndexedShopUuid(World world, int bx, int by, int bz) {
        if (world == null) return null;
        Map<Long, UUID> byPos = shopIndex.get(world.getUID());
        if (byPos == null) return null;
        return byPos.get(packBlockPos(bx, by, bz));
    }

    private Shop copyOf(Shop s) {
        if (s == null) return null;

        // Clone mutable fields (Location, ItemStack, Date); keep immutable/shared-safe ones (UUID, BigDecimal, World).
        return new Shop(
            s.getUuid(),
            s.getOwnerUuid(),
            s.getOwnerName(),
            s.getWorld(),
            s.getLocation() == null ? null : s.getLocation().clone(),
            s.getItemStack() == null ? null : s.getItemStack().clone(),
            s.getStackSize(),
            s.getItemStock(),
            s.getMoneyStock(),
            s.getBuyPrice(),
            s.getSellPrice(),
            s.getLastTransactionDate() == null ? null : new Date(s.getLastTransactionDate().getTime()),
            s.getInfiniteMoney(),
            s.getInfiniteStock()
        );
    }
}
