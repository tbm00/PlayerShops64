package dev.tbm00.papermc.playershops64.gui;

import java.util.Arrays;
import java.util.Collections;

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
import dev.tbm00.papermc.playershops64.utils.GuiUtils;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class SearchGui {
    
    /**
     * Creates an anvil gui for player to enter text and search shops with.
     */
    public SearchGui(PlayerShops64 javaPlugin, Player player, boolean isAdmin) {
        String title = "Search All Shops";


        if (javaPlugin.getConfigHandler().isFloodgateEnabled() && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            try {
                CustomForm form = CustomForm.builder()
                    .title("Search All Shops")
                    .label("Search for specific items or players")
                    .input("", "item/material or player", "")
                    .build();

                form.setResponseHandler(responseData -> {
                    CustomFormResponse response = form.parseResponse(responseData);
                    if (!response.isCorrect()) {
                        player.sendMessage("Search cancelled.");
                        return;
                    }

                    String query = response.next();
                    if (query == null || query.isBlank()) {
                        player.sendMessage("Please enter something to search.");
                        return;
                    }

                    GuiUtils.openGuiSearchResults(player, new String[]{query}, isAdmin);
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
        } else {
            ItemStack leftItem = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
            ItemMeta leftMeta = leftItem.getItemMeta();
            leftMeta.setDisplayName(" ");
            leftMeta.setItemName(" ");
            leftItem.setItemMeta(leftMeta);

            ItemStack rightItem = new ItemStack(Material.WRITABLE_BOOK);
            ItemMeta rightMeta = rightItem.getItemMeta();
            rightMeta.setDisplayName("item or player");
            rightMeta.setItemName("item or player");
            rightItem.setItemMeta(rightMeta);

            ItemStack outputItem = new ItemStack(Material.HOPPER);
            ItemMeta outputMeta = outputItem.getItemMeta();
            outputMeta.setDisplayName("click to search");
            outputMeta.setItemName("click to search");
            outputItem.setItemMeta(outputMeta);

            new AnvilGUI.Builder()
                .onClick((slot, stateSnapshot) -> {
                    if(slot != AnvilGUI.Slot.OUTPUT || stateSnapshot.getText().isBlank()) {
                        return Collections.emptyList();
                    }

                    String arr[] = {stateSnapshot.getText()}; 

                    return Arrays.asList(
                        AnvilGUI.ResponseAction.close(),
                        AnvilGUI.ResponseAction.run(() -> GuiUtils.openGuiSearchResults(player, arr, isAdmin))
                    );
                })
                .text(" ")
                .itemLeft(leftItem)
                .itemRight(rightItem)
                .itemOutput(outputItem)
                .title(title)
                .plugin(javaPlugin)
                .open(player);
        }
    }
}
