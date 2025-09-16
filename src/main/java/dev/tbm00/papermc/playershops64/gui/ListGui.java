package dev.tbm00.papermc.playershops64.gui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.PaginatedGui;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.Shop;
import dev.tbm00.papermc.playershops64.data.enums.QueryType;
import dev.tbm00.papermc.playershops64.data.enums.SortType;
import dev.tbm00.papermc.playershops64.utils.GuiUtils;

public class ListGui {
    PlayerShops64 javaPlugin;
    PaginatedGui gui;
    String label;
    Player viewer;
    boolean isAdmin;
    QueryType queryType;
    String query;

    List<Map.Entry<UUID, Shop>> shops;
    SortType sortType = SortType.UNSORTED;
    
    public ListGui(PlayerShops64 javaPlugin, Map<UUID, Shop> shopView, Player viewer, SortType sortType, QueryType queryType, String query, boolean isAdmin) {
        this.javaPlugin = javaPlugin;
        this.shops = new ArrayList<>(shopView.entrySet());
        this.viewer = viewer;
        this.sortType = sortType;
        this.queryType = queryType;
        this.query = query;
        this.isAdmin = isAdmin;
        switch (queryType) {
            case NO_QUERY:
                label = "All Shops";
                break;
            case STRING:
                label = query;
                break;
            case PLAYER_UUID:
                label = javaPlugin.getServer().getOfflinePlayer(UUID.fromString(query)).getName();
                break;
            default:
                label = "Shops";
                break;
        }

        gui = new PaginatedGui(6, 45, label);
        
        preProcessShops();
        GuiUtils.sortShops(this.shops, sortType);
        fillShops();
        setupFooter();

        if (isAdmin) gui.updateTitle(label + " (ADMIN) - " + gui.getCurrentPageNum() + "/" + gui.getPagesNum());
        else gui.updateTitle(label + " - " + gui.getCurrentPageNum() + "/" + gui.getPagesNum());
        gui.disableAllInteractions();
        gui.open(viewer);
    }

    private void preProcessShops() {
        Iterator<Map.Entry<UUID, Shop>> iter = shops.iterator();
        while(iter.hasNext()) {
            Map.Entry<UUID, Shop> entry = iter.next();
            Shop shop = entry.getValue();
            ItemStack shopItem = shop.getItemStack();
            
            if (shopItem == null) {
                if (isAdmin || (queryType.equals(QueryType.PLAYER_UUID) && UUID.fromString(query).equals(shop.getOwnerUuid())) ) {
                    shopItem = new ItemStack(Material.BARRIER);
                } else {
                    iter.remove();
                    continue;
                }
            }

            boolean remove = false;

            if (!isAdmin) {
                switch (queryType) {
                    case NO_QUERY: 
                        if (!passValidActiveChecks(shop)) remove = true;
                        break;
                    case PLAYER_UUID:
                        if (!passOwnerUuidCheck(shop)) remove = true;
                        break;
                    case STRING:
                        if (!passValidActiveChecks(shop)) remove = true;
                        else if (!passStringChecks(shop, shopItem)) remove = true;
                        break;
                    default: 
                        break;
                }
            } else {
                switch (queryType) {
                    case PLAYER_UUID: 
                        if (!passOwnerUuidCheck(shop)) remove = true;
                        break;
                    case STRING: 
                        if (!passStringChecks(shop, shopItem)) remove = true;
                        break;
                    case NO_QUERY:
                    default: 
                        break;
                }
            }

            if (remove) iter.remove();
        }
    }

    private boolean passStringChecks(Shop shop, ItemStack shopItem) {
        String mat = shopItem.getType().toString().replace("_", " ");
        ItemMeta meta = shopItem.getItemMeta();
        String name = meta.getDisplayName();
        String desc = shop.getDescription();
        if (mat!=null && StringUtils.containsIgnoreCase(mat, query)) return true; // if material contains query
        else if (name!=null && StringUtils.containsIgnoreCase(name, query)) return true; // if itemName contains query
        else if (desc!=null && StringUtils.containsIgnoreCase(desc, query)) return true; // if description contains query
        else if (shop.getOwnerUuid()!=null && shop.getOwnerName()!=null && StringUtils.containsIgnoreCase(shop.getOwnerName(), query)) {
            return true; // if ownerName contains query
        }

        return false;
    }

    private boolean passValidActiveChecks(Shop shop) {
        double buyPrice = shop.getBuyPrice().doubleValue(), sellPrice = shop.getSellPrice().doubleValue(), balance = shop.getMoneyStock().doubleValue();
        int stock = shop.getItemStock(), stackSize = shop.getStackSize();

        if (buyPrice<0 && sellPrice<0) return false; // if buy-from & sell-to are both disabled
        else if (sellPrice<0 && stock<stackSize && !shop.hasInfiniteStock()) return false; // if sell-to disabled & no stock to buy-from
        else if (buyPrice<0 && balance<sellPrice && !shop.hasInfiniteMoney()) return false; // if buy-from disabled & no money to sell-to
        else if (stock==0 && balance<sellPrice && !shop.hasInfiniteMoney()) return false; // if no stock & no money to sell-to

        return true;
    }

    private boolean passOwnerUuidCheck(Shop shop) {
        if (shop.getOwnerUuid() != null && shop.getOwnerUuid().equals((UUID.fromString(query)))) {
            return true; // if ownerUuid matches query
        }

        return false;
    }

    /**
     * Fills the GUI with items from the shop map.
     * Each shop that has a valid shop item and pricing information is converted into a clickable GUI item.
     *
     */
    private void fillShops() {
        Iterator<Map.Entry<UUID, Shop>> iter = shops.iterator();
        while(iter.hasNext()) {
            Map.Entry<UUID, Shop> entry = iter.next();
            Shop shop = entry.getValue();

            GuiUtils.addGuiItemShop(gui, shop, viewer, isAdmin);
        }
    }

    /**
     * Sets up the footer of the GUI with categories & all other buttons.
     */
    private void setupFooter() {
        ItemStack item = new ItemStack(Material.GLASS);
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();
        boolean viewersShops = queryType.equals(QueryType.PLAYER_UUID) && UUID.fromString(query).equals(viewer.getUniqueId());

        
        if (viewersShops) {
            // Your Shops
            item.setType(Material.ENDER_CHEST);
            lore.add("&8-----------------------");
            lore.add("&eCurrently viewing your shops");
            lore.add("&e(sorted by " + SortType.toString(sortType) + ")");
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dYour Shops"));
            item.setItemMeta(meta);
            gui.setItem(6, 1, ItemBuilder.from(item).asGuiItem(event -> {event.setCancelled(true);}));
            lore.clear();

            // All Shops
            GuiUtils.setGuiItemAllShops(gui, item, meta, lore, isAdmin);
        } else {
            // Your Shops
            GuiUtils.setGuiItemYourShops(gui, item, meta, lore, viewer, isAdmin);

            // All Shops
            lore.add("&8-----------------------");
            lore.add("&eCurrently viewing all shops");
            lore.add("&e(sorted by " + SortType.toString(sortType) + ")");
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dAll Shops"));
            item.setItemMeta(meta);
            item.setType(Material.CHEST);
            gui.setItem(6, 2, ItemBuilder.from(item).asGuiItem(event -> {event.setCancelled(true);}));
            lore.clear();
        }

        // Category
        GuiUtils.setGuiItemCategory(gui, item, meta, lore);

        // Empty
        gui.setItem(6, 4, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));

        // Previous Page or Empty
        if (gui.getPagesNum()>=2) GuiUtils.setGuiItemPageBack(gui, item, meta, lore, label);
        else gui.setItem(6, 5, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));

        // Next Page or Empty
        if (gui.getPagesNum()>=2)  GuiUtils.setGuiItemPageNext(gui, item, meta, lore, label);
        else gui.setItem(6, 6, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));

        // Sort
        GuiUtils.setGuiItemSortList(gui, item, meta, lore, sortType, queryType, query, isAdmin);

        // Search
        GuiUtils.setGuiItemSearch(gui, item, meta, lore, isAdmin);
        
        // Main Menu
        GuiUtils.setGuiItemMainMenu(gui, item, meta, lore);
    }
}
