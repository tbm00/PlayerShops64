package dev.tbm00.papermc.playershops64.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.tuple.Pair;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Rotatable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import net.md_5.bungee.api.chat.TextComponent;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.structure.Shop;

public class StaticUtils {
    private static PlayerShops64 javaPlugin;
    public static final List<String> pendingTeleports = new CopyOnWriteArrayList<>();
    
    public static final String PLAYER_PERM = "playershops64.player";
    public static final String ADMIN_PERM = "playershops64.admin";

    public static final String TBL_SHOPS = "playershops64_shops";

    public static final String CATEGORY_GUI_TITLE = "Shop Categories";

    public static NamespacedKey DISPLAY_KEY;
    public static NamespacedKey SHOP_KEY;
    public static NamespacedKey DESPOIT_WAND_KEY;
    public static NamespacedKey SELL_WAND_KEY;

    public static final Set<Material> CONTAINER_MATERIALS = EnumSet.of(
        Material.CHEST,
        Material.BARREL
    );

    public static void init(PlayerShops64 javaPlugin) {
        StaticUtils.javaPlugin = javaPlugin;
        SHOP_KEY = new NamespacedKey(javaPlugin, "shop-base");
        DISPLAY_KEY = new NamespacedKey(javaPlugin, "display-entity");
        DESPOIT_WAND_KEY = new NamespacedKey(javaPlugin, "deposit-wand");
        SELL_WAND_KEY = new NamespacedKey(javaPlugin, "sell-wand");
    }

    /**
     * Logs one or more messages to the server console with the prefix & specified chat color.
     *
     * @param chatColor the chat color to use for the log messages
     * @param strings one or more message strings to log
     */
    public static void log(ChatColor chatColor, String... strings) {
		for (String s : strings)
            javaPlugin.getServer().getConsoleSender().sendMessage("[PS64] " + chatColor + s);
	}

    /**
     * Normalizes big decimal to avoid money drift beyond 2 decimals places
     */
    public static BigDecimal normalizeBigDecimal(BigDecimal amount) {
        return amount == null ? null : amount.setScale(2, RoundingMode.DOWN);
    }

    public static double normalizeToDouble(BigDecimal amount) {
        BigDecimal normalized = normalizeBigDecimal(amount);
        return normalized == null ? 0.0 : normalized.doubleValue();
    }

    /**
     * Normalizes double to avoid money drift beyond 2 decimals places
     */
    public static double normalizeDouble(double amount) {
        BigDecimal normalized = normalizeBigDecimal(BigDecimal.valueOf(amount));
        return normalized == null ? 0.0 : normalized.doubleValue();
    }

    /**
     * Formats int to "200,000" style
     * 
     * @param amount the amount to format
     * @return the formatted string
     */
    public static String formatIntUS(int amount) {
        return NumberFormat.getNumberInstance(Locale.US).format(amount);
    }

    /**
     * Formats double to "200,000" style
     * 
     * @param amount the amount to format
     * @return the formatted string
     */
    public static String formatIntUS(double amount) {
        return formatIntUS((int) amount);
    }

    /**
     * Formats int to "200,000.00" style
     * 
     * @param amount the amount to format
     * @return the formatted string
     */
    public static String formatDoubleUS(double amount) {
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
        return formatter.format(amount);
    }

    /**
     * Formats double to "200,000.00" style
     * 
     * @param amount the amount to format
     * @return the formatted string
     */
    public static String formatDoubleUS(int amount) {
        return formatDoubleUS((double) amount);
    }

    /**
     * Formats String to title case (replaces `_` with ` `)
     */
    public static String formatTitleCase(String string) {
        StringBuilder builder = new StringBuilder();
        for(String word : string.toString().split("_")) {
            if (word.isEmpty()) continue;
            builder.append(word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase() + " ");
        }
     
        return builder.toString().trim();
    }

    public static String getItemName(ItemStack item) {
        if (item == null) return "&4(null item)";
        String name = null;
        ItemMeta meta = item.getItemMeta();
        if (meta!=null) {
            if (meta.hasDisplayName()) name = meta.getDisplayName();
            if (meta.hasItemName() && (name == null || name.isBlank())) name = meta.getItemName();
        }
        if (name == null || name.isBlank()) name = formatTitleCase(item.getType().toString());
        
        return name;
    }

    public static String getSaleItemNameWithQuantity(Shop shop) {
        if (shop.getItemStack()==null || shop.getItemStack().getType()==Material.AIR) {
            return ChatColor.translateAlternateColorCodes('&', "&c(null item)");
        } else {
            return ChatColor.translateAlternateColorCodes('&', (getItemName(shop.getItemStack()) + " &7x &f" + shop.getStackSize()));
        }
    }

    /**
     * Converts String to a Material
     */
    public static Material parseMaterial(String name) {
        if (name == null || name.isBlank()) return null;

        Material m = Material.matchMaterial(name);
        if (m != null) return m;
        try {
            return Material.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            log(ChatColor.RED, "Unknown material in config.yml: '" + name + "'");
            return null;
        }
    }

    /**
     * Retrieves a player by their name.
     * 
     * @param arg the name of the player to retrieve
     * @return the Player object, or null if not found
     */
    public static Player getPlayer(String arg) {
        return javaPlugin.getServer().getPlayer(arg);
    }

    /**
     * Checks if the sender has a specific permission.
     * 
     * @param sender the command sender
     * @param perm the permission string
     * @return true if the sender has the permission, false otherwise
     */
    public static boolean hasPermission(CommandSender sender, String perm) {
        if (sender instanceof Player && ((Player)sender).getGameMode()==GameMode.CREATIVE) return false;
        return sender.hasPermission(perm) || sender instanceof ConsoleCommandSender;
    }

    /**
     * Sends a message to a target CommandSender.
     * 
     * @param target the CommandSender to send the message to
     * @param string the message to send
     */
    public static void sendMessage(CommandSender target, String string) {
        if (string!=null && !string.isBlank())
            target.spigot().sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', javaPlugin.getConfigHandler().getChatPrefix() + string)));
    }

    /**
     * Executes a command as the console.
     * 
     * @param command the command to execute
     * @return true if the command was successfully executed, false otherwise
     */
    public static boolean runCommand(String command) {
        ConsoleCommandSender console = javaPlugin.getServer().getConsoleSender();
        try {
            return Bukkit.dispatchCommand(console, command);
        } catch (Exception e) {
            log(ChatColor.RED, "Caught exception running command " + command + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Executes a command as a specific player.
     * 
     * @param target the player to execute the command as
     * @param command the command to execute
     * @return true if the command was successfully executed, false otherwise
     */
    public static boolean sudoCommand(Player target, String command) {
        try {
            return Bukkit.dispatchCommand(target, command);
        } catch (Exception e) {
            log(ChatColor.RED, "Caught exception sudoing command: " + target.getName() + " : /" + command + ": " + e.getMessage());
            return false;
        }
    }

   /**
     * Executes a command as a specific human entity.
     * 
     * @param target the player to execute the command as
     * @param command the command to execute
     * @return true if the command was successfully executed, false otherwise
     */
    public static boolean sudoCommand(HumanEntity target, String command) {
        try {
            return Bukkit.dispatchCommand(target, command);
        } catch (Exception e) {
            log(ChatColor.RED, "Caught exception sudoing command: " + target.getName() + " : /" + command + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Teleports a player to the given world and coordinates after a 5-second delay.
     * If the player moves during the delay, the teleport is cancelled.
     *
     * @param player the player to teleport
     * @param worldName the target world's name
     * @param x target x-coordinate
     * @param y target y-coordinate
     * @param z target z-coordinate
     * @return true if the teleport countdown was started, false if the player was already waiting
     */
    public static boolean teleportPlayer(Player player, String worldName, double x, double y, double z) {
        String playerName = player.getName();
        if (pendingTeleports.contains(playerName)) {
            StaticUtils.sendMessage(player, "&cYou are already waiting for a teleport!");
            return false;
        }
        pendingTeleports.add(playerName);
        StaticUtils.sendMessage(player, "&aTeleporting in 3 seconds -- don't move!");

        // Schedule the teleport to run later
        Bukkit.getScheduler().runTaskLater(javaPlugin, () -> {
            if (pendingTeleports.contains(playerName)) {
                // Remove player from pending list and teleport
                pendingTeleports.remove(playerName);
                World targetWorld = Bukkit.getWorld(worldName);
                if (targetWorld != null) {
                    Location targetLocation = new Location(targetWorld, x, y, z);
                    player.teleport(targetLocation);
                } else {
                    StaticUtils.sendMessage(player, "&cWorld not found!");
                }
            }
        }, 60L);

        return true;
    }

    /**
     * Counts how many items in the player's inventory (storage + offhand) are "similar" to item
     */
    public static int countMatchingItems(Player player, ItemStack item) {
        if (player == null || item == null || item.getType().isAir()) return 0;

        ItemStack saleCopy = item.clone();
        saleCopy.setAmount(1);

        PlayerInventory inv = player.getInventory();
        int total = 0;

        // storage (hotbar + main, excludes armor)
        ItemStack[] storage = inv.getStorageContents();
        for (ItemStack stack : storage) {
            if (stack == null || stack.getType().isAir()) continue;
            if (stack.isSimilar(saleCopy)) total += stack.getAmount();
        }

        // offhand
        ItemStack off = inv.getItemInOffHand();
        if (off != null && !off.getType().isAir() && off.isSimilar(saleCopy)) {
            total += off.getAmount();
        }

        return total;
    }

    /**
     * Counts how many items in the player's inventory (storage + offhand) are "similar" to item
     */
    public static int countMatchingItemStacks(Player player, ItemStack item) {
        if (player == null || item == null || item.getType().isAir()) return 0;

        ItemStack saleCopy = item.clone();
        saleCopy.setAmount(1);

        PlayerInventory inv = player.getInventory();
        int total = 0;

        // storage (hotbar + main, excludes armor)
        ItemStack[] storage = inv.getStorageContents();
        for (ItemStack stack : storage) {
            if (stack == null || stack.getType().isAir()) continue;
            if (stack.isSimilar(saleCopy)) total += 1;
        }

        // offhand
        ItemStack off = inv.getItemInOffHand();
        if (off != null && !off.getType().isAir() && off.isSimilar(saleCopy)) {
            total += 1;
        }

        return total;
    }

    /**
     * Removes exactly {@code quantity} items similar to {@code item} from the player's inventory
     * (storage + offhand). Returns true iff all requested items were removed. Nothing is removed if
     * the player doesn't have enough.
     *
     * Must be called on the main thread (Bukkit thread).
     */
    public static boolean removeMatchingItems(Player player, ItemStack item, int quantity) {
        if (quantity <= 0) return true;
        if (player == null || item == null || item.getType().isAir()) return false;

        // Inventory changes must happen on the primary thread.
        if (!Bukkit.isPrimaryThread()) {
            StaticUtils.log(ChatColor.RED, "Cannot removeMatchingItems() for "+ player.getName() + " because it was called off the main thread..!");
            return false;
        }

        int playerHas = countMatchingItems(player, item);
        if (playerHas < quantity) return false; // all-or-nothing

        ItemStack itemCopy = item.clone();
        itemCopy.setAmount(1);

        PlayerInventory inv = player.getInventory();
        int remaining = quantity;

        // storage (hotbar + main, excludes armor)
        ItemStack[] storage = inv.getStorageContents();
        for (int i = 0; i < storage.length && remaining > 0; i++) {
            ItemStack stack = storage[i];
            if (stack == null || stack.getType().isAir()) continue;
            if (!stack.isSimilar(itemCopy)) continue;

            int take = Math.min(stack.getAmount(), remaining);
            int newAmt = stack.getAmount() - take;

            if (newAmt <= 0) {
                storage[i] = null;
            } else {
                stack.setAmount(newAmt);
                storage[i] = stack;
            }
            remaining -= take;
        } inv.setStorageContents(storage);

        // offhand
        if (remaining > 0) {
            ItemStack off = inv.getItemInOffHand();
            if (off != null && !off.getType().isAir() && off.isSimilar(itemCopy)) {
                int take = Math.min(off.getAmount(), remaining);
                int newAmt = off.getAmount() - take;

                if (newAmt <= 0) {
                    inv.setItemInOffHand(new ItemStack(Material.AIR));
                } else {
                    off.setAmount(newAmt);
                    inv.setItemInOffHand(off);
                }
                remaining -= take;
            }
        }

        return remaining == 0;
    }

    public static boolean addToInventoryOrDrop(Player player, ItemStack item) {
        if (player == null || item == null || item.getType().isAir()) return false;
        return addToInventoryOrDrop(player, item, item.getAmount());
    }

    public static boolean addToInventoryOrDrop(Player player, ItemStack item, int quantity) {
        if (quantity <= 0) return true;
        if (player == null || item == null || item.getType().isAir()) return false;

        if (!Bukkit.isPrimaryThread()) {
            StaticUtils.log(ChatColor.RED, "Cannot addToInventoryOrDrop() for "+ player.getName() + " because it was called off the main thread..!");
            return false;
        }

        PlayerInventory playerInv = player.getInventory();
        ItemStack itemCopy = item.clone();
        int maxStackSize = itemCopy.getType().getMaxStackSize();
        int remaining = quantity;
        itemCopy.setAmount(1);

        
        { // add to existing itemstacks in inv
            // main inv
            ItemStack[] storage = playerInv.getStorageContents();
            for (int i = 0; i < storage.length && remaining > 0; i++) {
                ItemStack invStack = storage[i];
                if (invStack == null || invStack.getType().isAir()) continue;
                if (!invStack.isSimilar(itemCopy)) continue;

                int invAmount = invStack.getAmount();
                int invStackSpace = maxStackSize - invAmount;
                if (invStackSpace<=0) continue;

                int add = Math.min(invStackSpace, remaining);
                invStack.setAmount(invAmount + add);
                storage[i] = invStack;
                remaining -= add;
            } playerInv.setStorageContents(storage);

            // offhand
            if (remaining > 0) {
                ItemStack offStack = playerInv.getItemInOffHand();
                if (offStack != null && !offStack.getType().isAir() && offStack.isSimilar(itemCopy)) {
                    int offStackSpace = maxStackSize - offStack.getAmount();
                    if (offStackSpace>0)  {
                        int add = Math.min(offStackSpace, remaining);
                        offStack.setAmount(offStack.getAmount() + add);
                        playerInv.setItemInOffHand(offStack);
                        remaining -= add;
                    }
                }
            }
        }
        
        if (remaining > 0) { // create new itemstacks in empty inv spots
            // storage (hotbar + main, excludes armor)
            ItemStack[] storage = playerInv.getStorageContents();
            for (int i = 0; i < storage.length && remaining > 0; i++) {
                ItemStack slot = storage[i];
                if (slot!=null && !slot.getType().isAir()) continue;

                ItemStack newStack = itemCopy.clone();
                int add = Math.min(maxStackSize, remaining);

                newStack.setAmount(add);
                storage[i] = newStack;
                remaining -= add;
            } playerInv.setStorageContents(storage);
        }

        
        boolean droppedOnFloor = false;
        while (remaining > 0) { // drop new itemstacks on the floor
            ItemStack newItem = itemCopy.clone();
            int add = Math.min(maxStackSize, remaining);

            newItem.setAmount(add);
            player.getWorld().dropItemNaturally(player.getLocation(), newItem);
            remaining -= add;
            droppedOnFloor = true;
        }

        if (droppedOnFloor) StaticUtils.sendMessage(player, "&eYour inventory is full -- check the ground for your items!");
        return remaining == 0;
    }

    public static boolean addToInventoryOrDrop(Inventory inv, ItemStack item, int quantity) {
        if (quantity <= 0) return true;
        if (inv == null || item == null || item.getType().isAir()) return false;

        if (!Bukkit.isPrimaryThread()) {
            StaticUtils.log(ChatColor.RED, "Cannot addToInventoryOrDrop() because it was called off the main thread..!");
            return false;
        }

        ItemStack itemCopy = item.clone();
        int maxStackSize = itemCopy.getType().getMaxStackSize();
        int remaining = quantity;
        itemCopy.setAmount(1);

        
        // add to existing itemstacks in inv
        ItemStack[] storage = inv.getStorageContents();
        for (int i = 0; i < storage.length && remaining > 0; i++) {
            ItemStack invStack = storage[i];
            if (invStack == null || invStack.getType().isAir()) continue;
            if (!invStack.isSimilar(itemCopy)) continue;

            int invAmount = invStack.getAmount();
            int invStackSpace = maxStackSize - invAmount;
            if (invStackSpace<=0) continue;

            int add = Math.min(invStackSpace, remaining);
            invStack.setAmount(invAmount + add);
            storage[i] = invStack;
            remaining -= add;
        } inv.setStorageContents(storage);
        
        
        if (remaining > 0) { // create new itemstacks in empty inv spots
            // storage (hotbar + main, excludes armor)
            ItemStack[] storage2 = inv.getStorageContents();
            for (int i = 0; i < storage2.length && remaining > 0; i++) {
                ItemStack slot = storage2[i];
                if (slot!=null && !slot.getType().isAir()) continue;

                ItemStack newStack = itemCopy.clone();
                int add = Math.min(maxStackSize, remaining);

                newStack.setAmount(add);
                storage2[i] = newStack;
                remaining -= add;
            } inv.setStorageContents(storage2);
        }

        if (remaining > 0) {
            StaticUtils.log(ChatColor.RED, "A physical inventory was full and couldn't accept " + remaining + " more " + item.getType().toString());
        }
        
        return remaining == 0;
    }

    public static ItemStack prepPlayerShopItemStack(Integer amount) {
        ItemStack lectern = new ItemStack(Material.LECTERN);
        ItemMeta meta = lectern.getItemMeta();

        meta.getPersistentDataContainer().set(StaticUtils.SHOP_KEY, PersistentDataType.STRING, "true");
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aPlayerShop"));

        lectern.setItemMeta(meta);
        if (amount!=null) lectern.setAmount(amount);

        return lectern;
    }

    public static ItemStack prepDepositWandItemStack(Integer amount) {
        ItemStack wand = new ItemStack(Material.SPYGLASS);
        ItemMeta meta = wand.getItemMeta();

        meta.getPersistentDataContainer().set(StaticUtils.DESPOIT_WAND_KEY, PersistentDataType.STRING, "true");
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6Deposit Wand"));
        meta.setLore(null);
        List<String> lore = new ArrayList<>();
        lore.add("&7&oShift-click a container to");
        lore.add("&7&odeposit items into your shops");
        meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
        meta.addEnchant(Enchantment.LURE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        wand.setItemMeta(meta);
        if (amount!=null) wand.setAmount(amount);

        return wand;
    }

    public static ItemStack prepSellWandItemStack(Integer amount) {
        ItemStack wand = new ItemStack(Material.SPYGLASS);
        ItemMeta meta = wand.getItemMeta();

        meta.getPersistentDataContainer().set(StaticUtils.SELL_WAND_KEY, PersistentDataType.STRING, "true");
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6Sell Wand"));
        meta.setLore(null);
        List<String> lore = new ArrayList<>();
        lore.add("&7&oShift-click a container to");
        lore.add("&7&osell items to matching shops");
        meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
        meta.addEnchant(Enchantment.LURE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        wand.setItemMeta(meta);
        if (amount!=null) wand.setAmount(amount);

        return wand;
    }

    public static BlockData applySameOrientation(BlockData from, BlockData to) {
        // Facing blocks: observers, dispensers, droppers, stairs (also have other props), wall signs, etc.
        if (from instanceof Directional f && to instanceof Directional t) {
            t.setFacing(f.getFacing());
        }
        // Rotatable blocks: standing signs, item frames (entity), skulls, etc.
        if (from instanceof Rotatable f && to instanceof Rotatable t) {
            t.setRotation(f.getRotation());
        }
        // Axis-based blocks: logs, pillars, chains, basalt, etc.
        if (from instanceof Orientable f && to instanceof Orientable t) {
            t.setAxis(f.getAxis());
        }
        return to;
    }

    // Headcache
    public static final Map<UUID, Pair<SkullMeta, Long>> headMetaCache = new HashMap<>();
    private static final Set<UUID> refreshInProgress = new HashSet<>();

    /**
     * Adds skin texture to head meta.
     * 
     * If player+skin is in cached map, retrieve it
     * else use setOwningPlayer and save the SkullMeta to cache
     *
     * @param headMeta the head meta to modify
     * @param player the player whose head we want
     */
    public static void applyHeadTexture(ItemStack head, OfflinePlayer player) {
        UUID uuid = player.getUniqueId();
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        Pair<SkullMeta, Long> entry = headMetaCache.get(uuid);

        if (isCacheValid(player, entry)) {
            head.setItemMeta(entry.getLeft().clone());
            return;
        }
        
        headMeta.setOwningPlayer(player);
        head.setItemMeta(headMeta);

        if (refreshInProgress.contains(uuid)) return;
        else refreshInProgress.add(uuid);
        
        // Delay to allow the server to apply the skin texture
        Bukkit.getScheduler().runTaskLater(javaPlugin, () -> {
            try {
                SkullMeta updatedMeta = (SkullMeta) head.getItemMeta();
                headMetaCache.put(uuid, Pair.of(updatedMeta, System.currentTimeMillis()));
            } finally {
                refreshInProgress.remove(uuid);
            }
        }, 20L);
    }

    private static boolean isCacheValid(OfflinePlayer player, Pair<SkullMeta, Long> entry) {
        if (entry==null) {
            return false;
        }
        
        if (player.isOnline() && player instanceof Player) {
            long currentTime = System.currentTimeMillis();
            if ((currentTime - entry.getRight()) >= 3600000) {
                return false;
            }
        }

        return true;
    }
}