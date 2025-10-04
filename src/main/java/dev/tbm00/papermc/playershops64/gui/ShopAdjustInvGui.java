package dev.tbm00.papermc.playershops64.gui;

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
import dev.tbm00.papermc.playershops64.data.enums.AdjustType;
import dev.tbm00.papermc.playershops64.utils.GuiUtils;
import dev.tbm00.papermc.playershops64.utils.ShopUtils;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class ShopAdjustInvGui {
    private final PlayerShops64 javaPlugin;
    private final Gui gui;
    private final Player viewer;
    private final UUID shopUuid;
    private final Shop shop;
    private final int quantity;
    private final AdjustAttribute attribute;
    private final boolean closeGuiAfter;
    private String label;
    
    public ShopAdjustInvGui(PlayerShops64 javaPlugin, Player viewer, UUID shopUuid, int quantity, AdjustAttribute attribute, boolean closeGuiAfter) {
        this.javaPlugin = javaPlugin;
        this.viewer = viewer;
        this.shopUuid = shopUuid;
        this.shop = javaPlugin.getShopHandler().getShop(shopUuid);
        this.quantity = attribute.equals(AdjustAttribute.DISPLAY_HEIGHT) ? quantity : Math.max(quantity, 1);
        this.gui = new Gui(6, label);
        this.attribute = attribute;
        this.closeGuiAfter = closeGuiAfter;

        String shopHint = shopUuid.toString().substring(0, 6);
        if (javaPlugin.getShopHandler().tryLockShop(shopUuid, viewer)) {
            StaticUtils.log(ChatColor.YELLOW, viewer.getName() + " opened shop "+shopHint+"'s adjust gui: "+AdjustAttribute.toString(attribute));
        } else return;

        switch (attribute) {
            case STOCK: 
                label = "Adjust "+AdjustAttribute.toString(attribute)+" (" + shop.getItemStock()+ " +/-/=> "+quantity+")";
                break;
            case BALANCE: 
                label = "Adjust "+AdjustAttribute.toString(attribute)+" ($" + StaticUtils.formatIntUS(shop.getMoneyStock().doubleValue())+ " +/-/=> $"+StaticUtils.formatIntUS(quantity)+")";
                break;
            case DISPLAY_HEIGHT: 
                label = "Adjust "+AdjustAttribute.toString(attribute)+" (" + shop.getDisplayHeight()+ " +/-/=> "+quantity+")";
                break;
            default:
                label = "Adjust "+AdjustAttribute.toString(attribute);
                break;
        }

        setup();

        gui.updateTitle(label);
        gui.disableAllInteractions();
        gui.open(viewer);
        gui.setCloseGuiAction(event -> {
            StaticUtils.log(ChatColor.GREEN, viewer.getName() + " opened shop "+shopHint+"'s adjust gui: "+AdjustAttribute.toString(attribute));
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
            gui.setItem(1, 9, ItemBuilder.from(item).asGuiItem(event -> {
                                                                        event.setCancelled(true);
                                                                        new ShopAdjustInvGui(javaPlugin, viewer, shopUuid, quantity, attribute, false);}));
        } else {
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&6Click to tell the GUI to close");
            lore.add("&6after each sale");
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dEnable Close After Sale"));
            item.setItemMeta(meta);
            item.setType(Material.GRAY_BANNER);
            gui.setItem(1, 9, ItemBuilder.from(item).asGuiItem(event -> {
                                                                        event.setCancelled(true);
                                                                        new ShopAdjustInvGui(javaPlugin, viewer, shopUuid, quantity, attribute, true);}));
        }

        if (shop.getItemStack()!=null) { // New Amount Item
            lore.clear();
            switch (attribute) {
                case STOCK: 
                    lore.add("&8-----------------------");
                    lore.add("&fShop Stock: &e" + shop.getItemStock());
                    lore.add("&fCurrent Amount: &e" + quantity);
                    item.setAmount(quantity);
                    break;
                case BALANCE: 
                    lore.add("&8-----------------------");
                    lore.add("&fShop Balance: &e$" + StaticUtils.formatDoubleUS(shop.getMoneyStock().doubleValue()));
                    lore.add("&fCurrent Amount: &e$" + StaticUtils.formatIntUS(quantity));
                    item.setAmount(1);
                    break;
                case DISPLAY_HEIGHT: 
                    lore.add("&8-----------------------");
                    lore.add("&fShop Display Height: &e" + shop.getDisplayHeight());
                    lore.add("&fCurrent Amount: &e" + quantity);
                    item.setAmount(Math.max(quantity, -1*quantity));
                    break;
                default:
                    item.setAmount(1);
                    break;
            }

            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            item.setItemMeta(meta);
            item.setType(Material.GLASS);
            gui.setItem(2, 6, ItemBuilder.from(item).asGuiItem(event -> {event.setCancelled(true);}));
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
            gui.setItem(2, 7, ItemBuilder.from(shopItem).asGuiItem(event -> {event.setCancelled(true);}));
        }

        { // Withdraw Button
            if (attribute.equals(AdjustAttribute.STOCK)) {
                lore.clear();
                lore.add("&8-----------------------");
                lore.add("&6Click to withdraw " + quantity + " from shop's stock");
                meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cWithdraw from Shop Stock"));
                item.setItemMeta(meta);
                item.setType(Material.RED_BANNER);
                gui.setItem(2, 2, ItemBuilder.from(item).asGuiItem(event -> {
                                                                            event.setCancelled(true);
                                                                            ShopUtils.adjustStock(viewer, shopUuid, AdjustType.REMOVE, quantity);
                                                                            if (closeGuiAfter) gui.close(viewer);}));
            } else if (attribute.equals(AdjustAttribute.BALANCE)) {
                lore.clear();
                lore.add("&8-----------------------");
                lore.add("&6Click to withdraw $" + StaticUtils.formatIntUS(quantity) + " from shop's balance");
                meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cWithdraw from Shop Balance"));
                item.setItemMeta(meta);
                item.setType(Material.RED_BANNER);
                gui.setItem(2, 2, ItemBuilder.from(item).asGuiItem(event -> {
                                                                            event.setCancelled(true);
                                                                            ShopUtils.adjustBalance(viewer, shopUuid, AdjustType.REMOVE, quantity);
                                                                            if (closeGuiAfter) gui.close(viewer);}));
            }
        }

        { // Set Button
            if (attribute.equals(AdjustAttribute.STOCK)) {
                lore.clear();
                lore.add("&8-----------------------");
                lore.add("&6Click to set shop's stock to " + quantity);
                meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&eSet Shop Stock"));
                item.setItemMeta(meta);
                item.setType(Material.YELLOW_BANNER);
                gui.setItem(2, 3, ItemBuilder.from(item).asGuiItem(event -> {
                                                                            event.setCancelled(true);
                                                                            ShopUtils.adjustStock(viewer, shopUuid, AdjustType.SET, quantity);
                                                                            if (closeGuiAfter) gui.close(viewer);;}));
            } else if (attribute.equals(AdjustAttribute.BALANCE)) {
                lore.clear();
                lore.add("&8-----------------------");
                lore.add("&6Click to set shop's balance to $" + StaticUtils.formatIntUS(quantity));
                meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&eSet Shop Balance"));
                item.setItemMeta(meta);
                item.setType(Material.YELLOW_BANNER);
                gui.setItem(2, 3, ItemBuilder.from(item).asGuiItem(event -> {
                                                                            event.setCancelled(true);
                                                                            ShopUtils.adjustBalance(viewer, shopUuid, AdjustType.SET, quantity);
                                                                            if (closeGuiAfter) gui.close(viewer);}));
            } else if (attribute.equals(AdjustAttribute.DISPLAY_HEIGHT)) {
                lore.clear();
                lore.add("&8-----------------------");
                lore.add("&6Click to set shop's display height to " + quantity);
                meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aSet Display Height"));
                item.setItemMeta(meta);
                item.setType(Material.GREEN_BANNER);
                gui.setItem(2, 3, ItemBuilder.from(item).asGuiItem(event -> {
                                                                            event.setCancelled(true);
                                                                            ShopUtils.setDisplayHeight(viewer, shopUuid, quantity);
                                                                            if (closeGuiAfter) gui.close(viewer);}));
            }
        }

        { // Deposit Button
            if (attribute.equals(AdjustAttribute.STOCK)) {
                lore.clear();
                lore.add("&8-----------------------");
                lore.add("&6Click to deposit " + quantity + " to shop's stock");
                meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aDeposit to Shop Stock"));
                item.setItemMeta(meta);
                item.setType(Material.GREEN_BANNER);
                gui.setItem(2, 4, ItemBuilder.from(item).asGuiItem(event -> {
                                                                            event.setCancelled(true);
                                                                            ShopUtils.adjustStock(viewer, shopUuid, AdjustType.ADD, quantity);
                                                                            if (closeGuiAfter) gui.close(viewer);}));
            } else if (attribute.equals(AdjustAttribute.BALANCE)) {
                lore.clear();
                lore.add("&8-----------------------");
                lore.add("&6Click to deposit $" + StaticUtils.formatIntUS(quantity) + " to shop's balance");
                meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aDeposit to Shop Balance"));
                item.setItemMeta(meta);
                item.setType(Material.GREEN_BANNER);
                gui.setItem(2, 4, ItemBuilder.from(item).asGuiItem(event -> {
                                                                            event.setCancelled(true);
                                                                            ShopUtils.adjustBalance(viewer, shopUuid, AdjustType.ADD, quantity);
                                                                            if (closeGuiAfter) gui.close(viewer);}));
            }
        }

        { // Amount Gui Button
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&6Click enter quantity with keyboard");
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dEnter Amount"));
            item.setItemMeta(meta);
            item.setType(Material.WRITABLE_BOOK);
            gui.setItem(3, 5, ItemBuilder.from(item).asGuiItem(event -> {
                                                                        event.setCancelled(true);
                                                                        // TODO: amount gui to recall this ShopAdjustTextGui()
                                                                        }));
        }


        { // Deposit Buttons
            if (attribute.equals(AdjustAttribute.STOCK)) {
                if (quantity>64) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -64, 4, 1);
                if (quantity>1) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -1, 4, 4);

                if (quantity>32) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -32, 5, 1);
                if (quantity>16) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -16, 5, 2);
                if (quantity>8) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -8, 5, 3);
                if (quantity>4) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -4, 5, 4);

                if (quantity>50) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -50, 6, 1);
                if (quantity>25) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -25, 6, 2);
                if (quantity>10) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -10, 6, 3);
                if (quantity>5) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -5, 6, 4);

                addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 1, 4, 6);
                addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 64, 4, 9);

                addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 4, 5, 6);
                addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 8, 5, 7);
                addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 16, 5, 8);
                addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 32, 5, 9);

                addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 5, 6, 6);
                addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 10, 6, 7);
                addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 25, 6, 8);
                addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 50, 6, 9);
            } else if (attribute.equals(AdjustAttribute.STOCK)) {
                if (quantity>-5) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -1, 4, 4);
                if (quantity<5) addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 1, 4, 6);
            } else if (attribute.equals(AdjustAttribute.BALANCE)) {
                if (quantity>10000) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -100000, 4, 1, 10);
                if (quantity>1) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -1, 4, 4, 1);

                if (quantity>32) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -500, 5, 1, 5);
                if (quantity>16) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -100, 5, 2, 4);
                if (quantity>8) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -50, 5, 3, 3);
                if (quantity>4) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -10, 5, 4, 2);

                if (quantity>50) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -50000, 6, 1, 9);
                if (quantity>25) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -10000, 6, 2, 8);
                if (quantity>10) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -5000, 6, 3, 7);
                if (quantity>5) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -1000, 6, 4, 6);

                addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 1, 4, 6, 1);
                addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 100000, 4, 9, 10);

                addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 10, 5, 6, 2);
                addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 50, 5, 7, 3);
                addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 100, 5, 8, 4);
                addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 500, 5, 9, 5);

                addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 1000, 6, 6, 6);
                addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 5000, 6, 7, 7);
                addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 10000, 6, 8, 8);
                addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 50000, 6, 9, 9);
            }
        }
    }

    private void addQuantityButton(ItemStack item, ItemMeta meta, List<String> lore, Material material, String name, int amount, int row, int col) {
        lore.clear();
        meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
        if (amount>0) meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&f+"+amount));
        else meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&f"+amount));
        item.setItemMeta(meta);
        item.setType(material);
        if (amount<0) item.setAmount((-1*amount));
        else item.setAmount(amount);
        gui.setItem(row, col, ItemBuilder.from(item).asGuiItem(event -> {GuiUtils.openGuiAdjustStock(viewer, shopUuid, quantity+amount, attribute);}));
    }

    private void addQuantityButton(ItemStack item, ItemMeta meta, List<String> lore, Material material, String name, int amount, int row, int col, int size) {
        lore.clear();
        meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
        if (amount>0) meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&f+$"+StaticUtils.formatIntUS(amount)));
        else meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&f$"+StaticUtils.formatIntUS(amount)));
        item.setItemMeta(meta);
        item.setType(material);
        item.setAmount(size);
        gui.setItem(row, col, ItemBuilder.from(item).asGuiItem(event -> {GuiUtils.openGuiAdjustStock(viewer, shopUuid, quantity+amount, attribute);}));
    }
}
