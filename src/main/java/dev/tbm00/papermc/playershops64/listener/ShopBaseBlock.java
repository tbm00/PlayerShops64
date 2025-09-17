package dev.tbm00.papermc.playershops64.listener;

import java.math.BigDecimal;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.Shop;
import dev.tbm00.papermc.playershops64.utils.GuiUtils;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class ShopBaseBlock implements Listener {
    private final PlayerShops64 javaPlugin;

    public ShopBaseBlock(PlayerShops64 javaPlugin) {
        this.javaPlugin = javaPlugin;
    }

    @EventHandler
    public void onShopBaseBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (!block.getType().equals(Material.LECTERN)) return;

        ItemStack heldItem = event.getItemInHand();
        ItemMeta heldItemMeta = heldItem.getItemMeta();
        if (heldItemMeta == null) return;

        // Verify held item has base block PDC key
        PersistentDataContainer itemDataContainer = heldItemMeta.getPersistentDataContainer();
        if (!itemDataContainer.has(StaticUtils.SHOP_KEY, PersistentDataType.STRING)) return;
        
        // Prevent duplicate shops at tsame location
        Location location = block.getLocation();
        if (javaPlugin.getShopHandler().hasShopAtBlock(location)) {
            event.setCancelled(true);
            StaticUtils.sendMessage(event.getPlayer(), "&cThere is already a PlayerShop at this block.");
            return;
        }

        // Apply PDC key to new block's tileState
        BlockState blockState = block.getState();
        if (blockState instanceof TileState tileState) {
            tileState.getPersistentDataContainer().set(StaticUtils.SHOP_KEY, PersistentDataType.STRING, "true");
            tileState.update(true, false); // apply to world
        }

        Player owner = event.getPlayer();
        World world = block.getWorld();


        Shop shop = new Shop(UUID.randomUUID(),
                            owner.getUniqueId(),
                            owner.getName(),
                            world,
                            location,
                            null,
                            1,
                            0,
                            BigDecimal.ZERO,
                            BigDecimal.valueOf(100.0),
                            BigDecimal.valueOf(50.0),
                            null,
                            false,
                            false,
                            null);

        javaPlugin.getShopHandler().upsertShop(shop);
        StaticUtils.sendMessage(owner, "&aCreated a new PlayerShop! Click to manage it, or sneak-left-click to start selling your held item");
        StaticUtils.log(ChatColor.GOLD, owner.getName() + " created a shop " + shop.getUuid() + " in " + world.getName()+ " @ " + block.getX() + "," + block.getY() + "," + block.getZ());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockClick(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if (!isProtected(block)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        Shop shop = javaPlugin.getShopHandler().getShopAtBlock(block.getLocation());
        if (shop == null) {
            StaticUtils.sendMessage(player, "&cShop data not found for this block. Try relogging or contact staff.");
            return;
        }

        boolean isOwner = player.getUniqueId().equals(shop.getOwnerUuid());
        boolean isSneaking = player.isSneaking();
        Action action = event.getAction();

        if (isOwner) {
            if (isSneaking) {
                if (action==Action.LEFT_CLICK_BLOCK) { // Delete shop
                    javaPlugin.getShopHandler().removeShop(shop.getUuid());
                    block.setType(Material.AIR, false);
                    StaticUtils.sendMessage(player, "&aDeleted shop!");
                    StaticUtils.giveItem(player, StaticUtils.prepPlayerShopItemStack(1));
                    return;
                } else if (action==Action.RIGHT_CLICK_BLOCK) { // Set shop item
                    ItemStack hand = player.getInventory().getItemInMainHand();
                    if (hand == null || hand.getType().isAir()) {
                        StaticUtils.sendMessage(player, "&cHold an item in your main hand to set the shop item while sneak-right-clicking.");
                        return;
                    }

                    if (shop.getItemStock()>0) {
                        StaticUtils.sendMessage(player, "&cShop must have an empty item stock before changing shop items.");
                        return;
                    }

                    int handCount = hand.getAmount();
                    ItemStack one = hand.clone();
                    one.setAmount(1);
                    shop.setItemStack(one);
                    shop.setItemStock(1);
                    shop.setStackSize(1);

                    if (handCount>1) {
                        hand.setAmount(handCount-1);
                    } else {player.getInventory().setItemInMainHand(null);}

                    javaPlugin.getShopHandler().upsertShop(shop);
                    StaticUtils.sendMessage(player, "&aShop item set to &e" + one.getType().name());
                    return;
                } else return;
            } else {
                if (action==Action.LEFT_CLICK_BLOCK || action==Action.RIGHT_CLICK_BLOCK) {
                    GuiUtils.openGuiManage(player, shop);
                    return;
                } else return;
            }
        } else {
            GuiUtils.openGuiTransaction(player, shop);
            return;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (isProtected(block)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(this::isProtected);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(this::isProtected);
    }

    private boolean isProtected(Block b) {
        BlockState blockState = b.getState();
        if (!(blockState instanceof TileState tileState)) return false;
        return tileState.getPersistentDataContainer().has(StaticUtils.SHOP_KEY, PersistentDataType.STRING);
    }
}