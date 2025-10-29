package dev.tbm00.papermc.playershops64.display;

import java.util.HashSet;
import java.util.Set;
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
import net.kyori.adventure.util.TriState;
import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.structure.Shop;
import dev.tbm00.papermc.playershops64.utils.ShopUtils;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class ShopDisplay {
    private final PlayerShops64 javaPlugin;
    private final UUID shopUuid;

    private Item itemDisplay;
    private ItemDisplay glassDisplay;
    private TextDisplay textDisplay;

    private float displayHeight;
    private String holoColor;
    private static double OFFX = 0.5, OFFY = 1.5, OFFZ = 0.5;
    private static float GLASS_SCALE = (float) 0.675;

    private String lastTextShown = "";
    private ItemStack lastItemShown; 

    private final Set<UUID> baseShownTo = new HashSet<>();
    private final Set<UUID> textShownTo = new HashSet<>();

    public ShopDisplay(PlayerShops64 javaPlugin, UUID shopUuid) {
        this.javaPlugin = javaPlugin;
        this.shopUuid = shopUuid;
        this.displayHeight = javaPlugin.getShopHandler().getShop(shopUuid).getDisplayHeight() / 10f;
        this.holoColor = javaPlugin.getConfigHandler().getDisplayHoloColor();
        update();
    }

    public void clear() {
        Runnable task = () -> {
            if (itemDisplay != null && itemDisplay.isValid()) itemDisplay.remove();
            if (glassDisplay != null && glassDisplay.isValid()) glassDisplay.remove();
            if (textDisplay != null && textDisplay.isValid()) textDisplay.remove();
            lastTextShown = "";
            baseShownTo.clear();
            textShownTo.clear();
        };

        if (!javaPlugin.isEnabled() || Bukkit.isPrimaryThread()) task.run();
        else Bukkit.getScheduler().runTask(javaPlugin, task);
    }

    public void update() {
        Shop shop = javaPlugin.getShopHandler().getShop(shopUuid);
        if (shop == null) return;
        World world = shop.getWorld();
        if (world == null || shop.getLocation() == null) return;

        Location base = shop.getLocation().clone();
        displayHeight = shop.getDisplayHeight() / 10f;
        updateText(world, base, ShopUtils.formatHologramText(shop));
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
        }*/

        boolean respawned = false;
        if (itemDisplay == null || !itemDisplay.isValid() || itemDisplay.isDead()) {
            itemDisplay = world.dropItem(loc, item, ent -> {
                ent.setVelocity(new Vector(0, 0, 0));       // no initial fling
                ent.setGravity(false);                       // keep it hovering
                ent.setCustomNameVisible(false);
                ent.setUnlimitedLifetime(true);              // never despawn
                ent.setPersistent(false);
                ent.setInvulnerable(true);
                ent.setPickupDelay(Integer.MAX_VALUE);       // Vanilla fallback
                ent.setCanPlayerPickup(false);               // Paper API
                ent.setCanMobPickup(false);                  // Paper API
                ent.setFrictionState(TriState.TRUE);
                ent.getPersistentDataContainer().set(StaticUtils.DISPLAY_KEY, PersistentDataType.STRING, "item");
            });
            respawned = true;
        } else {
            if (!sameDisplayItem(lastItemShown, item)) {
                itemDisplay.remove();

                itemDisplay = world.dropItem(loc, item, ent -> {
                    ent.setVelocity(new Vector(0, 0, 0));
                    ent.setGravity(false);
                    ent.setCustomNameVisible(false);
                    ent.setUnlimitedLifetime(true);
                    ent.setPersistent(false);
                    ent.setInvulnerable(true);
                    ent.setPickupDelay(Integer.MAX_VALUE);
                    ent.setCanPlayerPickup(false);
                    ent.setCanMobPickup(false);
                    ent.setFrictionState(TriState.TRUE);
                    ent.getPersistentDataContainer().set(StaticUtils.DISPLAY_KEY, PersistentDataType.STRING, "item");
                });
                respawned = true;
            } else {
                itemDisplay.setItemStack(item);
                itemDisplay.setVelocity(new Vector(0, 0, 0));
            }
        }

        itemDisplay.teleportAsync(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
        if (respawned) baseShownTo.clear();
        lastItemShown = item;
    }

    private void updateGlass(World world, Location base) {
        Location loc = base.clone().add(OFFX, OFFY+displayHeight, OFFZ);

        boolean respawned = false;
        if (glassDisplay == null || !glassDisplay.isValid() || glassDisplay.isDead()) {
            for (ItemDisplay nearby : world.getNearbyEntitiesByType(ItemDisplay.class, loc, 1.0)) {
                if (!nearby.getPersistentDataContainer().has(StaticUtils.DISPLAY_KEY, PersistentDataType.STRING)) continue;
                if (!"glass".equals(nearby.getPersistentDataContainer().get(StaticUtils.DISPLAY_KEY, PersistentDataType.STRING))) continue;
                nearby.remove();
            }
            glassDisplay = world.spawn(loc, ItemDisplay.class, ent -> {
                ent.setItemStack(new ItemStack(Material.GLASS));
                ent.setGravity(false);
                ent.setPersistent(false);
                ent.setNoPhysics(true);
                ent.setViewRange((float) javaPlugin.getConfigHandler().getDisplayViewDistance());
                ent.setTransformationMatrix(new Matrix4f().scale(GLASS_SCALE));
                ent.getPersistentDataContainer().set(StaticUtils.DISPLAY_KEY, PersistentDataType.STRING, "glass");
            });
            respawned = true;
        }

        glassDisplay.teleportAsync(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
        if (respawned) baseShownTo.clear();
    }

    private void updateText(World world, Location base, String newText) {
        Location loc = base.clone().add(OFFX, OFFY+displayHeight+0.45, OFFZ);
        boolean respawned = false;

        if (textDisplay == null || !textDisplay.isValid() || textDisplay.isDead()) {
            for (TextDisplay nearby : world.getNearbyEntitiesByType(TextDisplay.class, loc, 1.0)) {
                if (!nearby.getPersistentDataContainer().has(StaticUtils.DISPLAY_KEY, PersistentDataType.STRING)) continue;
                if (!"holo".equals(nearby.getPersistentDataContainer().get(StaticUtils.DISPLAY_KEY, PersistentDataType.STRING))) continue;
                nearby.remove();
            }
            textDisplay = world.spawn(loc, TextDisplay.class, ent -> {
                ent.setBillboard(Display.Billboard.VERTICAL);
                ent.setAlignment(TextDisplay.TextAlignment.CENTER);
                ent.setBackgroundColor(parseColor(holoColor));
                ent.setShadowed(false);
                ent.setVisibleByDefault(false); // shown when focused
                ent.setPersistent(true);        // false caused issues after chunk unloads
                ent.setNoPhysics(true);
                ent.setGravity(false);
                ent.setViewRange((float) javaPlugin.getConfigHandler().getDisplayViewDistance());
                ent.getPersistentDataContainer().set(StaticUtils.DISPLAY_KEY, PersistentDataType.STRING, "holo");
            });
            respawned = true;
        }

        if (textDisplay!=null) {
            if (respawned || !newText.equals(lastTextShown)) {
                Component comp = LegacyComponentSerializer.legacyAmpersand().deserialize(newText);
                textDisplay.text(comp);
                lastTextShown = newText;
            }
        }

        textDisplay.teleportAsync(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
        if (respawned) textShownTo.clear(); 
    }

    public void show(Player player, boolean showTextToo) {
        if (player == null || !player.isOnline()) return;

        UUID playerUuid = player.getUniqueId();
        if (!baseShownTo.contains(playerUuid)) {
            if (itemDisplay != null && itemDisplay.isValid()) player.showEntity(javaPlugin, itemDisplay);
            if (glassDisplay != null && glassDisplay.isValid()) player.showEntity(javaPlugin, glassDisplay);
            baseShownTo.add(playerUuid);
        }

        if (showTextToo) showText(player);
    }

    public void showText(Player player) {
        if (player == null || !player.isOnline()) return;

        UUID playerUuid = player.getUniqueId();
        if (!textShownTo.contains(playerUuid)) {
            if (textDisplay != null && textDisplay.isValid()) {
                player.showEntity(javaPlugin, textDisplay);
                textShownTo.add(playerUuid);
            }
        }
    }

    public void hide(Player player, boolean hideTextToo) {
        if (player == null) return;

        UUID playerUuid = player.getUniqueId();
        if (baseShownTo.remove(playerUuid)) {
            if (itemDisplay != null && itemDisplay.isValid()) player.hideEntity(javaPlugin, itemDisplay);
            if (glassDisplay != null && glassDisplay.isValid()) player.hideEntity(javaPlugin, glassDisplay);
        }
        if (hideTextToo) hideText(player);
    }

    public void hideText(Player player) {
        if (player == null) return;

        UUID playerUuid = player.getUniqueId();
        if (textShownTo.remove(playerUuid)) {
            if (textDisplay != null && textDisplay.isValid()) player.hideEntity(javaPlugin, textDisplay);
        }
    }

    public void purgeViewer(UUID playerUuid) {
        baseShownTo.remove(playerUuid);
        textShownTo.remove(playerUuid);
    }

    /*public boolean shouldSee(Player player, int viewDistance) {
        Shop shop = javaPlugin.getShopHandler().getShop(shopUuid);

        if (player == null || shop == null || shop.getLocation() == null || shop.getWorld() == null) return false;
        if (!player.getWorld().equals(shop.getWorld())) return false;

        return shop.getLocation().distance(player.getLocation()) <= viewDistance;
    }*/

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

    private static boolean sameDisplayItem(ItemStack a, ItemStack b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.isSimilar(b);
    }
}
