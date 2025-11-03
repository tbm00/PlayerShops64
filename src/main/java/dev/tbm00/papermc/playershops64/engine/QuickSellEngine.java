package dev.tbm00.papermc.playershops64.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.structure.ShopPriceNode;
import dev.tbm00.papermc.playershops64.data.structure.ShopPriceQueue;
import dev.tbm00.papermc.playershops64.data.structure.Shop;
import dev.tbm00.papermc.playershops64.utils.Logger;
import dev.tbm00.papermc.playershops64.utils.ShopUtils;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public final class QuickSellEngine {

    public static final class Plan {
        public final SellPlan sellPlan;
        public final ReturnPlan returnPlan;

        public Plan(SellPlan sellPlan, ReturnPlan returnPlan) {
            this.sellPlan = sellPlan;
            this.returnPlan = returnPlan;
        }
    }

    public static final class SellPlan {
        public final List<SellPlanEntry> entries = new ArrayList<>();
        public BigDecimal totalMoney = BigDecimal.ZERO;
        public int totalItems = 0;

        public Set<UUID> distinctshopUuids() {
            Set<UUID> s = new HashSet<>();
            for (SellPlanEntry e : entries) s.add(e.shopUuid);
            return s;
        }
    }

    public static final class SellPlanEntry {
        public final UUID shopUuid;
        public final ItemStack item;   // snapshot of the item the player put in
        public final int amount;       // items to sell
        public final BigDecimal unitPrice;
        public final BigDecimal totalPrice;

        public SellPlanEntry(UUID shopUuid, ItemStack item, int amount, BigDecimal unitPrice) {
            this.shopUuid = shopUuid;
            this.item = item.clone();
            this.amount = amount;
            this.unitPrice = unitPrice;
            this.totalPrice = StaticUtils.normalizeBigDecimal(unitPrice.multiply(BigDecimal.valueOf(amount)));
        }
    }

    public static final class ReturnPlan {
        public final List<ReturnPlanEntry> entries = new ArrayList<>();
    }

    public static final class ReturnPlanEntry {
        public final ItemStack item;   // snapshot of the item the player put in
        public final int amount;       // items to return

        public ReturnPlanEntry(ItemStack item, int amount) {
            StaticUtils.log(ChatColor.AQUA, "return plan entry: "+item.getType().toString()+" "+amount);
            this.item = item.clone();
            this.amount = amount;
        }
    }

    private static PlayerShops64 javaPlugin;
    public Plan plans;
    public Player player;
    public final Inventory physicalInv;
    public final boolean isPhysicalInv;

    public QuickSellEngine(PlayerShops64 javaPlugin, Player player, Inventory physicalInv) {
        QuickSellEngine.javaPlugin = javaPlugin;
        this.player = player;
        this.isPhysicalInv = (physicalInv!=null);
        this.physicalInv = physicalInv;
    }

    public void computePlans(Inventory inv, UUID playerUuid) {
        // 1) deep snapshots
        Map<UUID, Shop> simShops = javaPlugin.getShopHandler().snapshotShopMap();
        Map<Material, ShopPriceQueue> simMaterialPriceMap = javaPlugin.getShopHandler().snapshotMaterialPriceMap();

        SellPlan sellPlan = new SellPlan();
        ReturnPlan returnPlan = new ReturnPlan();

        // 2) iterate dump-inventory
        for (ItemStack invItem : inv.getContents()) {
            if (invItem == null || invItem.getType().isAir() || invItem.getAmount()==0) continue;
            int initialAmount = invItem.getAmount(),
                stackAmount = initialAmount;

            Material mat = invItem.getType();
            ShopPriceQueue matQueue = simMaterialPriceMap.get(mat);
            if (matQueue == null || matQueue.isEmpty()) {
                if (!isPhysicalInv && stackAmount>0) {
                    returnPlan.entries.add(new ReturnPlanEntry(invItem, stackAmount));
                }
                continue;
            }

            // Non-destructive descending iterator
            for (ShopPriceNode shopNode : matQueue) {
                if (stackAmount <= 0) break;

                Shop shop = simShops.get(shopNode.getUuid());
                if (shop == null) continue;

                if (shop.getOwnerUuid()!=null && shop.getOwnerUuid().equals(playerUuid)) continue;
                if (shop.isAssistant(playerUuid)) continue;

                // shop must match the item (lore/meta/etc)
                if (!shop.getItemStack().isSimilar(invItem)) continue;

                // get unit price
                BigDecimal unitPrice = shop.getSellPriceForOne();
                if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) continue;

                // apply max stock bound
                int sellingAmount = stackAmount;
                if (!shop.hasInfiniteStock()) {
                    int maxStock = javaPlugin.getConfigHandler().getMaxStock();
                    int freeSpace = Math.max(0, maxStock - shop.getItemStock());
                    if (freeSpace <= 0) continue;
                    sellingAmount = Math.min(sellingAmount, freeSpace);
                }

                // apply money bound
                if (!shop.hasInfiniteMoney()) {
                    BigDecimal money = shop.getMoneyStock() == null ? BigDecimal.ZERO : shop.getMoneyStock();
                    BigDecimal maxTotal = money.max(BigDecimal.ZERO);
                    if (maxTotal.compareTo(unitPrice) < 0) {
                        // cannot afford one
                        continue;
                    }
                    int afford = maxTotal.divide(unitPrice, 0, RoundingMode.DOWN).intValue();
                    if (afford <= 0) continue;
                    sellingAmount = Math.min(sellingAmount, afford);
                }

                if (sellingAmount <= 0) continue;

                // record planned slice
                sellPlan.entries.add(new SellPlanEntry(shop.getUuid(), invItem, sellingAmount, unitPrice));
                sellPlan.totalItems += sellingAmount;
                sellPlan.totalMoney = StaticUtils.normalizeBigDecimal(sellPlan.totalMoney.add(unitPrice.multiply(BigDecimal.valueOf(sellingAmount))));

                // mutate ONLY the simulated shop state (so later items see the effects)
                if (!shop.hasInfiniteStock()) shop.setItemStock(shop.getItemStock() + sellingAmount);
                if (!shop.hasInfiniteMoney()) {
                    BigDecimal dec = StaticUtils.normalizeBigDecimal(unitPrice.multiply(BigDecimal.valueOf(sellingAmount)));
                    shop.setMoneyStock((shop.getMoneyStock()==null ? BigDecimal.ZERO : shop.getMoneyStock()).subtract(dec));
                }
                stackAmount -= sellingAmount;
            }

            if (isPhysicalInv) {
                invItem.setAmount(stackAmount);
            } else if (!isPhysicalInv && stackAmount>0) {
                returnPlan.entries.add(new ReturnPlanEntry(invItem, stackAmount));
            }
        }
        this.plans = new Plan(sellPlan, returnPlan);
    }

    public void runConfirmedPlans() {
        // 1) lock all shops in stable order to prevent deadlocks
        List<UUID> lockOrder = new ArrayList<>(plans.sellPlan.distinctshopUuids());
        Collections.sort(lockOrder);
        List<UUID> acquired = new ArrayList<>();

        try {
            // validate shops are usable
            for (UUID shopUuid : lockOrder) {
                if (!javaPlugin.getShopHandler().tryLockShop(shopUuid, player)) {
                    unlockShops(acquired, "&cOne of the shops is busy. Please try again.");
                    returnAllItems();
                    return;
                }
                acquired.add(shopUuid);
            }

            // 2) validate variables still hold
            for (SellPlanEntry entry : plans.sellPlan.entries) {
                Shop liveShop = javaPlugin.getShopHandler().getShop(entry.shopUuid);
                if (liveShop == null) {
                    unlockShops(acquired, "&cA shop went missing -- offer expired!");
                    returnAllItems();
                    return;
                }

                if (!liveShop.getItemStack().isSimilar(entry.item)) {
                    unlockShops(acquired, "&cA shop changed its item -- offer expired!");
                    returnAllItems();
                    return;
                }
                
                BigDecimal unit = liveShop.getSellPriceForOne();
                if (unit == null || unit.compareTo(entry.unitPrice) != 0) {
                    unlockShops(acquired, "&cA shop price changed -- offer expired!");
                    returnAllItems();
                    return;
                }
                
                if (!liveShop.hasInfiniteStock()) {
                    int max = javaPlugin.getConfigHandler().getMaxStock();
                    int free = Math.max(0, max - liveShop.getItemStock());
                    if (free < entry.amount) {
                        unlockShops(acquired, "&cA shop no longer has enough capacity -- offer expired!");
                        returnAllItems();
                        return;
                    }
                }
                
                if (!liveShop.hasInfiniteMoney()) {
                    BigDecimal need = unit.multiply(BigDecimal.valueOf(entry.amount));
                    BigDecimal money = liveShop.getMoneyStock() == null ? BigDecimal.ZERO : liveShop.getMoneyStock();
                    if (money.compareTo(need) < 0) {
                        unlockShops(acquired, "&cA shop no longer has enough money -- offer expired!");
                        returnAllItems();
                        return;
                    }
                }
            }

            // 3) execute the planned slices in order (will persist + reindex each step)
            int totalSold = 0, totalEarned = 0;
            boolean failedAndReturn = false;
            for (SellPlanEntry entry : plans.sellPlan.entries) {
                if (!failedAndReturn) {
                    int[] result = quickSellToShop(player, entry.shopUuid, entry.amount);
                    
                    // given validation, this should succeed exactly
                    if (result[0]!=entry.amount || result[1]!=entry.totalPrice.intValue()) {
                        unlockShops(acquired, "&cA race condition was detected -- offer expired midway through sale..!");
                        plans.returnPlan.entries.add(new ReturnPlanEntry(entry.item, entry.amount));
                        failedAndReturn = true;
                    }

                    totalSold += result[0];
                    totalEarned += result[1];
                } else {
                    plans.returnPlan.entries.add(new ReturnPlanEntry(entry.item, entry.amount));
                }
            }

            StaticUtils.sendMessage(player, "&aSold " + totalSold + " items for a total of $" + StaticUtils.formatIntUS(totalEarned));
            returnNonmatchedItems();
        } finally {
            // 4) always unlock
            StaticUtils.log(ChatColor.RED, "finally unlock:");
            unlockShops(acquired, null);
        }
    }

    private void unlockShops(List<UUID> acquired, String msg) {
        StaticUtils.sendMessage(player, msg);
        for (UUID id : acquired) javaPlugin.getShopHandler().unlockShop(id, player.getUniqueId());
        acquired.clear();
    }

    public void returnNonmatchedItems() {
        if (!plans.returnPlan.entries.isEmpty()) {
            for (ReturnPlanEntry entry : plans.returnPlan.entries) {
                if (isPhysicalInv) StaticUtils.addToInventoryOrDrop(physicalInv, entry.item, entry.amount);
                else StaticUtils.addToInventoryOrDrop(player, entry.item, entry.amount);
            }
            //StaticUtils.sendMessage(player, "&a(returnPlan items should have been returned)");
        } else {
            //StaticUtils.sendMessage(player, "&a(returnPlans empty...)");
        }
    }

    public void returnMatchedItems() {
        if (!plans.sellPlan.entries.isEmpty()) {
            for (SellPlanEntry entry : plans.sellPlan.entries) {
                if (isPhysicalInv) StaticUtils.addToInventoryOrDrop(physicalInv, entry.item, entry.amount);
                else StaticUtils.addToInventoryOrDrop(player, entry.item, entry.amount);
            }
            //StaticUtils.sendMessage(player, "&a(sellPlan items should have been returned)");
        } else {
           // StaticUtils.sendMessage(player, "&a(sellPlans empty...)");
        }
    }

    public void returnAllItems() {
        returnNonmatchedItems();
        returnMatchedItems();
    }

    private int[] quickSellToShop(Player player, UUID shopUuid, int quantity) {
        if (!Bukkit.isPrimaryThread()) {
            StaticUtils.log(ChatColor.RED, player.getName() + " tried to quick sell to shop " + shopUuid + " off the main thread -- canceling..!");
            return new int[] {0, 0};
        }

        if (!javaPlugin.getShopHandler().tryLockShop(shopUuid, player)) {
            StaticUtils.sendMessage(player, "&cQuick Sell Error: shop being used by someone else");
            return new int[] {0, 0};
        }

        try {
            Shop shop = javaPlugin.getShopHandler().getShop(shopUuid);
            if (shop == null) {
                StaticUtils.sendMessage(player, "&cQuick Sell Error: a shop object was null");
                return new int[] {0, 0};
            }

            if (shop.getOwnerUuid()!=null && shop.getOwnerUuid().equals(player.getUniqueId())) {
                StaticUtils.sendMessage(player, "&cQuick Sell Error: cant sell to own shop");
                return new int[] {0, 0};
            }

            if (shop.isAssistant(player.getUniqueId())) {
                StaticUtils.sendMessage(player, "&cQuick Sell Error: cant sell to shops you are an assistant to");
                return new int[] {0, 0};
            }

            if (shop.getSellPrice()==null || shop.getSellPrice().compareTo(BigDecimal.ZERO)<0) {
                StaticUtils.sendMessage(player, "&cQuick Sell Error: bad sell price (p<0 || p==null)");
                return new int[] {0, 0};
            }

            ItemStack saleItem = shop.getItemStack();
            if (saleItem == null || saleItem.getType().isAir()) {
                StaticUtils.sendMessage(player, "&cQuick Sell Error: bad sell item");
                return new int[] {0, 0};
            }

            if (quantity <= 0) {
                StaticUtils.sendMessage(player, "&cQuick Sell Error: bad quantity (q<0)");
                return new int[] {0, 0};
            } 
            
            if (quantity+shop.getItemStock()>javaPlugin.getConfigHandler().getMaxStock()) {
                StaticUtils.sendMessage(player, "&cQuick Sell Error: bad quantity (new would be greater than configurable max)");
                return new int[] {0, 0};
            }

            BigDecimal unitPrice = shop.getSellPriceForOne();
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                StaticUtils.sendMessage(player, "&cQuick Sell Error: bad unit price (p<0 || p==null)");
                return new int[] {0, 0};
            }

            BigDecimal totalPrice = StaticUtils.normalizeBigDecimal( unitPrice.multiply(BigDecimal.valueOf(quantity)) );
            if (!shop.hasInfiniteMoney()) {
                BigDecimal moneyStock = shop.getMoneyStock() == null ? BigDecimal.ZERO : shop.getMoneyStock();
                if (moneyStock.compareTo(totalPrice) < 0) {
                    StaticUtils.sendMessage(player, "&cQuick Sell Error: shop cant afford quantity");
                    return new int[] {0, 0};
                }
            }
            
            totalPrice = totalPrice.max(BigDecimal.ZERO);

            boolean gaveMoneyToPlayer = javaPlugin.getVaultHook().giveMoney( player, totalPrice.doubleValue());
            if (!gaveMoneyToPlayer) {
                StaticUtils.sendMessage(player, "&cQuick Sell Error: failed to pay you $"+totalPrice.doubleValue());
                return new int[] {0, 0};
            }

            // edit shop
            if (!shop.hasInfiniteStock()) shop.setItemStock(shop.getItemStock() + quantity);
            if (!shop.hasInfiniteMoney()) shop.setMoneyStock((shop.getMoneyStock()==null) ? BigDecimal.ZERO : shop.getMoneyStock().subtract(totalPrice));
            shop.setLastTransactionDate(new Date());
            
            // apply updates
            javaPlugin.getShopHandler().upsertShopObject(shop);
            Logger.logEdit(player.getName()+" quick sold "+quantity+" "+StaticUtils.getItemName(saleItem)+" ("+StaticUtils.formatTitleCase(saleItem.getType().toString())+") to "+shop.getOwnerName()+"'s shop ("+ShopUtils.getShopHint(shopUuid)+") for $"+totalPrice.doubleValue()+". Shop's updated stock: "+shop.getItemStock() + ", Shop's updated balance: $"+shop.getMoneyStock().doubleValue());
            return new int[] {quantity, totalPrice.intValue()};
        } finally {
            //javaPlugin.getShopHandler().unlockShop(shopUuid, player.getUniqueId());
        }
    }
}
