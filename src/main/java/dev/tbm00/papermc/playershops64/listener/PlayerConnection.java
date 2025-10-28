package dev.tbm00.papermc.playershops64.listener;

import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.display.VisualTask;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class PlayerConnection implements Listener {
    PlayerShops64 javaPlugin;

    public PlayerConnection(PlayerShops64 javaPlugin) {
        this.javaPlugin = javaPlugin;
    }

    /**
     * Handles the player connection event.
     * Adds head to cache if its not there, or current cached head is old.
     *
     * @param event the PlayerJoinEvent
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        long currentTime = System.currentTimeMillis();
        if (StaticUtils.headMetaCache.containsKey(uuid) && ((currentTime - StaticUtils.headMetaCache.get(uuid).getRight()) < 3600000)) {
            return;
        }

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        headMeta.setOwningPlayer(event.getPlayer());
        head.setItemMeta(headMeta);
        
        final long updateTime = currentTime;
        // Delay to allow the server to apply the skin texture
        Bukkit.getScheduler().runTaskLater(javaPlugin, () -> {
            // Retrieve the updated SkullMeta from the ItemStack
            SkullMeta updatedMeta = (SkullMeta) head.getItemMeta();
            StaticUtils.headMetaCache.put(uuid, Pair.of(updatedMeta, updateTime));
        }, 20L);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        VisualTask task = javaPlugin.getShopHandler().visualTask;

        if (task!=null) {
            UUID playerUuid = event.getPlayer().getUniqueId();
            task.prevFocusedShop.remove(playerUuid);
            task.prevLoadedBases.remove(playerUuid);
            javaPlugin.getShopHandler().getDisplayManager().purgeViewer(playerUuid);
        }
    }
}