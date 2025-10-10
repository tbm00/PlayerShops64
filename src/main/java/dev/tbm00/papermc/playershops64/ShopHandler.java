package dev.tbm00.papermc.playershops64;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import dev.tbm00.papermc.playershops64.data.MySQLConnection;
import dev.tbm00.papermc.playershops64.data.Shop;
import dev.tbm00.papermc.playershops64.data.ShopDAO;
import dev.tbm00.papermc.playershops64.display.DisplayManager;
import dev.tbm00.papermc.playershops64.display.ShopDisplay;
import dev.tbm00.papermc.playershops64.display.VisualTask;
import dev.tbm00.papermc.playershops64.utils.ShopUtils;
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
        for (Shop shop : dao.getAllShopsFromSql()) {
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
        Map<UUID, Shop> copy = new LinkedHashMap<>(shops.size());
        for (var e : shops.entrySet()) copy.put(e.getKey(), copyOf(e.getValue()));
        return Collections.unmodifiableMap(copy);
    }

    public Shop getShop(UUID uuid) {
        return copyOf(shops.get(uuid));
    }

    public void upsertShopObject(Shop shop) {
        if (shop == null || shop.getUuid() == null) {
            return;
        }

        // run DB operations async
        javaPlugin.getServer().getScheduler().runTaskAsynchronously(javaPlugin, () -> {
            boolean ok = dao.upsertShopToSql(shop);
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
                ShopDisplay shopDisplay = displayManager.getOrCreate(shop.getUuid());
                if (shopDisplay != null) shopDisplay.update(shop.getWorld(), ShopUtils.formatHologramText(shop));
            });
        });
    }

    public void deleteShopObject(UUID uuid) {
        if (uuid == null || uuid.equals(null)) return;

        // run DB operations async
        javaPlugin.getServer().getScheduler().runTaskAsynchronously(javaPlugin, () -> {
            boolean ok = dao.deleteShopFromSql(uuid);
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

    public void unlockShop(UUID shopUuid, UUID expectedEditor) {
        StaticUtils.log(ChatColor.WHITE, "Unlocking shop: " + shopUuid.toString());
        Shop shop = getShop(shopUuid);
        if (shop==null || shop.equals(null)) {
            StaticUtils.log(ChatColor.YELLOW, "Tried to unlock a null shop!" +
                                            "\nShop: " + shopUuid.toString());
            return;
        }

        if (shop.getCurrentEditor()==null) {
            StaticUtils.log(ChatColor.RED, "CRITICAL ERROR: A shop's currentEditor lock doesn't match expected value:" +
                                            "\nShop: " + shopUuid.toString() +
                                            "\nExpected: " + expectedEditor.toString() +
                                            "\nActual: (null)");
        } else if (!shop.getCurrentEditor().equals(expectedEditor)) {
            StaticUtils.log(ChatColor.RED, "CRITICAL ERROR: A shop's currentEditor lock doesn't match expected value:" +
                                            "\nShop: " + shopUuid.toString() +
                                            "\nExpected: " + expectedEditor.toString() +
                                            "\nActual: " + shop.getCurrentEditor().toString());
        } else {
            shop.setCurrentEditor(null);
            shops.put(shopUuid, shop);
        }
    } 

    public boolean tryLockShop(UUID shopUuid, Player player) {
        StaticUtils.log(ChatColor.WHITE, player.getName() + " trying to lock shop: " + shopUuid.toString());
        Shop shop = getShop(shopUuid);
        if (shop==null || shop.equals(null)) {
            StaticUtils.log(ChatColor.YELLOW, player.getName() + " tried to tryLock a null shop!" +
                                            "\nShop: " + shopUuid.toString());
            StaticUtils.sendMessage(player, "&cShop object not found..!");
            return false;
        }

        if (shop.getCurrentEditor() != null && !shop.getCurrentEditor().equals(player.getUniqueId())) {
            StaticUtils.sendMessage(player, "&cThis shop is being used by " + javaPlugin.getServer().getOfflinePlayer(shop.getCurrentEditor()).getName());
            return false;
        }
        
        shop.setCurrentEditor(player.getUniqueId());
        shops.put(shopUuid, shop);
        return true;
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
        UUID shopId = getIndexedShopId(world, bx, by, bz);
        Shop live = (shopId == null) ? null : shops.get(shopId);
        return copyOf(live);
    }

    private UUID getIndexedShopId(World world, int bx, int by, int bz) {
        if (world == null) return null;
        Map<Long, UUID> byPos = shopIndex.get(world.getUID());
        if (byPos == null) return null;
        return byPos.get(packBlockPos(bx, by, bz));
    }

    private long packBlockPos(int x, int y, int z) {
        // Same idea as Mojang’s BlockPos long packing
        return ((x & 0x3FFFFFFL) << 38) | ((z & 0x3FFFFFFL) << 12) | (y & 0xFFFL);
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
            s.hasInfiniteMoney(),
            s.hasInfiniteStock(),
            s.getDescription(),
            s.getDisplayHeight(),
            s.getBaseMaterial(),
            s.getCurrentEditor()
        );
    }

    /*private boolean canPlayerEditShop(UUID shopUuid, Player player) {
        if (!isShopBeingEdited(shopUuid)) return true;

        Shop shop = getShop(shopUuid);
        if (shop.getCurrentEditor().equals(player.getUniqueId())) return true;

        StaticUtils.sendMessage(player, "&cThis shop is being used by " + javaPlugin.getServer().getOfflinePlayer(shop.getCurrentEditor()).getName());
        return false;
    }*/

    /*private boolean isShopBeingEdited(UUID shopUuid) {
        Shop shop = getShop(shopUuid);
        if (shop.getCurrentEditor()!=null) return true;
        else return false;
    }*/

    /*public void setCurrentShopEditor(UUID shopUuid, Player player) {
        Shop shop = getShop(shopUuid);
        shop.setCurrentEditor(player.getUniqueId());
        shops.put(shopUuid, shop);
        upsertShopObject(shop);
    }*/

    /* public void clearCurrentShopEditor(UUID shopUuid) {
        Shop shop = getShop(shopUuid);
        shop.setCurrentEditor(null);
        shops.put(shopUuid, shop);
        upsertShopObject(shop);
    } */
}
