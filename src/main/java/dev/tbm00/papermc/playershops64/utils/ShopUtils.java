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

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.enums.AdjustType;
import dev.tbm00.papermc.playershops64.data.structure.Shop;

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
                            null,
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
            StaticUtils.addToInventoryOrDrop(player, StaticUtils.prepPlayerShopItemStack(1));
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
                StaticUtils.sendMessage(player, "&cFailed to set sale item, you weren't holding an item!");
                return;
            }

            if (shop.getItemStock()>0) {
                StaticUtils.sendMessage(player, "&cShop must have an empty item stock before changing sale items!");
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
            StaticUtils.sendMessage(player, "&aShop's sale item set to &e" + StaticUtils.getItemName(one));
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

        if (newPrice!=null && newPrice>javaPlugin.getConfigHandler().getMaxBuyPrice()) {
            StaticUtils.sendMessage(player, "&cCannot set buy price greater than $" + StaticUtils.formatDoubleUS(javaPlugin.getConfigHandler().getMaxBuyPrice()));
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

            if (newPrice!=null && newPrice>javaPlugin.getConfigHandler().getMaxSellPrice()) {
                StaticUtils.sendMessage(player, "&cCannot set sell price greater than $" + StaticUtils.formatDoubleUS(javaPlugin.getConfigHandler().getMaxSellPrice()));
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

    public static void addAssistant(Player player, UUID shopUuid, String playerName) {
        if (!Bukkit.isPrimaryThread()) {
            StaticUtils.log(ChatColor.RED, player.getName() + " tried to add assistant to shop " + shopUuid + "'s off the main thread -- trying again during next tick on main thread!");
            javaPlugin.getServer().getScheduler().runTask(javaPlugin, () -> addAssistant(player, shopUuid, playerName));
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

            UUID addUuid = null;
            try {
                addUuid = javaPlugin.getServer().getOfflinePlayer(playerName).getUniqueId();
            } catch (Exception e) {
                StaticUtils.sendMessage(player, "&cCould not find player from input: '"+playerName+"'");
                return;
            }
            if (addUuid==null) {
                StaticUtils.sendMessage(player, "&cCould not find player from input: '"+playerName+"'");
                return;
            }

            shop.addAssistant(addUuid);

            // apply updates
            javaPlugin.getShopHandler().upsertShopObject(shop);
            StaticUtils.sendMessage(player, "&aAdded '" + playerName + "' to shop's assistants!");
        } finally {
            javaPlugin.getShopHandler().unlockShop(shopUuid, player.getUniqueId());
        }
    }

    public static void removeAssistant(Player player, UUID shopUuid, UUID playerUuid) {
        if (!Bukkit.isPrimaryThread()) {
            StaticUtils.log(ChatColor.RED, player.getName() + " tried to remove assistant to shop " + shopUuid + "'s off the main thread -- trying again during next tick on main thread!");
            javaPlugin.getServer().getScheduler().runTask(javaPlugin, () -> removeAssistant(player, shopUuid, playerUuid));
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

            shop.removeAssistant(playerUuid);

            // apply updates
            javaPlugin.getShopHandler().upsertShopObject(shop);
            StaticUtils.sendMessage(player, "&aRemoved '" + javaPlugin.getServer().getOfflinePlayer(playerUuid).getName() + "' from shop's assistants!");
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
            if (saleItem == null) {
                StaticUtils.sendMessage(player, "&cShop has no sale item set..!");
                return;
            }

            if (requestedAmount < 0) {
                StaticUtils.sendMessage(player, "&cInvalid amount!");
                return;
            }

            double playerBalance = javaPlugin.getVaultHook().getBalance(player);
            double shopBalance = StaticUtils.normalizeToDouble(shop.getMoneyStock());
            double max = javaPlugin.getConfigHandler().getMaxBalance();

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

                    double space = Math.max(0, max - shopBalance);
                    double workingAmount = Math.min(requestedAmount, Math.min(playerBalance, space));
                    if (workingAmount <= 0) {
                        StaticUtils.sendMessage(player, "&cShop is at max balance!");
                        return;
                    }

                    if (!javaPlugin.getVaultHook().removeMoney(player, workingAmount)) {
                        StaticUtils.sendMessage(player, "&cError removing $" + StaticUtils.formatDoubleUS(workingAmount) + "&r&c from your balance..!");
                        return;
                    }

                    double newBalance = shopBalance + workingAmount;
                    shop.setMoneyStock(BigDecimal.valueOf(newBalance));

                    // apply updates
                    javaPlugin.getShopHandler().upsertShopObject(shop);
                    StaticUtils.sendMessage(player, "&aAdded $" + StaticUtils.formatDoubleUS(workingAmount) + " to the shop's balance! Updated balance: $" + StaticUtils.formatDoubleUS(newBalance));
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

                    double workingAmount = Math.min(requestedAmount, shopBalance);
                    if (workingAmount <= 0) {
                        StaticUtils.sendMessage(player, "&cNothing to withdraw.");
                        return;
                    }

                    if (!javaPlugin.getVaultHook().giveMoney(player, workingAmount)) {
                        StaticUtils.sendMessage(player, "&cError adding $" + StaticUtils.formatDoubleUS(workingAmount) + "&r&c to your balance..!");
                        return;
                    }

                    double newBalance = shopBalance - workingAmount;
                    shop.setMoneyStock(BigDecimal.valueOf(newBalance));

                    // apply updates
                    javaPlugin.getShopHandler().upsertShopObject(shop);
                    StaticUtils.sendMessage(player, "&aRemoved $" + StaticUtils.formatDoubleUS(workingAmount) + " from the shop's balance! Updated balance: $" + StaticUtils.formatDoubleUS(newBalance));
                    break;
                }
                case SET: {
                    double target = requestedAmount;
                    if (target < 0) target = 0;
                    if (target > max) target = max;
                    if (Double.compare(shopBalance, target) == 0) return;

                    double delta = target - shopBalance; // positive: player -> shop,  negative: shop -> player
                    if (delta > 0) { // positive: player -> shop
                        if (playerBalance <= 0) {
                            StaticUtils.sendMessage(player, "&cYou don't have any money to deposit!");
                            return;
                        }
                        double space = Math.max(0, max - shopBalance);
                        double workingAmount = Math.min(delta, Math.min(playerBalance, space));
                        if (workingAmount <= 0) {
                            StaticUtils.sendMessage(player, "&cShop is at max balance!");
                            return;
                        }

                        if (!javaPlugin.getVaultHook().removeMoney(player, workingAmount)) {
                            StaticUtils.sendMessage(player, "&cError removing $" + StaticUtils.formatDoubleUS(workingAmount) + "&r&c from your balance..!");
                            return;
                        }

                        double newBalance = shopBalance + workingAmount;
                        shop.setMoneyStock(BigDecimal.valueOf(newBalance));

                        // apply updates
                        javaPlugin.getShopHandler().upsertShopObject(shop);
                        StaticUtils.sendMessage(player, "&aAdded $" + StaticUtils.formatDoubleUS(workingAmount) + " to the shop's balance! Updated balance: $" + StaticUtils.formatDoubleUS(newBalance));
                    } else { // negative: shop -> player
                        double needed = -delta;
                        if (shopBalance <= 0) {
                            StaticUtils.sendMessage(player, "&cThe shop doesn't have any money to withdraw!");
                            return;
                        }
                        double workingAmount = Math.min(needed, shopBalance);
                        if (workingAmount <= 0) {
                            StaticUtils.sendMessage(player, "&cNothing to withdraw.");
                            return;
                        }

                        if (!javaPlugin.getVaultHook().giveMoney(player, workingAmount)) {
                            StaticUtils.sendMessage(player, "&cError adding $" + StaticUtils.formatDoubleUS(workingAmount) + "&r&c to your balance..!");
                            return;
                        }

                        double newBalance = shopBalance - workingAmount;
                        shop.setMoneyStock(BigDecimal.valueOf(newBalance));

                        // apply updates
                        javaPlugin.getShopHandler().upsertShopObject(shop);
                        StaticUtils.sendMessage(player, "&aRemoved $" + StaticUtils.formatDoubleUS(workingAmount) + " from the shop's balance! Updated balance: $" + StaticUtils.formatDoubleUS(newBalance));
                    }
                    break;
                }
                default:
                    StaticUtils.sendMessage(player, "&cError: No adjustment type found!");
            }
        } finally {
            javaPlugin.getShopHandler().unlockShop(shopUuid, player.getUniqueId());
        }
    }

    public static void adjustStock(Player player, UUID shopUuid, AdjustType adjustType, int requestedAmount) {
        if (!Bukkit.isPrimaryThread()) {
            StaticUtils.log(ChatColor.RED, player.getName() + " tried to adjust shop " + shopUuid + "'s stock off the main thread -- trying again during next tick on main thread!");
            javaPlugin.getServer().getScheduler().runTask(javaPlugin, () -> adjustStock(player, shopUuid, adjustType, requestedAmount));
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
            if (saleItem == null) {
                StaticUtils.sendMessage(player, "&cShop has no sale item set..!");
                return;
            }

            if (requestedAmount < 0) {
                StaticUtils.sendMessage(player, "&cInvalid quantity.");
                return;
            }

            int playerStock = StaticUtils.countMatchingItems(player, saleItem);
            int shopStock = shop.getItemStock();
            int max = javaPlugin.getConfigHandler().getMaxStock();

            switch (adjustType) {
                case ADD: {
                    if (requestedAmount == 0) {
                        StaticUtils.sendMessage(player, "&cCannot deposit 0!");
                        return;
                    }
                    if (playerStock <= 0) {
                        StaticUtils.sendMessage(player, "&cYou don't have any matching items to deposit!");
                        return;
                    }

                    int space = Math.max(0, max - shopStock);
                    if (space <= 0) {
                        StaticUtils.sendMessage(player, "&cShop is at max stock!");
                        return;
                    }

                    int workingAmount = Math.min(requestedAmount, Math.min(playerStock, space));
                    if (workingAmount <= 0) {
                        StaticUtils.sendMessage(player, "&cNothing to deposit.");
                        return;
                    }

                    if (!StaticUtils.removeMatchingItems(player, saleItem, workingAmount)) {
                        StaticUtils.sendMessage(player, "&cError removing " + workingAmount + " x " + StaticUtils.getItemName(saleItem) + "&r&c from your inventory..!");
                        return;
                    }

                    int newStock = shopStock + workingAmount;
                    shop.setItemStock(newStock);
                    
                    // apply updates
                    javaPlugin.getShopHandler().upsertShopObject(shop);
                    StaticUtils.sendMessage(player, "&aAdded " + workingAmount + " to the shop's stock! Updated stock: " + newStock);
                    break;
                }
                case REMOVE: {
                    if (requestedAmount == 0) {
                        StaticUtils.sendMessage(player, "&cCannot withdraw 0!");
                        return;
                    }
                    if (shopStock <= 0) {
                        StaticUtils.sendMessage(player, "&cThe shop doesn't have any stock to withdraw!");
                        return;
                    }

                    int workingAmount = Math.min(requestedAmount, shopStock);
                    if (workingAmount <= 0) {
                        StaticUtils.sendMessage(player, "&cNothing to withdraw.");
                        return;
                    }

                    if (!StaticUtils.addToInventoryOrDrop(player, saleItem, workingAmount)) {
                        StaticUtils.sendMessage(player, "&cError adding " + workingAmount + " x " + StaticUtils.getItemName(saleItem) + "&r&c to your inventory..!");
                        return;
                    }

                    int newStock = shopStock - workingAmount;
                    shop.setItemStock(newStock);

                    // apply updates
                    javaPlugin.getShopHandler().upsertShopObject(shop);
                    StaticUtils.sendMessage(player, "&aRemoved " + workingAmount + " from the shop's stock! Updated stock: " + newStock);
                    break;
                }
                case SET: {
                    int target = requestedAmount;
                    if (target < 0) target = 0;
                    if (target > max) target = max;
                    if (shopStock == target) return;

                    int delta = target - shopStock; // positive: player -> shop,  negative: shop -> player
                    if (delta > 0) { // positive: player -> shop
                        if (playerStock <= 0) {
                            StaticUtils.sendMessage(player, "&cYou don't have any matching items to deposit!");
                            return;
                        }
                        int space = Math.max(0, max - shopStock);
                        if (space <= 0) {
                            StaticUtils.sendMessage(player, "&cShop is at max stock!");
                            return;
                        }

                        int workingAmount = Math.min(delta, Math.min(playerStock, space));
                        if (workingAmount <= 0) {
                            StaticUtils.sendMessage(player, "&cNothing to deposit.");
                            return;
                        }

                        if (!StaticUtils.removeMatchingItems(player, saleItem, workingAmount)) {
                            StaticUtils.sendMessage(player, "&cError removing " + workingAmount + " x " + StaticUtils.getItemName(saleItem) + "&r&c from your inventory..!");
                            return;
                        }

                        int newStock = shopStock + workingAmount;
                        shop.setItemStock(newStock);

                        // apply updates
                        javaPlugin.getShopHandler().upsertShopObject(shop);
                        StaticUtils.sendMessage(player, "&aAdded " + workingAmount + " to the shop's stock! Updated stock: " + newStock);
                    } else { // negative: shop -> player
                        if (shopStock <= 0) {
                            StaticUtils.sendMessage(player, "&cThe shop doesn't have any stock to withdraw!");
                            return;
                        }

                        int workingAmount = Math.min(-delta, shopStock);
                        if (workingAmount <= 0) {
                            StaticUtils.sendMessage(player, "&cNothing to withdraw.");
                            return;
                        }

                        if (!StaticUtils.addToInventoryOrDrop(player, saleItem, workingAmount)) {
                            StaticUtils.sendMessage(player, "&cError adding " + workingAmount + " x " + StaticUtils.getItemName(saleItem) + "&r&c to your inventory..!");
                            return;
                        }

                        int newStock = shopStock - workingAmount;
                        shop.setItemStock(newStock);

                        // apply updates
                        javaPlugin.getShopHandler().upsertShopObject(shop);
                        StaticUtils.sendMessage(player, "&aRemoved " + workingAmount + " from the shop's stock! Updated stock: " + newStock);
                    }
                    break;
                }
                default:
                    StaticUtils.sendMessage(player, "&cError: No adjust type found!");
            }
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

            if (shop.getOwnerUuid().equals(player.getUniqueId())) {
                StaticUtils.sendMessage(player, "&cYou cannot sell to your own shop!");
                return;
            }

            if (shop.isAssistant(player.getUniqueId())) {
                StaticUtils.sendMessage(player, "&cYou can't sell to shops you are an assistant to");
                return;
            }

            if (quantity <= 0) {
                StaticUtils.sendMessage(player, "&cInvalid quantity.");
                return;
            } int workingAmount = quantity;

            ItemStack saleItem = shop.getItemStack();
            if (saleItem == null || saleItem.getType().isAir()) {
                StaticUtils.sendMessage(player, "&cThis shop has no item set.");
                return;
            }

            int playerHas = StaticUtils.countMatchingItems(player, saleItem);
            if (playerHas < quantity) {
                workingAmount = playerHas;
            } if (playerHas <= 0) {
                StaticUtils.sendMessage(player, "&cYou don't have any matching items to sell!");
                return;
            } if (workingAmount+shop.getItemStock()>javaPlugin.getConfigHandler().getMaxStock()) {
                workingAmount = javaPlugin.getConfigHandler().getMaxStock()-shop.getItemStock();
            }

            if (shop.getSellPrice()==null || shop.getSellPrice().compareTo(BigDecimal.ZERO)<0 || shop.getMoneyStock()==null) {
                StaticUtils.sendMessage(player, "&cThis shop is not buying (sell-to is disabled)!");
                return;
            }

            BigDecimal unitPrice = shop.getSellPriceForOne();
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                StaticUtils.sendMessage(player, "&cInternal error: invalid per item sell price..!");
                return;
            }

            BigDecimal totalPrice = StaticUtils.normalizeBigDecimal( unitPrice.multiply(BigDecimal.valueOf(workingAmount)) );
            if (!shop.hasInfiniteMoney()) {
                BigDecimal moneyStock = shop.getMoneyStock() == null ? BigDecimal.ZERO : shop.getMoneyStock();
                if (moneyStock.compareTo(totalPrice) < 0) {
                    // get max afforded
                    int shopCanAfford = moneyStock.divide(unitPrice, 0, RoundingMode.DOWN).intValue();
                    if (shopCanAfford<=0) {
                        StaticUtils.sendMessage(player, "&cThe shop can't afford to buy any right now.");
                        return;
                    }

                    workingAmount = Math.min(shopCanAfford, playerHas);
                    totalPrice = StaticUtils.normalizeBigDecimal( unitPrice.multiply(BigDecimal.valueOf(workingAmount)) );
                }
            }

            if (workingAmount <= 0) {
                StaticUtils.sendMessage(player, "&cNothing to sell..!");
                return;
            } totalPrice = totalPrice.max(BigDecimal.ZERO);

            boolean gaveMoneyToPlayer = javaPlugin.getVaultHook().giveMoney( player, totalPrice.doubleValue());
            if (!gaveMoneyToPlayer) {
                StaticUtils.sendMessage(player, "&cPayment failed, nothing was sold!");
                return;
            }

            boolean removedItemsFromPlayer = StaticUtils.removeMatchingItems(player, saleItem, workingAmount);
            if (!removedItemsFromPlayer) {
                if (!javaPlugin.getVaultHook().removeMoney( player, totalPrice.doubleValue()))
                    {StaticUtils.log(ChatColor.RED, "CRITICAL ERROR: removeMoney("+player.getName()+", "+totalPrice.doubleValue()+") failed -- money was duped to player!");}
                StaticUtils.sendMessage(player, "&cError removing " +workingAmount+ " x " + StaticUtils.getItemName(saleItem) + "&r&c from your inventory..!");
                return;
            }

            // edit shop
            if (!shop.hasInfiniteStock()) shop.setItemStock(shop.getItemStock() + workingAmount);
            if (!shop.hasInfiniteMoney()) shop.setMoneyStock((shop.getMoneyStock()==null) ? BigDecimal.ZERO: shop.getMoneyStock().subtract(totalPrice));
            shop.setLastTransactionDate(new Date());
            
            // apply updates
            javaPlugin.getShopHandler().upsertShopObject(shop);
            StaticUtils.sendMessage(player, "&fSold " + workingAmount + " x " + StaticUtils.getItemName(saleItem) + "&r for &a$" + StaticUtils.formatDoubleUS(totalPrice.doubleValue()) + "&f.");
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

            if (shop.getOwnerUuid().equals(player.getUniqueId())) {
                StaticUtils.sendMessage(player, "&cYou cannot buy from your own shop!");
                return;
            }

            if (shop.isAssistant(player.getUniqueId())) {
                StaticUtils.sendMessage(player, "&cYou can't buy from shops you are an assistant to");
                return;
            }

            if (quantity <= 0) {
                StaticUtils.sendMessage(player, "&cInvalid quantity.");
                return;
            } int workingAmount = quantity;

            ItemStack saleItem = shop.getItemStack();
            if (saleItem == null || saleItem.getType().isAir()) {
                StaticUtils.sendMessage(player, "&cThis shop has no item set.");
                return;
            }

            int shopHas = shop.getItemStock();
            if (!shop.hasInfiniteStock()) {
                if (shopHas < quantity) {
                    workingAmount = shopHas;
                } if (shopHas <= 0) {
                    StaticUtils.sendMessage(player, "&cThis shop doesn't have any stock to sell!");
                    return;
                }
            }

            if (shop.getBuyPrice()==null || shop.getBuyPrice().compareTo(BigDecimal.ZERO)<0 || shop.getMoneyStock()==null) {
                StaticUtils.sendMessage(player, "&cThis shop is not selling (buy-from is disabled)!");
                return;
            }

            BigDecimal unitPrice = shop.getBuyPriceForOne();
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                StaticUtils.sendMessage(player, "&cInternal error: invalid per item buy price..!");
                return;
            }

            BigDecimal totalPrice = StaticUtils.normalizeBigDecimal( unitPrice.multiply(BigDecimal.valueOf(workingAmount)) );
            BigDecimal balance = BigDecimal.valueOf(javaPlugin.getVaultHook().getBalance(player));
            if (balance.compareTo(totalPrice) < 0) {
                // get max afforded
                int playerCanAfford = balance.divide(unitPrice, 0, RoundingMode.DOWN).intValue();
                if (playerCanAfford<=0) {
                    StaticUtils.sendMessage(player, "&cYou can't afford to buy any right now!");
                    return;
                }

                if (shop.hasInfiniteStock()) workingAmount = playerCanAfford;
                else workingAmount = Math.min(playerCanAfford, shopHas);
                totalPrice = StaticUtils.normalizeBigDecimal( unitPrice.multiply(BigDecimal.valueOf(workingAmount)) );
            }

            if (totalPrice.doubleValue()+shop.getMoneyStock().doubleValue()>javaPlugin.getConfigHandler().getMaxBalance()) {
                StaticUtils.sendMessage(player, "&cTransaction canceled as it would cause this shop to exceed the max stored balance!");
                return;
            }

            if (workingAmount <= 0) {
                StaticUtils.sendMessage(player, "&cNothing to buy..!");
                return;
            } totalPrice = totalPrice.max(BigDecimal.ZERO);

            boolean tookMoneyFromPlayer = javaPlugin.getVaultHook().removeMoney( player, totalPrice.doubleValue());
            if (!tookMoneyFromPlayer) {
                StaticUtils.sendMessage(player, "&cPayment failed, nothing was bought!");
                return;
            }

            boolean addedItemsToPlayer = StaticUtils.addToInventoryOrDrop(player, saleItem, workingAmount);
            if (!addedItemsToPlayer) {
                if (!javaPlugin.getVaultHook().giveMoney(player, totalPrice.doubleValue()))
                    {StaticUtils.log(ChatColor.RED, "CRITICAL ERROR: giveMoney("+player.getName()+", "+totalPrice.doubleValue()+") failed -- money was nuked from player!");}
                StaticUtils.sendMessage(player, "&cError adding " +workingAmount+ " x " + StaticUtils.getItemName(saleItem) + "&r&c to your inventory..!");
                return;
            }

            // edit shop
            if (!shop.hasInfiniteStock()) shop.setItemStock(shop.getItemStock() - workingAmount);
            if (!shop.hasInfiniteMoney()) shop.setMoneyStock((shop.getMoneyStock()==null) ? totalPrice : shop.getMoneyStock().add(totalPrice));
            shop.setLastTransactionDate(new Date());
            
            // apply updates
            javaPlugin.getShopHandler().upsertShopObject(shop);
            StaticUtils.sendMessage(player, "&fBought " + workingAmount + " x " + StaticUtils.getItemName(saleItem) + "&r for &a$" + StaticUtils.formatDoubleUS(totalPrice.doubleValue()) + "&f.");
        } finally {
            javaPlugin.getShopHandler().unlockShop(shopUuid, player.getUniqueId());
        }
    }

    public static int quickDepositToShop(Player player, UUID shopUuid, int quantity) {
        if (!Bukkit.isPrimaryThread()) {
            StaticUtils.log(ChatColor.RED, player.getName() + " tried to quick deposit to shop " + shopUuid + " off the main thread -- canceling..!");
            return 0;
        }

        if (!javaPlugin.getShopHandler().tryLockShop(shopUuid, player)) {
            return 0;
        }

        try {
            Shop shop = javaPlugin.getShopHandler().getShop(shopUuid);
            if (shop == null) {
                return 0;
            }

            if (!shop.getOwnerUuid().equals(player.getUniqueId())) {
                return 0;
            }

            ItemStack saleItem = shop.getItemStack();
            if (saleItem == null || saleItem.getType().isAir()) {
                return 0;
            }

            if (quantity <= 0) {
                return 0;
            } int workingAmount = quantity;
            if (workingAmount+shop.getItemStock()>javaPlugin.getConfigHandler().getMaxStock()) {
                workingAmount = javaPlugin.getConfigHandler().getMaxStock()-shop.getItemStock();
            }

            if (workingAmount <= 0) {
                return 0;
            }

            // edit shop
            if (!shop.hasInfiniteStock()) shop.setItemStock(shop.getItemStock() + workingAmount);
            
            // apply updates
            javaPlugin.getShopHandler().upsertShopObject(shop);
            return workingAmount;
        } finally {
            javaPlugin.getShopHandler().unlockShop(shopUuid, player.getUniqueId());
        }
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

    public static List<String> formatSaleItemLoreText(Shop shop, boolean includeStackSize) {
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


        String sizeLine = "";
        if (includeStackSize) sizeLine += "&7Stack Size: &e" + shop.getStackSize() + " ";
        if (stock<0) sizeLine += "&7Stock: &e∞";
        else sizeLine += "&7Stock: &e" + stock;
        lore.add(sizeLine);

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
}
