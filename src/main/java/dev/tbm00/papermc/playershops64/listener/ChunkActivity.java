package dev.tbm00.papermc.playershops64.listener;

import java.util.Set;
import java.util.UUID;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.structure.Shop;

public class ChunkActivity implements Listener {
    private final PlayerShops64 javaPlugin;

    public ChunkActivity(PlayerShops64 javaPlugin) {
        this.javaPlugin = javaPlugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        World world = event.getWorld();
        int cx = event.getChunk().getX();
        int cz = event.getChunk().getZ();

        Set<UUID> shopUuids = javaPlugin.getShopHandler().getShopsInChunk(world, cx, cz);
        if (shopUuids.isEmpty()) return;

        // (Re)spawn displays immediately so they exist when players arrive
        for (UUID shopUuid : shopUuids) {
            Shop shop = javaPlugin.getShopHandler().getShop(shopUuid);
            if (shop == null) continue;
            javaPlugin.getShopHandler().getDisplayManager().upsertDisplay(shop);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        World world = event.getWorld();
        int cx = event.getChunk().getX();
        int cz = event.getChunk().getZ();

        Set<UUID> chunkShopUuids = javaPlugin.getShopHandler().getShopsInChunk(world, cx, cz);
        if (chunkShopUuids.isEmpty()) return;

        // Nuke entity refs so we never hold onto dead handles
        javaPlugin.getShopHandler().getDisplayManager().invalidateDisplays(chunkShopUuids);
    }
}
