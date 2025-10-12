package dev.tbm00.papermc.playershops64.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.Shop;
import dev.tbm00.papermc.playershops64.data.enums.AdjustType;

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
                            Material.LECTERN,
                            null);

        javaPlugin.getShopHandler().upsertShopObject(shop);
        StaticUtils.log(ChatColor.GOLD, owner.getName() + " created a shop " + shop.getUuid() + " in " + world.getName()+ " @ " + (int)location.getX() + "," + (int)location.getY() + "," + (int)location.getZ());
        return shopUuid;
    }

    // Shop Edits
    public static void deleteShop(Player player, UUID shopUuid, Block block) {
        if (!Bukkit.isPrimaryThread()) {
            StaticUtils.log(ChatColor.RED, player.getName() + " tried to delete shop " + shopUuid + " off the main thread -- trying again during next tick on main thread!");
            javaPlugin.getServer().getScheduler().runTask(javaPlugin, () -> deleteShop(player, shopUuid, block));
            return;
        }

        // guard
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

            if (shop.getItemStock()>0) {
                StaticUtils.sendMessage(player, "&cShop must have an empty item stock before deleting it!");
                return;
            }

            if (shop.getMoneyStock().compareTo(BigDecimal.ZERO) == 1) {
                javaPlugin.getVaultHook().giveMoney(player, shop.getMoneyStock().doubleValue());
            }

            // delete shop
            javaPlugin.getShopHandler().deleteShopObject(shop.getUuid());
            if (block==null || block.equals(null)) {
                javaPlugin.getServer().getWorld(shop.getWorld().getUID()).getBlockAt(shop.getLocation()).setType(Material.AIR, false);
            } else block.setType(Material.AIR, false);
            StaticUtils.addToInventoryOrDrop(player, ShopUtils.prepPlayerShopItemStack(1));
            StaticUtils.sendMessage(player, "&aDeleted shop!");
        } finally {
            javaPlugin.getShopHandler().unlockShop(shopUuid, player.getUniqueId());
        }
    }

    public static void setShopItem(Player player, UUID shopUuid) {
        if (!Bukkit.isPrimaryThread()) {
            StaticUtils.log(ChatColor.RED, player.getName() + " tried to set shop " + shopUuid + "'s item off the main thread -- trying again during next tick on main thread!");
            javaPlugin.getServer().getScheduler().runTask(javaPlugin, () -> setShopItem(player, shopUuid));
            return;
        }

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

            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType().isAir()) {
                StaticUtils.sendMessage(player, "&cFailed to set shop item, you weren't holding an item!");
                return;
            }

            if (shop.getItemStock()>0) {
                StaticUtils.sendMessage(player, "&cShop must have an empty item stock before changing shop items!");
                return;
            }

            // edit shop
            int handCount = hand.getAmount();
            ItemStack one = hand.clone();
            one.setAmount(1);
            shop.setItemStack(one);
            shop.setItemStock(1);
            shop.setStackSize(1);

            // apply updates
            if (handCount>1) {
                hand.setAmount(handCount-1);
            } else {player.getInventory().setItemInMainHand(null);}
            javaPlugin.getShopHandler().upsertShopObject(shop);
            StaticUtils.sendMessage(player, "&aShop item set to &e" + StaticUtils.getItemName(one));
        } finally {
            javaPlugin.getShopHandler().unlockShop(shopUuid, player.getUniqueId());
        }
    }

    public static void clearShopItem(Player player, UUID shopUuid) {
        if (!Bukkit.isPrimaryThread()) {
            StaticUtils.log(ChatColor.RED, player.getName() + " tried to clear shop " + shopUuid + "'s item off the main thread -- trying again during next tick on main thread!");
            javaPlugin.getServer().getScheduler().runTask(javaPlugin, () -> clearShopItem(player, shopUuid));
            return;
        }

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

            if (shop.getItemStock()>0) {
                StaticUtils.sendMessage(player, "&cShop must have an empty item stock before clearing shop item!");
                return;
            }

            // edit shop
            shop.setItemStack(null);
            shop.setStackSize(1);

            // apply updates
            javaPlugin.getShopHandler().upsertShopObject(shop);
            StaticUtils.sendMessage(player, "&aShop item set cleared!");
        } finally {
            javaPlugin.getShopHandler().unlockShop(shopUuid, player.getUniqueId());
        }
    }

    public static void setBuyPrice(Player player, UUID shopUuid, Double newPrice) {
        if (!Bukkit.isPrimaryThread()) {
            StaticUtils.log(ChatColor.RED, player.getName() + " tried to set shop " + shopUuid + "'s buy price off the main thread -- trying again during next tick on main thread!");
            javaPlugin.getServer().getScheduler().runTask(javaPlugin, () -> setBuyPrice(player, shopUuid, newPrice));
            return;
        }

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

            // edit shop
            if (newPrice==null || newPrice.equals(null)) shop.setBuyPrice(null);
            else shop.setBuyPrice(BigDecimal.valueOf(newPrice));

            // apply updates
            javaPlugin.getShopHandler().upsertShopObject(shop);
            if (newPrice!=null && !newPrice.equals(null)) {
                StaticUtils.sendMessage(player, "&aSet buy price to $" + StaticUtils.formatDoubleUS(shop.getBuyPrice().doubleValue()) + "!");
            } else {
                StaticUtils.sendMessage(player, "&aDisabled buying from this shop!");
            }
        } finally {
            javaPlugin.getShopHandler().unlockShop(shopUuid, player.getUniqueId());
        }
    }

    public static void setSellPrice(Player player, UUID shopUuid, Double newPrice) {
        if (!Bukkit.isPrimaryThread()) {
            StaticUtils.log(ChatColor.RED, player.getName() + " tried to set shop " + shopUuid + "'s sell price off the main thread -- trying again during next tick on main thread!");
            javaPlugin.getServer().getScheduler().runTask(javaPlugin, () -> setSellPrice(player, shopUuid, newPrice));
            return;
        }

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

            // edit shop
            if (newPrice==null || newPrice.equals(null)) shop.setSellPrice(null);
            else shop.setSellPrice(BigDecimal.valueOf(newPrice));

            // apply updates
            javaPlugin.getShopHandler().upsertShopObject(shop);
            if (newPrice!=null && !newPrice.equals(null)) {
                StaticUtils.sendMessage(player, "&aSet sell price to $" + StaticUtils.formatDoubleUS(shop.getSellPrice().doubleValue()) + "!");
            } else {
                StaticUtils.sendMessage(player, "&aDisabled selling to this shop!");
            }
        } finally {
            javaPlugin.getShopHandler().unlockShop(shopUuid, player.getUniqueId());
        }
    }

    public static void setDisplayHeight(Player player, UUID shopUuid, int newHeight) {
        if (!Bukkit.isPrimaryThread()) {
            StaticUtils.log(ChatColor.RED, player.getName() + " tried to set shop " + shopUuid + "'s display height off the main thread -- trying again during next tick on main thread!");
            javaPlugin.getServer().getScheduler().runTask(javaPlugin, () -> setDisplayHeight(player, shopUuid, newHeight));
            return;
        }

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

            // edit shop
            shop.setDisplayHeight(newHeight);

            // apply updates
            javaPlugin.getShopHandler().upsertShopObject(shop);
            StaticUtils.sendMessage(player, "&aSet display height to " + shop.getDisplayHeight() + "!");
        } finally {
            javaPlugin.getShopHandler().unlockShop(shopUuid, player.getUniqueId());
        }
    }

    public static void setDescription(Player player, UUID shopUuid, String newDescription) {
        if (!Bukkit.isPrimaryThread()) {
            StaticUtils.log(ChatColor.RED, player.getName() + " tried to set shop " + shopUuid + "'s description off the main thread -- trying again during next tick on main thread!");
            javaPlugin.getServer().getScheduler().runTask(javaPlugin, () -> setDescription(player, shopUuid, newDescription));
            return;
        }

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

            if (newDescription!=null && newDescription.length()>28) {
                StaticUtils.sendMessage(player, "&cDescription too long..!");
                return;
            }

            // edit shop
            shop.setDescription(newDescription);

            // apply updates
            javaPlugin.getShopHandler().upsertShopObject(shop);
            if (newDescription!=null) StaticUtils.sendMessage(player, "&aSet description to '" + newDescription + "'!");
        } finally {
            javaPlugin.getShopHandler().unlockShop(shopUuid, player.getUniqueId());
        }
    }

    public static void adjustBalance(Player player, UUID shopUuid, AdjustType adjustType, double requestedAmount) {
        if (!Bukkit.isPrimaryThread()) {
            StaticUtils.log(ChatColor.RED, player.getName() + " tried to adjust shop " + shopUuid + "'s balance off the main thread -- trying again during next tick on main thread!");
            javaPlugin.getServer().getScheduler().runTask(javaPlugin, () -> adjustBalance(player, shopUuid, adjustType, requestedAmount));
            return;
        }

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

            ItemStack saleItem = shop.getItemStack();
            if (saleItem == null || saleItem.equals(null)) {
                StaticUtils.sendMessage(player, "&cShop has no sale item set..!");
                return;
            }

            if (requestedAmount < 0) {
                StaticUtils.sendMessage(player, "&cInvalid quantity.");
                return;
            } double workingAmount = requestedAmount;

            double playerBalance = javaPlugin.getVaultHook().getBalance(player);
            double shopBalance = StaticUtils.normalizeToDouble(shop.getMoneyStock());

            // edit shop
            switch (adjustType) {
                case ADD: {
                    if (requestedAmount == 0) {
                        StaticUtils.sendMessage(player, "&cCannot deposit 0!");
                        return;
                    }

                    if (playerBalance <= 0) {
                        StaticUtils.sendMessage(player, "&cYou don't have any money to deposit!");
                        return;
                    }

                    if (playerBalance < requestedAmount) workingAmount = playerBalance;
                    
                    boolean removedMoneyFromPlayer = javaPlugin.getVaultHook().removeMoney(player, workingAmount);
                    if (!removedMoneyFromPlayer) {
                        StaticUtils.sendMessage(player, "&cError removing $" +StaticUtils.formatDoubleUS(workingAmount)+ "&r&c from your balance..!");
                        return;
                    }

                    shop.setMoneyStock(BigDecimal.valueOf(shopBalance+workingAmount));
                    StaticUtils.sendMessage(player, "&aAdded $"+StaticUtils.formatDoubleUS(workingAmount)+" to the shop's balance! Updated balance: $" +StaticUtils.formatDoubleUS((shopBalance+workingAmount)));
                    break;
                }
                case REMOVE: {
                    if (requestedAmount == 0) {
                        StaticUtils.sendMessage(player, "&cCannot withdraw 0!");
                        return;
                    }

                    if (shopBalance <= 0) {
                        StaticUtils.sendMessage(player, "&cThe shop doesn't have any money to withdraw!");
                        return;
                    }

                    if (shopBalance < requestedAmount) workingAmount = shopBalance;

                    boolean addMoneyToPlayer = javaPlugin.getVaultHook().giveMoney(player, workingAmount);
                    if (!addMoneyToPlayer) {
                        StaticUtils.sendMessage(player, "&cError adding $" +StaticUtils.formatDoubleUS(workingAmount)+ "&r&c to your balance..!");
                        return;
                    }

                    shop.setMoneyStock(BigDecimal.valueOf(shopBalance-workingAmount));
                    StaticUtils.sendMessage(player, "&aRemoved $"+StaticUtils.formatDoubleUS(workingAmount)+" from the shop's balance! Updated balance: $" +StaticUtils.formatDoubleUS((shopBalance-workingAmount)));
                    break;
                }
                case SET: {
                    if (shopBalance == requestedAmount) return;
                    if (shopBalance > requestedAmount) {
                        if (shopBalance <= 0) {
                            StaticUtils.sendMessage(player, "&cThe shop doesn't have any money to withdraw!");
                            return;
                        }

                        workingAmount = shopBalance-requestedAmount;

                        boolean addMoneyToPlayer = javaPlugin.getVaultHook().giveMoney(player, workingAmount);
                        if (!addMoneyToPlayer) {
                            StaticUtils.sendMessage(player, "&cError adding $" +StaticUtils.formatDoubleUS(workingAmount)+ "&r&c to your balance..!");
                            return;
                        }

                        shop.setMoneyStock(BigDecimal.valueOf(shopBalance-workingAmount));
                        StaticUtils.sendMessage(player, "&aRemoved $"+StaticUtils.formatDoubleUS(workingAmount)+" from the shop's balance! Updated balance: $" +StaticUtils.formatDoubleUS((shopBalance-workingAmount)));
                        break;
                    } else if (shopBalance < requestedAmount) {
                        if (playerBalance <= 0) {
                            StaticUtils.sendMessage(player, "&cYou don't have any money to deposit!");
                            return;
                        }

                        workingAmount = requestedAmount-shopBalance;

                        boolean removedMoneyFromPlayer = javaPlugin.getVaultHook().removeMoney(player, workingAmount);
                        if (!removedMoneyFromPlayer) {
                            StaticUtils.sendMessage(player, "&cError removing $" +StaticUtils.formatDoubleUS(workingAmount)+ "&r&c from your balance..!");
                            return;
                        }

                        shop.setMoneyStock(BigDecimal.valueOf(shopBalance+workingAmount));
                        StaticUtils.sendMessage(player, "&aAdded $"+StaticUtils.formatDoubleUS(workingAmount)+" to the shop's balance! Updated balance: $" +StaticUtils.formatDoubleUS((shopBalance+workingAmount)));
                        break;
                    }
                    break;
                }
                default:
                    StaticUtils.sendMessage(player, "&cError: No adjust type found!");
                    return;
            }

            // apply updates
            javaPlugin.getShopHandler().upsertShopObject(shop);
        } finally {
            javaPlugin.getShopHandler().unlockShop(shopUuid, player.getUniqueId());
        }
    }

    public static void adjustStock(Player player, UUID shopUuid, AdjustType adjustType, int requestedQuantity) {
            if (!Bukkit.isPrimaryThread()) {
                StaticUtils.log(ChatColor.RED, player.getName() + " tried to adjust shop " + shopUuid + "'s stock off the main thread -- trying again during next tick on main thread!");
                javaPlugin.getServer().getScheduler().runTask(javaPlugin, () -> adjustStock(player, shopUuid, adjustType, requestedQuantity));
                return;
            }

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

                ItemStack saleItem = shop.getItemStack();
                if (saleItem == null || saleItem.equals(null)) {
                    StaticUtils.sendMessage(player, "&cShop has no sale item set..!");
                    return;
                }

                if (requestedQuantity < 0) {
                    StaticUtils.sendMessage(player, "&cInvalid quantity.");
                    return;
                } int workingQuantity = requestedQuantity;
                int playerStock = StaticUtils.countMatchingItems(player, saleItem);
                int shopStock = shop.getItemStock();

                // edit shop
                switch (adjustType) {
                    case ADD: {
                        if (requestedQuantity == 0) {
                            StaticUtils.sendMessage(player, "&cCannot deposit 0!");
                            return;
                        }

                        if (playerStock <= 0) {
                            StaticUtils.sendMessage(player, "&cYou don't have any matching items to deposit!");
                            return;
                        }

                        if (playerStock < requestedQuantity) workingQuantity = playerStock;
                        
                        boolean removedItemsFromPlayer = StaticUtils.removeMatchingItems(player, saleItem, workingQuantity);
                        if (!removedItemsFromPlayer) {
                            StaticUtils.sendMessage(player, "&cError removing " +workingQuantity+ " x " + StaticUtils.getItemName(saleItem) + "&r&c from your inventory..!");
                            return;
                        }

                        shop.setItemStock(shopStock+workingQuantity);
                        StaticUtils.sendMessage(player, "&aAdded "+workingQuantity+" to the shop's stock! Updated stock: " +(shopStock+workingQuantity));
                        break;
                    }
                    case REMOVE: {
                        if (requestedQuantity == 0) {
                            StaticUtils.sendMessage(player, "&cCannot withdraw 0!");
                            return;
                        }

                        if (shopStock <= 0) {
                            StaticUtils.sendMessage(player, "&cThe shop doesn't have any stock to withdraw!");
                            return;
                        }

                        if (shopStock < requestedQuantity) workingQuantity = shopStock;

                        boolean addItemsToPlayer = StaticUtils.addToInventoryOrDrop(player, saleItem, workingQuantity);
                        if (!addItemsToPlayer) {
                            StaticUtils.sendMessage(player, "&cError adding " +workingQuantity+ " x " + StaticUtils.getItemName(saleItem) + "&r&c to your inventory..!");
                            return;
                        }

                        shop.setItemStock(shopStock-workingQuantity);
                        StaticUtils.sendMessage(player, "&aRemoved "+workingQuantity+" from the shop's stock! Updated stock: " +(shopStock-workingQuantity));
                        break;
                    }
                    case SET: {
                        if (shopStock == requestedQuantity) return;
                        if (shopStock > requestedQuantity) {
                            if (shopStock <= 0) {
                                StaticUtils.sendMessage(player, "&cThe shop doesn't have any stock to withdraw!");
                                return;
                            }

                            workingQuantity = shopStock-requestedQuantity;

                            boolean addItemsToPlayer = StaticUtils.addToInventoryOrDrop(player, saleItem, workingQuantity);
                            if (!addItemsToPlayer) {
                                StaticUtils.sendMessage(player, "&cError adding " +workingQuantity+ " x " + StaticUtils.getItemName(saleItem) + "&r&c to your inventory..!");
                                return;
                            }

                            shop.setItemStock(shopStock-workingQuantity);
                            StaticUtils.sendMessage(player, "&aRemoved "+workingQuantity+" from the shop's stock! Updated stock: " +(shopStock-workingQuantity));
                        } else if (shopStock < requestedQuantity) {
                            if (playerStock <= 0) {
                                StaticUtils.sendMessage(player, "&cYou don't have any matching items to deposit!");
                                return;
                            }

                            workingQuantity = requestedQuantity-shopStock;

                            boolean removeItemsFromPlayer = StaticUtils.removeMatchingItems(player, saleItem, workingQuantity);
                            if (!removeItemsFromPlayer) {
                                StaticUtils.sendMessage(player, "&cError removing " +workingQuantity+ " x " + StaticUtils.getItemName(saleItem) + "&r&c from your inventory..!");
                                return;
                            }

                            shop.setItemStock(shopStock+workingQuantity);
                            StaticUtils.sendMessage(player, "&aAdded "+workingQuantity+" to the shop's stock! Updated stock: " +(shopStock+workingQuantity));
                        }
                        break;
                    }
                    default:
                        StaticUtils.sendMessage(player, "&cError: No adjust type found!");
                        return;
                }

                // apply updates
                javaPlugin.getShopHandler().upsertShopObject(shop);
            } finally {
                javaPlugin.getShopHandler().unlockShop(shopUuid, player.getUniqueId());
            }
        }

    public static void sellToShop(Player player, UUID shopUuid, int quantity) {
        if (!Bukkit.isPrimaryThread()) {
            StaticUtils.log(ChatColor.RED, player.getName() + " tried to sell to shop " + shopUuid + " off the main thread -- trying again during next tick on main thread!");
            javaPlugin.getServer().getScheduler().runTask(javaPlugin, () -> sellToShop(player, shopUuid, quantity));
            return;
        }

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

            // edit shop
            if (!shop.hasInfiniteStock()) shop.setItemStock(shop.getItemStock() + workingQuantity);
            if (!shop.hasInfiniteMoney()) shop.setMoneyStock((shop.getMoneyStock()==null) ? BigDecimal.ZERO: shop.getMoneyStock().subtract(totalPrice));
            shop.setLastTransactionDate(new Date());
            
            // apply updates
            javaPlugin.getShopHandler().upsertShopObject(shop);
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

            // edit shop
            if (!shop.hasInfiniteStock()) shop.setItemStock(shop.getItemStock() - workingQuantity);
            if (!shop.hasInfiniteMoney()) shop.setMoneyStock((shop.getMoneyStock()==null) ? totalPrice : shop.getMoneyStock().add(totalPrice));
            shop.setLastTransactionDate(new Date());
            
            // apply updates
            javaPlugin.getShopHandler().upsertShopObject(shop);
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
        String name = (item==null) ? "&c(null item)" : StaticUtils.getItemName(item);
        String stackSize = (!((1<=shop.getStackSize())&&(shop.getStackSize()<=64))) ? "error" : shop.getStackSize() + "";
        String buy = (shop.getBuyPrice()==null) ? "disabled" : StaticUtils.formatDoubleUS(shop.getBuyPrice().doubleValue());
        String sell = (shop.getSellPrice()==null) ? "disabled" : StaticUtils.formatDoubleUS(shop.getSellPrice().doubleValue());
        String stock = (shop.hasInfiniteStock()) ? "∞" : shop.getItemStock() + "";
        String balance = (shop.hasInfiniteMoney()) ? "∞" : (shop.getMoneyStock()==null) ? "null" : StaticUtils.formatDoubleUS(shop.getMoneyStock().doubleValue());
        String owner = (shop.getOwnerName()==null) ? "null" : shop.getOwnerName();

        String loreLine = null;
        if (item!=null) {
            ItemMeta meta = item.getItemMeta();
            if (meta!=null && meta.hasLore()) {
                loreLine = meta.getLore().get(0);
            }
        }

        if (shop.getDescription()!=null && !shop.getDescription().isEmpty())
            loreLine = shop.getDescription();
        if (shop.getDescription()!=null && shop.getDescription().isBlank()) loreLine = null;

        String returnText = name + " &7x &f" + stackSize;

        if (loreLine!=null && !loreLine.isBlank()) returnText += "\n&7&o" + loreLine;
        if (shop.getBuyPrice()!=null) returnText += "\n&7Buy for &a$" + buy;
        if (shop.getSellPrice()!=null) returnText += "\n&7Sell for &c$" + sell;
        returnText += "\n&7Stock: &e" + stock + "&7, Balance: &e$" + balance;
        returnText += "\n&7Owner: &e" + owner;

        return returnText;
    }

    public static List<String> formatSaleItemLoreText(Shop shop) {
        if (shop==null) return null;

        Double buyPrice = (shop.getBuyPrice()==null) ? null : shop.getBuyPrice().doubleValue();
        Double sellPrice = (shop.getSellPrice()==null) ? null : shop.getSellPrice().doubleValue();
        Double balance = (shop.getMoneyStock()==null) ? null : shop.getMoneyStock().doubleValue();
        int stock = shop.getItemStock();

        ItemStack item = null;
        if (shop.getItemStack()==null) {
            item = new ItemStack(Material.BARRIER);
        } else {
            item = shop.getItemStack().clone();
        }
        
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();
        String priceLine = "";
        UUID ownerUuid = shop.getOwnerUuid();

        if (meta.hasLore() && !meta.getLore().isEmpty()) {
            for (String line : meta.getLore()) {
                lore.add(line);
            }
        }

        lore.add("&8-----------------------");
        if (shop.getDescription()!=null && !shop.getDescription().isBlank())
            lore.add("&7" + shop.getDescription());

        if (buyPrice==null) priceLine = "&7B: &4(disabled) ";
        else priceLine = "&7B: &a$" + StaticUtils.formatDoubleUS(buyPrice) + " ";

        if (sellPrice==null) priceLine += "&7S: &4(disabled) ";
        else priceLine += "&7S: &c$" + StaticUtils.formatDoubleUS(sellPrice);
        lore.add(priceLine);

        if (stock<0) lore.add("&7Stock: &e∞");
        else lore.add("&7Stock: &e" + stock);

        if (shop.hasInfiniteMoney()) lore.add("&7Balance: &e$&e∞");
        else if (balance==null) lore.add("&7Balance: &e(null)");
        else lore.add("&7Balance: &e$" + StaticUtils.formatDoubleUS(balance));

        if (ownerUuid!=null && (!shop.hasInfiniteMoney() && !shop.hasInfiniteStock()))
            lore.add("&7Owner: &f" + shop.getOwnerName());

        lore.add("&7"+shop.getWorld().getName()+": &f"
                +(int)shop.getLocation().getX()+"&7, &f"
                +(int)shop.getLocation().getY()+"&7, &f"
                +(int)shop.getLocation().getZ());

        return lore;
    }

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
