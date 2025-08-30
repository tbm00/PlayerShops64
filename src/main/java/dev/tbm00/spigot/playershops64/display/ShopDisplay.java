package dev.tbm00.spigot.playershops64.display;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.joml.Matrix4f;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import net.kyori.adventure.text.Component;

import dev.tbm00.spigot.playershops64.PlayerShops64;
import dev.tbm00.spigot.playershops64.data.Shop;

public class ShopDisplay {
    public static final String META_KEY = "playershops64-entity";
    public static final NamespacedKey PDC_KEY = new NamespacedKey("playershops64", "entity-role");

    private final PlayerShops64 plugin;
    private final Shop shop;

    private ItemDisplay itemDisplay;
    private ItemDisplay glassDisplay;
    private TextDisplay textDisplay;

    private final List<UUID> tracked = new ArrayList<>();
    private String lastText = "";

    public ShopDisplay(PlayerShops64 plugin, Shop shop) {
        this.plugin = plugin;
        this.shop = shop;
    }

    public void clear() {
        World w = shop.getWorld();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (itemDisplay != null && itemDisplay.isValid()) itemDisplay.remove();
            if (glassDisplay != null && glassDisplay.isValid()) glassDisplay.remove();
            if (textDisplay != null && textDisplay.isValid()) textDisplay.remove();
            tracked.clear();
        });
    }

    public void update(World world, String text, float itemScale,
                       double offX, double offY, double offZ) {

        if (world == null || shop.getLocation() == null) return;

        Location base = shop.getLocation().clone(); // lectern block pos (your base shop block)
        updateItem(world, base, itemScale, offX, offY, offZ);
        updateGlass(world, base);
        updateText(world, base, text);
    }

    private void updateItem(World world, Location base, float scale,
                            double x, double y, double z) {

        ItemStack stack = (shop.getItemStack() != null) ? shop.getItemStack() : new ItemStack(Material.BARRIER);
        boolean isBlock = stack.getType().isBlock() && stack.getType() != Material.BARRIER;

        // Anchor slightly above the lectern center
        Location loc = base.clone().add(0.5 + x, (isBlock ? 0.4 : 1.4) + y, 0.5 + z);

        if (itemDisplay == null || !itemDisplay.isValid() || itemDisplay.isDead()) {
            // Remove any stray PS64 item display nearby
            for (ItemDisplay nearby : world.getNearbyEntitiesByType(ItemDisplay.class, loc, 1.0)) {
                if (!nearby.getPersistentDataContainer().has(PDC_KEY, PersistentDataType.STRING)) continue;
                if (!"item".equals(nearby.getPersistentDataContainer().get(PDC_KEY, PersistentDataType.STRING))) continue;
                nearby.remove();
                tracked.remove(nearby.getUniqueId());
            }
            itemDisplay = world.spawn(loc, ItemDisplay.class, ent -> {
                ent.setItemStack(stack);
                ent.setGravity(false);
                ent.setPersistent(true);
                ent.setNoPhysics(true);
                ent.setViewRange(0.2f);
                ent.setTransformationMatrix(new Matrix4f().scale(scale));
                ent.getPersistentDataContainer().set(PDC_KEY, PersistentDataType.STRING, "item");
                ent.setMetadata(META_KEY, new FixedMetadataValue(plugin, ""));
                if (!tracked.contains(ent.getUniqueId())) tracked.add(ent.getUniqueId());
            });
        } else {
            itemDisplay.setItemStack(stack);
            // keep it pegged in place
            itemDisplay.teleportAsync(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
        }
    }

    private void updateGlass(World world, Location base) {
        if (plugin.getConfig().getBoolean("display.hide-glass", false)) return;

        double scale = plugin.getConfig().getDouble("display.glass-scale", 1.0);
        Location loc = base.clone().add(0.5, 1.4, 0.5);

        if (glassDisplay == null || !glassDisplay.isValid() || glassDisplay.isDead()) {
            for (ItemDisplay nearby : world.getNearbyEntitiesByType(ItemDisplay.class, loc, 1.0)) {
                if (!nearby.getPersistentDataContainer().has(PDC_KEY, PersistentDataType.STRING)) continue;
                if (!"glass".equals(nearby.getPersistentDataContainer().get(PDC_KEY, PersistentDataType.STRING))) continue;
                nearby.remove();
                tracked.remove(nearby.getUniqueId());
            }
            glassDisplay = world.spawn(loc, ItemDisplay.class, ent -> {
                ent.setItemStack(new ItemStack(Material.GLASS));
                ent.setGravity(false);
                ent.setPersistent(true);
                ent.setNoPhysics(true);
                ent.setViewRange(0.2f);
                ent.setTransformationMatrix(new Matrix4f().scale((float) scale));
                ent.getPersistentDataContainer().set(PDC_KEY, PersistentDataType.STRING, "glass");
                ent.setMetadata(META_KEY, new FixedMetadataValue(plugin, ""));
                if (!tracked.contains(ent.getUniqueId())) tracked.add(ent.getUniqueId());
            });
        } else {
            glassDisplay.teleportAsync(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
        }
    }

    private void updateText(World world, Location base, String text) {
        Location loc = base.clone().add(0.5, 1.8, 0.5);

        if (textDisplay == null || !textDisplay.isValid() || textDisplay.isDead()) {
            for (TextDisplay nearby : world.getNearbyEntitiesByType(TextDisplay.class, loc, 1.0)) {
                if (!nearby.getPersistentDataContainer().has(PDC_KEY, PersistentDataType.STRING)) continue;
                if (!"line".equals(nearby.getPersistentDataContainer().get(PDC_KEY, PersistentDataType.STRING))) continue;
                nearby.remove();
                tracked.remove(nearby.getUniqueId());
            }
            textDisplay = world.spawn(loc, TextDisplay.class, ent -> {
                ent.setBillboard(Display.Billboard.VERTICAL);
                ent.setAlignment(TextDisplay.TextAlignment.CENTER);
                ent.setBackgroundColor(parseColor(plugin.getConfig().getString("display.line-bg", "60,0,0,0")));
                ent.setShadowed(false);
                ent.setVisibleByDefault(false); // shown when focused
                ent.setPersistent(true);
                ent.setNoPhysics(true);
                ent.setGravity(false);
                ent.setViewRange(0.2f);
                ent.getPersistentDataContainer().set(PDC_KEY, PersistentDataType.STRING, "line");
                ent.setMetadata(META_KEY, new FixedMetadataValue(plugin, ""));
                if (!tracked.contains(ent.getUniqueId())) tracked.add(ent.getUniqueId());
            });
        }

        if (!text.equals(lastText)) {
            if (textDisplay != null) textDisplay.text(Component.text(text)); // plug your serializer if you want colors/§ codes
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
        if (itemDisplay != null && itemDisplay.isValid()) player.showEntity(plugin, itemDisplay);
        if (glassDisplay != null && glassDisplay.isValid()) player.showEntity(plugin, glassDisplay);
        if (textDisplay != null && textDisplay.isValid()) {
            if (focused) player.showEntity(plugin, textDisplay);
            else player.hideEntity(plugin, textDisplay);
        }
    }

    public void hide(Player player, boolean hideAll) {
        if (textDisplay != null && textDisplay.isValid()) player.hideEntity(plugin, textDisplay);
        if (hideAll) {
            if (itemDisplay != null && itemDisplay.isValid()) player.hideEntity(plugin, itemDisplay);
            if (glassDisplay != null && glassDisplay.isValid()) player.hideEntity(plugin, glassDisplay);
        }
    }

    /** Basic visibility heuristic to cull by distance/world. */
    public boolean shouldSee(Player player, int viewDistance) {
        if (player == null || shop.getLocation() == null || shop.getWorld() == null) return false;
        if (!player.getWorld().equals(shop.getWorld())) return false;
        return shop.getLocation().distance(player.getLocation()) <= viewDistance;
    }

    public Shop getShop() { return shop; }
}
