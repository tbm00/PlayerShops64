package dev.tbm00.papermc.playershops64.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import dev.tbm00.papermc.playershops64.PlayerShops64;

public class ServerStartup implements Listener {
    private final PlayerShops64 javaPlugin;
    private boolean ran = false;

    public ServerStartup(PlayerShops64 javaPlugin) {
        this.javaPlugin = javaPlugin;
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        if (ran) return;
        if (event.getType() != ServerLoadEvent.LoadType.STARTUP) return;
        ran = true;
        
        javaPlugin.getServer().getScheduler().runTaskLater(javaPlugin, () -> {
            javaPlugin.shopHandler.loadShops();
        }, 600L);
    }
}
