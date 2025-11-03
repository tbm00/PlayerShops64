

package dev.tbm00.papermc.playershops64.command;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.enums.QueryType;
import dev.tbm00.papermc.playershops64.data.enums.SortType;
import dev.tbm00.papermc.playershops64.data.structure.Shop;
import dev.tbm00.papermc.playershops64.gui.ListShopsGui;
import dev.tbm00.papermc.playershops64.utils.*;

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
        } else if (args.length == 0) {
            Player player = (Player) sender;
            return handleMenuCmd(player);
        }

        Player player = (Player) sender;
        String subCmd = args[0].toLowerCase();
        switch (subCmd) {
            case "help":
                return handleHelpCmd(player);
            case "menu":
            case "gui":
                return handleMenuCmd(player);
            case "give":
                return handleGiveCmd(player, args);
            case "transferdisplayshopsdata":
                return handleTransferCmd(player);
            case "deleteplayershopsdata":
                return handleDeleteCmd(player);
            default: {
                return handleSearchCmd(player, args);
            }
        }
    }
    
    private boolean handleHelpCmd(Player player) {
        player.sendMessage(ChatColor.DARK_PURPLE + "--- " + ChatColor.LIGHT_PURPLE + "Admin Shop Commands" + ChatColor.DARK_PURPLE + " ---\n"
            + ChatColor.WHITE + "/shopadmin give <player> <item> [amount]" + ChatColor.GRAY + " Give shop or wand item to a player\n"
            + ChatColor.WHITE + "/shopadmin <player>" + ChatColor.GRAY + " Find & manage all <player>'s shops\n"
        );

        return true;
    }

    private boolean handleMenuCmd(Player player) {
        new ListShopsGui(javaPlugin, javaPlugin.getShopHandler().getShopView(), player, true, SortType.MATERIAL, QueryType.NO_QUERY, null);
        return true;
    }

    private boolean handleSearchCmd(Player player, String[] args) {
        GuiUtils.openGuiSearchResults(player, args, true);
        return true;
    }

    private boolean handleDeleteCmd(Player player) {
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
        } else if (argument2.equalsIgnoreCase("SHOP") || argument2.equalsIgnoreCase("SHOPBLOCK") || argument2.equalsIgnoreCase("BASEBLOCK")) {
            StaticUtils.sendMessage(player, "&aReceived " + amount + " player shops!");
            StaticUtils.addToInventoryOrDrop(player, StaticUtils.prepPlayerShopItemStack(amount));
        } else {
            sender.sendMessage(ChatColor.RED + "'"+argument2+"' is not defined!");
            return false;
        }

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
            String[] subCmds = new String[]{"give","<player>","transferDisplayShopsData","deletePlayerShopsData"};
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
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give")) {
                String[] subCmds = new String[]{"SHOP_BLOCK","SELL_WAND","DEPOSIT_WAND"};
                for (String n : subCmds) {
                    if (n!=null && n.startsWith(args[2].toUpperCase())) 
                        list.add(n);
                }
            }
        }
        return list;
    }
}