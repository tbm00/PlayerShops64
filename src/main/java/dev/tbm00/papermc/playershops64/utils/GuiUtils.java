package dev.tbm00.papermc.playershops64.utils;

import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.PaginatedGui;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.Shop;
import dev.tbm00.papermc.playershops64.data.enums.QueryType;
import dev.tbm00.papermc.playershops64.data.enums.SortType;
import dev.tbm00.papermc.playershops64.data.structure.GuiSearchCategory;
import dev.tbm00.papermc.playershops64.data.structure.GuiSearchQuery;
import dev.tbm00.papermc.playershops64.gui.ListShopsGui;
import dev.tbm00.papermc.playershops64.gui.ListCategoriesGui;
import dev.tbm00.papermc.playershops64.gui.ListQueriesGui;
import dev.tbm00.papermc.playershops64.gui.SearchGui;
import dev.tbm00.papermc.playershops64.gui.ShopTransactionGui;

public class GuiUtils {
    private static PlayerShops64 javaPlugin;

    public static void init(PlayerShops64 javaPlugin) {
        GuiUtils.javaPlugin = javaPlugin;
    }

    public static boolean openGuiTransaction(Player player, UUID shopUuid) {
        if (!javaPlugin.getShopHandler().canPlayerEditShop(shopUuid, player)) return false;

        new ShopTransactionGui(javaPlugin, player, shopUuid, 1);
        return true;
    }

    public static boolean openGuiManage(Player player, UUID shopUuid) {
        if (!javaPlugin.getShopHandler().canPlayerEditShop(shopUuid, player)) return false;

        // TODO: call ShopManageGui for player+shopUuid
        return true;
    }

    public static boolean openGuiSearchResults(Player sender, String[] args, boolean isAdmin) {
        // 1st: search shops for target player
        while (args[0].startsWith(" ")) {
            args[0] = args[0].substring(1);
        }

        String targetName = args[0];
        String targetUUID = null;

        if (!args[0].isBlank()) {
            try {
                OfflinePlayer target = javaPlugin.getServer().getOfflinePlayer(targetName);
                if (target.hasPlayedBefore()) targetUUID = target.getUniqueId().toString();
            } catch (Exception e) {
                StaticUtils.log(ChatColor.RED, "Caught exception getting offline player while searching shops: " + e.getMessage());
            }
        }
        if (targetUUID!=null) {
            StaticUtils.log(ChatColor.YELLOW, "calling ListGui(), query type: PLAYER_UUID " + targetName + " " + targetUUID);
            new ListShopsGui(javaPlugin, javaPlugin.getShopHandler().getShopView(), sender, SortType.MATERIAL, QueryType.PLAYER_UUID, targetUUID, isAdmin);
            return true;
        }

        // 2nd: search shops for target item String
        String query = null;
        int i=0;
        for (String arg : args) {
            if (i==0) {
                query = arg;
                ++i;
            } else query = query + " " + arg;
        }

        if (query==null || query.isEmpty()) return false;
        else {
            StaticUtils.log(ChatColor.YELLOW, "calling ListGui(), query type: STRING '" + query + "'");
            new ListShopsGui(javaPlugin, javaPlugin.getShopHandler().getShopView(), sender, SortType.MATERIAL, QueryType.STRING, query, isAdmin);
            return true;
        }
    }

    public static void handleClickListAll(InventoryClickEvent event, boolean isAdmin) {
        event.setCancelled(false);
        new ListShopsGui(javaPlugin, javaPlugin.getShopHandler().getShopView(), (Player) event.getWhoClicked(), SortType.MATERIAL, QueryType.NO_QUERY, null, isAdmin);
    }

    public static void handleClickSortList(InventoryClickEvent event, SortType sortType, QueryType queryType, String query, boolean isAdmin) {
        event.setCancelled(false);
        new ListShopsGui(javaPlugin, javaPlugin.getShopHandler().getShopView(), (Player) event.getWhoClicked(), sortType, queryType, query, isAdmin);
    }

    public static void handleClickYourShops(InventoryClickEvent event, boolean isAdmin) {
        event.setCancelled(false);
        new ListShopsGui(javaPlugin, javaPlugin.getShopHandler().getShopView(), (Player) event.getWhoClicked(), SortType.MATERIAL, QueryType.PLAYER_UUID, ((Player) event.getWhoClicked()).getUniqueId().toString(), isAdmin);
    }

    public static void handleClickSearch(InventoryClickEvent event, boolean isAdmin) {
        event.setCancelled(true);
        event.getWhoClicked().closeInventory();
        new SearchGui(javaPlugin, (Player) event.getWhoClicked(), isAdmin);
    }

    public static void handleClickPage(InventoryClickEvent event, PaginatedGui gui, boolean next, String label) {
        event.setCancelled(true);
        if (next) gui.next();
        else gui.previous();
        gui.updateTitle(label + gui.getCurrentPageNum() + "/" + gui.getPagesNum());
    }

    public static void handleClickShop(InventoryClickEvent event, Player sender, Shop shop, boolean isAdmin) {
        event.setCancelled(true);
        
        if (event.isShiftClick() && (isAdmin || sender.getUniqueId().equals(shop.getOwnerUuid()))) {
            openGuiManage(sender, shop.getUuid());
        } else StaticUtils.teleportPlayerToShop(sender, shop);
    }

    public static void handleClickCategory(InventoryClickEvent event, boolean isAdmin) {
        event.setCancelled(true);
        new ListCategoriesGui(javaPlugin, (Player) event.getWhoClicked(), isAdmin);
    }

    public static void handleClickSearchCategory(InventoryClickEvent event, GuiSearchCategory category, boolean isAdmin) {
        event.setCancelled(true);
        new ListQueriesGui(javaPlugin, (Player) event.getWhoClicked(), category, isAdmin);
    }

    public static void handleClickSearchQuery(InventoryClickEvent event, GuiSearchQuery query, boolean isAdmin) {
        event.setCancelled(true);
        new ListShopsGui(javaPlugin, javaPlugin.getShopHandler().getShopView(), (Player) event.getWhoClicked(), SortType.MATERIAL, QueryType.STRING, query.getQuery(), isAdmin);
    }

    public static void handleClickMainMenu(InventoryClickEvent event) {
        event.setCancelled(true);
        StaticUtils.sudoCommand(event.getWhoClicked(), "commandpanel menu");
    }

    public static void setGuiItemSearchCategory(Gui gui, ItemStack item, ItemMeta meta, List<String> lore, boolean isAdmin, GuiSearchCategory category) {
        lore.add("&8-----------------------");
        lore.add(category.getLore());
        meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', category.getName()));
        item.setItemMeta(meta);
        item.setType(category.getMaterial());
        gui.setItem(category.getSlot(), ItemBuilder.from(item).asGuiItem(event -> handleClickSearchCategory(event, category, isAdmin)));
        lore.clear();
    }

    public static void setGuiItemYourShops(PaginatedGui gui, ItemStack item, ItemMeta meta, List<String> lore, Player player, boolean isAdmin) {
        item.setType(Material.ENDER_CHEST);
        lore.add("&8-----------------------");
        lore.add("&6Click to view your shops");
        meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dYour Shops"));
        item.setItemMeta(meta);
        gui.setItem(6, 1, ItemBuilder.from(item).asGuiItem(event -> handleClickYourShops(event, isAdmin)));
        lore.clear();
    } public static void setGuiItemYourShops(Gui gui, ItemStack item, ItemMeta meta, List<String> lore, Player player, boolean isAdmin) {
        item.setType(Material.ENDER_CHEST);
        lore.add("&8-----------------------");
        lore.add("&6Click to view your shops");
        meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dYour Shops"));
        item.setItemMeta(meta);
        gui.setItem(6, 1, ItemBuilder.from(item).asGuiItem(event -> handleClickYourShops(event, isAdmin)));
        lore.clear();
    }

    public static void setGuiItemAllShops(PaginatedGui gui, ItemStack item, ItemMeta meta, List<String> lore, boolean isAdmin) {
        lore.add("&8-----------------------");
        lore.add("&6Click to view all shops");
        meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dAll Shops"));
        item.setItemMeta(meta);
        item.setType(Material.CHEST);
        gui.setItem(6, 2, ItemBuilder.from(item).asGuiItem(event -> handleClickListAll(event, isAdmin)));
        lore.clear();
    } public static void setGuiItemAllShops(Gui gui, ItemStack item, ItemMeta meta, List<String> lore, boolean isAdmin) {
        lore.add("&8-----------------------");
        lore.add("&6Click to view all shops");
        meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dAll Shops"));
        item.setItemMeta(meta);
        item.setType(Material.CHEST);
        gui.setItem(6, 2, ItemBuilder.from(item).asGuiItem(event -> handleClickListAll(event, isAdmin)));
        lore.clear();
    }

    public static void setGuiItemCategory(PaginatedGui gui, ItemStack item, ItemMeta meta, List<String> lore, boolean isAdmin) {
        lore.add("&8-----------------------");
        lore.add("&6Click to open category selector");
        meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dShop Categories"));
        item.setItemMeta(meta);
        item.setType(Material.NETHER_STAR);
        gui.setItem(6, 3, ItemBuilder.from(item).asGuiItem(event -> handleClickCategory(event, isAdmin)));
        lore.clear();
    } public static void setGuiItemCategory(Gui gui, ItemStack item, ItemMeta meta, List<String> lore, boolean isAdmin) {
        lore.add("&8-----------------------");
        lore.add("&6Click to open category selector");
        meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dShop Categories"));
        item.setItemMeta(meta);
        item.setType(Material.NETHER_STAR);
        gui.setItem(6, 3, ItemBuilder.from(item).asGuiItem(event -> handleClickCategory(event, isAdmin)));
        lore.clear();
    }

    public static void setGuiItemPageBack(PaginatedGui gui, ItemStack item, ItemMeta meta, List<String> lore, String label) {
        lore.add("&8-----------------------");
        lore.add("&6Click to go to the previous page");
        meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fPrevious Page"));
        item.setItemMeta(meta);
        item.setType(Material.STONE_BUTTON);
        gui.setItem(6, 5, ItemBuilder.from(item).asGuiItem(event -> handleClickPage(event, gui, false, label)));
        lore.clear();
    }

    public static void setGuiItemPageNext(PaginatedGui gui, ItemStack item, ItemMeta meta, List<String> lore, String label) {
        lore.add("&8-----------------------");
        lore.add("&6Click to go to the next page");
        meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fNext Page"));
        item.setItemMeta(meta);
        item.setType(Material.STONE_BUTTON);
        gui.setItem(6, 6, ItemBuilder.from(item).asGuiItem(event -> handleClickPage(event, gui, true, label)));
        lore.clear();
    }

    public static void setGuiItemSortList(PaginatedGui gui, ItemStack item, ItemMeta meta, List<String> lore, SortType sortType, QueryType queryType, String query, boolean isAdmin) {
        lore.add("&8-----------------------");
        
        if (!isAdmin) lore.add("&6Click to change sort order");
        else lore.add("&6Click to change sort order &c(ADMIN)");
        lore.add("&6("+ SortType.toString(sortType) + " -> " + SortType.toString(SortType.nextType(sortType)) + ")");
        meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fSort Shops"));
        item.setItemMeta(meta);
        item.setType(Material.HOPPER);
        gui.setItem(6, 7, ItemBuilder.from(item).asGuiItem(event -> handleClickSortList(event, SortType.nextType(sortType), queryType, query, isAdmin)));
        lore.clear();
    }

    public static void setGuiItemSearch(PaginatedGui gui, ItemStack item, ItemMeta meta, List<String> lore, boolean isAdmin) {
        lore.add("&8-----------------------");
        lore.add("&6Click to search for a specific shop");
        meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dSearch Shops"));
        item.setItemMeta(meta);
        item.setType(Material.WRITABLE_BOOK);
        gui.setItem(6, 8, ItemBuilder.from(item).asGuiItem(event -> handleClickSearch(event, isAdmin)));
        lore.clear();
    } public static void setGuiItemSearch(Gui gui, ItemStack item, ItemMeta meta, List<String> lore, boolean isAdmin) {
        lore.add("&8-----------------------");
        lore.add("&6Click to search for a specific shop");
        meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dSearch Shops"));
        item.setItemMeta(meta);
        item.setType(Material.WRITABLE_BOOK);
        gui.setItem(6, 8, ItemBuilder.from(item).asGuiItem(event -> handleClickSearch(event, isAdmin)));
        lore.clear();
    }

    public static void setGuiItemMainMenu(PaginatedGui gui, ItemStack item, ItemMeta meta, List<String> lore) {
        lore.clear();
        meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fGo to Main Menu"));
        item.setItemMeta(meta);
        item.setType(Material.STONE_BUTTON);
        gui.setItem(6, 9, ItemBuilder.from(item).asGuiItem(event -> handleClickMainMenu(event)));
        lore.clear();
    } public static void setGuiItemMainMenu(Gui gui, ItemStack item, ItemMeta meta, List<String> lore) {
        lore.clear();
        meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fGo to Main Menu"));
        item.setItemMeta(meta);
        item.setType(Material.STONE_BUTTON);
        gui.setItem(6, 9, ItemBuilder.from(item).asGuiItem(event -> handleClickMainMenu(event)));
        lore.clear();
    }
}
