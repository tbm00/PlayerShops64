package dev.tbm00.papermc.playershops64.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.enums.AdjustAttribute;
import dev.tbm00.papermc.playershops64.data.structure.Shop;
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
    private final String label = "Shop Management";
    private String shopHint;
    
    public ShopManageGui(PlayerShops64 javaPlugin, Player viewer, boolean isAdmin, UUID shopUuid) {
        this.javaPlugin = javaPlugin;
        this.viewer = viewer;
        this.isAdmin = isAdmin;
        this.shopUuid = shopUuid;
        this.shop = javaPlugin.getShopHandler().getShop(shopUuid);
        this.gui = new Gui(6, label);
        this.shopHint = shopUuid.toString().substring(0, 6);

        if (!javaPlugin.getShopHandler().tryLockShop(shopUuid, viewer)) {
            return;
        } StaticUtils.log(ChatColor.YELLOW, viewer.getName() + " opened shop "+shopHint+"'s manage gui");

        //label = "Shop Management (" + shopHint+ ")";
        gui.updateTitle(label);
        setup();
        gui.disableAllInteractions();
        gui.setCloseGuiAction(event -> {
            closeAction();
        });

        gui.open(viewer);
    }

    private void closeAction() {
        StaticUtils.log(ChatColor.GREEN, viewer.getName() + " closed shop "+shopHint+"'s manage gui");
        javaPlugin.getShopHandler().unlockShop(shopUuid, viewer.getUniqueId());
    }

    /**
     * Sets up all buttons.
     */
    private void setup() {
        ItemStack item = new ItemStack(Material.GLASS);
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();
        
        if (shop.getItemStack()==null) {
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
                                                                            //gui.close(viewer);
                                                                            viewer.closeInventory();
                                                                            
                                                                            ShopUtils.setShopItem(viewer, shopUuid);
                                                                        }));
        } else { 
            // Sale Item
            ItemStack shopItem = shop.getItemStack();
            ItemMeta shopMeta = shopItem.getItemMeta();
            List<String> shopLore = shopMeta.getLore();

            shopLore = ShopUtils.formatSaleItemLoreText(shop, true);

            shopMeta.setLore(shopLore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            shopMeta.setDisplayName(StaticUtils.getFormattedSaleItemName(shop));
            shopItem.setItemMeta(shopMeta);
            shopItem.setAmount(shop.getStackSize());
            gui.setItem(2, 5, ItemBuilder.from(shopItem).asGuiItem(event -> {
                                                                                event.setCancelled(true);
                                                                            }));
            
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
                                                                            //gui.close(viewer);
                                                                            viewer.closeInventory();

                                                                            ShopUtils.clearShopItem(viewer, shopUuid);
                                                                        }));
        }

        { // Set Sell Price
            lore.clear();
            lore.add("&8-----------------------");
            if (shop.getSellPrice()!=null) {
                lore.add("&6Click to set this shop's sell price");
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&4Sell Price: &f$" + StaticUtils.formatDoubleUS(shop.getSellPrice().doubleValue())));
            } else { 
                lore.add("&6Click to re-enable selling & set this shop's sell price");
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&4Sell Price: &cDISABLED"));
            }
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            item.setItemMeta(meta);
            item.setType(Material.GOLD_INGOT);
            gui.setItem(3, 4, ItemBuilder.from(item).asGuiItem(event -> {
                                                                            event.setCancelled(true);
                                                                            gui.setCloseGuiAction(null);
                                                                            new ShopAdjustTextGui(javaPlugin, viewer, isAdmin, shopUuid, AdjustAttribute.SELL_PRICE, true);
                                                                        }));
        } if (shop.getSellPrice()!=null) {
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
                                                                            Bukkit.getScheduler().runTaskLater(javaPlugin, () -> {
                                                                                gui.setCloseGuiAction(null);
                                                                                new ShopManageGui(javaPlugin, viewer, isAdmin, shopUuid);
                                                                            }, 2L);
                                                                        }));
        }

        { // Set Buy Price
            lore.clear();
            lore.add("&8-----------------------");
            if (shop.getBuyPrice()!=null) {
                lore.add("&6Click to set this shop's buy price");
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&2Buy Price: &f$" + StaticUtils.formatDoubleUS(shop.getBuyPrice().doubleValue())));
            } else { 
                lore.add("&6Click to re-enable buying & set this shop's buy price");
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&2Buy Price: &cDISABLED"));
            }
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            item.setItemMeta(meta);
            item.setType(Material.GOLD_INGOT);
            gui.setItem(3, 6, ItemBuilder.from(item).asGuiItem(event -> {
                                                                            event.setCancelled(true);
                                                                            gui.setCloseGuiAction(null);
                                                                            new ShopAdjustTextGui(javaPlugin, viewer, isAdmin, shopUuid, AdjustAttribute.BUY_PRICE, false);
                                                                        }));
        } if (shop.getBuyPrice()!=null) {
            // Disable Buying
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
                                                                            Bukkit.getScheduler().runTaskLater(javaPlugin, () -> {
                                                                                gui.setCloseGuiAction(null);
                                                                                new ShopManageGui(javaPlugin, viewer, isAdmin, shopUuid);
                                                                            }, 2L);
                                                                        }));
        }

        { // Set Balance
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&6Click to manage this shop's balance");
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            if (shop.getMoneyStock()!=null) 
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&eStored Balance: &f$" + StaticUtils.formatDoubleUS(shop.getMoneyStock().doubleValue())));
            else meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&eStored Balance: &f$(null)"));
            item.setItemMeta(meta);
            item.setType(Material.HOPPER_MINECART);
            gui.setItem(3, 5, ItemBuilder.from(item).asGuiItem(event -> {
                                                                            event.setCancelled(true);
                                                                            gui.setCloseGuiAction(null);
                                                                            new ShopAdjustInvGui(javaPlugin, viewer, isAdmin, shopUuid, 0, AdjustAttribute.BALANCE, false);
                                                                        }));
        }

        { // Set Stock
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&6Click to manage this shop's stock");
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&eStored Stock: &f" + StaticUtils.formatIntUS(shop.getItemStock())));
            item.setItemMeta(meta);
            item.setType(Material.BARREL);
            gui.setItem(4, 5, ItemBuilder.from(item).asGuiItem(event -> {
                                                                            event.setCancelled(true);
                                                                            gui.setCloseGuiAction(null);
                                                                            new ShopAdjustInvGui(javaPlugin, viewer, isAdmin, shopUuid, 0, AdjustAttribute.STOCK, false);
                                                                        }));
        }

        {
            // Set Description
            lore.clear();
            if (shop.getDescription()!=null) {
                lore.add("&8-----------------------");
                lore.add("&eCurrent description:");
                lore.add("&7'"+shop.getDescription()+"'");
            }
            lore.add("&8-----------------------");
            lore.add("&6Click to set this shop's description");
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dSet Description"));
            item.setItemMeta(meta);
            item.setType(Material.PAPER);
            gui.setItem(6, 4, ItemBuilder.from(item).asGuiItem(event -> {
                                                                            event.setCancelled(true);
                                                                            gui.setCloseGuiAction(null);
                                                                            new ShopAdjustTextGui(javaPlugin, viewer, isAdmin, shopUuid, AdjustAttribute.DESCRIPTION, false);
                                                                        }));
        } if (shop.getDescription()!=null) {
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
                                                                            ShopUtils.setDescription(viewer, shopUuid, null);
                                                                            Bukkit.getScheduler().runTaskLater(javaPlugin, () -> {
                                                                                gui.setCloseGuiAction(null);
                                                                                new ShopManageGui(javaPlugin, viewer, isAdmin, shopUuid);
                                                                            }, 2L);
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
                                                                            gui.setCloseGuiAction(null);
                                                                            new ShopAdjustInvGui(javaPlugin, viewer, isAdmin, shopUuid, 0, AdjustAttribute.DISPLAY_HEIGHT, false);
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
                                                                            gui.setCloseGuiAction(null);
                                                                            new ListMaterialsGui(javaPlugin, viewer, shopUuid, isAdmin);
                                                                        }));
        }

        {
            // Manage Assistants
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&7Assistants can modify the shop entirely!");
            if (!shop.getAssistants().isEmpty()) {
                lore.add("&8-----------------------");
                lore.add("&7Assistants:");
                for (UUID id : shop.getAssistants()) {
                    lore.add("&7 - " + javaPlugin.getServer().getOfflinePlayer(id).getName());
                }
            }
            lore.add("&8-----------------------");
            lore.add("&6Click to manage this shop's assistants");
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dManage Assistants"));
            item.setItemMeta(meta);
            item.setType(Material.PLAYER_HEAD);
            gui.setItem(1, 1, ItemBuilder.from(item).asGuiItem(event -> {
                                                                            event.setCancelled(true);
                                                                            gui.setCloseGuiAction(null);
                                                                            new ListAssistantsGui(javaPlugin, viewer, shopUuid, isAdmin);
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
            gui.setItem(1, 9, ItemBuilder.from(item).asGuiItem(event -> {
                                                                            event.setCancelled(true);
                                                                            //gui.close(viewer);
                                                                            viewer.closeInventory();
                                                                            
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
            gui.setItem(6, 9, ItemBuilder.from(item).asGuiItem(event -> {
                                                                            event.setCancelled(true);
                                                                            gui.setCloseGuiAction(null);
                                                                            closeAction();
                                                                            GuiUtils.handleClickYourShops(event, isAdmin);
                                                                        }));
        } 
    }
}
