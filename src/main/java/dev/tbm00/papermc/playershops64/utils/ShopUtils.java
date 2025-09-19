package dev.tbm00.papermc.playershops64.utils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.Shop;

public class ShopUtils {
    private static PlayerShops64 javaPlugin;

    public static void init(PlayerShops64 javaPlugin) {
        ShopUtils.javaPlugin = javaPlugin;
    }

    // Create/modify/remove 
    public static UUID createShop(Player owner, World world, Location location) {
        UUID shopUuid = UUID.randomUUID();

        Shop shop = new Shop(shopUuid,
                            owner.getUniqueId(),
                            owner.getName(),
                            world,
                            location,
                            null,
                            1,
                            0,
                            BigDecimal.ZERO,
                            BigDecimal.valueOf(100.0),
                            BigDecimal.valueOf(50.0),
                            null,
                            false,
                            false,
                            null);

        javaPlugin.getShopHandler().upsertShop(shop);
        StaticUtils.log(ChatColor.GOLD, owner.getName() + " created a shop " + shop.getUuid() + " in " + world.getName()+ " @ " + (int)location.getX() + "," + (int)location.getY() + "," + (int)location.getZ());
        return shopUuid;
    }

    public static void sellToShop(Player player, UUID shopUuid, int quantity) {
        if (!Bukkit.isPrimaryThread()) {
            javaPlugin.getServer().getScheduler().runTask(javaPlugin, () -> sellToShop(player, shopUuid, quantity));
            StaticUtils.log(ChatColor.RED, player + " tried to sell to shop " + shopUuid + " off the main thread..!");
            return;
        }

        // Re-guard
        if (!javaPlugin.getShopHandler().canPlayerEditShop(shopUuid, player)) {
            StaticUtils.sendMessage(player, "&cThis shop is currently being used by someone else.");
            return;
        } else {
            javaPlugin.getShopHandler().setCurrentShopEditor(shopUuid, player);
        }

        Shop shop = javaPlugin.getShopHandler().getShop(shopUuid);
        if (shop == null) {
            StaticUtils.sendMessage(player, "&cShop not found.");
            return;
        }

        if (quantity <= 0) {
            StaticUtils.sendMessage(player, "&cInvalid quantity.");
            return;
        }

        ItemStack saleItem = shop.getItemStack();
        if (saleItem == null) {
            StaticUtils.sendMessage(player, "&cThis shop has no item set.");
            return;
        }

        int playerHas = StaticUtils.countMatchingItems(player, saleItem);
        if (playerHas < quantity) {
            StaticUtils.sendMessage(player, "&cYou only have &e" + playerHas + "&c of that item.");
            return;
        }

        BigDecimal per = BigDecimal.valueOf(getShopSellPriceForOne(shop));
        BigDecimal totalPrice = StaticUtils.normalizeBigDecimal(per.multiply(BigDecimal.valueOf((long)quantity)) );
        if (!shop.hasInfiniteMoney() && shop.getMoneyStock().doubleValue() < totalPrice.doubleValue()) {
            StaticUtils.sendMessage(player, "&cThe shop doesn't have enough funds to buy that right now.");
            return;
        }

        // Apply effects on main thread
        boolean removedItemsFromPlayer = StaticUtils.removeMatchingItems(player, saleItem, quantity);
        if (!removedItemsFromPlayer) {
            StaticUtils.sendMessage(player, "&cError removing " +quantity+ " " + saleItem.getItemMeta().getDisplayName() + "&r&c from your inventory..!");
            return;
        }

        boolean gaveMoneyToPlayer = javaPlugin.getVaultHook().giveMoney((OfflinePlayer) player, totalPrice.doubleValue());
        if (!gaveMoneyToPlayer) {
            // rollback items if you want strong guarantees
            StaticUtils.addToInventoryOrDrop(player, saleItem, quantity);
            StaticUtils.sendMessage(player, "&cPayment failed, nothing was sold!");
            return;
        }

        // Update shop copy, then persist
        if (!shop.hasInfiniteStock()) shop.setItemStock(shop.getItemStock() + quantity);
        if (!shop.hasInfiniteMoney()) shop.setMoneyStock(shop.getMoneyStock().subtract(totalPrice));
        shop.setLastTransactionDate(new Date());

        javaPlugin.getShopHandler().upsertShop(shop);
        StaticUtils.sendMessage(player, "&fSold " + quantity + " x " + StaticUtils.getItemName(saleItem) + " &rfor &a$" + StaticUtils.formatDoubleUS(totalPrice.doubleValue()) + "&f.");
    }

    // Getters
    public static double getShopBuyPriceForOne(Shop shop) {
        if (shop==null) return 0;
        
        return shop.getBuyPrice().doubleValue()/shop.getStackSize();
    }

    public static double getShopSellPriceForOne(Shop shop) {
        if (shop==null) return 0;
        
        return shop.getSellPrice().doubleValue()/shop.getStackSize();
    }

    // One offs
    public static void teleportPlayerToShop(Player player, Shop shop) {
        double x=shop.getLocation().getX(), y=shop.getLocation().getY(), z=shop.getLocation().getZ();
        String world=shop.getWorld().getName();

        StaticUtils.teleportPlayer(player, world, x, y+1, z);
    }

    public static String formatHologramText(Shop shop) {
        ItemStack item = shop.getItemStack();
        String name = StaticUtils.getItemName(item);
        String stackSize = (!((1<=shop.getStackSize())&&(shop.getStackSize()<=64))) ? "error" : shop.getStackSize() + "";
        String buy = (shop.getBuyPrice()==null) ? "null" : (shop.getBuyPrice().compareTo(BigDecimal.valueOf(-1.0))==0) ? "disabled" : StaticUtils.formatDoubleUS(shop.getBuyPrice().doubleValue());
        String sell = (shop.getSellPrice()==null) ? "null" : (shop.getSellPrice().compareTo(BigDecimal.valueOf(-1.0))==0) ? "disabled" : StaticUtils.formatDoubleUS(shop.getSellPrice().doubleValue());
        String stock = (shop.hasInfiniteStock()) ? "∞" : shop.getItemStock() + "";
        String balance = (shop.hasInfiniteMoney()) ? "∞" : (shop.getMoneyStock()==null) ? "null" : StaticUtils.formatDoubleUS(shop.getMoneyStock().doubleValue());
        String owner = (shop.getOwnerName()==null) ? "null" : shop.getOwnerName();
        String lore0 = "";
        try {
            if (item!=null) {
                ItemMeta meta = item.getItemMeta();
                if (meta!=null) {
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

    /**
     * Prepares an base block ItemStack for usage.
     * 
     * @param amount the amount to give
     */
    public static ItemStack prepPlayerShopItemStack(Integer amount) {
        ItemStack lectern = new ItemStack(Material.LECTERN);
        ItemMeta meta = lectern.getItemMeta();

        meta.getPersistentDataContainer().set(StaticUtils.SHOP_KEY, PersistentDataType.STRING, "true");
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aPlayerShop"));

        if (amount!=null) lectern.setItemMeta(meta);
        lectern.setAmount(amount);

        return lectern;
    }
}
