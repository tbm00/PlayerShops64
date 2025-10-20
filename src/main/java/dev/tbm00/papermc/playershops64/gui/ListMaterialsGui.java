package dev.tbm00.papermc.playershops64.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.PaginatedGui;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.utils.GuiUtils;
import dev.tbm00.papermc.playershops64.utils.ShopUtils;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class ListMaterialsGui {
    private final PlayerShops64 javaPlugin;
    private final Player viewer;
    private final boolean isAdmin;
    private final UUID shopUuid;
    private final PaginatedGui gui;
    private final Material currentMaterial;
    private final String label = "Base Block Material ";
    
    public ListMaterialsGui(PlayerShops64 javaPlugin, Player viewer, UUID shopUuid, boolean isAdmin) {
        this.javaPlugin = javaPlugin;
        this.viewer = viewer;
        this.isAdmin = isAdmin;
        this.shopUuid = shopUuid;
        this.gui = new PaginatedGui(6, 45, label);
        this.currentMaterial = javaPlugin.getShopHandler().getShop(shopUuid).getBaseMaterial();
        
        if (!javaPlugin.getShopHandler().tryLockShop(shopUuid, viewer)) {
            return;
        } String shopHint = shopUuid.toString().substring(0, 6);
        StaticUtils.log(ChatColor.YELLOW, viewer.getName() + " opened shop "+shopHint+"'s manage base block material gui");

        fillMaterials();
        setupFooter();
        
        gui.disableAllInteractions();
        gui.setCloseGuiAction(event -> {
            StaticUtils.log(ChatColor.GREEN, viewer.getName() + " closed shop "+shopHint+"'s manage base block material gui");
            javaPlugin.getShopHandler().unlockShop(shopUuid, viewer.getUniqueId());
        });

        gui.open(viewer);
    }

    /**
     * Fills the GUI with players from the gang.
     *
     * @param sender the player for whom the GUI is being built
     */
    private void fillMaterials() {
        for (Material material : javaPlugin.getConfigHandler().getBaseBlockMaterials()) {
            addGuiItemMaterial(material);
        }
    }

    private void setupFooter() {
        ItemStack item = new ItemStack(Material.GLASS);
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();

        { // Back Button
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&6Click to go back to the manage GUI");
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dExit to Manage GUI"));
            item.setItemMeta(meta);
            item.setType(Material.POLISHED_BLACKSTONE_BUTTON);
            gui.setItem(6, 5, ItemBuilder.from(item).asGuiItem(event -> {
                                                                            event.setCancelled(true);
                                                                            gui.setCloseGuiAction(null);
                                                                            new ShopManageGui(javaPlugin, viewer, isAdmin, shopUuid);
                                                                        }));
        }
    
        lore.clear();
        gui.setItem(6, 1, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));
        gui.setItem(6, 2, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));
        gui.setItem(6, 2, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));
        gui.setItem(6, 3, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));
        if (gui.getPagesNum()>=2) GuiUtils.setGuiItemPageBack(gui, item, meta, lore, label, 4);
        else gui.setItem(6, 4, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));

        if (gui.getPagesNum()>=2) GuiUtils.setGuiItemPageNext(gui, item, meta, lore, label, 6);
        else gui.setItem(6, 6, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));
        gui.setItem(6, 7, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));
        gui.setItem(6, 8, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));
        gui.setItem(6, 9, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));
    }

    private void addGuiItemMaterial(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();


        if (material.equals(currentMaterial)) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            lore.add("&8-----------------------");
            lore.add("&eCurrently set to "+material.toString());
        } else {
            lore.add("&8-----------------------");
            lore.add("&6Click to change base to " + material.toString());
        }

        meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
        item.setItemMeta(meta);
        item.setAmount(1);

        if (material.equals(currentMaterial)) {
            gui.addItem(ItemBuilder.from(item).asGuiItem(event -> {
                                                                event.setCancelled(true);
                                                            }));
        } else {
            gui.addItem(ItemBuilder.from(item).asGuiItem(event -> {
                                                                event.setCancelled(true);
                                                                ShopUtils.setBaseMaterial(viewer, shopUuid, material);
                                                                gui.setCloseGuiAction(null);
                                                                new ListMaterialsGui(javaPlugin, viewer, shopUuid, isAdmin);
                                                            }));
        }
    }
}