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
import dev.tbm00.papermc.playershops64.data.enums.AdjustType;
import dev.tbm00.papermc.playershops64.data.structure.Shop;
import dev.tbm00.papermc.playershops64.utils.ShopUtils;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class ShopAdjustInvGui {
    private final PlayerShops64 javaPlugin;
    private final Gui gui;
    private final Player viewer;
    private final boolean isAdmin;
    private final UUID shopUuid;
    private final Shop shop;
    private final int quantity;
    private final AdjustAttribute attribute;
    private final boolean closeGuiAfter;
    private String label = "Adjust Shop";
    
    public ShopAdjustInvGui(PlayerShops64 javaPlugin, Player viewer, boolean isAdmin, UUID shopUuid, int quantity, AdjustAttribute attribute, boolean closeGuiAfter) {
        this.javaPlugin = javaPlugin;
        this.viewer = viewer;
        this.isAdmin = isAdmin;
        this.shopUuid = shopUuid;
        this.shop = javaPlugin.getShopHandler().getShop(shopUuid);
        this.quantity = attribute.equals(AdjustAttribute.DISPLAY_HEIGHT) ? quantity : Math.max(quantity, 0);
        this.attribute = attribute;
        this.closeGuiAfter = closeGuiAfter;
        this.gui = new Gui(6, label);
        
        if (!javaPlugin.getShopHandler().tryLockShop(shopUuid, viewer)) {
            return;
        } //StaticUtils.log(ChatColor.YELLOW, viewer.getName() + " opened shop "+ShopUtils.getShopHint(shopUuid)+"'s adjust inv gui: "+AdjustAttribute.toString(attribute));

        label = "Adjust "+AdjustAttribute.toString(attribute);
        switch (attribute) {
            case BALANCE:
                label += ": $" + StaticUtils.formatUS(this.quantity);
                break;
            case STOCK:
                label += ": " + StaticUtils.formatUS(this.quantity);
                break;
            case DISPLAY_HEIGHT:
                label += ": " + this.quantity;
                break;
            default:
                break;
        }
        gui.updateTitle(label);
        setup();
        gui.disableAllInteractions();
        gui.setCloseGuiAction(event -> {
            //StaticUtils.log(ChatColor.GREEN, viewer.getName() + " closed shop "+ShopUtils.getShopHint(shopUuid)+"'s adjust inv gui: "+AdjustAttribute.toString(attribute));
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
            lore.add("&6after each adjustment");
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dDisable Close After Adjust"));
            item.setItemMeta(meta);
            item.setType(Material.LIGHT_GRAY_BANNER);
            gui.setItem(2, 8, ItemBuilder.from(item).asGuiItem(event -> {
                                                                            event.setCancelled(true);
                                                                            gui.setCloseGuiAction(null);
                                                                            new ShopAdjustInvGui(javaPlugin, viewer, isAdmin, shopUuid, quantity, attribute, false);
                                                                        }));
        } else {
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&6Click to tell the GUI to close");
            lore.add("&6after each adjustment");
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dEnable Close After Adjust"));
            item.setItemMeta(meta);
            item.setType(Material.GRAY_BANNER);
            gui.setItem(2, 8, ItemBuilder.from(item).asGuiItem(event -> {
                                                                            event.setCancelled(true);
                                                                            gui.setCloseGuiAction(null);
                                                                            new ShopAdjustInvGui(javaPlugin, viewer, isAdmin, shopUuid, quantity, attribute, true);
                                                                        }));
        }

        { // Back Button
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&6Click to go back to the manage GUI");
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dExit to Manage GUI"));
            item.setItemMeta(meta);
            item.setType(Material.POLISHED_BLACKSTONE_BUTTON);
            gui.setItem(2, 9, ItemBuilder.from(item).asGuiItem(event -> {
                                                                            event.setCancelled(true);
                                                                            gui.setCloseGuiAction(null);
                                                                            new ShopManageGui(javaPlugin, viewer, false, shopUuid);
                                                                        }));
        }

        { // New Amount Item
            lore.clear();
            switch (attribute) {
                case STOCK: 
                    lore.add("&8-----------------------");
                    lore.add("&7Current Selected Amount: &6" + StaticUtils.formatUS(quantity));
                    lore.add("&7Current Stock: &e" + StaticUtils.formatUS(shop.getItemStock()));
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fSelection: &6" + StaticUtils.formatUS(quantity)));
                    break;
                case BALANCE: 
                    lore.add("&8-----------------------");
                    lore.add("&7Current Selected Amount: &6$" + StaticUtils.formatUS(quantity));
                    lore.add("&7Current Balance: &e$" + StaticUtils.formatUS(shop.getMoneyStock().doubleValue()));
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fSelection: &6$" + StaticUtils.formatUS(quantity)));
                    break;
                case DISPLAY_HEIGHT: 
                    lore.add("&8-----------------------");
                    lore.add("&7Current Selected Amount: &6" + quantity);
                    lore.add("&7Current Display Height: &e" + shop.getDisplayHeight());
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fSelection: &6" + quantity));
                    break;
                default:
                    break;
            }

            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            item.setItemMeta(meta);
            item.setAmount(1);
            item.setType(Material.GLASS);
            gui.setItem(4, 5, ItemBuilder.from(item).asGuiItem(event -> {
                                                                            event.setCancelled(true);
                                                                        }));
        }

        if (shop.getItemStack()!=null) { // Sale Item
            ItemStack shopItem = shop.getItemStack();
            ItemMeta shopMeta = shopItem.getItemMeta();
            List<String> shopLore = shopMeta.getLore();

            shopLore = ShopUtils.formatSaleItemLoreText(shop, true);

            shopMeta.setLore(shopLore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            shopMeta.setDisplayName(StaticUtils.getSaleItemNameWithQuantity(shop));
            shopItem.setItemMeta(shopMeta);
            shopItem.setAmount(shop.getStackSize());
            gui.setItem(2, 2, ItemBuilder.from(shopItem).asGuiItem(event -> {
                                                                                event.setCancelled(true);
                                                                            }));
        }

        { // Withdraw Button
            if (attribute.equals(AdjustAttribute.STOCK)) {
                lore.clear();
                lore.add("&8-----------------------");
                lore.add("&6Click to withdraw " + StaticUtils.formatUS(quantity) + " from shop's stock");
                meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cWithdraw from Shop Stock"));
                item.setItemMeta(meta);
                item.setType(Material.RED_BANNER);
                gui.setItem(2, 4, ItemBuilder.from(item).asGuiItem(event -> {
                                                                                event.setCancelled(true);
                                                                                ShopUtils.adjustStock(viewer, shopUuid, AdjustType.REMOVE, quantity);
                                                                                if (!closeGuiAfter) {
                                                                                    gui.setCloseGuiAction(null);
                                                                                    Bukkit.getScheduler().runTaskLater(javaPlugin, () -> {
                                                                                        new ShopAdjustInvGui(javaPlugin, viewer, isAdmin, shopUuid, quantity, attribute, closeGuiAfter);
                                                                                    }, 2L);
                                                                                } else {
                                                                                    gui.setCloseGuiAction(null);
                                                                                    //gui.close(viewer);
                                                                                    viewer.closeInventory();
                                                                                    //StaticUtils.log(ChatColor.GREEN, viewer.getName() + " closed shop "+ShopUtils.getShopHint(shopUuid)+"'s adjust inv gui: "+AdjustAttribute.toString(attribute));
                                                                                }
                                                                            }));
            } else if (attribute.equals(AdjustAttribute.BALANCE)) {
                lore.clear();
                lore.add("&8-----------------------");
                lore.add("&6Click to withdraw $" + StaticUtils.formatUS(quantity) + " from shop's balance");
                meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cWithdraw from Shop Balance"));
                item.setItemMeta(meta);
                item.setType(Material.RED_BANNER);
                gui.setItem(2, 4, ItemBuilder.from(item).asGuiItem(event -> {
                                                                                event.setCancelled(true);
                                                                                ShopUtils.adjustBalance(viewer, shopUuid, AdjustType.REMOVE, quantity, true);
                                                                                if (!closeGuiAfter) {
                                                                                    gui.setCloseGuiAction(null);
                                                                                    Bukkit.getScheduler().runTaskLater(javaPlugin, () -> {
                                                                                        new ShopAdjustInvGui(javaPlugin, viewer, isAdmin, shopUuid, quantity, attribute, closeGuiAfter);
                                                                                    }, 2L);
                                                                                } else {
                                                                                    gui.setCloseGuiAction(null);
                                                                                    //gui.close(viewer);
                                                                                    viewer.closeInventory();
                                                                                    //StaticUtils.log(ChatColor.GREEN, viewer.getName() + " closed shop "+ShopUtils.getShopHint(shopUuid)+"'s adjust inv gui: "+AdjustAttribute.toString(attribute));
                                                                                }
                                                                            }));
            }
        }

        { // Set Button
            if (attribute.equals(AdjustAttribute.STOCK)) {
                lore.clear();
                lore.add("&8-----------------------");
                lore.add("&6Click to set shop's stock to " + StaticUtils.formatUS(quantity));
                meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&eSet Shop Stock"));
                item.setItemMeta(meta);
                item.setType(Material.YELLOW_BANNER);
                gui.setItem(2, 5, ItemBuilder.from(item).asGuiItem(event -> {
                                                                                event.setCancelled(true);
                                                                                ShopUtils.adjustStock(viewer, shopUuid, AdjustType.SET, quantity);
                                                                                if (!closeGuiAfter) {
                                                                                    gui.setCloseGuiAction(null);
                                                                                    Bukkit.getScheduler().runTaskLater(javaPlugin, () -> {
                                                                                        new ShopAdjustInvGui(javaPlugin, viewer, isAdmin, shopUuid, quantity, attribute, closeGuiAfter);
                                                                                    }, 2L);
                                                                                } else {
                                                                                    gui.setCloseGuiAction(null);
                                                                                    //gui.close(viewer);
                                                                                    viewer.closeInventory();
                                                                                    //StaticUtils.log(ChatColor.GREEN, viewer.getName() + " closed shop "+ShopUtils.getShopHint(shopUuid)+"'s adjust inv gui: "+AdjustAttribute.toString(attribute));
                                                                                }
                                                                            }));
            } else if (attribute.equals(AdjustAttribute.BALANCE)) {
                lore.clear();
                lore.add("&8-----------------------");
                lore.add("&6Click to set shop's balance to $" + StaticUtils.formatUS(quantity));
                meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&eSet Shop Balance"));
                item.setItemMeta(meta);
                item.setType(Material.YELLOW_BANNER);
                gui.setItem(2, 5, ItemBuilder.from(item).asGuiItem(event -> {
                                                                                event.setCancelled(true);
                                                                                ShopUtils.adjustBalance(viewer, shopUuid, AdjustType.SET, quantity, true);
                                                                                if (!closeGuiAfter) {
                                                                                    gui.setCloseGuiAction(null);
                                                                                    Bukkit.getScheduler().runTaskLater(javaPlugin, () -> {
                                                                                        new ShopAdjustInvGui(javaPlugin, viewer, isAdmin, shopUuid, quantity, attribute, closeGuiAfter);
                                                                                    }, 2L);
                                                                                } else {
                                                                                    gui.setCloseGuiAction(null);
                                                                                    //gui.close(viewer);
                                                                                    viewer.closeInventory();
                                                                                    //StaticUtils.log(ChatColor.GREEN, viewer.getName() + " closed shop "+ShopUtils.getShopHint(shopUuid)+"'s adjust inv gui: "+AdjustAttribute.toString(attribute));
                                                                                }
                                                                            }));
            } else if (attribute.equals(AdjustAttribute.DISPLAY_HEIGHT)) {
                lore.clear();
                lore.add("&8-----------------------");
                lore.add("&6Click to set shop's display height to " + quantity);
                meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aSet Display Height"));
                item.setItemMeta(meta);
                item.setType(Material.GREEN_BANNER);
                gui.setItem(2, 5, ItemBuilder.from(item).asGuiItem(event -> {
                                                                                event.setCancelled(true);
                                                                                ShopUtils.setDisplayHeight(viewer, shopUuid, quantity);
                                                                                if (!closeGuiAfter) {
                                                                                    gui.setCloseGuiAction(null);
                                                                                    Bukkit.getScheduler().runTaskLater(javaPlugin, () -> {
                                                                                        new ShopAdjustInvGui(javaPlugin, viewer, isAdmin, shopUuid, quantity, attribute, closeGuiAfter);
                                                                                    }, 2L);
                                                                                } else {
                                                                                    gui.setCloseGuiAction(null);
                                                                                    //gui.close(viewer);
                                                                                    viewer.closeInventory();
                                                                                    //StaticUtils.log(ChatColor.GREEN, viewer.getName() + " closed shop "+ShopUtils.getShopHint(shopUuid)+"'s adjust inv gui: "+AdjustAttribute.toString(attribute));
                                                                                }
                                                                            }));
            }
        }

        { // Deposit Button
            if (attribute.equals(AdjustAttribute.STOCK)) {
                lore.clear();
                lore.add("&8-----------------------");
                lore.add("&6Click to deposit " + StaticUtils.formatUS(quantity) + " to shop's stock");
                meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aDeposit to Shop Stock"));
                item.setItemMeta(meta);
                item.setType(Material.GREEN_BANNER);
                gui.setItem(2, 6, ItemBuilder.from(item).asGuiItem(event -> {
                                                                                event.setCancelled(true);
                                                                                ShopUtils.adjustStock(viewer, shopUuid, AdjustType.ADD, quantity);
                                                                                if (!closeGuiAfter) {
                                                                                    gui.setCloseGuiAction(null);
                                                                                    Bukkit.getScheduler().runTaskLater(javaPlugin, () -> {
                                                                                        new ShopAdjustInvGui(javaPlugin, viewer, isAdmin, shopUuid, quantity, attribute, closeGuiAfter);
                                                                                    }, 2L);
                                                                                } else {
                                                                                    gui.setCloseGuiAction(null);
                                                                                    //gui.close(viewer);
                                                                                    viewer.closeInventory();
                                                                                    //StaticUtils.log(ChatColor.GREEN, viewer.getName() + " closed shop "+ShopUtils.getShopHint(shopUuid)+"'s adjust inv gui: "+AdjustAttribute.toString(attribute));
                                                                                }
                                                                            }));
            } else if (attribute.equals(AdjustAttribute.BALANCE)) {
                lore.clear();
                lore.add("&8-----------------------");
                lore.add("&6Click to deposit $" + StaticUtils.formatUS(quantity) + " to shop's balance");
                meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aDeposit to Shop Balance"));
                item.setItemMeta(meta);
                item.setType(Material.GREEN_BANNER);
                gui.setItem(2, 6, ItemBuilder.from(item).asGuiItem(event -> {
                                                                                event.setCancelled(true);
                                                                                ShopUtils.adjustBalance(viewer, shopUuid, AdjustType.ADD, quantity, true);
                                                                                if (!closeGuiAfter) {
                                                                                    gui.setCloseGuiAction(null);
                                                                                    Bukkit.getScheduler().runTaskLater(javaPlugin, () -> {
                                                                                        new ShopAdjustInvGui(javaPlugin, viewer, isAdmin, shopUuid, quantity, attribute, closeGuiAfter);
                                                                                    }, 2L);
                                                                                } else {
                                                                                    gui.setCloseGuiAction(null);
                                                                                    //gui.close(viewer);
                                                                                    viewer.closeInventory();
                                                                                    //StaticUtils.log(ChatColor.GREEN, viewer.getName() + " closed shop "+ShopUtils.getShopHint(shopUuid)+"'s adjust inv gui: "+AdjustAttribute.toString(attribute));
                                                                                }
                                                                            }));
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
            gui.setItem(6, 5, ItemBuilder.from(item).asGuiItem(event -> {
                                                                            event.setCancelled(true);
                                                                            gui.setCloseGuiAction(null);
                                                                            new ShopAdjustTextGui(javaPlugin, viewer, isAdmin, shopUuid, attribute, closeGuiAfter);
                                                                        }));
        }


        { // Amount Buttons
            if (attribute.equals(AdjustAttribute.STOCK)) {
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
            } else if (attribute.equals(AdjustAttribute.DISPLAY_HEIGHT)) {
                if (quantity>-8) addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -1, 5, 3);
                if (quantity<8) addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 1, 5, 7);
            } else if (attribute.equals(AdjustAttribute.BALANCE)) {
                addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -1, 4, 3, 1);
                addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -10, 4, 2, 2);
                addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -100, 4, 1, 3);
                addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -1000, 5, 3, 4);
                addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -10000, 5, 2, 5);
                addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -100000, 5, 1, 6);
                addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -1000000, 6, 2, 7);
                addQuantityButton(item, meta, lore, Material.CRIMSON_BUTTON, label, -10000000, 6, 1, 8);

                addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 1, 4, 7, 1);
                addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 10, 4, 8, 2);
                addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 100, 4, 9, 3);
                addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 1000, 5, 7, 4);
                addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 10000, 5, 8, 5);
                addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 100000, 5, 9, 6);
                addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 1000000, 6, 8, 7);
                addQuantityButton(item, meta, lore, Material.WARPED_BUTTON, label, 10000000, 6, 9, 8);
            }
        }
    }

    private void addQuantityButton(ItemStack item, ItemMeta meta, List<String> lore, Material material, String name, int amount, int row, int col) {
        lore.clear();
        meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
        if (amount<0) meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cRemove "+StaticUtils.formatUS(-1*amount)));
        else meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aAdd "+StaticUtils.formatUS(amount)));
        item.setItemMeta(meta);
        item.setType(material);
        if (amount<0) item.setAmount((-1*amount));
        else item.setAmount(amount);
        gui.setItem(row, col, ItemBuilder.from(item).asGuiItem(event -> {
                                                                    event.setCancelled(true);
                                                                    gui.setCloseGuiAction(null);
                                                                    new ShopAdjustInvGui(javaPlugin, viewer, isAdmin, shopUuid, quantity+amount, attribute, closeGuiAfter);
                                                                }));
    }

    private void addQuantityButton(ItemStack item, ItemMeta meta, List<String> lore, Material material, String name, int amount, int row, int col, int size) {
        lore.clear();
        meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
        if (amount<0) meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cRemove $"+StaticUtils.formatUS(-1*amount)));
        else meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aAdd $"+StaticUtils.formatUS(amount)));
        item.setItemMeta(meta);
        item.setType(material);
        item.setAmount(size);
        gui.setItem(row, col, ItemBuilder.from(item).asGuiItem(event -> {
                                                                    event.setCancelled(true);
                                                                    gui.setCloseGuiAction(null);
                                                                    new ShopAdjustInvGui(javaPlugin, viewer, isAdmin, shopUuid, quantity+amount, attribute, closeGuiAfter);
                                                                }));
    }
}
