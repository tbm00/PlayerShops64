package dev.tbm00.papermc.playershops64.utils;

import java.math.BigDecimal;
import java.util.UUID;

import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
        String name = (item==null) ? "null" : item.getType().name();
        String stackSize = (!((1<=shop.getStackSize())&&(shop.getStackSize()<=64))) ? "error" : shop.getStackSize() + "";
        String buy = (shop.getBuyPrice()==null) ? "null" : (shop.getBuyPrice().compareTo(BigDecimal.valueOf(-1.0))==0) ? "disabled" : shop.getBuyPrice().toPlainString();
        String sell = (shop.getSellPrice()==null) ? "null" : (shop.getSellPrice().compareTo(BigDecimal.valueOf(-1.0))==0) ? "disabled" : shop.getSellPrice().toPlainString();
        String stock = (shop.hasInfiniteStock()) ? "infinite" : shop.getItemStock() + "";
        String balance = (shop.hasInfiniteMoney()) ? "infinite" : (shop.getMoneyStock()==null) ? "null" : shop.getMoneyStock().toPlainString();
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

}
