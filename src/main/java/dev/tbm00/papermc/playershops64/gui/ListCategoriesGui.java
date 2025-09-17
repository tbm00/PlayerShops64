package dev.tbm00.papermc.playershops64.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.structure.GuiSearchCategory;
import dev.tbm00.papermc.playershops64.utils.GuiUtils;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class ListCategoriesGui {
    PlayerShops64 javaPlugin;
    Gui gui;
    String label;
    Player viewer;
    boolean isAdmin;
    
    public ListCategoriesGui(PlayerShops64 javaPlugin, Player viewer, boolean isAdmin) {
        this.javaPlugin = javaPlugin;
        this.viewer = viewer;
        this.isAdmin = isAdmin;
        label = StaticUtils.CATEGORY_GUI_TITLE;

        gui = new Gui(6, label);

        setupFooter();
        fillCategories();

        if (isAdmin) gui.updateTitle(label + " (ADMIN)");
        else gui.updateTitle(label);
        gui.disableAllInteractions();
        gui.open(viewer);
    }

    private void fillCategories() {
        for (GuiSearchCategory category : javaPlugin.getConfigHandler().getSearchCategories()) {
            addGuiItemCategory(category);
        }
    }

    private void addGuiItemCategory(GuiSearchCategory category) {
        ItemStack item = null; 
        if (category.getMaterial()==null) {
            item = new ItemStack(Material.BARRIER);
        } else {
            item = new ItemStack(category.getMaterial());
        }
        
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();
        meta.setLore(null);
        lore.add("&8-----------------------");
        lore.add(category.getLore());
        meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());

        String firstLine = "&dCategory: " + category.getName() + "";
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', firstLine));

        meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        meta.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
        meta.addItemFlags(ItemFlag.HIDE_DYE);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        //for (Enchantment enchant : new HashSet<>(meta.getEnchants().keySet()))
        //    meta.removeEnchant(enchant);

        item.setItemMeta(meta);
        item.setAmount(1);

        gui.setItem(category.getSlot(), ItemBuilder.from(item).asGuiItem(event -> GuiUtils.handleClickSearchCategory(event, category, isAdmin)));
    }

    /**
     * Sets up the footer of the GUI with categories & all other buttons.
     */
    private void setupFooter() {
        ItemStack item = new ItemStack(Material.GLASS);
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();

        // Your Shops
        GuiUtils.setGuiItemYourShops(gui, item, meta, lore, viewer, isAdmin);

        // All Shops
        GuiUtils.setGuiItemAllShops(gui, item, meta, lore, isAdmin);

        // Category
        lore.add("&8-----------------------");
        lore.add("&eCurrently viewing category selector");
        meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dShop Categories"));
        item.setItemMeta(meta);
        item.setType(Material.NETHER_STAR);
        gui.setItem(6, 3, ItemBuilder.from(item).asGuiItem(event -> {event.setCancelled(true);}));
        lore.clear();

        // Empty
        gui.setItem(6, 4, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));
        gui.setItem(6, 5, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));
        gui.setItem(6, 6, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));
        gui.setItem(6, 7, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));

        // Search
        GuiUtils.setGuiItemSearch(gui, item, meta, lore, isAdmin);
        
        // Main Menu
        GuiUtils.setGuiItemMainMenu(gui, item, meta, lore);
    }
}
