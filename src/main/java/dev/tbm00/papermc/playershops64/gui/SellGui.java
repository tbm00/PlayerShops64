package dev.tbm00.papermc.playershops64.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.Bukkit;

import dev.triumphteam.gui.guis.Gui;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.engine.QuickSellEngine;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class SellGui {
    //private final PlayerShops64 javaPlugin;
    private final Gui gui;
    //private final Player player;

    /**
     * Empty gui that try to sell all items inside once closed.
     */
    public SellGui(PlayerShops64 javaPlugin, Player player) {
        //this.javaPlugin = javaPlugin;
        this.gui = new Gui(6, "Sell Gui");
        //this.player = player;

        gui.setCloseGuiAction(event -> {
            BukkitScheduler scheduler = Bukkit.getScheduler();
            scheduler.runTask(javaPlugin, () -> {
                if (!player.isOnline() || player.isDead()) {
                    for (ItemStack item : event.getInventory().getStorageContents()) {
                        StaticUtils.addToInventoryOrDrop(player, item);
                    } return;
                }

                QuickSellEngine engine = new QuickSellEngine(javaPlugin, player);
                engine.computePlans(event.getInventory(), player.getUniqueId());
                
                if (engine.plans.sellPlan.totalItems<=0) {
                    StaticUtils.sendMessage(player, "&cCouldn't find any applicable shops for your items!");
                    engine.returnUnsoldItems();
                    return;
                }

                new SellConfirmGui(javaPlugin, player, engine);
            });
        });
        gui.enableAllInteractions();
        gui.open(player);
    }
}