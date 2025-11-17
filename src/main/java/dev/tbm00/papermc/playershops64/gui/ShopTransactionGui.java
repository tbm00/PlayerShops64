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
import dev.tbm00.papermc.playershops64.data.enums.AdjustAttribute;
import dev.tbm00.papermc.playershops64.data.enums.QueryType;
import dev.tbm00.papermc.playershops64.data.enums.SortType;
import dev.tbm00.papermc.playershops64.data.structure.Shop;
import dev.tbm00.papermc.playershops64.utils.ShopUtils;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class ShopTransactionGui {
    private final PlayerShops64 javaPlugin;
    private final Gui gui;
    private final Player viewer;
    private final boolean isAdmin;
    private final UUID shopUuid;
    private final Shop shop;
    private final int quantity;
    private final boolean closeGuiAfter;
    private String label = "Shop Transaction";
    
    public ShopTransactionGui(PlayerShops64 javaPlugin, Player viewer, boolean isAdmin, UUID shopUuid, Integer quantity, boolean closeGuiAfter) {
        this.javaPlugin = javaPlugin;
        this.viewer = viewer;
        this.isAdmin = isAdmin;
        this.shopUuid = shopUuid;
        this.shop = javaPlugin.getShopHandler().getShop(shopUuid);
        this.quantity = (quantity==null) ? 0 : Math.max(quantity, 0);
        this.gui = new Gui(6, label);
        this.closeGuiAfter = closeGuiAfter;

        if (shop.getItemStack()==null) {
            StaticUtils.sendMessage(viewer, "&cError: This shop does not have a sale item set!");
            return;
        }

        if (!javaPlugin.getShopHandler().tryLockShop(shopUuid, viewer)) {
            return;
        } StaticUtils.log(ChatColor.YELLOW, viewer.getName() + " opened shop "+ShopUtils.getShopHint(shopUuid)+"'s transcation gui: " + this.quantity);

        label = "Shop Transaction: " + StaticUtils.formatIntUS(this.quantity);
        gui.updateTitle(label);
        setup();
        gui.disableAllInteractions();
        gui.setCloseGuiAction(event -> {
            StaticUtils.log(ChatColor.GREEN, viewer.getName() + " closed shop "+ShopUtils.getShopHint(shopUuid)+"'s transaction gui: " + this.quantity);
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
                                                                            new ShopTransactionGui(javaPlugin, viewer, isAdmin, shopUuid, quantity, false);
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
                                                                            new ShopTransactionGui(javaPlugin, viewer, isAdmin, shopUuid, quantity, true);
                                                                        }));
        }

        if (shop.getItemStack()!=null) { //  Similar Search Button
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&6Click to find similar shops");
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dSimilar Shops"));
            item.setItemMeta(meta);
            item.setType(Material.STONE_BUTTON);
            gui.setItem(2, 2, ItemBuilder.from(item).asGuiItem(event -> {
                                                                            event.setCancelled(true);
                                                                            new ListShopsGui(javaPlugin, javaPlugin.getShopHandler().getShopView(), viewer, isAdmin, SortType.MATERIAL, QueryType.STRING, shop.getItemStack().getType().toString());
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
            gui.setItem(3, 5, ItemBuilder.from(item).asGuiItem(event -> {
                                                                            event.setCancelled(true);
                                                                        }));
            item.setAmount(1);
        }
        
        if (shop.getItemStack()!=null) { // Sale Item
            ItemStack shopItem = shop.getItemStack();
            ItemMeta shopMeta = shopItem.getItemMeta();
            List<String> shopLore = shopMeta.getLore();

            shopLore = ShopUtils.formatSaleItemLoreText(shop, true);
            shopItem.setAmount(shop.getStackSize());
            shopMeta.setDisplayName(StaticUtils.getItemName(shop.getItemStack()));
            shopMeta.setLore(shopLore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            shopItem.setItemMeta(shopMeta);
            gui.setItem(2, 5, ItemBuilder.from(shopItem).asGuiItem(event -> {
                                                                                event.setCancelled(true);
                                                                            }));
        }

        if (shop.getSellPrice()!=null) { // Sell Button
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&6Click to &csell " + quantity + " &6for $" + StaticUtils.formatDoubleUS((shop.getSellPriceForOne().multiply(BigDecimal.valueOf(quantity))).doubleValue()));
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cConfirm Sell"));
            item.setItemMeta(meta);
            item.setType(Material.RED_BANNER);
            gui.setItem(3, 4, ItemBuilder.from(item).asGuiItem(event -> {
                                                                            event.setCancelled(true);
                                                                            ShopUtils.sellToShop(viewer, shopUuid, quantity);
                                                                            if (!closeGuiAfter) {
                                                                                gui.setCloseGuiAction(null);
                                                                                new ShopTransactionGui(javaPlugin, viewer, isAdmin, shopUuid, quantity, closeGuiAfter);
                                                                            } else {
                                                                                //gui.close(viewer);
                                                                                gui.setCloseGuiAction(null);
                                                                                viewer.closeInventory();
                                                                                StaticUtils.log(ChatColor.GREEN, viewer.getName() + " closed shop "+ShopUtils.getShopHint(shopUuid)+"'s transaction gui: " + this.quantity);
                                                                            }
                                                                        }));
        }

        if (shop.getBuyPrice()!=null) { // Buy Button
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&6Click to &abuy " + quantity + " &6for $" + StaticUtils.formatDoubleUS((shop.getBuyPriceForOne().multiply(BigDecimal.valueOf(quantity))).doubleValue()));
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aConfirm Buy"));
            item.setItemMeta(meta);
            item.setType(Material.GREEN_BANNER);
            gui.setItem(3, 6, ItemBuilder.from(item).asGuiItem(event -> {
                                                                            event.setCancelled(true);
                                                                            ShopUtils.buyFromShop(viewer, shopUuid, quantity);
                                                                            if (!closeGuiAfter) {
                                                                                gui.setCloseGuiAction(null);
                                                                                new ShopTransactionGui(javaPlugin, viewer, isAdmin, shopUuid, quantity, closeGuiAfter);
                                                                            } else {
                                                                                //gui.close(viewer);
                                                                                gui.setCloseGuiAction(null);
                                                                                viewer.closeInventory();
                                                                                StaticUtils.log(ChatColor.GREEN, viewer.getName() + " closed shop "+ShopUtils.getShopHint(shopUuid)+"'s transaction gui: " + this.quantity);
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
                                                                            new ShopAdjustTextGui(javaPlugin, viewer, isAdmin, shopUuid, AdjustAttribute.TRANSACTION, closeGuiAfter);
                                                                        }));
        }

        { // Amount Buttons
            addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -10, 5, 1);
            addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -5, 5, 2);
            addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -1, 5, 3);
            addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -64, 6, 1);
            addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -32, 6, 2);

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
                                                                    new ShopTransactionGui(javaPlugin, viewer, isAdmin, shopUuid, quantity+amount, closeGuiAfter);
                                                                }));
    }
}
