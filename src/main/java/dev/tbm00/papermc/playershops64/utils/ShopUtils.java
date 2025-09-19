package dev.tbm00.papermc.playershops64.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
            StaticUtils.log(ChatColor.RED, player + " tried to sell to shop " + shopUuid + " off the main thread -- trying again during next tick on main thread!");
            javaPlugin.getServer().getScheduler().runTask(javaPlugin, () -> sellToShop(player, shopUuid, quantity));
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
            StaticUtils.sendMessage(player, "&cShop not found..!");
            javaPlugin.getShopHandler().clearCurrentShopEditor(shopUuid);
            return;
        }

        if (quantity <= 0) {
            StaticUtils.sendMessage(player, "&cInvalid quantity.");
            javaPlugin.getShopHandler().clearCurrentShopEditor(shopUuid);
            return;
        }

        int w_quantity = quantity;

        ItemStack saleItem = shop.getItemStack();
        if (saleItem == null || saleItem.getType().isAir()) {
            StaticUtils.sendMessage(player, "&cThis shop has no item set.");
            javaPlugin.getShopHandler().clearCurrentShopEditor(shopUuid);
            return;
        }

        int playerHas = StaticUtils.countMatchingItems(player, saleItem);
        if (playerHas < quantity) {
            /*StaticUtils.sendMessage(player, "&cYou don't have enough items -- you only have " + playerHas + "!");
            javaPlugin.getShopHandler().clearCurrentShopEditor(shopUuid);
            return;*/
            w_quantity = playerHas;
        } if (playerHas <= 0) {
            StaticUtils.sendMessage(player, "&cYou don't have any matching items to sell!");
        }

        if (shop.getSellPrice()==null || shop.getSellPrice().compareTo(BigDecimal.ZERO)<0) {
            StaticUtils.sendMessage(player, "&cThis shop is not buying (sell-to is disabled)!");
            javaPlugin.getShopHandler().clearCurrentShopEditor(shopUuid);
            return;
        }

        BigDecimal perItem = getShopSellPriceForOne(shop);
        if (perItem == null || perItem.compareTo(BigDecimal.ZERO) <= 0) {
            StaticUtils.sendMessage(player, "&cInternal error: invalid per item sell price..!");
            javaPlugin.getShopHandler().clearCurrentShopEditor(shopUuid);
            return;
        }

        BigDecimal totalPrice = StaticUtils.normalizeBigDecimal( perItem.multiply(BigDecimal.valueOf(w_quantity)) );
        if (!shop.hasInfiniteMoney()) {
            BigDecimal moneyStock = shop.getMoneyStock() == null ? BigDecimal.ZERO : shop.getMoneyStock();
            if (moneyStock.compareTo(totalPrice) < 0) {
                // Compute max affordable quantity with floor division
                int shopCanAfford = moneyStock.divide(perItem, 0, RoundingMode.DOWN).intValue();
                if (shopCanAfford<=0) {
                    StaticUtils.sendMessage(player, "&cThe shop can't afford to buy any right now.");
                    javaPlugin.getShopHandler().clearCurrentShopEditor(shopUuid);
                    return;
                }

                w_quantity = shopCanAfford;
                totalPrice = StaticUtils.normalizeBigDecimal( perItem.multiply(BigDecimal.valueOf(w_quantity)) );
                /*StaticUtils.sendMessage(player, "&cThe shop doesn't have enough funds to buy that right now.");
                javaPlugin.getShopHandler().clearCurrentShopEditor(shopUuid);
                return;*/
            }
        }

        boolean removedItemsFromPlayer = StaticUtils.removeMatchingItems(player, saleItem, w_quantity);
        if (!removedItemsFromPlayer) {
            StaticUtils.sendMessage(player, "&cError removing " +w_quantity+ " x " + StaticUtils.getItemName(saleItem) + "&r&c from your inventory..!");
            javaPlugin.getShopHandler().clearCurrentShopEditor(shopUuid);
            return;
        }

        boolean gaveMoneyToPlayer = (javaPlugin.getVaultHook()==null) ? false: javaPlugin.getVaultHook().giveMoney((OfflinePlayer) player, totalPrice.doubleValue());
        if (!gaveMoneyToPlayer) {
            StaticUtils.addToInventoryOrDrop(player, saleItem, w_quantity);
            StaticUtils.sendMessage(player, "&cPayment failed, nothing was sold!");
            javaPlugin.getShopHandler().clearCurrentShopEditor(shopUuid);
            return;
        }

        if (!shop.hasInfiniteStock()) shop.setItemStock(shop.getItemStock() + w_quantity);
        if (!shop.hasInfiniteMoney()) shop.setMoneyStock(shop.getMoneyStock().subtract(totalPrice));
        shop.setLastTransactionDate(new Date());

        javaPlugin.getShopHandler().upsertShop(shop);
        javaPlugin.getShopHandler().clearCurrentShopEditor(shopUuid);
        StaticUtils.sendMessage(player, "&fSold " + w_quantity + " x " + StaticUtils.getItemName(saleItem) + "&r for &a$" + StaticUtils.formatDoubleUS(totalPrice.doubleValue()) + "&f.");
    }

    // Getters
    public static BigDecimal getShopBuyPriceForOne(Shop shop) {
        if (shop==null) return null;
        if (shop.getBuyPrice()==null) return null;
        
        return shop.getBuyPrice().divide(BigDecimal.valueOf(shop.getStackSize()), 2, RoundingMode.DOWN);
    }

    public static BigDecimal getShopSellPriceForOne(Shop shop) {
        if (shop==null) return null;
        if (shop.getSellPrice()==null) return null;
        
        return shop.getSellPrice().divide(BigDecimal.valueOf(shop.getStackSize()), 2, RoundingMode.DOWN);
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
        String buy = (shop.getBuyPrice()==null) ? "disabled" : StaticUtils.formatDoubleUS(shop.getBuyPrice().doubleValue());
        String sell = (shop.getSellPrice()==null) ? "disabled" : StaticUtils.formatDoubleUS(shop.getSellPrice().doubleValue());
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
