package dev.tbm00.papermc.playershops64.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class PlayerMovement implements Listener {

    /**
     * Handles the player movement event.
     * Cancels pending teleports if any.
     *
     * @param event the PlayerMoveEvent
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (StaticUtils.pendingTeleports.contains(event.getPlayer().getName())) {
            StaticUtils.pendingTeleports.remove(event.getPlayer().getName());
            StaticUtils.sendMessage(event.getPlayer(), "&cTeleport countdown cancelled -- you moved!");
        }
    }
}