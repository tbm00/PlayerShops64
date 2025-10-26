

package dev.tbm00.papermc.playershops64.command;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.enums.AdjustType;
import dev.tbm00.papermc.playershops64.data.enums.QueryType;
import dev.tbm00.papermc.playershops64.data.enums.SortType;
import dev.tbm00.papermc.playershops64.data.structure.Shop;
import dev.tbm00.papermc.playershops64.gui.DepositGui;
import dev.tbm00.papermc.playershops64.gui.ListCategoriesGui;
import dev.tbm00.papermc.playershops64.gui.ListShopsGui;
import dev.tbm00.papermc.playershops64.gui.SellGui;
import dev.tbm00.papermc.playershops64.utils.*;

public class ShopCmd implements TabExecutor {
    private final PlayerShops64 javaPlugin;

    public ShopCmd(PlayerShops64 javaPlugin) {
        this.javaPlugin = javaPlugin;
    }

    /**
     * Handles the /testshop command.
     * 
     * @param player the command sender
     * @param consoleCommand the command being executed
     * @param alias the alias used for the command
     * @param args the arguments passed to the command
     * @return true if the command was handled successfully, false otherwise
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (sender instanceof ConsoleCommandSender) {
            StaticUtils.sendMessage(sender, "&cThis command cannot be run through the console!");
            return true;
        } else if (!StaticUtils.hasPermission(sender, StaticUtils.PLAYER_PERM)) {
            StaticUtils.sendMessage(sender, "&cNo permission!");
            return true;
        } else if (args.length == 0) {
            new ListCategoriesGui(javaPlugin, (Player) sender, false);
            return true;
        }

        Player player = (Player) sender;
        String subCmd = args[0].toLowerCase();
        switch (subCmd) {
            case "help":
                return handleHelpCmd(player);
            case "buy":
                return handleBuyCmd(player, args);
            case "manage":
                return handleManageCmd(player);
            case "all":
                return handleAllCmd(player);
            case "hand":
                return handleHandCmd(player);
            case "sellgui":
                return handleSellGuiCmd(player);
            case "depositgui":
                return handleDepositGuiCmd(player);
            case "depositmoney":
                return handleDepositCmd(player, args);
            case "withdrawmoney":
                return handleWithdrawCmd(player, args);
            default: {
                return handleSearchCmd(player, args);
            }
        }
    }
    
    private boolean handleHelpCmd(Player player) {
        player.sendMessage(ChatColor.DARK_PURPLE + "--- " + ChatColor.LIGHT_PURPLE + "Player Shop Commands" + ChatColor.DARK_PURPLE + " ---\n"
            + ChatColor.WHITE + "/testshop" + ChatColor.GRAY + " Base player command\n"
        );

        player.sendMessage(ChatColor.DARK_PURPLE + "--- " + ChatColor.LIGHT_PURPLE + "Shop Owner Commands" + ChatColor.DARK_PURPLE + " ---\n"
            + ChatColor.WHITE + "/shop buy <amount>" + ChatColor.GRAY + " Buy shop creation item(s)\n"
            + ChatColor.WHITE + "/shop manage" + ChatColor.GRAY + " View and manage all your shops\n"
            + ChatColor.WHITE + "/shop depositGui" + ChatColor.GRAY + " Open a GUI and deposit items into your shops\n"
            + ChatColor.WHITE + "/shop depositMoney all/view max/<amount>" + ChatColor.GRAY + " Deposit money into your shops\n"
            + ChatColor.WHITE + "/shop withdrawMoney all/view max/<amount>" + ChatColor.GRAY + " Withdraw money from your shops\n"
        );
        player.sendMessage(ChatColor.DARK_AQUA + "--- " + ChatColor.AQUA + "Shopper Commands" + ChatColor.DARK_AQUA + " ---\n"
            + ChatColor.WHITE + "/shop" + ChatColor.GRAY + " Open shop category GUI\n"
            + ChatColor.WHITE + "/shop all" + ChatColor.GRAY + " Open all shops GUI\n"
            + ChatColor.WHITE + "/shop hand" + ChatColor.GRAY + " Find all <item in hand> shops\n"
            + ChatColor.WHITE + "/shop <item>" + ChatColor.GRAY + " Find all <item> shops\n"
            + ChatColor.WHITE + "/shop <player>" + ChatColor.GRAY + " Find all <player>'s shops\n"
            + ChatColor.WHITE + "/sellgui" + ChatColor.GRAY + " Open a GUI and sell items to the best priced avaliable shops"
        );
        return true;
    }

    private boolean handleBuyCmd(Player player, String[] args) {
        Integer amount = 1;
        try {
            if (args.length>1) {
                amount = Integer.parseInt(args[1]);
            }
        } catch (Exception ignored) {
            StaticUtils.sendMessage(player, "&cError: Failed to parse argument!");
            return false;
        }
        
        double totalCost = javaPlugin.getConfigHandler().getShopBlockBuyPrice() * amount;

        if (!javaPlugin.getVaultHook().hasMoney(player, totalCost)) {
            StaticUtils.sendMessage(player, "&cYou don't have enough money to buy "+amount+" shops for $"+StaticUtils.formatIntUS(javaPlugin.getConfigHandler().getShopBlockBuyPrice())+" each!");
            return true;
        }

        if (!javaPlugin.getVaultHook().removeMoney(player, totalCost)) {
            StaticUtils.sendMessage(player, "&cError: Failed to remove $"+totalCost+" from your balance!");
            return true;
        }

        StaticUtils.addToInventoryOrDrop(player, StaticUtils.prepPlayerShopItemStack(amount));
        StaticUtils.sendMessage(player, "&aYou bought "+amount+" shops for $"+StaticUtils.formatIntUS(totalCost)+"!");
        return true;
    }

    private boolean handleDepositGuiCmd(Player player) {
        new DepositGui(javaPlugin, player);
        return true;
    }

    private boolean handleSellGuiCmd(Player player) {
        new SellGui(javaPlugin, player);
        return true;
    }

    private boolean handleAllCmd(Player player) {
        new ListShopsGui(javaPlugin, javaPlugin.getShopHandler().getShopView(), player, false, SortType.MATERIAL, QueryType.NO_QUERY, null);
        return true;
    }

    private boolean handleManageCmd(Player player) {
        new ListShopsGui(javaPlugin, javaPlugin.getShopHandler().getShopView(), player, false, SortType.MATERIAL, QueryType.PLAYER_UUID, player.getUniqueId().toString());
        return true;
    }

    private boolean handleHandCmd(Player player) {
        ItemStack handItem = player.getItemInHand();

        if (handItem==null || handItem.getType().equals(Material.AIR)) {
            StaticUtils.sendMessage(player, "&cError: No item in your hand!");
            return true;
        }

        String[] arr = {StaticUtils.getItemName(handItem)};
        GuiUtils.openGuiSearchResults(player, arr, false);
        return true;
    }

    private boolean handleSearchCmd(Player player, String[] args) {
        GuiUtils.openGuiSearchResults(player, args, false);
        return true;
    }

    /**
     * Handles the sub command for depositing money into shops.
     * 
     * @param player the command sender
     * @param args the arguments passed to the command
     * @return true if command was processed successfully
     */
    private boolean handleDepositCmd(Player player, String[] args) {
        if (args.length<3) {
            StaticUtils.sendMessage(player, "&f/shop depositMoney view/all max/<amount> &7Deposit money into your shops");
            return true;
        }

        UUID playerUuid = player.getUniqueId();
        double pocket_balance = javaPlugin.getVaultHook().getBalance(player), deposit_per, total_deposited = 0;

        String arg2 = args[1].toLowerCase(), arg3 = args[2].toLowerCase();
        switch (arg2) {
            case "all": {
                List<UUID> ownedShops = new ArrayList<>(javaPlugin.getShopHandler().getPlayersShops(playerUuid));
                if (ownedShops.size()<1) {
                    StaticUtils.sendMessage(player, "&cError: Couldn't find any of your DisplayShops!");
                    return true;
                }

                if (arg3.equals("max")) {
                    deposit_per = Math.floor(pocket_balance / ownedShops.size());
                } else {
                    try {
                        deposit_per = Double.parseDouble(arg3);
                    } catch (Exception e) {
                        StaticUtils.sendMessage(player, "&cError: Couldn't detect a number from '"+arg3+"'!");
                        return true;
                    }
                }

                int i = 0;
                for (UUID shopUuid : ownedShops) {
                    i++;
                    total_deposited += ShopUtils.adjustBalance(player, shopUuid, AdjustType.ADD, deposit_per, false);
                }
                StaticUtils.sendMessage(player, "&aDeposited a total of $" + StaticUtils.formatIntUS(total_deposited) + " into " + i + " of your shops!");
                return true;
            }
            case "view": {
                if (arg3.equals("max")) {
                    deposit_per = pocket_balance;
                } else {
                    try {
                        deposit_per = Double.parseDouble(arg3);
                    } catch (Exception e) {
                        StaticUtils.sendMessage(player, "&cError: Couldn't detect a number from '"+arg3+"'!");
                        return true;
                    }
                }

                Shop shop = javaPlugin.getShopHandler().getShopInFocus(player);
                if (shop==null) {
                    StaticUtils.sendMessage(player, "&cError: No shop in your view!");
                    return true;
                }

                if (shop.getOwnerUuid()==null || !shop.getOwnerUuid().equals(playerUuid)) {
                    StaticUtils.sendMessage(player, "&cError: This shop is not yours!");
                    return true;
                }

                if (shop.getItemStack()==null) {
                    StaticUtils.sendMessage(player, "&cError: Shop has no item set!");
                    return true;
                }
                
                ShopUtils.adjustBalance(player, shop.getUuid(), AdjustType.ADD, deposit_per, true);
                return true;
            }
            default: {
                StaticUtils.sendMessage(player, "&cError: Unknown argument: " + arg2);
                return true;
            }
        }
    }

    /**
     * Handles the sub command for withdrawing money from shops.
     * 
     * @param player the command sender
     * @param args the arguments passed to the command
     * @return true if command was processed successfully
     */
    private boolean handleWithdrawCmd(Player player, String[] args) {
        if (args.length<3) {
            StaticUtils.sendMessage(player, "&f/shop withdrawMoney view/all max/<amount> &7Withdraw money from your shops");
            return true;
        }

        UUID playerUuid = player.getUniqueId();
        double total_withdrawn = 0;

        String arg2 = args[1].toLowerCase(), arg3 = args[2].toLowerCase();
        switch (arg2) {
            case "all": {
                List<UUID> ownedShops = new ArrayList<>(javaPlugin.getShopHandler().getPlayersShops(playerUuid));
                if (ownedShops.size()<1) {
                    StaticUtils.sendMessage(player, "&cError: Couldn't find any of your DisplayShops!");
                    return true;
                }

                int i = 0;

                if (arg3.equals("max")) {
                    for (UUID shopUuid : ownedShops) {
                        Shop shop = javaPlugin.getShopHandler().getShop(shopUuid);

                        if (shop.getItemStack()==null) continue;
                        if (shop.getOwnerUuid()==null || !shop.getOwnerUuid().equals(playerUuid)) continue;

                        i++;
                        total_withdrawn += ShopUtils.adjustBalance(player, shopUuid, AdjustType.SET, 0, false);
                    }
                } else {
                    double withdraw_per;
                    try {
                        withdraw_per = Double.parseDouble(arg3);
                    } catch (Exception e) {
                        StaticUtils.sendMessage(player, "&cError: Couldn't detect a number from '"+arg3+"'!");
                        return true;
                    }

                    for (UUID shopUuid : ownedShops) {
                        i++;
                        total_withdrawn += ShopUtils.adjustBalance(player, shopUuid, AdjustType.REMOVE, withdraw_per, false);
                    }
                }

                StaticUtils.sendMessage(player, "&aWithdrawn a total of $" + StaticUtils.formatIntUS(total_withdrawn) + " from " + i + " of your shops!");
                return true;
            }
            case "view": {
                Shop shop = javaPlugin.getShopHandler().getShopInFocus(player);
                if (shop==null) {
                    StaticUtils.sendMessage(player, "&cError: No shop in your view!");
                    return true;
                }

                if (shop.getOwnerUuid()==null || !shop.getOwnerUuid().equals(playerUuid)) {
                    StaticUtils.sendMessage(player, "&cError: This shop is not yours!");
                    return true;
                }

                if (shop.getItemStack()==null) {
                    StaticUtils.sendMessage(player, "&cError: Shop has no item set!");
                    return true;
                }

                if (arg3.equals("max")) {
                    ShopUtils.adjustBalance(player, shop.getUuid(), AdjustType.SET, 0, true);
                    return true;
                } else {
                    double withdraw_per;
                    try {
                        withdraw_per = Double.parseDouble(arg3);
                    } catch (Exception e) {
                        StaticUtils.sendMessage(player, "&cError: Couldn't detect a number from '"+arg3+"'!");
                        return true;
                    }

                    ShopUtils.adjustBalance(player, shop.getUuid(), AdjustType.REMOVE, withdraw_per, true);
                    return true;
                }
            }
            default: {
                StaticUtils.sendMessage(player, "&cError: Unknown argument: " + arg2);
                return true;
            }
        }
    }

    /**
     * Handles tab completion for the /testshop command.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.clear();
            String[] subCmds = new String[]{"help","buy","manage","depositGui","depositMoney","withdrawMoney","all","hand","<item>","<player>","sellGui",};

            for (String n : subCmds) {
                if (n!=null && n.startsWith(args[0])) 
                    list.add(n);
            }
            Bukkit.getOnlinePlayers().forEach(player -> {
                if (player.getName().startsWith(args[0])&&args[0].length()>0)
                    list.add(player.getName());
            });
            for (Material mat : Material.values()) {
                if (mat.name().toLowerCase().startsWith(args[0].toLowerCase())&&args[0].length()>1)
                    list.add(mat.name().toLowerCase());
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("depositMoney")) {
                if ("view".startsWith(args[1])) list.add("view");
                if ("all".startsWith(args[1])) list.add("all");
            } else if (args[0].equalsIgnoreCase("withdrawMoney")) {
                if ("view".startsWith(args[1])) list.add("view");
                if ("all".startsWith(args[1])) list.add("all");
            } else if (args[0].equalsIgnoreCase("buy")) {
                if ("<amount>".startsWith(args[1])) list.add("<amount>");
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("depositMoney")) {
                if ("max".startsWith(args[2])) list.add("max");
                if ("<amount>".startsWith(args[2])) list.add("<amount>");
            } else if (args[0].equalsIgnoreCase("withdrawMoney")) {
                if ("max".startsWith(args[2])) list.add("max");
                if ("<amount>".startsWith(args[2])) list.add("<amount>");
            }
        }
        return list;
    }
}