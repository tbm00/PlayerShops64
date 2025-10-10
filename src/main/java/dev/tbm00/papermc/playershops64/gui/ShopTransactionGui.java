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
import dev.tbm00.papermc.playershops64.data.enums.AdjustAttribute;
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
    private String label = "Shop Transaction";
    
    public ShopTransactionGui(PlayerShops64 javaPlugin, Player viewer, UUID shopUuid, Integer quantity, boolean closeGuiAfter) {
        this.javaPlugin = javaPlugin;
        this.viewer = viewer;
        this.shopUuid = shopUuid;
        this.shop = javaPlugin.getShopHandler().getShop(shopUuid);
        if (quantity == null) this.quantity = 1;
        else this.quantity = Math.max(quantity, 1);
        this.gui = new Gui(6, label);
        this.closeGuiAfter = closeGuiAfter;

        if (!javaPlugin.getShopHandler().tryLockShop(shopUuid, viewer)) {
            return;
        } String shopHint = shopUuid.toString().substring(0, 6);
        StaticUtils.log(ChatColor.YELLOW, viewer.getName() + " opened shop "+shopHint+"'s transcation gui: " + quantity);

        label = "Shop Transaction (" + shopHint+ ")";
        gui.updateTitle(label);
        setup();
        gui.disableAllInteractions();
        gui.setCloseGuiAction(event -> {
            StaticUtils.log(ChatColor.GREEN, viewer.getName() + " closed shop "+shopHint+"'s transaction gui: " + quantity);
            javaPlugin.getShopHandler().unlockShop(shopUuid, viewer.getUniqueId());
        });

        gui.open(viewer);
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
                                                                            gui.setCloseGuiAction(null);
                                                                            new ShopTransactionGui(javaPlugin, viewer, shopUuid, quantity, false);
                                                                        }));
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
                                                                            gui.setCloseGuiAction(null);
                                                                            new ShopTransactionGui(javaPlugin, viewer, shopUuid, quantity, true);
                                                                        }));
        }

        if (shop.getItemStack()!=null) { // New Amount Item
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&7Current Selected Amount: &6" + quantity);
            lore.add("&7Shop Stock: &e" + shop.getItemStock());
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fSelection: &6" + quantity));
            item.setItemMeta(meta);
            item.setAmount(1);
            item.setType(Material.GLASS);
            gui.setItem(4, 5, ItemBuilder.from(item).asGuiItem(event -> {
                                                                            event.setCancelled(true);
                                                                        }));
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
            gui.setItem(2, 2, ItemBuilder.from(shopItem).asGuiItem(event -> {
                                                                                event.setCancelled(true);
                                                                            }));
        }

        if (shop.getSellPrice()!=null) { // Sell Button
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&6Click to &csell " + quantity + " &6for $" + StaticUtils.formatDoubleUS((ShopUtils.getShopSellPriceForOne(shop).multiply(BigDecimal.valueOf(quantity))).doubleValue()));
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cConfirm Sell"));
            item.setItemMeta(meta);
            item.setType(Material.RED_BANNER);
            gui.setItem(2, 4, ItemBuilder.from(item).asGuiItem(event -> {
                                                                            event.setCancelled(true);
                                                                            ShopUtils.sellToShop(viewer, shopUuid, quantity);
                                                                            if (!closeGuiAfter) {
                                                                                gui.setCloseGuiAction(null);
                                                                                new ShopTransactionGui(javaPlugin, viewer, shopUuid, quantity, closeGuiAfter);
                                                                            } else {
                                                                                gui.close(viewer);
                                                                            }
                                                                        }));
        }

        if (shop.getBuyPrice()!=null) { // Buy Button
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&6Click to &abuy " + quantity + " &6for $" + StaticUtils.formatDoubleUS((ShopUtils.getShopBuyPriceForOne(shop).multiply(BigDecimal.valueOf(quantity))).doubleValue()));
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aConfirm Buy"));
            item.setItemMeta(meta);
            item.setType(Material.GREEN_BANNER);
            gui.setItem(2, 6, ItemBuilder.from(item).asGuiItem(event -> {
                                                                            event.setCancelled(true);
                                                                            ShopUtils.buyFromShop(viewer, shopUuid, quantity);
                                                                            if (!closeGuiAfter) {
                                                                                gui.setCloseGuiAction(null);
                                                                                new ShopTransactionGui(javaPlugin, viewer, shopUuid, quantity, closeGuiAfter);
                                                                            } else {
                                                                                gui.close(viewer);
                                                                            }
                                                                        }));
        }

        { // Amount Gui Button
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&6Click enter quantity with keyboard");
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dEnter Amount"));
            item.setItemMeta(meta);
            item.setType(Material.WRITABLE_BOOK);
            gui.setItem(6, 5, ItemBuilder.from(item).asGuiItem(event -> {
                                                                            event.setCancelled(true);
                                                                            gui.setCloseGuiAction(null);
                                                                            new ShopAdjustTextGui(javaPlugin, viewer, shopUuid, AdjustAttribute.TRANSACTION, closeGuiAfter);
                                                                        }));
        }

        { // Amount Buttons
            if (quantity>10) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -10, 5, 1);
            if (quantity>5) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -5, 5, 2);
            if (quantity>1) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -1, 5, 3);

            if (quantity>64) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -64, 6, 1);
            if (quantity>32) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -32, 6, 2);

            addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 1, 5, 7);
            addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 5, 5, 8);
            addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 10, 5, 9);

            addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 32, 6, 8);
            addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 64, 6, 9);
        }
    }

    private void addQuantityButton(ItemStack item, ItemMeta meta, List<String> lore, Material material, String name, int amount, int row, int col) {
        lore.clear();
        meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
        if (amount<0) meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cRemove "+(-1*amount)));
        else meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aAdd "+amount));
        item.setItemMeta(meta);
        item.setType(material);
        if (amount<0) item.setAmount((-1*amount));
        else item.setAmount(amount);
        gui.setItem(row, col, ItemBuilder.from(item).asGuiItem(event -> {
                                                                    event.setCancelled(true);
                                                                    gui.setCloseGuiAction(null);
                                                                    new ShopTransactionGui(javaPlugin, viewer, shopUuid, quantity+amount, closeGuiAfter);
                                                                }));
    }
}
