

package dev.tbm00.papermc.playershops64.command;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;

/* import java.util.Set;
import java.util.UUID;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashSet;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import org.bukkit.inventory.ItemStack; */

import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.enums.QueryType;
import dev.tbm00.papermc.playershops64.data.enums.SortType;
import dev.tbm00.papermc.playershops64.data.structure.Shop;
import dev.tbm00.papermc.playershops64.gui.ListShopsGui;
import dev.tbm00.papermc.playershops64.gui.ShopManageGui;
import dev.tbm00.papermc.playershops64.utils.GuiUtils;
import dev.tbm00.papermc.playershops64.utils.ShopUtils;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class AdminCmd implements TabExecutor {
    private final PlayerShops64 javaPlugin;

    public AdminCmd(PlayerShops64 javaPlugin) {
        this.javaPlugin = javaPlugin;
    }

    /**
     * Handles the /shopadmin command.
     * 
     * @param player the command sender
     * @param consoleCommand the command being executed
     * @param alias the alias used for the command
     * @param args the arguments passed to the command
     * @return true if the command was handled successfully, false otherwise
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!StaticUtils.hasPermission(sender, StaticUtils.ADMIN_PERM)) {
            StaticUtils.sendMessage(sender, "&cNo permission!");
            return true;
        } else if (args == null || args.length == 0) {
            Player player = (Player) sender;
            return handleMenuCmd(player, args);
        }

        String subCmd = args[0].toLowerCase();
        switch (subCmd) {
            case "help":
                return handleHelpCmd((Player) sender);
            case "menu":
            case "gui":
                return handleMenuCmd((Player) sender, args);
            case "give":
                return handleGiveCmd(sender, args);
            case "info":
                return handleInfoCmd((Player) sender);
            case "manage":
                return handleManageCmd((Player) sender);
            case "setstock":
                return handleSetStockCmd((Player) sender, args);
            case "setitem":
                return handleSetItemCmd((Player) sender);
            case "region":
                return handleRegionCmd((Player) sender, args);
            /*case "transferdisplayshopsdata":
                return handleTransferCmd(player);
            case "deleteplayershopsdata":
                return handleDeleteCmd(player);*/
            default: {
                return handleSearchCmd((Player) sender, args);
            }
        }
    }
    
    private boolean handleHelpCmd(Player player) {
        player.sendMessage(ChatColor.DARK_PURPLE + "--- " + ChatColor.LIGHT_PURPLE + "Admin Shop Commands" + ChatColor.DARK_PURPLE + " ---\n"
            + ChatColor.WHITE + "/shopadmin" + ChatColor.GRAY + " Open shop GUI as admin\n"
            + ChatColor.WHITE + "/shopadmin give <player> <item> [amount]" + ChatColor.GRAY + " Give shop or wand item to a player\n"
            + ChatColor.WHITE + "/shopadmin <player>" + ChatColor.GRAY + " Find & manage all <player>'s shops\n"
            + ChatColor.WHITE + "/shopadmin region <subCommand>" + ChatColor.GRAY + " Region system\n"
            + ChatColor.WHITE + "/shopadmin info" + ChatColor.GRAY + " Get info about shop in your view\n"
        );

        return true;
    }

    private boolean handleMenuCmd(Player player, String[] args) {
        if (args==null || args.length==0 || args.length==1) {
            new ListShopsGui(javaPlugin, javaPlugin.getShopHandler().getShopView(), player, true, SortType.MATERIAL, QueryType.NO_QUERY, null);
        } else {
            new ListShopsGui(javaPlugin, javaPlugin.getShopHandler().getShopView(), player, true, SortType.fromString(args[1]), QueryType.NO_QUERY, null);
        }
        return true;
    }

    private boolean handleSearchCmd(Player player, String[] args) {
        if (args.length>1) {
            String[] search = Arrays.copyOfRange(args, 1, args.length);
            GuiUtils.openGuiSearchResults(player, search, true, SortType.fromString(args[0]));
        } else {
            GuiUtils.openGuiSearchResults(player, args, true, SortType.MATERIAL);
        }
        return true;
    }

    private boolean handleGiveCmd(CommandSender sender, String[] args) {
        String argument = args.length >= 2 ? args[1] : null; // should be player
        String argument2 = args.length >= 3 ? args[2] : null; // should be item type
        String argument3 = args.length >= 4 ? args[3] : "1"; // should be amount

        int amount = 1;
        Integer j = Integer.parseInt(argument3);
        if (j!=null) amount = j;

        Player player = getPlayerFromCommand(sender, argument);
        if (player == null) {
            sender.sendMessage(ChatColor.RED + "Could not find target player!");
            return false;
        }

        if (argument2==null || argument2.isBlank()) {
            sender.sendMessage(ChatColor.RED + "Can't give nothing!");
            return false;
        } argument2 = argument2.replace("_","");

        if (argument2.equalsIgnoreCase("SELLWAND")) {
            StaticUtils.sendMessage(player, "&aReceived " + amount + " sell wand!");
            StaticUtils.addToInventoryOrDrop(player, StaticUtils.prepSellWandItemStack(amount));
        } else if (argument2.equalsIgnoreCase("DEPOSITWAND")) {
            StaticUtils.sendMessage(player, "&aReceived " + amount + " deposit wand!");
            StaticUtils.addToInventoryOrDrop(player, StaticUtils.prepDepositWandItemStack(amount));
        } else if (argument2.equalsIgnoreCase("REGIONWAND")) {
            StaticUtils.sendMessage(player, "&aReceived " + amount + " region wand!");
            StaticUtils.addToInventoryOrDrop(player, StaticUtils.prepRegionWandItemStack(amount));
        } else if (argument2.equalsIgnoreCase("SHOP") || argument2.equalsIgnoreCase("SHOPBLOCK") || argument2.equalsIgnoreCase("BASEBLOCK")) {
            StaticUtils.sendMessage(player, "&aReceived " + amount + " player shops!");
            StaticUtils.addToInventoryOrDrop(player, StaticUtils.prepPlayerShopItemStack(amount));
        } else if (argument2.equalsIgnoreCase("COUPON")) {
            StaticUtils.sendMessage(player, "&aReceived " + amount + " AdminShop coupons!");
            StaticUtils.addToInventoryOrDrop(player, StaticUtils.prepCouponItemStack(amount));
        } else {
            sender.sendMessage(ChatColor.RED + "'"+argument2+"' is not defined!");
            return false;
        }

        sender.sendMessage(ChatColor.GREEN + "Gave "+player.getName()+" "+amount+" "+argument2+"s!");
        return true;
    }

    private boolean handleInfoCmd(Player player) {
        Shop shop = javaPlugin.getShopHandler().getShopInFocus(player);
        if (shop==null) {
            StaticUtils.sendMessage(player, "&cError: No shop in your view!");
            return true;
        }

        String msg = ChatColor.DARK_PURPLE + "--- " + ChatColor.LIGHT_PURPLE + ShopUtils.getShopHint(shop.getUuid()) + ChatColor.DARK_PURPLE + " ---\n";
        if (shop.getItemStack()!=null) msg += ChatColor.WHITE + "- Item: " + ChatColor.GRAY + StaticUtils.getItemName(shop.getItemStack())+" x "+shop.getStackSize()+"\n";
        msg += ChatColor.WHITE + "- Owner: " + ChatColor.GRAY + shop.getOwnerName()+" "+shop.getOwnerUuid()+"\n";
        msg += ChatColor.WHITE + "- Stock: " + ChatColor.GRAY + shop.getItemStock()+" "+shop.hasInfiniteStock()+"\n";
        msg += ChatColor.WHITE + "- Money: " + ChatColor.GRAY + StaticUtils.formatDoubleUS(shop.getMoneyStock().doubleValue())+" "+shop.hasInfiniteMoney()+"\n";
        if (shop.getBuyPrice()!=null) msg += ChatColor.WHITE + "- BuyPrice: " + ChatColor.GRAY + StaticUtils.formatDoubleUS(shop.getBuyPrice().doubleValue())+"\n";
        if (shop.getSellPrice()!=null) msg += ChatColor.WHITE + "- SellPrice: " + ChatColor.GRAY + StaticUtils.formatDoubleUS(shop.getSellPrice().doubleValue())+"\n";
        if (shop.getLastTransactionDate()!=null) msg += ChatColor.WHITE + "- LastTransaction: " + ChatColor.GRAY + shop.getLastTransactionDate().toString()+"\n";
        if (shop.getCurrentEditor()!=null) msg += ChatColor.WHITE + "- CurrentEditor: " + ChatColor.GRAY + javaPlugin.getServer().getOfflinePlayer(shop.getCurrentEditor()).getName()+" "+shop.getCurrentEditor()+"\n";

        player.sendMessage(msg);
        return true;
    }

    private boolean handleManageCmd(Player player) {
        Shop shop = javaPlugin.getShopHandler().getShopInFocus(player);
        if (shop==null) {
            StaticUtils.sendMessage(player, "&cError: No shop in your view!");
            return true;
        }

        new ShopManageGui(javaPlugin, player, true, shop.getUuid());
        return true;
    }

    private boolean handleSetItemCmd(Player player) {
        Shop shop = javaPlugin.getShopHandler().getShopInFocus(player);
        if (shop==null) {
            StaticUtils.sendMessage(player, "&cError: No shop in your view!");
            return true;
        }

        ShopUtils.setShopItem(player, shop.getUuid(), true);
        return true;
    }

    private boolean handleSetStockCmd(Player player, String[] args) {
        if (args.length<2) {
            StaticUtils.sendMessage(player, "&cMust supply new stock amount..!");
            return true;
        }

        int value;
        try {
            value = Integer.parseInt(args[1]);
        } catch (Exception e) {
            StaticUtils.sendMessage(player, "&cCould not parse integer from "+args[1]+"!");
            return true;
        }

        Shop shop = javaPlugin.getShopHandler().getShopInFocus(player);
        if (shop==null) {
            StaticUtils.sendMessage(player, "&cError: No shop in your view!");
            return true;
        }

        ShopUtils.setShopStock(player, shop.getUuid(), value);
        return true;
    }

    private boolean handleRegionCmd(Player player, String[] args) {
        if (args.length < 2) {
            StaticUtils.sendMessage(player, "&f/shop region <info/deleteAll/setOwner> [newOwner]");
            return true;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "info":
                return handleRegionInfoCmd(player);
            case "setowner":
                return handleRegionSetOwnerCmd(player, args);
            case "setinfinitestock":
                return handleRegionSetInfStockCmd(player, args);
            case "setinfinitemoney":
                return handleRegionSetInfMoneyCmd(player, args);
            case "adjustbuyprice":
                return handleRegionAdjustBuyPriceCmd(player, args);
            case "deleteall":
                return handleRegionDeleteAllCmd(player);
            default:
                StaticUtils.sendMessage(player, "&f/shop region <info/deleteAll/setOwner/setInfiniteMoney/setInfiniteStock/adjustBuyPrice> [arg]");
                return true;
        }
    }


    private boolean handleRegionInfoCmd(Player player) {
        UUID uuid = player.getUniqueId();
        Pair<Location, Location> pair = javaPlugin.getShopHandler().regionPositionMap.get(uuid);

        if (pair == null) {
            StaticUtils.sendMessage(player, "&eYou don't have a region selected!");
            return true;
        }

        Location pos1 = pair.getLeft();
        Location pos2 = pair.getRight();
        
        StaticUtils.sendMessage(player, "&d--- &5Region Info &d---");
        if (pos1 != null) 
            StaticUtils.sendMessage(player, "&aPos1: &f" + pos1.getWorld().getName() + " " + pos1.getBlockX() + " " + pos1.getBlockY() + " " + pos1.getBlockZ());
        else StaticUtils.sendMessage(player, "&aPos1: &f<not set>");
        
        if (pos2 != null) 
            StaticUtils.sendMessage(player, "&aPos2: &f" + pos2.getWorld().getName() + " " + pos2.getBlockX() + " " + pos2.getBlockY() + " " + pos2.getBlockZ());
        else StaticUtils.sendMessage(player, "&aPos2: &f<not set>");
        
        return true;
    }

    private boolean handleRegionSetOwnerCmd(Player player, String[] args) {
        if (args.length < 3) {
            StaticUtils.sendMessage(player, "&eUsage: &f/shopadmin region setOwner <playerName>");
            return true;
        }

        String targetName = args[2];

        OfflinePlayer target;
        if (targetName.equalsIgnoreCase("null")) target = null;
        else {
            target = Bukkit.getOfflinePlayer(targetName);
            if (target == null || ( !target.isOnline() && !target.hasPlayedBefore())) {
                StaticUtils.sendMessage(player, "&cCould not find player " + targetName + "!");
                return true;
            } 
        }

        if (target == null) {
            StaticUtils.sendMessage(player, "&eSetting null owner..!");
        } else if (target.getName() != null) targetName = target.getName();

        UUID playerUuid = player.getUniqueId();
        Pair<Location, Location> pair = javaPlugin.getShopHandler().regionPositionMap.get(playerUuid);
        if (pair == null || pair.getLeft() == null || pair.getRight() == null) {
            StaticUtils.sendMessage(player, "&cYou must select both positions with the region wand first!");
            return true;
        }

        Location pos1 = pair.getLeft();
        Location pos2 = pair.getRight();
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            StaticUtils.sendMessage(player, "&cPos1 and Pos2 must be in the same world!");
            return true;
        }

        // cuboid bounds
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        String worldName = pos1.getWorld().getName();

        int changed = 0;
        for (Shop shop : javaPlugin.getShopHandler().getShopView().values()) {
            Location shopLocation = shop.getLocation();
            if (shopLocation == null || !shopLocation.getWorld().getName().equals(worldName)) continue;

            int x = shopLocation.getBlockX();
            int y = shopLocation.getBlockY();
            int z = shopLocation.getBlockZ();

            if (x < minX || x > maxX) continue;
            if (y < minY || y > maxY) continue;
            if (z < minZ || z > maxZ) continue;

            if (ShopUtils.setShopOwner(player, shop.getUuid(), target)) changed++;
        }

        StaticUtils.sendMessage(player, "&aSet owner to " + targetName + " for " + changed + " shops in the selected region!");
        return true;
    }

    private boolean handleRegionAdjustBuyPriceCmd(Player player, String[] args) {
        if (args.length < 3) {
            StaticUtils.sendMessage(player, "&eUsage: &f/shopadmin region adjustBuyPrice <value>");
            return true;
        }

        double value;
        try {
            value = Double.parseDouble(args[2]);
        } catch (Exception e) {
            StaticUtils.sendMessage(player, "&cCould not parse double from "+args[2]+"!");
            return true;
        }

        UUID playerUuid = player.getUniqueId();
        Pair<Location, Location> pair = javaPlugin.getShopHandler().regionPositionMap.get(playerUuid);
        if (pair == null || pair.getLeft() == null || pair.getRight() == null) {
            StaticUtils.sendMessage(player, "&cYou must select both positions with the region wand first!");
            return true;
        }

        Location pos1 = pair.getLeft();
        Location pos2 = pair.getRight();
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            StaticUtils.sendMessage(player, "&cPos1 and Pos2 must be in the same world!");
            return true;
        }

        // cuboid bounds
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        String worldName = pos1.getWorld().getName();

        int attempted = 0;
        for (Shop shop : javaPlugin.getShopHandler().getShopView().values()) {
            Location shopLocation = shop.getLocation();
            if (shopLocation == null || !shopLocation.getWorld().getName().equals(worldName)) continue;

            int x = shopLocation.getBlockX();
            int y = shopLocation.getBlockY();
            int z = shopLocation.getBlockZ();

            if (x < minX || x > maxX) continue;
            if (y < minY || y > maxY) continue;
            if (z < minZ || z > maxZ) continue;

            BigDecimal ogPrice = shop.getBuyPrice();
            Double newPrice = ogPrice.doubleValue() * value;

            ShopUtils.setBuyPrice(player, shop.getUuid(), newPrice);
            attempted++;
        }

        StaticUtils.sendMessage(player, "&aAttempted changing buy price of " + attempted + " shops in the selected region!");
        return true;
    }

    private boolean handleRegionSetInfMoneyCmd(Player player, String[] args) {
        if (args.length < 3) {
            StaticUtils.sendMessage(player, "&eUsage: &f/shopadmin region setInfiniteMoney <true/false>");
            return true;
        }

        boolean infinite = Boolean.parseBoolean(args[2]);

        UUID playerUuid = player.getUniqueId();
        Pair<Location, Location> pair = javaPlugin.getShopHandler().regionPositionMap.get(playerUuid);
        if (pair == null || pair.getLeft() == null || pair.getRight() == null) {
            StaticUtils.sendMessage(player, "&cYou must select both positions with the region wand first!");
            return true;
        }

        Location pos1 = pair.getLeft();
        Location pos2 = pair.getRight();
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            StaticUtils.sendMessage(player, "&cPos1 and Pos2 must be in the same world!");
            return true;
        }

        // cuboid bounds
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        String worldName = pos1.getWorld().getName();

        int changed = 0;
        for (Shop shop : javaPlugin.getShopHandler().getShopView().values()) {
            Location shopLocation = shop.getLocation();
            if (shopLocation == null || !shopLocation.getWorld().getName().equals(worldName)) continue;

            int x = shopLocation.getBlockX();
            int y = shopLocation.getBlockY();
            int z = shopLocation.getBlockZ();

            if (x < minX || x > maxX) continue;
            if (y < minY || y > maxY) continue;
            if (z < minZ || z > maxZ) continue;

            if (ShopUtils.setShopInfiniteMoney(player, shop.getUuid(), infinite)) changed++;
        }

        StaticUtils.sendMessage(player, "&aSet infinite money to " + infinite + " for " + changed + " shops in the selected region!");
        return true;
    }

    private boolean handleRegionSetInfStockCmd(Player player, String[] args) {
        if (args.length < 3) {
            StaticUtils.sendMessage(player, "&eUsage: &f/shopadmin region setInfiniteStock <true/false>");
            return true;
        }

        boolean infinite = Boolean.parseBoolean(args[2]);

        UUID playerUuid = player.getUniqueId();
        Pair<Location, Location> pair = javaPlugin.getShopHandler().regionPositionMap.get(playerUuid);
        if (pair == null || pair.getLeft() == null || pair.getRight() == null) {
            StaticUtils.sendMessage(player, "&cYou must select both positions with the region wand first!");
            return true;
        }

        Location pos1 = pair.getLeft();
        Location pos2 = pair.getRight();
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            StaticUtils.sendMessage(player, "&cPos1 and Pos2 must be in the same world!");
            return true;
        }

        // cuboid bounds
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        String worldName = pos1.getWorld().getName();

        int changed = 0;
        for (Shop shop : javaPlugin.getShopHandler().getShopView().values()) {
            Location shopLocation = shop.getLocation();
            if (shopLocation == null || !shopLocation.getWorld().getName().equals(worldName)) continue;

            int x = shopLocation.getBlockX();
            int y = shopLocation.getBlockY();
            int z = shopLocation.getBlockZ();

            if (x < minX || x > maxX) continue;
            if (y < minY || y > maxY) continue;
            if (z < minZ || z > maxZ) continue;

            if (ShopUtils.setShopInfiniteStock(player, shop.getUuid(), infinite)) changed++;
        }

        StaticUtils.sendMessage(player, "&aSet infinite stock to " + infinite + " for " + changed + " shops in the selected region!");
        return true;
    }

    private boolean handleRegionDeleteAllCmd(Player player) {
        UUID playerUuid = player.getUniqueId();
        Pair<Location, Location> pair = javaPlugin.getShopHandler().regionPositionMap.get(playerUuid);
        if (pair == null || pair.getLeft() == null || pair.getRight() == null) {
            StaticUtils.sendMessage(player, "&cYou must select both positions with the region wand first!");
            return true;
        }

        Location pos1 = pair.getLeft();
        Location pos2 = pair.getRight();
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            StaticUtils.sendMessage(player, "&cPos1 and Pos2 must be in the same world!");
            return true;
        }

        // cuboid bounds
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        String worldName = pos1.getWorld().getName();

        int deleted = 0;
        for (Shop shop : javaPlugin.getShopHandler().getShopView().values()) {
            Location shopLocation = shop.getLocation();
            if (shopLocation == null || !shopLocation.getWorld().getName().equals(worldName)) continue;

            int x = shopLocation.getBlockX();
            int y = shopLocation.getBlockY();
            int z = shopLocation.getBlockZ();

            if (x < minX || x > maxX) continue;
            if (y < minY || y > maxY) continue;
            if (z < minZ || z > maxZ) continue;

            if (ShopUtils.deleteShop(player, shop.getUuid(), null)) deleted++;
        }

        StaticUtils.sendMessage(player, "&aDeleted " + deleted + " shops in the selected region!");
        return true;
    }

    private Player getPlayerFromCommand(CommandSender sender, String arg) {
        if (arg == null) {
            sender.sendMessage(ChatColor.RED + "Could not find target player!");
            return null;
        } else {
            Player player = javaPlugin.getServer().getPlayer(arg);
            if (player == null) {
            }
            return player;
        }
    }

    /**
     * Handles tab completion for the /shopadmin command.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.clear();
            String[] subCmds = new String[]{"give","<player>","info","manage","setStock","setItem","region"/*,"transferDisplayShopsData","deletePlayerShopsData"*/};
            for (String n : subCmds) {
                if (n!=null && n.startsWith(args[0].toLowerCase())) 
                    list.add(n);
            }
            Bukkit.getOnlinePlayers().forEach(player -> {
                if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())&&args[0].length()>0)
                    list.add(player.getName());
            });
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give")) {
                Bukkit.getOnlinePlayers().forEach(player -> {
                    if (player.getName().toLowerCase().startsWith(args[1].toLowerCase()))
                        list.add(player.getName());
                });
            } else if (args[0].equalsIgnoreCase("region")) {
                String[] regionSub = new String[]{"info","setOwner","deleteAll","setInfiniteMoney","setInfiniteStock","adjustBuyPrice"};
                for (String n : regionSub) {
                    if (n.toLowerCase().startsWith(args[1].toLowerCase()))
                        list.add(n);
                }
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give")) {
                String[] subCmds = new String[]{"SHOP_BLOCK","SELL_WAND","DEPOSIT_WAND","REGION_WAND","COUPON"};
                for (String n : subCmds) {
                    if (n!=null && n.startsWith(args[2].toUpperCase())) 
                        list.add(n);
                }
            } else if (args[0].equalsIgnoreCase("region") && args[1].equalsIgnoreCase("setOwner")) {
                Bukkit.getOnlinePlayers().forEach(player -> {
                    if (player.getName().toLowerCase().startsWith(args[2].toLowerCase()))
                        list.add(player.getName());
                });
            }
        }
        return list;
    }


    /*private boolean handleDeleteCmd(Player player) {
        List<UUID> toDelete = new ArrayList<>();
        for (Shop shop : javaPlugin.getShopHandler().getShopView().values()) {
            toDelete.add(shop.getUuid());
        }

        int i = 0;
        for (UUID id : toDelete) {
            javaPlugin.getShopHandler().deleteShopObject(id);
            i++;
        }

        player.sendMessage(ChatColor.GREEN + "Deleted PlayerShops64 data, touched "+i+" psShops!");
        return true;
    }

    private boolean handleTransferCmd(Player player) {
        if (javaPlugin.dsHook==null) {
            player.sendMessage(ChatColor.YELLOW + "DisplayShops not loaded...");
            return true;
        }

        List<xzot1k.plugins.ds.api.objects.Shop> shops = new ArrayList<>(javaPlugin.dsHook.getManager().getShopMap().values());

        final int perTick = 50;
        UUID playerId = player.getUniqueId();

        new org.bukkit.scheduler.BukkitRunnable() {
            private int index = 0;
            private int touched = 0;

            @Override
            public void run() {
                int processedThisTick = 0;

                while (index < shops.size() && processedThisTick < perTick) {
                    xzot1k.plugins.ds.api.objects.Shop dsShop = shops.get(index++);
                    if (createPSfromDS(dsShop)) touched++;
                    processedThisTick++;
                }

                if (index >= shops.size()) {
                    Player p = Bukkit.getPlayer(playerId);
                    if (p != null) p.sendMessage(ChatColor.GREEN + "Mimicked DisplayShops data, touched " + touched + " dsShops!");
                    cancel();
                }
            }
        }.runTaskTimer(javaPlugin, 0L, 1L);

        player.sendMessage(ChatColor.YELLOW + "Starting DisplayShops migration in batches (50/tick)...");
        return true;
    }

    private boolean createPSfromDS(xzot1k.plugins.ds.api.objects.Shop dsShop) {
        UUID ownerUuid = dsShop.getOwnerUniqueId();
        String ownerName = ownerUuid==null ? null : javaPlugin.getServer().getOfflinePlayer(ownerUuid).getName();
        
        String worldName = dsShop.getBaseLocation().getWorldName();
        World world = javaPlugin.getServer().getWorld(worldName);
        if (world == null) {
            StaticUtils.log(ChatColor.RED, "DS->PS transfer: world '" + worldName + "' is not loaded, skipping shop.");
            return false;
        }

        Location location = new Location(world, (int)dsShop.getBaseLocation().getX(), (int)dsShop.getBaseLocation().getY(), (int)dsShop.getBaseLocation().getZ());
        if (javaPlugin.getShopHandler().getShopAtBlock(location)!=null) {  // or via your handler
            StaticUtils.log(ChatColor.YELLOW, "DS->PS: shop already exists at " + location + ", skipping.");
            return false;
        }

        Block shopBlock = world.getBlockAt(location);
        boolean invalidShopBlock = shopBlock.getType().equals(Material.AIR);
        Material baseMaterial = invalidShopBlock ? null : shopBlock.getType();

        int itemStock = dsShop.getStock();
        BigDecimal moneyStock = BigDecimal.valueOf(dsShop.getStoredBalance());
        boolean infMoney = false, infStock = false;
        if (moneyStock.compareTo(BigDecimal.ZERO) < 0) {
            infMoney = true;
            infStock = true;
            moneyStock = BigDecimal.ZERO;
            itemStock = 0;
            StaticUtils.log(ChatColor.BLUE, "DS->PS transfer: Found adminshop by dsShop's moneyStock<0..!\n"+ownerName+"'s "+StaticUtils.getItemName(dsShop.getShopItem())+" -- B: "+dsShop.getBuyPrice(false)+", S: "+dsShop.getSellPrice(false));
        } else if (itemStock < 0) {
            infMoney = true;
            infStock = true;
            moneyStock = BigDecimal.ZERO;
            itemStock = 0;
            StaticUtils.log(ChatColor.BLUE, "DS->PS transfer: Found adminshop by dsShop's itemStock<0..!\n"+ownerName+"'s "+StaticUtils.getItemName(dsShop.getShopItem())+" -- B: "+dsShop.getBuyPrice(false)+", S: "+dsShop.getSellPrice(false));
        }

        ItemStack itemStack = (dsShop.getShopItem()==null) ? null : dsShop.getShopItem().clone();
        if (invalidShopBlock && !infStock) {
            if (itemStack==null || itemStack.getType().equals(Material.AIR) || itemStock==0) {
                if (moneyStock.compareTo(BigDecimal.ZERO) > 0) dsShop.returnBalance();
                return true;
            } else {
                try {
                    world.getBlockAt(location).setType(Material.LECTERN);
                    baseMaterial = Material.LECTERN;
                } catch (Exception e) {
                    StaticUtils.log(ChatColor.RED, "DS->PS transfer: Caught exception placing new lectern shop block for DS->PS shop transfer..!");
                    e.printStackTrace();
                    return false;
                }
            }
        }

        if (itemStack!=null) itemStack.setAmount(1);
        
        int dsStackSize = dsShop.getShopItemAmount();
        if (dsStackSize<1) {
            dsStackSize = 1;
        }

        BigDecimal buyPrice = null;
        double dsBuy = dsShop.getBuyPrice(false);
        if (dsBuy > 0) {
            buyPrice = BigDecimal
                .valueOf(dsBuy)
                .divide(BigDecimal.valueOf(dsStackSize), 2, RoundingMode.DOWN);
        }

        BigDecimal sellPrice = null;
        double dsSell = dsShop.getSellPrice(false);
        if (dsSell > 0) {
            sellPrice = BigDecimal
                .valueOf(dsSell)
                .divide(BigDecimal.valueOf(dsStackSize), 2, RoundingMode.DOWN);
        }

        long lastTs = Math.max(dsShop.getLastBuyTimeStamp(), dsShop.getLastSellTimeStamp());
        Date lastTranscationDate = (lastTs > 0) ? new Date(lastTs) : null;

        Set<UUID> assistants = new HashSet<>();
        if (dsShop.getAssistants()!=null && !dsShop.getAssistants().isEmpty()) {
            for (UUID assistantUuid : dsShop.getAssistants()) {
                assistants.add(assistantUuid);
            }
        }

        UUID shopUuid = UUID.randomUUID();

        Shop psShop = new Shop(shopUuid,
                    ownerUuid,
                    ownerName,
                    world,
                    location,
                    itemStack,
                    1,
                    itemStock,
                    moneyStock,
                    buyPrice,
                    sellPrice,
                    lastTranscationDate,
                    infMoney,
                    infStock,
                    null,
                    0,
                    baseMaterial,
                    assistants,
                    null);

        ShopUtils.createShop(psShop);
        return true;
    }*/
}