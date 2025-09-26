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
import dev.tbm00.papermc.playershops64.utils.GuiUtils;
import dev.tbm00.papermc.playershops64.utils.ShopUtils;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class ShopManageGui {
    private final PlayerShops64 javaPlugin;
    private final Gui gui;
    private final Player viewer;
    private final boolean isAdmin;
    private final UUID shopUuid;
    private final Shop shop;
    private String label;
    
    public ShopManageGui(PlayerShops64 javaPlugin, Player viewer, boolean isAdmin, UUID shopUuid) {
        this.javaPlugin = javaPlugin;
        this.viewer = viewer;
        this.isAdmin = isAdmin;
        this.shopUuid = shopUuid;
        this.shop = javaPlugin.getShopHandler().getShop(shopUuid);
        this.gui = new Gui(6, label);

        String shopHint = shopUuid.toString().substring(0, 6);
        if (javaPlugin.getShopHandler().tryLockShop(shopUuid, viewer)) {
            StaticUtils.log(ChatColor.YELLOW, viewer.getName() + " opened shop "+shopHint+"'s manage gui");
        } else return;

        label = "Shop Management (" + shopHint+ ")";

        setup();

        gui.updateTitle(label);
        gui.disableAllInteractions();
        gui.open(viewer);
        gui.setCloseGuiAction(event -> {
            StaticUtils.log(ChatColor.GREEN, viewer.getName() + " closed shop "+shopHint+"'s manage gui");
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
        
        if (shop.getItemStack()==null || shop.getItemStack().equals(null)) {
            // Null Sale Item
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&6Click to set this shop's sale item");
            lore.add("&6to the item currently in your hand");
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cNo Sale Item"));
            item.setItemMeta(meta);
            item.setType(Material.BARRIER);
            gui.setItem(2, 5, ItemBuilder.from(item).asGuiItem(event -> {
                                                                        event.setCancelled(true);
                                                                        ShopUtils.setShopItem(viewer, shopUuid);
                                                                        }));                                   
        } else { 
            // Sale Item
            ItemStack shopItem = shop.getItemStack();
            if (shop.getStackSize()!=1) {
                ItemMeta shopMeta = shopItem.getItemMeta();
                shopMeta.setDisplayName(StaticUtils.getItemName(shopItem) + " &7x " + shop.getStackSize());
                shopItem.setItemMeta(shopMeta);
            } 
            shopItem.setAmount(shop.getStackSize());
            gui.setItem(2, 5, ItemBuilder.from(shopItem).asGuiItem(event -> {event.setCancelled(true);}));
            
            // Clear Sale Item
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&6Click to clear this shop's sale item");
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cClear Sale Item"));
            item.setItemMeta(meta);
            item.setType(Material.RED_BANNER);
            gui.setItem(1, 5, ItemBuilder.from(item).asGuiItem(event -> {
                                                                        event.setCancelled(true);
                                                                        ShopUtils.clearShopItem(viewer, shopUuid);
                                                                        }));
        }

        { // Set Sell Price
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&6Click to set this shop's sell price");
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            if (shop.getSellPrice()!=null && !shop.getSellPrice().equals(null)) 
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&4Sell Price: &f$" + StaticUtils.formatDoubleUS(shop.getSellPrice().doubleValue())));
            else meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&4Sell Price: &cDISABLED"));
            item.setItemMeta(meta);
            item.setType(Material.GOLD_INGOT);
            gui.setItem(3, 4, ItemBuilder.from(item).asGuiItem(event -> {
                                                                        event.setCancelled(true);
                                                                        // TODO: set sell price (AdjustType.ADD, AdjustType.REMOVE, or AdjustType.SET) then ModType.SELL_PRICE
                                                                        }));
        } if (shop.getSellPrice()!=null && !shop.getSellPrice().equals(null)) {
            // Disable Selling
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&6Click to prevent players");
            lore.add("&6from selling to this shop");
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cDisable Selling to Shop"));
            item.setItemMeta(meta);
            item.setType(Material.RED_BANNER);
            gui.setItem(3, 3, ItemBuilder.from(item).asGuiItem(event -> {
                                                                        event.setCancelled(true);
                                                                        ShopUtils.setSellPrice(viewer, shopUuid, null);
                                                                        }));                       
        }

        { // Set Buy Price
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&6Click to set this shop's buy price");
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            if (shop.getBuyPrice()!=null && !shop.getBuyPrice().equals(null)) 
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&2Buy Price: &f$" + StaticUtils.formatDoubleUS(shop.getBuyPrice().doubleValue())));
            else meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&2Buy Price: &cDISABLED"));
            item.setItemMeta(meta);
            item.setType(Material.GOLD_INGOT);
            gui.setItem(3, 6, ItemBuilder.from(item).asGuiItem(event -> {
                                                                        event.setCancelled(true);
                                                                        // TODO: set buy price (AdjustType.ADD, AdjustType.REMOVE, or AdjustType.SET) then ModType.BUY_PRICE
                                                                        }));
        } if (shop.getBuyPrice()!=null && !shop.getBuyPrice().equals(null)) {
            // Disable Selling
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&6Click to prevent players");
            lore.add("&6from buying from this shop");
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cDisable Buying from Shop"));
            item.setItemMeta(meta);
            item.setType(Material.RED_BANNER);
            gui.setItem(3, 7, ItemBuilder.from(item).asGuiItem(event -> {
                                                                        event.setCancelled(true);
                                                                        ShopUtils.setBuyPrice(viewer, shopUuid, null);
                                                                        }));                       
        }

        { // Set Balance
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&6Click to set this shop's balance");
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            if (shop.getBuyPrice()!=null && !shop.getBuyPrice().equals(null)) 
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&eStored Balance: &f$" + StaticUtils.formatDoubleUS(shop.getMoneyStock().doubleValue())));
            item.setItemMeta(meta);
            item.setType(Material.HOPPER_MINECART);
            gui.setItem(3, 5, ItemBuilder.from(item).asGuiItem(event -> {
                                                                        event.setCancelled(true);
                                                                        // TODO: set balance (AdjustType.ADD, AdjustType.REMOVE, or AdjustType.SET) then ModType.BALANCE
                                                                        }));
        }

        { // Set Stock
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&6Click to set this shop's stock");
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            if (shop.getBuyPrice()!=null && !shop.getBuyPrice().equals(null)) 
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&eStored Stock: &f" + StaticUtils.formatIntUS(shop.getItemStock())));
            item.setItemMeta(meta);
            item.setType(Material.BARREL);
            gui.setItem(4, 5, ItemBuilder.from(item).asGuiItem(event -> {
                                                                        event.setCancelled(true);
                                                                        // TODO: set stock (AdjustType.ADD, AdjustType.REMOVE, or AdjustType.SET) then ModType.STOCK
                                                                        }));
        }

        {
            // Set Description
            lore.clear();
            if (shop.getDescription()!=null && !shop.getDescription().isBlank()) {
                lore.add("&8-----------------------");
                lore.add("&eCurrent description:");
                lore.add(shop.getDescription());
            }
            lore.add("&8-----------------------");
            lore.add("&6Click to set this shop's description");
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dSet Description"));
            item.setItemMeta(meta);
            item.setType(Material.PAPER);
            gui.setItem(6, 4, ItemBuilder.from(item).asGuiItem(event -> {
                                                                        event.setCancelled(true);
                                                                        // TODO: set shop description
                                                                        }));
        } if (shop.getDescription()!=null && !shop.getDescription().equals(null)) {
            // Clear Description
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&6Click to clear this shop's description");
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cClear Description"));
            item.setItemMeta(meta);
            item.setType(Material.RED_BANNER);
            gui.setItem(6, 3, ItemBuilder.from(item).asGuiItem(event -> {
                                                                        event.setCancelled(true);
                                                                        // TODO: clear shop description (set desc to null)
                                                                        }));                       
        }

        {
            // Set Display Height
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&eCurrent height: "+shop.getDisplayHeight());
            lore.add("&8-----------------------");
            lore.add("&6Click to set this shop's display height");
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dSet Display Height"));
            item.setItemMeta(meta);
            item.setType(Material.GLASS);
            gui.setItem(6, 5, ItemBuilder.from(item).asGuiItem(event -> {
                                                                        event.setCancelled(true);
                                                                        // TODO: set shop display height
                                                                        }));
        }

        {
            // Set Base Material
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&eCurrent material: " + StaticUtils.formatTitleCase(shop.getBaseMaterial().toString()));
            lore.add("&8-----------------------");
            lore.add("&6Click to set this shop's base block");
            lore.add("&6material type");
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dSet Base Material"));
            item.setItemMeta(meta);
            item.setType(Material.LECTERN);
            gui.setItem(6, 6, ItemBuilder.from(item).asGuiItem(event -> {
                                                                        event.setCancelled(true);
                                                                        // TODO: set shop base material
                                                                        }));
        }

        {
            // Destroy Shop
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&6Click to &cDESTROY this shop");
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&4Destroy Shop"));
            item.setItemMeta(meta);
            item.setType(Material.BARRIER);
            gui.setItem(1, 6, ItemBuilder.from(item).asGuiItem(event -> {
                                                                        event.setCancelled(true);
                                                                        ShopUtils.deleteShop(viewer, shopUuid, null);
                                                                        }));
        } 

        {
            // Your Shops
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&6Click to view your shops");
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dYour Shops"));
            item.setItemMeta(meta);
            item.setType(Material.STONE_BUTTON);
            gui.setItem(9, 6, ItemBuilder.from(item).asGuiItem(event -> {
                                                                        event.setCancelled(true);
                                                                        GuiUtils.handleClickYourShops(event, isAdmin);
                                                                        }));
        } 
    }
}
