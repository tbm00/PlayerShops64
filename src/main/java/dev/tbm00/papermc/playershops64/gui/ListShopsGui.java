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
import dev.tbm00.papermc.playershops64.utils.ShopUtils;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class ListShopsGui {
    //private final PlayerShops64 javaPlugin;
    private final PaginatedGui gui;
    private final Player viewer;
    private final boolean isAdmin;
    private final SortType sortType;
    private final QueryType queryType;
    private final String query;
    private String label = "List Shops";

    private List<Map.Entry<UUID, Shop>> shops;
    
    public ListShopsGui(PlayerShops64 javaPlugin, Map<UUID, Shop> shopView, Player viewer, boolean isAdmin, SortType sortType, QueryType queryType, String query) {
        //this.javaPlugin = javaPlugin;
        shops = new ArrayList<>(shopView.entrySet());
        this.viewer = viewer;
        this.isAdmin = isAdmin;
        this.sortType = sortType;
        this.queryType = queryType;
        this.query = query;
        this.gui = new PaginatedGui(6, 45, label);

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
        } if (isAdmin) gui.updateTitle(label + " (ADMIN) - " + gui.getCurrentPageNum() + "/" + gui.getPagesNum());
        else gui.updateTitle(label + " - " + gui.getCurrentPageNum() + "/" + gui.getPagesNum());
        setupFooter();
        preProcessShops();
        sortShops(this.shops, sortType);
        fillShops();
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

                    Double buy1 = (shop1.getBuyPrice()==null) ? null : shop1.getBuyPrice().doubleValue();
                    Double buy2 = (shop2.getBuyPrice()==null) ? null : shop2.getBuyPrice().doubleValue();

                    if (buy1 == null && buy2 == null) return 0; // no movement
                    if (buy1 == null) return 1;  // shop1 goes after shop2
                    if (buy2 == null) return -1; // shop2 goes after shop1

                    double unit1 = buy1 / shop1.getStackSize();
                    double unit2 = buy2 / shop2.getStackSize();
                    return Double.compare(unit1, unit2);
                });
                break;

            case SELL_PRICE: // Sell Price per Item
                shops.sort((entry1, entry2) -> {
                    Shop shop1 = entry1.getValue();
                    Shop shop2 = entry2.getValue();
                    Double sell1 = (shop1.getSellPrice()==null) ? null : shop1.getSellPrice().doubleValue();
                    Double sell2 = (shop2.getSellPrice()==null) ? null : shop2.getSellPrice().doubleValue();

                    if (sell1 == null && sell2 == null) return 0; // no movement
                    if (sell1 == null) return 1;  // shop1 goes after shop2
                    if (sell2 == null) return -1; // shop2 goes after shop1

                    double unit1 = sell1 / shop1.getStackSize();
                    double unit2 = sell2 / shop2.getStackSize();
                    return Double.compare(unit2, unit1);
                });
                break;

            case BALANCE: // Stored Balance
                shops.sort((entry1, entry2) -> {
                    Shop shop1 = entry1.getValue();
                    Shop shop2 = entry2.getValue();
                    Double bal1 = (shop1.getMoneyStock()==null) ? null : shop1.getMoneyStock().doubleValue();
                    Double bal2 = (shop2.getMoneyStock()==null) ? null : shop2.getMoneyStock().doubleValue();

                    //if (shop1.isAdminShop() && shop2.isAdminShop()) return 0;  // no movement
                    //if (shop1.isAdminShop()) return -1;  // shop2 goes after shop1
                    //if (shop2.isAdminShop()) return 1;   // shop1 goes after shop2

                    if (shop1.hasInfiniteMoney() && shop2.hasInfiniteMoney()) return 0;  // no movement
                    if (shop1.hasInfiniteMoney()) return -1;  // shop2 goes after shop1
                    if (shop2.hasInfiniteMoney()) return 1;   // shop1 goes after shop2

                    if (bal1==null && bal2==null) {
                        return 0;
                    } else if (bal1==null) {
                        return 1;
                    } else if (bal2==null) {
                        return -1;
                    } else return Double.compare(bal2, bal1);
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
        String materialName = shopItem.getType().toString();
        ItemMeta meta = shopItem.getItemMeta();
        String displayName = (meta.hasDisplayName()) ? meta.getDisplayName() : "";
        String itemName = (meta.hasItemName()) ? meta.getItemName() : "";
        String desc = shop.getDescription();

        String query_ = query.replace(" ", "_");
        String queryS = query.replace("_", " ");

        if (checkString(query, shop, materialName, meta, displayName, itemName, desc)) return true;
        if (checkString(query_, shop, materialName, meta, displayName, itemName, desc)) return true;
        if (checkString(queryS, shop, materialName, meta, displayName, itemName, desc)) return true;

        return false;
    }

    private boolean checkString(String testQuery, Shop shop, String materialName, ItemMeta meta, String displayName, String itemName, String desc) {
        if (materialName!=null && StringUtils.containsIgnoreCase(materialName, query)) return true; // if material contains query
        if (displayName!=null && StringUtils.containsIgnoreCase(displayName, query)) return true; // if displayName contains query
        if (itemName!=null && StringUtils.containsIgnoreCase(itemName, query)) return true; // if itemName contains query
        if (desc!=null && StringUtils.containsIgnoreCase(desc, query)) return true; // if description contains query
        if (shop.getOwnerUuid()!=null && shop.getOwnerName()!=null && StringUtils.containsIgnoreCase(shop.getOwnerName(), query)) {
            return true; // if ownerName contains query
        }
        return false;
    }

    @SuppressWarnings("null")
    private boolean passValidActiveChecks(Shop shop) {
        Double buyPrice = (shop.getBuyPrice()==null) ? null : shop.getBuyPrice().doubleValue();
        Double sellPrice = (shop.getSellPrice()==null) ? null : shop.getSellPrice().doubleValue();
        Double balance = (shop.getMoneyStock()==null) ? null : shop.getMoneyStock().doubleValue();
        int stock = shop.getItemStock(), stackSize = shop.getStackSize();

        if (balance==null) return false;

        if (buyPrice==null && sellPrice==null) return false; // if buy-from & sell-to are both disabled
        else if (sellPrice==null && stock<stackSize && !shop.hasInfiniteStock()) return false; // if sell-to disabled & no stock to buy-from
        else if (buyPrice==null && balance<sellPrice && !shop.hasInfiniteMoney()) return false; // if buy-from disabled & no money to sell-to
        else if (stock==0 && ( (sellPrice==null) || (balance<sellPrice && !shop.hasInfiniteMoney()))) return false; // if no stock & no money to sell-to (or sell-to disabled)

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

            if (queryType.equals(QueryType.NO_QUERY)) {
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
            } else {
                GuiUtils.setGuiItemAllShops(gui, item, meta, lore, isAdmin);
            }
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
        ItemStack shopItem;
        if (shop.getItemStack()==null) shopItem = new ItemStack(Material.BARRIER);
        else shopItem = shop.getItemStack().clone();

        ItemMeta shopMeta = shopItem.getItemMeta();
        List<String> shopLore = shopMeta.getLore();
        UUID ownerUuid = shop.getOwnerUuid();

        shopLore = ShopUtils.formatSaleItemLoreText(shop, true);
        shopLore.add("&8-----------------------");
        shopLore.add("&6Click to TP to this shop");
        if (isAdmin || (ownerUuid!=null && viewer.getUniqueId().equals(ownerUuid)))
            shopLore.add("&eShift-click to edit this shop");

        shopMeta.setLore(shopLore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
        shopMeta.setDisplayName(StaticUtils.getFormattedSaleItemName(shop));
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
        shopItem.setAmount(shop.getStackSize());

        gui.addItem(ItemBuilder.from(shopItem).asGuiItem(event -> {
                                                            event.setCancelled(true);
                                                            GuiUtils.handleClickShop(event, viewer, shop, isAdmin);
                                                        }));
    }
}
