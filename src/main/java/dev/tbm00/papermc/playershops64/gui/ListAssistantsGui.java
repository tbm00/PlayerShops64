package dev.tbm00.papermc.playershops64.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.PaginatedGui;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.enums.AdjustAttribute;
import dev.tbm00.papermc.playershops64.data.structure.Shop;
import dev.tbm00.papermc.playershops64.utils.GuiUtils;
import dev.tbm00.papermc.playershops64.utils.ShopUtils;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class ListAssistantsGui {
    private final PlayerShops64 javaPlugin;
    private final Player viewer;
    private final boolean isAdmin;
    private final UUID shopUuid;
    private final PaginatedGui gui;
    private final String label = "Manage Assistants";
    private final Shop shop;
    private final boolean isOwner;
    
    public ListAssistantsGui(PlayerShops64 javaPlugin, Player viewer, UUID shopUuid, boolean isAdmin) {
        this.javaPlugin = javaPlugin;
        this.viewer = viewer;
        this.isAdmin = isAdmin;
        this.shopUuid = shopUuid;
        this.shop = javaPlugin.getShopHandler().getShop(shopUuid);
        this.isOwner = (shop.getOwnerUuid()==null) ? false : viewer.getUniqueId().equals(shop.getOwnerUuid());
        this.gui = new PaginatedGui(6, 45, label);
        
        if (!javaPlugin.getShopHandler().tryLockShop(shopUuid, viewer)) {
            return;
        } //StaticUtils.log(ChatColor.YELLOW, viewer.getName() + " opened shop "+ShopUtils.getShopHint(shopUuid)+"'s manage assistants gui");
        
        fillPlayers(shop);
        setupFooter();
        
        gui.disableAllInteractions();
        gui.setCloseGuiAction(event -> {
            //StaticUtils.log(ChatColor.GREEN, viewer.getName() + " closed shop "+ShopUtils.getShopHint(shopUuid)+"'s manage assistants gui");
            javaPlugin.getShopHandler().unlockShop(shopUuid, viewer.getUniqueId());
        });

        gui.open(viewer);
    }

    /**
     * Fills the GUI with players from the gang.
     *
     * @param sender the player for whom the GUI is being built
     */
    private void fillPlayers(Shop shop) {
        for (UUID playerUuid : shop.getAssistants()) {
            addGuiItemAssistant(playerUuid);
        }
    }

    private void setupFooter() {
        ItemStack item = new ItemStack(Material.GLASS);
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();

        if (isOwner) { // Add Assistant
            lore.clear();
            lore.add("&8-----------------------");
            lore.add("&6Click to add an assistant to this shop");
            meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dAdd Assistant"));
            item.setItemMeta(meta);
            item.setType(Material.GREEN_BANNER);
            gui.setItem(6, 1, ItemBuilder.from(item).asGuiItem(event -> {
                                                                            event.setCancelled(true);
                                                                            gui.setCloseGuiAction(null);
                                                                            new ShopAdjustTextGui(javaPlugin, viewer, isAdmin, shopUuid, AdjustAttribute.ASSISTANT, isAdmin);
                                                                        }));
        } else gui.setItem(6, 1, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));

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
        gui.setItem(6, 2, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));
        gui.setItem(6, 3, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));
        if (gui.getPagesNum()>=2) GuiUtils.setGuiItemPageBack(gui, item, meta, lore, label, 4, isAdmin);
        else gui.setItem(6, 4, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));

        if (gui.getPagesNum()>=2) GuiUtils.setGuiItemPageNext(gui, item, meta, lore, label, 6, isAdmin);
        else gui.setItem(6, 6, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));
        gui.setItem(6, 7, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));
        gui.setItem(6, 8, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));
        gui.setItem(6, 9, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).setName(" ").asGuiItem(event -> event.setCancelled(true)));
    }

    private void addGuiItemAssistant(UUID playerUuid) {
        ItemStack headItem = new ItemStack(Material.PLAYER_HEAD);

        OfflinePlayer player = javaPlugin.getServer().getOfflinePlayer(playerUuid);
        StaticUtils.applyHeadTexture(headItem, player);
        SkullMeta headMeta = (SkullMeta) headItem.getItemMeta();
        List<String> lore = new ArrayList<>();

        if (isOwner) {
            lore.add("&8-----------------------");
            lore.add("&6Click to remove this assistant");
        }

        headMeta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
        headMeta.setDisplayName(player.getName());

        headItem.setItemMeta(headMeta);
        headItem.setAmount(1);

        gui.addItem(ItemBuilder.from(headItem).asGuiItem(event -> {
                                                            event.setCancelled(true);
                                                            if (isOwner) {
                                                                ShopUtils.removeAssistant(viewer, shopUuid, playerUuid, true);
                                                                gui.setCloseGuiAction(null);
                                                                new ListAssistantsGui(javaPlugin, viewer, shopUuid, isAdmin);
                                                            }
                                                        }));
    }
}