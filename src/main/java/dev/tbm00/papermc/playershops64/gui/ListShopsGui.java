package dev.tbm00.papermc.playershops64.gui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
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
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class ListShopsGui {
    //private final PlayerShops64 javaPlugin;
    private final PaginatedGui gui;
    private final Player viewer;
    private final boolean isAdmin;
    private final SortType sortType;
    private final QueryType queryType;
    private final String query;
    private String label;

    private List<Map.Entry<UUID, Shop>> shops;
    
    public ListShopsGui(PlayerShops64 javaPlugin, Map<UUID, Shop> shopView, Player viewer, boolean isAdmin, SortType sortType, QueryType queryType, String query) {
        //this.javaPlugin = javaPlugin;
        shops = new ArrayList<>(shopView.entrySet());
        this.viewer = viewer;
        this.isAdmin = isAdmin;
        this.sortType = sortType;
        this.queryType = queryType;
        this.query = query;
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
        } gui = new PaginatedGui(6, 45, label);

        setupFooter();
        preProcessShops();
        sortShops(this.shops, sortType);
        fillShops();

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

    private void sortShops(List<Map.Entry<UUID, Shop>> shops, SortType type) {
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
                    
                    String mat1 = shop1.getItemStack().getType().toString();
                    String mat2 = shop2.getItemStack().getType().toString();
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

            addGuiItemShop(shop);
        }
    }

    private boolean passStringChecks(Shop shop, ItemStack shopItem) {
        String query_ = query.replace(" ", "_");

        String mat = shopItem.getType().toString();
        ItemMeta meta = shopItem.getItemMeta();
        String name = meta.getDisplayName();
        String desc = shop.getDescription();
        if (mat!=null && StringUtils.containsIgnoreCase(mat, query)) return true; // if material contains query
        if (mat!=null && StringUtils.containsIgnoreCase(mat, query_)) return true; // if material contains query_
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
        GuiUtils.setGuiItemCategory(gui, item, meta, lore, isAdmin);

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

    private void addGuiItemShop(Shop shop) {
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

        gui.addItem(ItemBuilder.from(item).asGuiItem(event -> GuiUtils.handleClickShop(event, viewer, shop, isAdmin)));
    }
}
