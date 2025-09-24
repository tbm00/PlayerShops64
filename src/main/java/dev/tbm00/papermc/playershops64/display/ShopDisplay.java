package dev.tbm00.papermc.playershops64.display;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.joml.Matrix4f;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.Shop;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class ShopDisplay {
    private final PlayerShops64 javaPlugin;
    private final UUID shopId;

    private Item itemDisplay;
    private ItemDisplay glassDisplay;
    private TextDisplay textDisplay;
    private float displayHeight;
    private String holoColor;
    private static double OFFX = 0.5, OFFY = 1.5, OFFZ = 0.5;
    private static float GLASS_SCALE = (float) 0.675; 

    private final List<UUID> tracked = new ArrayList<>();
    private String lastText = "";

    public ShopDisplay(PlayerShops64 javaPlugin, UUID shopId) {
        this.javaPlugin = javaPlugin;
        this.shopId = shopId;
        this.displayHeight = javaPlugin.getShopHandler().getShop(shopId).getDisplayHeight() / 10f;
        this.holoColor = javaPlugin.getConfigHandler().getDisplayHoloColor();
    }

    public void clear() {
        Runnable task = () -> {
            if (itemDisplay != null && itemDisplay.isValid()) itemDisplay.remove();
            if (glassDisplay != null && glassDisplay.isValid()) glassDisplay.remove();
            if (textDisplay != null && textDisplay.isValid()) textDisplay.remove();
            tracked.clear();
        };

        // If plugin is disabled OR we are on the main thread, run immediately (no scheduler).
        if (!javaPlugin.isEnabled() || Bukkit.isPrimaryThread()) {
            task.run();
            //StaticUtils.log(ChatColor.YELLOW, "Display cleared via main thread");
        } else {
            Bukkit.getScheduler().runTask(javaPlugin, task);
            //StaticUtils.log(ChatColor.YELLOW, "Display cleared via scheduler");
        }
    }

    public void update(World world, String text) {
        Shop shop = javaPlugin.getShopHandler().getShop(shopId);
        if (world == null || shop == null || shop.getLocation() == null) return;

        Location base = shop.getLocation().clone();
        updateText(world, base, text);
        updateGlass(world, base);
        updateItem(world, base, shop);
    }

    private void updateItem(World world, Location base, Shop shop) {
        ItemStack item = (shop.getItemStack() != null) ? shop.getItemStack() : new ItemStack(Material.BARRIER);
        Location loc = base.clone().add(OFFX, OFFY+displayHeight-0.3, OFFZ);

        // Remove any stray PS64 item display nearby
        /*for (Item nearby : world.getNearbyEntitiesByType(Item.class, loc, 1.0)) {
            if (!nearby.getPersistentDataContainer().has(StaticUtils.DISPLAY_KEY, PersistentDataType.STRING)) continue;
            if (!"item".equals(nearby.getPersistentDataContainer().get(StaticUtils.DISPLAY_KEY, PersistentDataType.STRING))) continue;
            nearby.remove();
            tracked.remove(nearby.getUniqueId());
        }*/

        if (itemDisplay == null || !itemDisplay.isValid() || itemDisplay.isDead()) {
            itemDisplay = world.dropItem(loc, item, ent -> {
                ent.setVelocity(new Vector(0, 0, 0));       // no initial fling
                ent.setGravity(false);                       // keep it hovering
                ent.setCustomNameVisible(false);
                ent.setCanPlayerPickup(false);               // Paper API
                ent.setCanMobPickup(false);                  // Paper API
                ent.setUnlimitedLifetime(true);              // never despawn
                ent.setPersistent(false);
                ent.setInvulnerable(true);
                ent.getPersistentDataContainer().set(StaticUtils.DISPLAY_KEY, PersistentDataType.STRING, "item");
            });
            tracked.add(itemDisplay.getUniqueId());
        } else {
            // keep the same stack & pin position
            itemDisplay.setItemStack(item);
            itemDisplay.setVelocity(new Vector(0, 0, 0));
            itemDisplay.teleportAsync(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
        }
    }

    private void updateGlass(World world, Location base) {
        Location loc = base.clone().add(OFFX, OFFY+displayHeight, OFFZ);

        if (glassDisplay == null || !glassDisplay.isValid() || glassDisplay.isDead()) {
            for (ItemDisplay nearby : world.getNearbyEntitiesByType(ItemDisplay.class, loc, 1.0)) {
                if (!nearby.getPersistentDataContainer().has(StaticUtils.DISPLAY_KEY, PersistentDataType.STRING)) continue;
                if (!"glass".equals(nearby.getPersistentDataContainer().get(StaticUtils.DISPLAY_KEY, PersistentDataType.STRING))) continue;
                nearby.remove();
                tracked.remove(nearby.getUniqueId());
            }
            glassDisplay = world.spawn(loc, ItemDisplay.class, ent -> {
                ent.setItemStack(new ItemStack(Material.GLASS));
                ent.setGravity(false);
                ent.setPersistent(false);
                ent.setNoPhysics(true);
                ent.setViewRange(0.2f);
                ent.setTransformationMatrix(new Matrix4f().scale(GLASS_SCALE));
                ent.getPersistentDataContainer().set(StaticUtils.DISPLAY_KEY, PersistentDataType.STRING, "glass");
                if (!tracked.contains(ent.getUniqueId())) tracked.add(ent.getUniqueId());
            });
        } else {
            glassDisplay.teleportAsync(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
        }
    }

    private void updateText(World world, Location base, String text) {
        Location loc = base.clone().add(OFFX, OFFY+displayHeight+0.45, OFFZ);

        if (textDisplay == null || !textDisplay.isValid() || textDisplay.isDead()) {
            for (TextDisplay nearby : world.getNearbyEntitiesByType(TextDisplay.class, loc, 1.0)) {
                if (!nearby.getPersistentDataContainer().has(StaticUtils.DISPLAY_KEY, PersistentDataType.STRING)) continue;
                if (!"holo".equals(nearby.getPersistentDataContainer().get(StaticUtils.DISPLAY_KEY, PersistentDataType.STRING))) continue;
                nearby.remove();
                tracked.remove(nearby.getUniqueId());
            }
            textDisplay = world.spawn(loc, TextDisplay.class, ent -> {
                ent.setBillboard(Display.Billboard.VERTICAL);
                ent.setAlignment(TextDisplay.TextAlignment.CENTER);
                ent.setBackgroundColor(parseColor(holoColor));
                ent.setShadowed(false);
                ent.setVisibleByDefault(false); // shown when focused
                ent.setPersistent(false);
                ent.setNoPhysics(true);
                ent.setGravity(false);
                ent.setViewRange(0.2f);
                ent.getPersistentDataContainer().set(StaticUtils.DISPLAY_KEY, PersistentDataType.STRING, "holo");
                if (!tracked.contains(ent.getUniqueId())) tracked.add(ent.getUniqueId());
            });
        }

        if (!text.equals(lastText) && textDisplay != null) {
            Component comp = LegacyComponentSerializer.legacyAmpersand().deserialize(text);
            textDisplay.text(comp);
            lastText = text;
        }
        if (textDisplay != null) {
            textDisplay.teleportAsync(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
        }
    }

    private static Color parseColor(String csv) {
        try {
            String[] p = csv.split(",");
            int a = Math.max(0, Math.min(Integer.parseInt(p[0].trim()), 100));
            int r = clamp255(p[1]);
            int g = clamp255(p[2]);
            int b = clamp255(p[3]);
            return Color.fromARGB(a, r, g, b);
        } catch (Exception ignored) {
            return Color.fromARGB(60, 0, 0, 0);
        }
    }

    private static int clamp255(String s) {
        int v = Integer.parseInt(s.trim());
        return Math.max(0, Math.min(v, 255));
    }

    /** Per-player show/hide: text only toggles on focus; item/glass always shown. */
    public void show(Player player, boolean focused) {
        if (itemDisplay != null && itemDisplay.isValid()) player.showEntity(javaPlugin, itemDisplay);
        if (glassDisplay != null && glassDisplay.isValid()) player.showEntity(javaPlugin, glassDisplay);
        if (textDisplay != null && textDisplay.isValid()) {
            if (focused) player.showEntity(javaPlugin, textDisplay);
            else player.hideEntity(javaPlugin, textDisplay);
        }
    }

    public void hide(Player player, boolean hideAll) {
        if (textDisplay != null && textDisplay.isValid()) player.hideEntity(javaPlugin, textDisplay);
        if (hideAll) {
            if (itemDisplay != null && itemDisplay.isValid()) player.hideEntity(javaPlugin, itemDisplay);
            if (glassDisplay != null && glassDisplay.isValid()) player.hideEntity(javaPlugin, glassDisplay);
        }
    }

    /** Basic visibility heuristic to cull by distance/world. */
    public boolean shouldSee(Player player, int viewDistance) {
        Shop shop = javaPlugin.getShopHandler().getShop(shopId);
        if (player == null || shop == null || shop.getLocation() == null || shop.getWorld() == null) return false;
        if (!player.getWorld().equals(shop.getWorld())) return false;
        return shop.getLocation().distance(player.getLocation()) <= viewDistance;
    }

    //public Shop getShop() { return javaPlugin.getShopHandler().getShop(shopId); }
}
