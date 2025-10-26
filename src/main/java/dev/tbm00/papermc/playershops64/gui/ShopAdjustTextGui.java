package dev.tbm00.papermc.playershops64.gui;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import org.geysermc.cumulus.CustomForm;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import net.wesjd.anvilgui.AnvilGUI;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.enums.AdjustAttribute;
import dev.tbm00.papermc.playershops64.utils.ShopUtils;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class ShopAdjustTextGui {

    /**
     * Creates an anvil gui for player to enter text and search shops with.
     */
    public ShopAdjustTextGui(PlayerShops64 javaPlugin, Player player, boolean isAdmin, UUID shopUuid, AdjustAttribute attribute, boolean closeGuiAfter) {
        if (attribute.equals(AdjustAttribute.DESCRIPTION)) {
            if (javaPlugin.getConfigHandler().isFloodgateEnabled() && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
                setBedrockDescription(javaPlugin, player, isAdmin, shopUuid, attribute);
            } else {
                setJavaDescription(javaPlugin, player, isAdmin, shopUuid, attribute);
            }
        } else if (attribute.equals(AdjustAttribute.ASSISTANT)) {
            if (javaPlugin.getConfigHandler().isFloodgateEnabled() && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
                addBedrockAssistant(javaPlugin, player, isAdmin, shopUuid, attribute);
            } else {
                addJavaAssistant(javaPlugin, player, isAdmin, shopUuid, attribute);
            }
        } else {
            if (javaPlugin.getConfigHandler().isFloodgateEnabled() && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
                adjustBedrockInt(javaPlugin, player, isAdmin, shopUuid, attribute, closeGuiAfter);
            } else {
                adjustJavaInt(javaPlugin, player, isAdmin, shopUuid, attribute, closeGuiAfter);
            }
        }
    }

    private void adjustBedrockInt(PlayerShops64 javaPlugin, Player player, boolean isAdmin, UUID shopUuid, AdjustAttribute attribute, boolean closeGuiAfter) {
        try {
            CustomForm form = CustomForm.builder()
                .title("Enter Amount")
                .label("Enter new amount to set")
                .input("", "integer", "")
                .build();

            form.setResponseHandler(responseData -> {
                CustomFormResponse response = form.parseResponse(responseData);

                try {
                    if (!response.isCorrect()) {
                        StaticUtils.sendMessage(player, "&cInput cancelled!");
                        return;
                    }

                    String query = response.next();
                    if (query == null || query.isBlank()) {
                        StaticUtils.sendMessage(player, "&cPlease enter an integer!");
                        return;
                    } while (query.startsWith(" ")) {
                        query = query.substring(1);
                    }

                    handleIntAdjust(javaPlugin, player, isAdmin, shopUuid, closeGuiAfter, attribute, query);

                } finally {
                    unlock(javaPlugin, shopUuid, player, attribute);
                }
            });

            Bukkit.getScheduler().runTaskLater(javaPlugin, () -> {
                if (player.isOnline()) {
                    if (FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
                        FloodgatePlayer fgp = FloodgateApi.getInstance().getPlayer(player.getUniqueId());
                        fgp.sendForm(form);
                    }
                }
            }, 1L);

            return;
        } catch (Exception e) {
            StaticUtils.log(ChatColor.RED, "Caught exception creating custom bedrock form: " + e.getMessage());
        }
    }

    private void setBedrockDescription(PlayerShops64 javaPlugin, Player player, boolean isAdmin, UUID shopUuid, AdjustAttribute attribute) {
        try {
            CustomForm form = CustomForm.builder()
                .title("Enter Description")
                .label("Enter new description to set")
                .input("", "short description", "")
                .build();

            form.setResponseHandler(responseData -> {
                CustomFormResponse response = form.parseResponse(responseData);
                try {
                    if (!response.isCorrect()) {
                        StaticUtils.sendMessage(player, "&cInput cancelled!");
                        return;
                    }

                    String query = response.next();
                    if (query == null) {
                        StaticUtils.sendMessage(player, "&cPlease enter a description!");
                        return;
                    } if (query.isEmpty()) {
                        query = " ";
                    }

                    try {
                        ShopUtils.setDescription(player, shopUuid, query);
                        new ShopManageGui(javaPlugin, player, isAdmin, shopUuid);
                    } catch (Exception e) {
                        StaticUtils.log(ChatColor.RED, "Caught exception setting description and/or opening new manage inv from bedrock form: " + e.getMessage());
                        StaticUtils.sendMessage(player, "&cError setting a number from your input!");
                    }
                } finally {
                    unlock(javaPlugin, shopUuid, player, attribute);
                }
            });

            Bukkit.getScheduler().runTaskLater(javaPlugin, () -> {
                if (player.isOnline()) {
                    if (FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
                        FloodgatePlayer fgp = FloodgateApi.getInstance().getPlayer(player.getUniqueId());
                        fgp.sendForm(form);
                    }
                }
            }, 1L);

            return;
        } catch (Exception e) {
            StaticUtils.log(ChatColor.RED, "Caught exception creating custom bedrock form: " + e.getMessage());
        }
    }

    private void addBedrockAssistant(PlayerShops64 javaPlugin, Player player, boolean isAdmin, UUID shopUuid, AdjustAttribute attribute) {
        try {
            CustomForm form = CustomForm.builder()
                .title("Enter Player")
                .label("Enter assistant to add")
                .input("", "player name", "")
                .build();

            form.setResponseHandler(responseData -> {
                CustomFormResponse response = form.parseResponse(responseData);
                try {
                    if (!response.isCorrect()) {
                        StaticUtils.sendMessage(player, "&cInput cancelled!");
                        return;
                    }

                    String query = response.next();
                    if (query == null) {
                        StaticUtils.sendMessage(player, "&cPlease enter a player name!");
                        return;
                    } if (query.isEmpty()) {
                        query = " ";
                    }

                    try {
                        ShopUtils.addAssistant(player, shopUuid, query);
                        new ListAssistantsGui(javaPlugin, player, shopUuid, isAdmin);
                    } catch (Exception e) {
                        StaticUtils.log(ChatColor.RED, "Caught exception adding assistant and/or opening new manage inv from bedrock form: " + e.getMessage());
                        StaticUtils.sendMessage(player, "&cError adding an assistant from your input!");
                    }
                } finally {
                    unlock(javaPlugin, shopUuid, player, attribute);
                }
            });

            Bukkit.getScheduler().runTaskLater(javaPlugin, () -> {
                if (player.isOnline()) {
                    if (FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
                        FloodgatePlayer fgp = FloodgateApi.getInstance().getPlayer(player.getUniqueId());
                        fgp.sendForm(form);
                    }
                }
            }, 1L);

            return;
        } catch (Exception e) {
            StaticUtils.log(ChatColor.RED, "Caught exception creating custom bedrock form: " + e.getMessage());
        }
    }

    private void addJavaAssistant(PlayerShops64 javaPlugin, Player player, boolean isAdmin, UUID shopUuid, AdjustAttribute attribute) {
            ItemStack leftItem = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
            ItemMeta leftMeta = leftItem.getItemMeta();
            leftMeta.setDisplayName(" ");
            leftMeta.setItemName(" ");
            leftItem.setItemMeta(leftMeta);

            ItemStack rightItem = new ItemStack(Material.WRITABLE_BOOK);
            ItemMeta rightMeta = rightItem.getItemMeta();
            rightMeta.setDisplayName("type player name");
            rightMeta.setItemName("type player name");
            rightItem.setItemMeta(rightMeta);

            ItemStack outputItem = new ItemStack(Material.GREEN_BANNER);
            ItemMeta outputMeta = outputItem.getItemMeta();
            outputMeta.setDisplayName("click to add assistant");
            outputMeta.setItemName("click to add assistant");
            outputItem.setItemMeta(outputMeta);

            if (!tryLock(javaPlugin, shopUuid, player, attribute)) return;

            new AnvilGUI.Builder()
                .onClose((stateSnapshot) -> {
                    unlock(javaPlugin, shopUuid, player, attribute);
                })
                .onClick((slot, stateSnapshot) -> {
                    if(slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }

                    String query = stateSnapshot.getText();
                    if (query.isEmpty() || query.isBlank()) {
                        query = " ";
                    } else {
                        query = query.stripLeading();
                    }
                    String finalQuery = (query.isEmpty()) ? " " : query;

                    return Arrays.asList(
                        AnvilGUI.ResponseAction.close(),
                        AnvilGUI.ResponseAction.run(() -> {
                            try {
                                ShopUtils.addAssistant(player, shopUuid, finalQuery);
                                new ListAssistantsGui(javaPlugin, player, shopUuid, isAdmin);
                            } catch (Exception e) {
                                StaticUtils.log(ChatColor.RED, "Caught exception adding assistant and/or opening new manage inv from anvil gui: " + e.getMessage());
                                StaticUtils.sendMessage(player, "&cError adding an assistant from your input!");
                            }})
                    );
                })
                .text(" ")
                .itemLeft(leftItem)
                .itemRight(rightItem)
                .itemOutput(outputItem)
                .title("Enter Player")
                .plugin(javaPlugin)
                .open(player);
    }

    private void setJavaDescription(PlayerShops64 javaPlugin, Player player, boolean isAdmin, UUID shopUuid, AdjustAttribute attribute) {
            ItemStack leftItem = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
            ItemMeta leftMeta = leftItem.getItemMeta();
            leftMeta.setDisplayName(" ");
            leftMeta.setItemName(" ");
            leftItem.setItemMeta(leftMeta);

            ItemStack rightItem = new ItemStack(Material.WRITABLE_BOOK);
            ItemMeta rightMeta = rightItem.getItemMeta();
            rightMeta.setDisplayName("type short description");
            rightMeta.setItemName("type short description");
            rightItem.setItemMeta(rightMeta);

            ItemStack outputItem = new ItemStack(Material.GREEN_BANNER);
            ItemMeta outputMeta = outputItem.getItemMeta();
            outputMeta.setDisplayName("click to set description");
            outputMeta.setItemName("click to set description");
            outputItem.setItemMeta(outputMeta);

            if (!tryLock(javaPlugin, shopUuid, player, attribute)) return;

            new AnvilGUI.Builder()
                .onClose((stateSnapshot) -> {
                    unlock(javaPlugin, shopUuid, player, attribute);
                })
                .onClick((slot, stateSnapshot) -> {
                    if(slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }

                    String query = stateSnapshot.getText();
                    String finalQuery;
                    if (query.isEmpty() || query.isBlank()) {
                        finalQuery = " ";
                    } else {
                        finalQuery = query.stripLeading();
                    }

                    return Arrays.asList(
                        AnvilGUI.ResponseAction.close(),
                        AnvilGUI.ResponseAction.run(() -> {
                            try {
                                ShopUtils.setDescription(player, shopUuid, finalQuery);
                                new ShopManageGui(javaPlugin, player, isAdmin, shopUuid); 
                            } catch (Exception e) {
                                StaticUtils.log(ChatColor.RED, "Caught exception setting description and/or opening new manage inv from anvil gui: " + e.getMessage());
                                StaticUtils.sendMessage(player, "&cError setting a description from your input!");
                            }})
                    );
                })
                .text(" ")
                .itemLeft(leftItem)
                .itemRight(rightItem)
                .itemOutput(outputItem)
                .title("Enter Description")
                .plugin(javaPlugin)
                .open(player);
    }

    private void adjustJavaInt(PlayerShops64 javaPlugin, Player player, boolean isAdmin, UUID shopUuid, AdjustAttribute attribute, boolean closeGuiAfter) {
            ItemStack leftItem = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
            ItemMeta leftMeta = leftItem.getItemMeta();
            leftMeta.setDisplayName(" ");
            leftMeta.setItemName(" ");
            leftItem.setItemMeta(leftMeta);

            ItemStack rightItem = new ItemStack(Material.WRITABLE_BOOK);
            ItemMeta rightMeta = rightItem.getItemMeta();
            rightMeta.setDisplayName("type a number");
            rightMeta.setItemName("type a number");
            rightItem.setItemMeta(rightMeta);

            ItemStack outputItem = new ItemStack(Material.GREEN_BANNER);
            ItemMeta outputMeta = outputItem.getItemMeta();
            outputMeta.setDisplayName("click to set amount");
            outputMeta.setItemName("click to set amount");
            outputItem.setItemMeta(outputMeta);

            if (!tryLock(javaPlugin, shopUuid, player, attribute)) return;

            new AnvilGUI.Builder()
                .onClose(stateSnapshot -> {
                    unlock(javaPlugin, shopUuid, player, attribute);
                })
                .onClick((slot, stateSnapshot) -> {
                    if(slot != AnvilGUI.Slot.OUTPUT || stateSnapshot.getText().isBlank()) {
                        return Collections.emptyList();
                    }

                    String arr[] = {stateSnapshot.getText()}; 
                    while (arr[0].startsWith(" ")) {
                        arr[0] = arr[0].substring(1);
                    } String finalQuery = arr[0];
                    
                    return Arrays.asList(
                        AnvilGUI.ResponseAction.close(),
                        AnvilGUI.ResponseAction.run(() -> {
                            handleIntAdjust(javaPlugin, player, isAdmin, shopUuid, closeGuiAfter, attribute, finalQuery);
                        })
                    );
                })
                .text(" ")
                .itemLeft(leftItem)
                .itemRight(rightItem)
                .itemOutput(outputItem)
                .title("Enter Amount")
                .plugin(javaPlugin)
                .open(player);
    }

    private boolean tryLock(PlayerShops64 javaPlugin, UUID shopUuid, Player player, AdjustAttribute attribute) {
        if (!javaPlugin.getShopHandler().tryLockShop(shopUuid, player)) return false;
    
        StaticUtils.log(ChatColor.YELLOW, player.getName() + " opened shop "+ShopUtils.getShopHint(shopUuid)+"'s adjust text gui: "+AdjustAttribute.toString(attribute));
        return true;
    }

    private void unlock(PlayerShops64 javaPlugin, UUID shopUuid, Player player, AdjustAttribute attribute) {
        javaPlugin.getShopHandler().unlockShop(shopUuid, player.getUniqueId());

        StaticUtils.log(ChatColor.GREEN, player.getName() + " closed shop "+ShopUtils.getShopHint(shopUuid)+"'s adjust text gui: "+AdjustAttribute.toString(attribute));
    }

    private void handleIntAdjust(PlayerShops64 javaPlugin, Player player, boolean isAdmin, UUID shopUuid, boolean closeGuiAfter, AdjustAttribute attribute, String query) {
        try {
            switch (attribute) {
                case TRANSACTION: {
                    Double amount = Double.parseDouble(query);
                    new ShopTransactionGui(javaPlugin, player, isAdmin, shopUuid, amount.intValue(), closeGuiAfter);
                    break;
                }
                case BUY_PRICE: {
                    Double amount = Double.parseDouble(query);
                    ShopUtils.setBuyPrice(player, shopUuid, amount);
                    break;
                }
                case SELL_PRICE: {
                    Double amount = Double.parseDouble(query);
                    ShopUtils.setSellPrice(player, shopUuid, amount);
                    break;
                }
                case STOCK: 
                case BALANCE: 
                case DISPLAY_HEIGHT: 
                default: {
                    Double amount = Double.parseDouble(query);
                    new ShopAdjustInvGui(javaPlugin, player, isAdmin, shopUuid, amount.intValue(), attribute, closeGuiAfter);
                    break;
                }
            }
        } catch (Exception e) {
            StaticUtils.log(ChatColor.RED, "Caught exception opening new inv from anvil gui: " + e.getMessage());
            StaticUtils.sendMessage(player, "&cError getting a number from your input!");
        }
    }
}
