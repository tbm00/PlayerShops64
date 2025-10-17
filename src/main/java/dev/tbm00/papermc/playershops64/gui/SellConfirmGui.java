package dev.tbm00.papermc.playershops64.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.structure.Shop;
import dev.tbm00.papermc.playershops64.utils.SellGuiEngine;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;
import dev.tbm00.papermc.playershops64.utils.SellGuiEngine.SellPlanEntry;

public final class SellConfirmGui {
    private final PlayerShops64 javaPlugin;
    private final Player player;
    private final SellGuiEngine engine;
    private final Gui gui;
    private String label = "Confirm Sales";

    public SellConfirmGui(PlayerShops64 javaPlugin, Player player, SellGuiEngine engine) {
        this.javaPlugin = javaPlugin;
        this.player = player;
        this.engine = engine;
        this.label += " for $" +StaticUtils.formatIntUS(engine.plans.sellPlan.totalMoney.intValue());
        this.gui = new Gui(6, label);

        gui.setCloseGuiAction(event -> {
                                        engine.returnAllItems();
                                    });
        gui.setDefaultClickAction(event -> {
                                        event.setCancelled(true);
                                    });
        gui.disableAllInteractions();

        fillSales();
        setupFooter();

        gui.open(player);
    }

    private void fillSales() {
        int i = 0;
        for (SellPlanEntry entry : engine.plans.sellPlan.entries) {
            if (i<45) addGuiItemSaleEntry(entry);
            ++i;
        }
    }

    private void addGuiItemSaleEntry(SellPlanEntry entry) {
        Shop shop = javaPlugin.getShopHandler().getShop(entry.shopUuid);

        ItemStack shopItem = entry.item.clone();
        if (shopItem==null) shopItem = new ItemStack(Material.BARRIER);

        ItemMeta shopMeta = shopItem.getItemMeta();
        List<String> shopLore = shopMeta.getLore();
        if (shopLore==null) shopLore = new ArrayList<>();

        shopLore.add("&8-----------------------");
        shopLore.add("&7Selling &e" + StaticUtils.formatIntUS(entry.amount) + " for");
        shopLore.add("&a$" + StaticUtils.formatDoubleUS(entry.unitPrice.doubleValue()) + " &7each");
        shopLore.add("&7to " + shop.getOwnerName());
        shopLore.add(" ");
        shopLore.add("&2Total: $" + StaticUtils.formatDoubleUS(entry.totalPrice.doubleValue()));

        shopMeta.setLore(shopLore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
        /*shopMeta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        shopMeta.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM);
        shopMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        shopMeta.addItemFlags(ItemFlag.HIDE_DESTROYS);
        shopMeta.addItemFlags(ItemFlag.HIDE_DYE);
        shopMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        shopMeta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
        shopMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);*/
        //for (Enchantment enchant : new HashSet<>(shopMeta.getEnchants().keySet()))
        //    shopMeta.removeEnchant(enchant);

        shopItem.setItemMeta(shopMeta);
        shopItem.setAmount(entry.amount);

        gui.addItem(ItemBuilder.from(shopItem).asGuiItem(event -> {
                                                            event.setCancelled(true);
                                                        }));
    }

    private void setupFooter() {
        ItemStack item = new ItemStack(Material.GLASS);
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();
        
        lore.add("&8-----------------------");
        lore.add("&6Click to &aconfirm & sell &6" + StaticUtils.formatIntUS(engine.plans.sellPlan.totalItems));
        lore.add("&6for &a$" + StaticUtils.formatDoubleUS(engine.plans.sellPlan.totalMoney.doubleValue()));
        meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dConfirm Sale"));
        item.setItemMeta(meta);
        item.setType(Material.GREEN_BANNER);
        gui.setItem(6, 5, ItemBuilder.from(item).asGuiItem(event -> {
                                                                event.setCancelled(true);
                                                                gui.setCloseGuiAction(null);
                                                                player.closeInventory();
                                                                //gui.close(player);

                                                                engine.runConfirmedPlans();
                                                            }));
        lore.clear();

        gui.setItem(6, 1, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));
        gui.setItem(6, 2, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));
        gui.setItem(6, 3, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));
        gui.setItem(6, 4, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));

        gui.setItem(6, 6, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));
        gui.setItem(6, 7, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));
        gui.setItem(6, 8, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));
        gui.setItem(6, 9, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));
    }
}
