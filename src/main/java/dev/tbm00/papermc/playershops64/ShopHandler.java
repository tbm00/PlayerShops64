package dev.tbm00.papermc.playershops64;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.tuple.Pair;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import dev.tbm00.papermc.playershops64.data.MySQLConnection;
import dev.tbm00.papermc.playershops64.data.ShopDAO;
import dev.tbm00.papermc.playershops64.data.structure.ShopPriceQueue;
import dev.tbm00.papermc.playershops64.data.structure.Shop;
import dev.tbm00.papermc.playershops64.display.DisplayManager;
import dev.tbm00.papermc.playershops64.display.ShopDisplay;
import dev.tbm00.papermc.playershops64.display.VisualTask;
import dev.tbm00.papermc.playershops64.utils.ShopUtils;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class ShopHandler {
    private final PlayerShops64 javaPlugin;
    private final ShopDAO dao;
    private final DisplayManager displayManager;
    private final Map<UUID, Shop> shopMap = new LinkedHashMap<>();
    private final Map<UUID, Map<Long, UUID>> shopLocationMap = new HashMap<>(); 
               // Map<WorldUID, Map<PackedBlockPos, ShopUUID>>
    private final Map<UUID, Map<Long, Set<UUID>>> shopChunkMap = new HashMap<>();
                // Map<WorldUID, Map<ChunkKey, Set<ShopUUID>>>
    private final Map<Material, ShopPriceQueue> shopMaterialPriceMap = new HashMap<>();
    private final Map<UUID, Set<UUID>> shopOwnerMap = new HashMap<>();

                // Map<PlayerUuid, Pair<left block location, right block location>>
    public final Map<UUID, Pair<Location, Location>> regionPositionMap = new ConcurrentHashMap<>();

    public final Set<UUID> activeCoupons = new HashSet<>();

    public VisualTask visualTask;

    public ShopHandler(PlayerShops64 javaPlugin, MySQLConnection db) {
        this.javaPlugin = javaPlugin;
        this.dao = new ShopDAO(db);
        
        this.displayManager = new DisplayManager(javaPlugin);
        this.visualTask = new VisualTask(javaPlugin, this);

        this.visualTask.runTaskTimer(javaPlugin, 20L, Math.max(1L, javaPlugin.getConfigHandler().getDisplayTickCycle()));
        StaticUtils.log(ChatColor.GREEN, "ShopHandler initialized.");
        loadShops();

        Bukkit.getScheduler().runTaskTimer(javaPlugin, () -> {
            for (ShopDisplay shopDisplay : displayManager.getAll().values()) {
                if (shopDisplay.isVisibleToAnyone()) {
                    shopDisplay.animate();
                }
            }
        }, 1L, 1L);
    }

    public void loadShops() {
        StaticUtils.log(ChatColor.YELLOW, "ShopHandler loading shops from DB...");
        int loaded = 0, skippedNullShop = 0, skippedNullWorld = 0, skippedNullLoc = 0;

        shopLocationMap.clear();
        shopMaterialPriceMap.clear();
        shopOwnerMap.clear();
        shopMap.clear();
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

            shopMap.put(shop.getUuid(), shop);
            indexShop(shop);
            loaded++;
        }

        StaticUtils.log(ChatColor.GREEN, "ShopHandler loaded " + loaded + " shops, " +
                "\nskippedNullShop: " + skippedNullShop +
                ", skippedNullWorld:" + skippedNullWorld +
                ", skippedNullLocation: " + skippedNullLoc);
    }

    public void validateShops() {
        StaticUtils.log(ChatColor.YELLOW, "ShopHandler validating shops...");
        int valid = 0, missingBaseBlockFixed = 0, missingBaseNoItemDeleted = 0;

        for (Map.Entry<UUID, Shop> entry : getShopView().entrySet()) {
            Shop shop = entry.getValue();
            if (!ShopUtils.hasBaseBlock(shop)) {
                if (shop.getItemStack()==null) {
                    ShopUtils.deleteShop(shop.getUuid(), null);
                    missingBaseNoItemDeleted++;
                    continue;
                } else {
                    if (shop.getBaseMaterial().equals(Material.AIR)) 
                        ShopUtils.setBaseMaterial(shop.getUuid(), Material.LECTERN);
                    else ShopUtils.setBaseMaterial(shop.getUuid(), shop.getBaseMaterial());
                    missingBaseBlockFixed++;
                }
            }

            //ShopUtils.ensureProperDisplayHeight(shop);

            valid++;
        }

        StaticUtils.log(ChatColor.GREEN, "ShopHandler validated " + valid + " shops, " +
                "\nmissingBaseBlockFixed: " + missingBaseBlockFixed +
                ", missingBaseNoItemDeleted:" + missingBaseNoItemDeleted);
    }

    public void shutdown() {
        if (visualTask != null) {
            visualTask.cancel();
            visualTask = null;
        }
        displayManager.deleteAll();
    }

    public void upsertShopObject(Shop shop) {
        if (!Bukkit.isPrimaryThread()) {
            StaticUtils.log(ChatColor.RED, "Tried to upsert shop off main thread... rescheduling on main thread!");
            javaPlugin.getServer().getScheduler().runTask(javaPlugin, () -> upsertShopObject(shop));
            return;
        }

        if (shop == null || shop.getUuid() == null) {
            return;
        }

        // on main thread first (so all internal references to it are immediately updated, rather than being delayed by async operation)
        // update memory + visuals 
        Shop prev = shopMap.put(shop.getUuid(), shop);
        if (prev != null) deindexShop(prev);
        indexShop(shop);

        // instantly refresh this shop's display
        ShopDisplay shopDisplay = displayManager.getOrCreate(shop.getUuid());
        if (shopDisplay != null) shopDisplay.update();

        // run DB operations async
        javaPlugin.getServer().getScheduler().runTaskAsynchronously(javaPlugin, () -> {
            boolean ok = dao.upsertShopToSql(shop);
            if (!ok) {
                StaticUtils.log(ChatColor.RED, "DB upsert failed for shop " + shop.getUuid() + "... Upserting previously saved shop..!");
                return;
            } else {
                StaticUtils.log(ChatColor.GREEN, "DB upsert passed for shop " + shop.getUuid());
            }
        });
    }

    public void deleteShopObject(UUID uuid) {
        if (!Bukkit.isPrimaryThread()) {
            StaticUtils.log(ChatColor.RED, "Tried to delete shop off main thread... rescheduling on main thread!");
            javaPlugin.getServer().getScheduler().runTask(javaPlugin, () -> deleteShopObject(uuid));
            return;
        }

        if (uuid == null) return;

        // update memory + visuals 
        Shop removed = shopMap.remove(uuid);
        if (removed != null) deindexShop(removed);
        displayManager.delete(uuid);

        // run DB operations async
        javaPlugin.getServer().getScheduler().runTaskAsynchronously(javaPlugin, () -> {
            boolean ok = dao.deleteShopFromSql(uuid);
            if (!ok) {
                StaticUtils.log(ChatColor.RED, "DB delete failed for shop " + uuid);
                return;
            } else {
                StaticUtils.log(ChatColor.GREEN, "DB delete passed for shop " + uuid);
            }
        });
    }

    public boolean tryLockShop(UUID shopUuid, Player player) {
        StaticUtils.log(ChatColor.WHITE, player.getName() + " trying to lock shop: " + shopUuid.toString());
        Shop shop = getShop(shopUuid);
        if (shop==null) {
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
        shopMap.put(shopUuid, shop);
        return true;
    }

    public void unlockShop(UUID shopUuid, UUID expectedEditor) {
        StaticUtils.log(ChatColor.WHITE, "Unlocking shop: " + shopUuid.toString());
        Shop shop = getShop(shopUuid);
        if (shop==null) {
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
            shopMap.put(shopUuid, shop);
        }
    }

    private void indexShop(Shop shop) {
        if (shop==null) return;

        if (shop.getOwnerUuid()!=null) {
            Set<UUID> playersShops = shopOwnerMap.computeIfAbsent(shop.getOwnerUuid(), k -> new java.util.HashSet<>());
            playersShops.add(shop.getUuid());
        }

        if (shop.getWorld()!=null && shop.getLocation()!=null) {
            UUID worldUuid = shop.getWorld().getUID();

            Map<Long, UUID> byPos = shopLocationMap.computeIfAbsent(worldUuid, k -> new HashMap<>());
            long key = packBlockPos(shop.getLocation().getBlockX(), shop.getLocation().getBlockY(), shop.getLocation().getBlockZ());

            UUID old = byPos.put(key, shop.getUuid());
            if (old != null && !old.equals(shop.getUuid())) {
                StaticUtils.log(ChatColor.RED, "Seems there are two shops at same location: " + shop.getWorld().getName() +" @ " + shop.getLocation().getX() + ", " +shop.getLocation().getY()+ ", " + shop.getLocation().getZ() + "\n" +
                                "Old shop: " + old + "\n",
                                "New shop: " + shop.getUuid());
            }

            long chunkKey = packChunkFromBlock(shop.getLocation().getBlockX(), shop.getLocation().getBlockZ());
            Map<Long, Set<UUID>> byChunk = shopChunkMap.computeIfAbsent(worldUuid, k -> new HashMap<>());
            byChunk.computeIfAbsent(chunkKey, k -> new HashSet<>()).add(shop.getUuid());
        }

        if (shop.getItemStack()!=null) {
            //if (shop.getSellPrice()==null) return;

            Material material = shop.getItemStack().getType();
            if (material==null) return;

            if (!shopMaterialPriceMap.containsKey(material) || shopMaterialPriceMap.get(material)==null) {
                shopMaterialPriceMap.put(material, new ShopPriceQueue());
            }

            ShopPriceQueue queue = shopMaterialPriceMap.get(material);
            BigDecimal sellPrice = (shop.getSellPrice()==null) ? BigDecimal.ZERO : shop.getSellPrice();

            if (queue.contains(shop.getUuid())) queue.update(shop.getUuid(), sellPrice);
            else queue.insert(shop.getUuid(), sellPrice);

            shopMaterialPriceMap.put(material, queue);
        }
    }

    private void deindexShop(Shop shop) {
        if (shop==null) return;

        if (shop.getOwnerUuid()!=null) {
            Set<UUID> playersShops = shopOwnerMap.get(shop.getOwnerUuid());
            if (playersShops != null) {
                playersShops.remove(shop.getUuid());
                if (playersShops.isEmpty()) {
                    shopOwnerMap.remove(shop.getOwnerUuid());
                }
            }
        }

        if (shop.getWorld()!=null && shop.getLocation()!=null) {
            UUID worldUuid = shop.getWorld().getUID();

            Map<Long, UUID> byPos = shopLocationMap.get(worldUuid);
            if (byPos != null) {
                long key = packBlockPos(shop.getLocation().getBlockX(), shop.getLocation().getBlockY(), shop.getLocation().getBlockZ());
                byPos.remove(key);
                if (byPos.isEmpty()) shopLocationMap.remove(worldUuid);
            }

            Map<Long, Set<UUID>> byChunk = shopChunkMap.get(worldUuid);
            if (byChunk != null) {
                long chunkKey = packChunkFromBlock(shop.getLocation().getBlockX(), shop.getLocation().getBlockZ());
                Set<UUID> s = byChunk.get(chunkKey);
                if (s != null) {
                    s.remove(shop.getUuid());
                    if (s.isEmpty()) byChunk.remove(chunkKey);
                }
                if (byChunk.isEmpty()) shopChunkMap.remove(worldUuid);
            }
        }

        if (shop.getItemStack()!=null) {
            if (shop.getSellPrice()==null) return;

            Material material = shop.getItemStack().getType();
            if (material==null) return;

            if (!shopMaterialPriceMap.containsKey(material) || shopMaterialPriceMap.get(material)==null) {
                return;
            }

            ShopPriceQueue queue = shopMaterialPriceMap.get(material);
            if (queue.contains(shop.getUuid())) {
                queue.delete(shop.getUuid());
                if (queue.isEmpty()) shopMaterialPriceMap.remove(material);
            }
        }
    }

    public DisplayManager getDisplayManager() {
        return displayManager;
    }

    public Shop getShop(UUID uuid) {
        return copyOf(shopMap.get(uuid));
    }

    public boolean isShopMapEmpty() {
        return shopMap.isEmpty();
    }

    public Map<UUID, Shop> getShopView() {
        Map<UUID, Shop> copy = new LinkedHashMap<>(shopMap.size());
        for (var e : shopMap.entrySet()) copy.put(e.getKey(), copyOf(e.getValue()));
        return Collections.unmodifiableMap(copy);
    }

    public Map<UUID, Shop> snapshotShopMap() {
        return new LinkedHashMap<>(getShopView());
    }

    public Map<Material, ShopPriceQueue> snapshotMaterialPriceMap() {
        Map<Material, ShopPriceQueue> copy = new HashMap<>(shopMaterialPriceMap.size());
        for (Entry<Material, ShopPriceQueue> e : shopMaterialPriceMap.entrySet()) {
            copy.put(e.getKey(), e.getValue() == null ? null : e.getValue().snapshot());
        }
        return copy;
    }

    public Map<Material, ShopPriceQueue> getShopMaterialPriceMap() {
        return Collections.unmodifiableMap(shopMaterialPriceMap);
    }

    public ShopPriceQueue getShopPriceQueue(Material material) {
        return shopMaterialPriceMap.containsKey(material) ? shopMaterialPriceMap.get(material) : null;
    }

    public Set<UUID> getPlayersShops(UUID playerUuid) {
        Set<UUID> s = shopOwnerMap.get(playerUuid);
        return (s == null) ? java.util.Collections.emptySet() : new HashSet<>(s);
    }

    public Set<UUID> getShopsInChunk(World world, int cx, int cz) {
        if (world == null) return java.util.Collections.emptySet();

        Map<Long, Set<UUID>> byChunk = shopChunkMap.get(world.getUID());
        if (byChunk == null) return java.util.Collections.emptySet();

        Set<UUID> s = byChunk.get(packChunk(cx, cz));
        return (s == null) ? java.util.Collections.emptySet() : new HashSet<>(s);
    }

    private Shop getIndexedShop(World world, int bx, int by, int bz) {
        UUID shopUuid = getIndexedshopUuid(world, bx, by, bz);
        Shop live = (shopUuid == null) ? null : shopMap.get(shopUuid);
        return copyOf(live);
    }

    private UUID getIndexedshopUuid(World world, int bx, int by, int bz) {
        if (world == null) return null;
        Map<Long, UUID> byPos = shopLocationMap.get(world.getUID());
        if (byPos == null) return null;
        return byPos.get(packBlockPos(bx, by, bz));
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
        /*if (shop == null) {
            StaticUtils.log(ChatColor.RED, "location not found in shopLocationMap");
        }*/
        return shop;
    }

    private long packBlockPos(int x, int y, int z) {
        // Same idea as Mojang’s BlockPos long packing
        return ((x & 0x3FFFFFFL) << 38) | ((z & 0x3FFFFFFL) << 12) | (y & 0xFFFL);
    }

    private static long packChunk(int cx, int cz) {
        return ((long) (cx) & 0xffffffffL) << 32 | ((long) cz & 0xffffffffL);
    }

    private static long packChunkFromBlock(int bx, int bz) {
        return packChunk(bx >> 4, bz >> 4);
    }

    private Shop copyOf(Shop s) {
        if (s == null) return null;

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
            s.getAssistants(),
            s.getCurrentEditor()
        );
    }
}
