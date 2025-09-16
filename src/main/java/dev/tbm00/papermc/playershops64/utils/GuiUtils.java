package dev.tbm00.papermc.playershops64.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.PaginatedGui;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.Shop;
import dev.tbm00.papermc.playershops64.data.enums.QueryType;
import dev.tbm00.papermc.playershops64.data.enums.SortType;
import dev.tbm00.papermc.playershops64.gui.ListGui;
import dev.tbm00.papermc.playershops64.gui.SearchGui;

public class GuiUtils {
    private static PlayerShops64 javaPlugin;

    public static void init(PlayerShops64 javaPlugin) {
        GuiUtils.javaPlugin = javaPlugin;
    }

    public static void openGuiTransaction(Player player, Shop shop) {
        if (!StaticUtils.canPlayerEditShop(shop, player)) return;


    }

    public static void openGuiManage(Player player, Shop shop) {
        if (!StaticUtils.canPlayerEditShop(shop, player)) return;


    }

    public static boolean openGuiSearchResults(Player sender, String[] args, boolean isAdmin) {
        
        // search shops for target player
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
            //StaticUtils.log(ChatColor.YELLOW, "calling ListGui(), query type: PLAYER_UUID " + targetName + " " + targetUUID);
            new ListGui(javaPlugin, javaPlugin.getShopHandler().getShopView(), sender, SortType.MATERIAL, QueryType.PLAYER_UUID, targetUUID, isAdmin);
            return true;
        }

        // search shops for target item String
        String query = null;
        int i=0;
        for (String arg : args) {
            if (i==0) {
                query = arg;
                ++i;
            } else query = query + " " + arg;
        }

        if (query==null || query.isEmpty()) return false;
        query = query.replace("_", " ");
        
        //StaticUtils.log(ChatColor.YELLOW, "calling ListGui(), query type: STRING '" + query + "'");
        new ListGui(javaPlugin, javaPlugin.getShopHandler().getShopView(), sender, SortType.MATERIAL, QueryType.STRING, query, isAdmin);
        return true;
    }

    public static void handleClickListAll(InventoryClickEvent event, boolean isAdmin) {
        event.setCancelled(false);
        new ListGui(javaPlugin, javaPlugin.getShopHandler().getShopView(), (Player) event.getWhoClicked(), SortType.MATERIAL, QueryType.NO_QUERY, null, isAdmin);
    }

    public static void handleClickSortList(InventoryClickEvent event, SortType sortType, QueryType queryType, String query, boolean isAdmin) {
        event.setCancelled(false);
        new ListGui(javaPlugin, javaPlugin.getShopHandler().getShopView(), (Player) event.getWhoClicked(), sortType, queryType, query, isAdmin);
    }

    public static void handleClickYourShops(InventoryClickEvent event, boolean isAdmin) {
        event.setCancelled(false);
        new ListGui(javaPlugin, javaPlugin.getShopHandler().getShopView(), (Player) event.getWhoClicked(), SortType.MATERIAL, QueryType.PLAYER_UUID, ((Player) event.getWhoClicked()).getUniqueId().toString(), isAdmin);
    }

    public static void handleClickSearch(InventoryClickEvent event, boolean isAdmin) {
        event.setCancelled(true);
        event.getWhoClicked().closeInventory();
        new SearchGui(javaPlugin, (Player) event.getWhoClicked(), isAdmin);
    }

    public static void handleClickCategory(InventoryClickEvent event, String command) {
        event.setCancelled(true);
        StaticUtils.sudoCommand(event.getWhoClicked(), command);
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
            openGuiManage(sender, shop);
        } else StaticUtils.teleportPlayerToShop(sender, shop);
    }

    public static void handleClickMainMenu(InventoryClickEvent event) {
        event.setCancelled(true);
        StaticUtils.sudoCommand(event.getWhoClicked(), "commandpanel menu");
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
    }

    public static void setGuiItemCategory(PaginatedGui gui, ItemStack item, ItemMeta meta, List<String> lore) {
        lore.add("&8-----------------------");
        lore.add("&6Click to open category selector");
        meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dShop Categories"));
        item.setItemMeta(meta);
        item.setType(Material.NETHER_STAR);
        gui.setItem(6, 3, ItemBuilder.from(item).asGuiItem(event -> handleClickCategory(event, "commandpanel shopgui")));
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
        gui.setItem(6, 7, ItemBuilder.from(item).asGuiItem(event -> handleClickSortList(event, sortType, queryType, query, isAdmin)));
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

    public static void addGuiItemShop(PaginatedGui gui, Shop shop, Player viewer, boolean isAdmin) {
        boolean emptyShop = false;
        double buyPrice = shop.getBuyPrice().doubleValue(), sellPrice = shop.getSellPrice().doubleValue(),
                balance = shop.getMoneyStock().doubleValue();
        int stock = shop.getItemStock();

        ItemStack item = null; 
        if (shop.getItemStack()==null) {
            emptyShop = true;
            item = new ItemStack(Material.BARRIER);
        } else {
            item = shop.getItemStack().clone();
        }
        
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();
        String priceLine = "", firstLine=null;
        UUID ownerUuid = shop.getOwnerUuid();

        meta.setLore(null);
        lore.add("&8-----------------------");
        lore.add("&c" + shop.getDescription());

        if (buyPrice>=0) priceLine = "&7B: &a$" + StaticUtils.formatInt(buyPrice) + " ";
        if (sellPrice>=0) priceLine += "&7S: &c$" + StaticUtils.formatInt(sellPrice);
        lore.add(priceLine);

        if (stock<0) lore.add("&7Stock: &e∞");
        else lore.add("&7Stock: &e" + stock);

        if (shop.hasInfiniteMoney()) lore.add("&7Balance: &e$&e∞");
        else lore.add("&7Balance: &e$" + StaticUtils.formatInt(balance));

        if (ownerUuid!=null && (!shop.hasInfiniteMoney() && !shop.hasInfiniteStock()))
            lore.add("&7Owner: &f" + shop.getOwnerName());

        lore.add("&7"+shop.getWorld().getName()+": &f"
                +(int)shop.getLocation().getX()+"&7, &f"
                +(int)shop.getLocation().getY()+"&7, &f"
                +(int)shop.getLocation().getZ());

        lore.add("&8-----------------------");
        lore.add("&6Click to TP to this shop");

        if (isAdmin || (ownerUuid!=null && viewer.getUniqueId().equals(ownerUuid)))
            lore.add("&eShift-click to edit this shop");

        meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());

        if (emptyShop) {
            firstLine = "&c(no shop item)";
        } else {
            if (meta.getDisplayName()==null || meta.getDisplayName().isBlank())
                firstLine = StaticUtils.formatMaterial(item.getType()) + " &7x &f" + shop.getStackSize();
            else firstLine = meta.getDisplayName() + " &7x &f" + shop.getStackSize();
        }

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
        item.setAmount(shop.getStackSize());

        gui.addItem(ItemBuilder.from(item).asGuiItem(event -> handleClickShop(event, viewer, shop, isAdmin)));
    }

    public static void sortShops(List<Map.Entry<UUID, Shop>> shops, SortType type) {
        switch (type) {
            case UNSORTED: // Unsorted
                break;

            case MATERIAL: // Material
                shops.sort((entry1, entry2) -> {
                    Shop shop1 = entry1.getValue();
                    Shop shop2 = entry2.getValue();
                    
                    if (shop1.getItemStack() == null || shop1.getItemStack().getType() == null) {
                        if (shop2.getItemStack() == null || shop2.getItemStack().getType() == null) return 0; // no movement
                        return 1; // shop1 goes after shop2
                    }
                    if (shop2.getItemStack() == null || shop2.getItemStack().getType() == null) {
                        return -1; // shop2 goes after shop1
                    }
                    
                    String mat1 = shop1.getItemStack().getType().toString().replace("_", " ");
                    String mat2 = shop2.getItemStack().getType().toString().replace("_", " ");
                    return mat1.compareToIgnoreCase(mat2);
                });
                break;

            case BUY_PRICE: // Buy Price per Item
                shops.sort((entry1, entry2) -> {
                    Shop shop1 = entry1.getValue();
                    Shop shop2 = entry2.getValue();
                    double buy1 = shop1.getBuyPrice().doubleValue();
                    double buy2 = shop2.getBuyPrice().doubleValue();

                    if (buy1 == -1 && buy2 == -1) return 0; // no movement
                    if (buy1 == -1) return 1;  // shop1 goes after shop2
                    if (buy2 == -1) return -1; // shop2 goes after shop1

                    double unit1 = buy1 / shop1.getStackSize();
                    double unit2 = buy2 / shop2.getStackSize();
                    return Double.compare(unit1, unit2);
                });
                break;

            case SELL_PRICE: // Sell Price per Item
                shops.sort((entry1, entry2) -> {
                    Shop shop1 = entry1.getValue();
                    Shop shop2 = entry2.getValue();
                    double sell1 = shop1.getSellPrice().doubleValue();
                    double sell2 = shop2.getSellPrice().doubleValue();

                    if (sell1 == -1 && sell2 == -1) return 0; // no movement
                    if (sell1 == -1) return 1;  // shop1 goes after shop2
                    if (sell2 == -1) return -1; // shop2 goes after shop1

                    double unit1 = sell1 / shop1.getStackSize();
                    double unit2 = sell2 / shop2.getStackSize();
                    return Double.compare(unit2, unit1);
                });
                break;

            case BALANCE: // Stored Balance
                shops.sort((entry1, entry2) -> {
                    Shop shop1 = entry1.getValue();
                    Shop shop2 = entry2.getValue();
                    double bal1 = shop1.getMoneyStock().doubleValue();
                    double bal2 = shop2.getMoneyStock().doubleValue();

                    //if (shop1.isAdminShop() && shop2.isAdminShop()) return 0;  // no movement
                    //if (shop1.isAdminShop()) return -1;  // shop2 goes after shop1
                    //if (shop2.isAdminShop()) return 1;   // shop1 goes after shop2

                    if (shop1.hasInfiniteMoney() && shop2.hasInfiniteMoney()) return 0;  // no movement
                    if (shop1.hasInfiniteMoney()) return -1;  // shop2 goes after shop1
                    if (shop2.hasInfiniteMoney()) return 1;   // shop1 goes after shop2

                    return Double.compare(bal2, bal1);
                });
                break;

            case STOCK: // Stored Stock
                shops.sort((entry1, entry2) -> {
                    Shop shop1 = entry1.getValue();
                    Shop shop2 = entry2.getValue();
                    int stock1 = shop1.getItemStock();
                    int stock2 = shop2.getItemStock();

                    if (shop1.hasInfiniteStock() && shop2.hasInfiniteStock()) return 0;  // no movement
                    if (shop1.hasInfiniteStock()) return -1;  // shop2 goes after shop1
                    if (shop2.hasInfiniteStock()) return 1;   // shop1 goes after shop2

                    return Integer.compare(stock2, stock1);
                });
                break;

            default:
                break;
        }
    }
}
