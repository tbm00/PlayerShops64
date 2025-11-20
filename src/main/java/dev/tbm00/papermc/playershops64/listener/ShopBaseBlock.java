package dev.tbm00.papermc.playershops64.listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.structure.Shop;
import dev.tbm00.papermc.playershops64.data.enums.AdjustType;
import dev.tbm00.papermc.playershops64.gui.ShopManageGui;
import dev.tbm00.papermc.playershops64.gui.ShopTransactionGui;
import dev.tbm00.papermc.playershops64.utils.ShopUtils;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class ShopBaseBlock implements Listener {
    private final PlayerShops64 javaPlugin;

    public ShopBaseBlock(PlayerShops64 javaPlugin) {
        this.javaPlugin = javaPlugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
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

        Player owner = event.getPlayer();
        World world = block.getWorld();

        ShopUtils.createShop(owner, world, location);

        StaticUtils.sendMessage(owner, "&aCreated a new PlayerShop! Click to manage it, or sneak-right-click to start selling your held item");
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockClick(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if (!isProtectedShopBlock(block)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        Shop shop = javaPlugin.getShopHandler().getShopAtBlock(block.getLocation());
        if (shop == null) {
            StaticUtils.sendMessage(player, "&cError: Shop data not found for this block..!");
            return;
        }

        ItemStack heldItem = player.getItemInHand().clone();
        Action action = event.getAction();
        boolean isSneaking = player.isSneaking();
        boolean isAdmin = StaticUtils.hasPermission(player, StaticUtils.ADMIN_PERM);
        boolean isManager = (shop.getOwnerUuid()!=null && player.getUniqueId().equals(shop.getOwnerUuid())) || shop.isAssistant(player.getUniqueId());
        boolean isHoldingShopItem = (shop.getItemStack()==null) ? false : shop.getItemStack().isSimilar(heldItem);

        if (isAdmin && !isManager) {
            if (isSneaking) {
                new ShopManageGui(javaPlugin, player, true, shop.getUuid());
                return;
            } else {
                if (shop.getItemStack()==null) {
                    StaticUtils.sendMessage(player, "&cThis shop does not have a sale item set up!");
                    return;
                }
                if (shop.getBuyPrice()==null && shop.getSellPrice()==null) {
                    StaticUtils.sendMessage(player, "&cThis shop has buying and selling both disabled!");
                    return;
                }
                new ShopTransactionGui(javaPlugin, player, true, shop.getUuid(), 1, true);
                return;
            }
        }

        if (isManager) {
            if (isSneaking) {
                if (action==Action.LEFT_CLICK_BLOCK) {
                    if (isHoldingShopItem) {
                        ShopUtils.depositStockFromHand(player, shop.getUuid());
                        return;
                    } else if (shop.getItemStack()==null) {
                        StaticUtils.sendMessage(player, "&eHold an item then sneak-right-click to set a sale item!");
                        return;
                    } else {
                        ShopUtils.adjustStock(player, shop.getUuid(), AdjustType.ADD, 1);
                        return;
                    }
                } else if (action==Action.RIGHT_CLICK_BLOCK) {
                    if (shop.getItemStack()==null && heldItem!=null && !heldItem.getType().equals(Material.AIR)) {
                        ShopUtils.setShopItem(player, shop.getUuid(), false);
                        return;
                    } else if (shop.getItemStack()==null) {
                        StaticUtils.sendMessage(player, "&eHold an item then sneak-right-click to set a sale item!");
                        return;
                    } else {
                        ShopUtils.adjustStock(player, shop.getUuid(), AdjustType.REMOVE, 1);
                        return;
                    }
                } else return;
            } else {
                if (action==Action.LEFT_CLICK_BLOCK || action==Action.RIGHT_CLICK_BLOCK) {
                    new ShopManageGui(javaPlugin, player, false, shop.getUuid());
                    return;
                } else return;
            }
        }

        /* regular player */ {
            if (shop.getItemStack()==null) {
                StaticUtils.sendMessage(player, "&cThis shop does not have a sale item set up!");
                return;
            }
            if (shop.getBuyPrice()==null && shop.getSellPrice()==null) {
                StaticUtils.sendMessage(player, "&cThis shop has buying and selling both disabled!");
                return;
            }
            new ShopTransactionGui(javaPlugin, player, true, shop.getUuid(), 1, true);
            return;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        Block piston = event.getBlock();
        BlockFace dir = event.getDirection();

        Block head = piston.getRelative(dir);
        if (isProtectedShopBlock(head)) {
            event.setCancelled(true);
            return;
        }

        for (Block moved : event.getBlocks()) {
            Block destination = moved.getRelative(dir);
            if (isProtectedShopBlock(moved) || isProtectedShopBlock(destination)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        BlockFace dir = event.getDirection();
        BlockFace pullToward = dir.getOppositeFace();

        Block head = event.getBlock().getRelative(dir);
        if (isProtectedShopBlock(head)) {
            event.setCancelled(true);
            return;
        }

        for (Block moved : event.getBlocks()) {
            Block destination = moved.getRelative(pullToward);
            if (isProtectedShopBlock(moved) || isProtectedShopBlock(destination)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (isProtectedShopBlock(event.getBlock())) event.setCancelled(true);
        
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (isProtectedShopBlock(event.getBlock())) event.setCancelled(true);
        
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (isProtectedShopBlock(event.getBlock())) event.setCancelled(true);
    
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (isProtectedShopBlock(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(this::isProtectedShopBlock);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(this::isProtectedShopBlock);
    }

    private boolean isProtectedShopBlock(Block b) {
        return javaPlugin.getShopHandler().hasShopAtBlock(b.getLocation());
    }
}