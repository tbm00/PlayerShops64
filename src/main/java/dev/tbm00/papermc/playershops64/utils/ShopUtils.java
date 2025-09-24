package dev.tbm00.papermc.playershops64.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
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
                            null,
                            0,
                            Material.LECTERN);

        javaPlugin.getShopHandler().upsertShop(shop);
        StaticUtils.log(ChatColor.GOLD, owner.getName() + " created a shop " + shop.getUuid() + " in " + world.getName()+ " @ " + (int)location.getX() + "," + (int)location.getY() + "," + (int)location.getZ());
        return shopUuid;
    }

    public static void sellToShop(Player player, UUID shopUuid, int quantity) {
        if (!Bukkit.isPrimaryThread()) {
            StaticUtils.log(ChatColor.RED, player.getName() + " tried to sell to shop " + shopUuid + " off the main thread -- trying again during next tick on main thread!");
            javaPlugin.getServer().getScheduler().runTask(javaPlugin, () -> sellToShop(player, shopUuid, quantity));
            return;
        }

        // Re-guard
        if (!javaPlugin.getShopHandler().tryLockShop(shopUuid, player)) {
            StaticUtils.sendMessage(player, "&cThis shop is currently being used by someone else.");
            return;
        }

        try {
            Shop shop = javaPlugin.getShopHandler().getShop(shopUuid);
            if (shop == null) {
                StaticUtils.sendMessage(player, "&cShop not found..!");
                return;
            }

            if (quantity <= 0) {
                StaticUtils.sendMessage(player, "&cInvalid quantity.");
                return;
            } int workingQuantity = quantity;

            ItemStack saleItem = shop.getItemStack();
            if (saleItem == null || saleItem.getType().isAir()) {
                StaticUtils.sendMessage(player, "&cThis shop has no item set.");
                return;
            }

            int playerHas = StaticUtils.countMatchingItems(player, saleItem);
            if (playerHas < quantity) {
                workingQuantity = playerHas;
            } if (playerHas <= 0) {
                StaticUtils.sendMessage(player, "&cYou don't have any matching items to sell!");
                return;
            }

            if (shop.getSellPrice()==null || shop.getSellPrice().compareTo(BigDecimal.ZERO)<0) {
                StaticUtils.sendMessage(player, "&cThis shop is not buying (sell-to is disabled)!");
                return;
            }

            BigDecimal perItem = getShopSellPriceForOne(shop);
            if (perItem == null || perItem.compareTo(BigDecimal.ZERO) <= 0) {
                StaticUtils.sendMessage(player, "&cInternal error: invalid per item sell price..!");
                return;
            }

            BigDecimal totalPrice = StaticUtils.normalizeBigDecimal( perItem.multiply(BigDecimal.valueOf(workingQuantity)) );
            if (!shop.hasInfiniteMoney()) {
                BigDecimal moneyStock = shop.getMoneyStock() == null ? BigDecimal.ZERO : shop.getMoneyStock();
                if (moneyStock.compareTo(totalPrice) < 0) {
                    // get max afforded
                    int shopCanAfford = moneyStock.divide(perItem, 0, RoundingMode.DOWN).intValue();
                    if (shopCanAfford<=0) {
                        StaticUtils.sendMessage(player, "&cThe shop can't afford to buy any right now.");
                        return;
                    }

                    workingQuantity = Math.min(shopCanAfford, playerHas);
                    totalPrice = StaticUtils.normalizeBigDecimal( perItem.multiply(BigDecimal.valueOf(workingQuantity)) );
                }
            }

            if (workingQuantity <= 0) {
                StaticUtils.sendMessage(player, "&cNothing to sell..!");
                return;
            } totalPrice = totalPrice.max(BigDecimal.ZERO);

            boolean gaveMoneyToPlayer = javaPlugin.getVaultHook().giveMoney( player, totalPrice.doubleValue());
            if (!gaveMoneyToPlayer) {
                StaticUtils.sendMessage(player, "&cPayment failed, nothing was sold!");
                return;
            }

            boolean removedItemsFromPlayer = StaticUtils.removeMatchingItems(player, saleItem, workingQuantity);
            if (!removedItemsFromPlayer) {
                if (!javaPlugin.getVaultHook().removeMoney( player, totalPrice.doubleValue()))
                    {StaticUtils.log(ChatColor.RED, "CRITICAL ERROR: removeMoney("+player.getName()+", "+totalPrice.doubleValue()+") failed -- money was duped to player!");}
                StaticUtils.sendMessage(player, "&cError removing " +workingQuantity+ " x " + StaticUtils.getItemName(saleItem) + "&r&c from your inventory..!");
                return;
            }

            // update shop
            if (!shop.hasInfiniteStock()) shop.setItemStock(shop.getItemStock() + workingQuantity);
            if (!shop.hasInfiniteMoney()) shop.setMoneyStock((shop.getMoneyStock()==null) ? BigDecimal.ZERO: shop.getMoneyStock().subtract(totalPrice));
            shop.setLastTransactionDate(new Date());
            javaPlugin.getShopHandler().upsertShop(shop);

            StaticUtils.sendMessage(player, "&fSold " + workingQuantity + " x " + StaticUtils.getItemName(saleItem) + "&r for &a$" + StaticUtils.formatDoubleUS(totalPrice.doubleValue()) + "&f.");
        } finally {
            javaPlugin.getShopHandler().unlockShop(shopUuid, player.getUniqueId());
        }
    
    }

    public static void buyFromShop(Player player, UUID shopUuid, int quantity) {
        if (!Bukkit.isPrimaryThread()) {
            StaticUtils.log(ChatColor.RED, player.getName() + " tried to buy from shop " + shopUuid + " off the main thread -- trying again during next tick on main thread!");
            javaPlugin.getServer().getScheduler().runTask(javaPlugin, () -> buyFromShop(player, shopUuid, quantity));
            return;
        }

        // Re-guard
        if (!javaPlugin.getShopHandler().tryLockShop(shopUuid, player)) {
            StaticUtils.sendMessage(player, "&cThis shop is currently being used by someone else.");
            return;
        }
        
        try {
            Shop shop = javaPlugin.getShopHandler().getShop(shopUuid);
            if (shop == null) {
                StaticUtils.sendMessage(player, "&cShop not found..!");
                return;
            }

            if (quantity <= 0) {
                StaticUtils.sendMessage(player, "&cInvalid quantity.");
                return;
            } int workingQuantity = quantity;

            ItemStack saleItem = shop.getItemStack();
            if (saleItem == null || saleItem.getType().isAir()) {
                StaticUtils.sendMessage(player, "&cThis shop has no item set.");
                return;
            }

            int shopHas = shop.getItemStock();
            if (!shop.hasInfiniteStock()) {
                if (shopHas < quantity) {
                    workingQuantity = shopHas;
                } if (shopHas <= 0) {
                    StaticUtils.sendMessage(player, "&cThis shop doesn't have any stock to sell!");
                    return;
                }
            }

            if (shop.getBuyPrice()==null || shop.getBuyPrice().compareTo(BigDecimal.ZERO)<0) {
                StaticUtils.sendMessage(player, "&cThis shop is not selling (buy-from is disabled)!");
                return;
            }

            BigDecimal perItem = getShopBuyPriceForOne(shop);
            if (perItem == null || perItem.compareTo(BigDecimal.ZERO) <= 0) {
                StaticUtils.sendMessage(player, "&cInternal error: invalid per item buy price..!");
                return;
            }

            BigDecimal totalPrice = StaticUtils.normalizeBigDecimal( perItem.multiply(BigDecimal.valueOf(workingQuantity)) );
            BigDecimal balance = BigDecimal.valueOf(javaPlugin.getVaultHook().getBalance(player));
            if (balance.compareTo(totalPrice) < 0) {
                // get max afforded
                int playerCanAfford = balance.divide(perItem, 0, RoundingMode.DOWN).intValue();
                if (playerCanAfford<=0) {
                    StaticUtils.sendMessage(player, "&cYou can't afford to buy any right now!");
                    return;
                }

                if (shop.hasInfiniteStock()) workingQuantity = playerCanAfford;
                else workingQuantity = Math.min(playerCanAfford, shopHas);
                totalPrice = StaticUtils.normalizeBigDecimal( perItem.multiply(BigDecimal.valueOf(workingQuantity)) );
            }

            if (workingQuantity <= 0) {
                StaticUtils.sendMessage(player, "&cNothing to buy..!");
                return;
            } totalPrice = totalPrice.max(BigDecimal.ZERO);

            boolean tookMoneyFromPlayer = javaPlugin.getVaultHook().removeMoney( player, totalPrice.doubleValue());
            if (!tookMoneyFromPlayer) {
                StaticUtils.sendMessage(player, "&cPayment failed, nothing was bought!");
                return;
            }

            boolean addedItemsToPlayer = StaticUtils.addToInventoryOrDrop(player, saleItem, workingQuantity);
            if (!addedItemsToPlayer) {
                if (!javaPlugin.getVaultHook().giveMoney(player, totalPrice.doubleValue()))
                    {StaticUtils.log(ChatColor.RED, "CRITICAL ERROR: giveMoney("+player.getName()+", "+totalPrice.doubleValue()+") failed -- money was nuked from player!");}
                StaticUtils.sendMessage(player, "&cError adding " +workingQuantity+ " x " + StaticUtils.getItemName(saleItem) + "&r&c to your inventory..!");
                return;
            }

            // update shop
            if (!shop.hasInfiniteStock()) shop.setItemStock(shop.getItemStock() - workingQuantity);
            if (!shop.hasInfiniteMoney()) shop.setMoneyStock((shop.getMoneyStock()==null) ? totalPrice : shop.getMoneyStock().add(totalPrice));
            shop.setLastTransactionDate(new Date());
            javaPlugin.getShopHandler().upsertShop(shop);

            StaticUtils.sendMessage(player, "&fBought " + workingQuantity + " x " + StaticUtils.getItemName(saleItem) + "&r for &a$" + StaticUtils.formatDoubleUS(totalPrice.doubleValue()) + "&f.");
        } finally {
            javaPlugin.getShopHandler().unlockShop(shopUuid, player.getUniqueId());
        }
    }

    // Getters
    public static BigDecimal getShopBuyPriceForOne(Shop shop) {
        if (shop==null) return null;
        if (shop.getBuyPrice()==null) return null;
        if (shop.getStackSize()<=0) return null;
        
        return shop.getBuyPrice().divide(BigDecimal.valueOf(shop.getStackSize()), 2, RoundingMode.DOWN);
    }

    public static BigDecimal getShopSellPriceForOne(Shop shop) {
        if (shop==null) return null;
        if (shop.getSellPrice()==null) return null;
        if (shop.getStackSize()<=0) return null;
        
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

        lectern.setItemMeta(meta);
        if (amount!=null) lectern.setAmount(amount);

        return lectern;
    }
}
