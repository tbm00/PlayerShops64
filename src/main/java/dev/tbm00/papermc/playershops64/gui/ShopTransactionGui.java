package dev.tbm00.papermc.playershops64.gui;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.Shop;
import dev.tbm00.papermc.playershops64.utils.GuiUtils;
import dev.tbm00.papermc.playershops64.utils.ShopUtils;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class ShopTransactionGui {
    private final PlayerShops64 javaPlugin;
    private final Gui gui;
    private final Player viewer;
    private final UUID shopUuid;
    private final Shop shop;
    private final int quantity;
    private final boolean closeGuiAfter;
    private String label;
    
    public ShopTransactionGui(PlayerShops64 javaPlugin, Player viewer, UUID shopUuid, int quantity, boolean closeGuiAfter) {
        this.javaPlugin = javaPlugin;
        this.viewer = viewer;
        this.shopUuid = shopUuid;
        this.shop = javaPlugin.getShopHandler().getShop(shopUuid);
        this.quantity = Math.max(quantity, 1);
        this.gui = new Gui(6, label);
        this.closeGuiAfter = closeGuiAfter;

        String shopHint = shopUuid.toString().substring(0, 6);
        if (javaPlugin.getShopHandler().tryLockShop(shopUuid, viewer)) {
            StaticUtils.log(ChatColor.YELLOW, viewer.getName() + " opened shop "+shopHint+"'s transaction gui");
        } else return;

        label = "Shop Transaction (" + shopHint+ ")";

        setup();

        gui.updateTitle(label);
        gui.disableAllInteractions();
        gui.open(viewer);
        gui.setCloseGuiAction(event -> {
            StaticUtils.log(ChatColor.GREEN, viewer.getName() + " opened shop "+shopHint+"'s transaction gui");
            javaPlugin.getShopHandler().unlockShop(shopUuid, viewer.getUniqueId());
        });
    }

    /**
     * Sets up all buttons.
     */
    private void setup() {
        ItemStack item = new ItemStack(Material.GLASS);
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();

        if (closeGuiAfter) { // Close Gui After Sale Item
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&6Click to stop the GUI from closing");
            lore.add("&6after each sale");
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dDisable Close After Sale"));
            item.setItemMeta(meta);
            item.setType(Material.LIGHT_GRAY_BANNER);
            gui.setItem(2, 8, ItemBuilder.from(item).asGuiItem(event -> {
                                                                        event.setCancelled(true);
                                                                        new ShopTransactionGui(javaPlugin, viewer, shopUuid, quantity, false);}));
        } else {
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&6Click to tell the GUI to close");
            lore.add("&6after each sale");
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dEnable Close After Sale"));
            item.setItemMeta(meta);
            item.setType(Material.GRAY_BANNER);
            gui.setItem(2, 8, ItemBuilder.from(item).asGuiItem(event -> {
                                                                        event.setCancelled(true);
                                                                        new ShopTransactionGui(javaPlugin, viewer, shopUuid, quantity, true);}));
        }

        if (shop.getItemStack()!=null) { // New Amount Item
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&fShop Stock: &e" + shop.getItemStock());
            lore.add("&fSelected Amount: &e" + quantity);
            item.setAmount(quantity);
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            item.setItemMeta(meta);
            item.setType(Material.GLASS);
            gui.setItem(2, 5, ItemBuilder.from(item).asGuiItem(event -> {event.setCancelled(true);}));
            item.setAmount(1);
        }
        
        if (shop.getItemStack()!=null) { // Sale Item
            ItemStack shopItem = shop.getItemStack();
            ItemMeta shopMeta = shopItem.getItemMeta();
            List<String> shopLore = shopMeta.getLore();

            shopLore = GuiUtils.getSaleItemLore(shop);
            shopItem.setAmount(shop.getStackSize());

            shopMeta.setLore(shopLore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            shopItem.setItemMeta(shopMeta);
            gui.setItem(2, 2, ItemBuilder.from(shopItem).asGuiItem(event -> {event.setCancelled(true);}));
        }

        if (shop.getSellPrice()!=null) { // Sell Button
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&6Click to &csell " + quantity + " &6for $" + (ShopUtils.getShopSellPriceForOne(shop).multiply(BigDecimal.valueOf(quantity))));
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cConfirm Sell"));
            item.setItemMeta(meta);
            item.setType(Material.RED_BANNER);
            gui.setItem(2, 4, ItemBuilder.from(item).asGuiItem(event -> {
                                                                        event.setCancelled(true);
                                                                        ShopUtils.sellToShop(viewer, shopUuid, quantity);
                                                                        if (closeGuiAfter) gui.close(viewer);}));
        }

        if (shop.getBuyPrice()!=null) { // Buy Button
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&6Click to &abuy " + quantity + " &6for $" + (ShopUtils.getShopBuyPriceForOne(shop).multiply(BigDecimal.valueOf(quantity))));
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aConfirm Buy"));
            item.setItemMeta(meta);
            item.setType(Material.GREEN_BANNER);
            gui.setItem(2, 6, ItemBuilder.from(item).asGuiItem(event -> {
                                                                        event.setCancelled(true);
                                                                        ShopUtils.buyFromShop(viewer, shopUuid, quantity);
                                                                        if (closeGuiAfter) gui.close(viewer);}));
        }

        { // Deposit Buttons
            if (quantity>64) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -64, 4, 1);
            if (quantity>1) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -1, 4, 2);

            if (quantity>32) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -32, 5, 1);
            if (quantity>16) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -16, 5, 2);
            if (quantity>8) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -8, 5, 3);
            if (quantity>4) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -4, 5, 4);

            if (quantity>50) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -50, 6, 1);
            if (quantity>25) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -25, 6, 2);
            if (quantity>10) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -10, 6, 3);
            if (quantity>5) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -5, 6, 4);

            addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, 1, 4, 8);
            addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, 64, 4, 9);

            addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 4, 5, 6);
            addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 8, 5, 7);
            addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 16, 5, 8);
            addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 32, 5, 9);

            addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 5, 6, 6);
            addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 10, 6, 7);
            addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 25, 6, 8);
            addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 50, 6, 9);
        }
    }

    private void addQuantityButton(ItemStack item, ItemMeta meta, List<String> lore, Material material, String name, int amount, int row, int col) {
        lore.clear();
        meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&f"+amount));
        item.setItemMeta(meta);
        item.setType(material);
        if (amount<0) item.setAmount((-1*amount));
        else item.setAmount(amount);
        gui.setItem(row, col, ItemBuilder.from(item).asGuiItem(event -> {GuiUtils.openGuiTransaction(viewer, shopUuid, quantity+amount, closeGuiAfter);}));
    }
}
