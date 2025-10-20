package dev.tbm00.papermc.playershops64.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.data.structure.GuiSearchCategory;
import dev.tbm00.papermc.playershops64.data.structure.GuiSearchQuery;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class ConfigHandler {
    private final PlayerShops64 javaPlugin;

    private boolean floodgateEnabled = false;

    private String chatPrefix;
    
    private int displayTickCycle = 5;
    private int displayViewDistance = 16;
    private int displayFocusDistance = 5;
    private double displayDisplayHeight = 0.0;
    private String displayHoloColor = "60,0,0,0";

    private int maxStock = 10000;
    private int maxBalance = 100000000;
    private int maxBuyPrice = 10000000;
    private int maxSellPrice = 10000000;
    private Set<Material> baseBlockMaterials = new HashSet<>();

    private List<GuiSearchCategory> searchCategories = new ArrayList<>();
    

    /**
     * Constructs a ConfigHandler instance.
     * Loads configuration values for the plugin.
     *
     * @param javaPlugin the main plugin instance
     */
    public ConfigHandler(PlayerShops64 javaPlugin) {
        this.javaPlugin = javaPlugin;
        boolean passed = true;
        try {
            if (!loadHookSection()) passed = false;
            if (!loadLanguageSection()) passed = false;
            if (!loadShopSection()) passed = false;
            if (!loadDisplaySection()) passed = false;
            if (!loadGuiSection()) passed = false;
            
            if (passed)
                StaticUtils.log(ChatColor.GREEN, "ConfigHandler initialized.");
            else StaticUtils.log(ChatColor.DARK_RED, "Config not loaded properly!");
        } catch (Exception e) {
            StaticUtils.log(ChatColor.RED, "Caught exception loading config: " + e.getMessage());
        }
    }

    /**
     * Loads the "hooks" section of the configuration.
     */
    private boolean loadHookSection() {
        ConfigurationSection hooks = javaPlugin.getConfig().getConfigurationSection("hooks");
        if (hooks!=null)
            floodgateEnabled = hooks.contains("floodgate") ? hooks.getBoolean("floodgate") : false;
        
        return true;
    }

    /**
     * Loads the "lang" section of the configuration.
     */
    private boolean loadLanguageSection() {
        ConfigurationSection lang = javaPlugin.getConfig().getConfigurationSection("lang");
        if (lang!=null)
            chatPrefix = lang.contains("prefix") ? lang.getString("prefix") : null;
        
        return true;
    }

    /**
     * Loads the "display" section of the configuration.
     */
    private boolean loadDisplaySection() {
        ConfigurationSection display = javaPlugin.getConfig().getConfigurationSection("display");
        if (display != null) {
            displayTickCycle = display.getInt("tickCycle", 5);
            displayViewDistance = display.getInt("viewDistance", 16);
            displayFocusDistance = display.getInt("focusDistance", 5);
            displayDisplayHeight = display.getDouble("displayHeight", 0.0);
            displayHoloColor = display.getString("holoColor", "60,0,0,0");
        }

        return true;
    }

    /**
     * Loads the "shop" section of the configuration.
     */
    private boolean loadShopSection() {
        ConfigurationSection shop = javaPlugin.getConfig().getConfigurationSection("shop");
        if (shop != null) {
            maxStock = shop.getInt("maxStock", 10000);
            maxStock = shop.getInt("maxBalance", 100000000);
            maxStock = shop.getInt("maxBuyPrice", 10000000);
            maxStock = shop.getInt("maxSellPrice", 10000000);

            List<String> materialStrings = shop.getStringList("baseBlockMaterials");
            for (String string : materialStrings) {
                Material material = Material.getMaterial(string);
                if (material == null) StaticUtils.log(ChatColor.RED, "Error loading base block material '" + string + "' from config.yml!");
                else baseBlockMaterials.add(material);
            }
        }

        return true;
    }

    /**
     * Loads the "gui" section of the configuration.
     */
    private boolean loadGuiSection() {
        this.searchCategories.clear();
        ConfigurationSection gui = javaPlugin.getConfig().getConfigurationSection("gui");
        
        if (gui == null) {
            StaticUtils.log(ChatColor.RED, "'gui' section not found in config!");
            return false;
        }

        ConfigurationSection categoriesSec = gui.getConfigurationSection("categories");
        if (categoriesSec == null) {
            StaticUtils.log(ChatColor.RED, "'gui.categories' section not found in config!");
            return false;
        }

        // Parse categories
        for (String slotKey : categoriesSec.getKeys(false)) {
            ConfigurationSection catSec = categoriesSec.getConfigurationSection(slotKey);
            if (catSec == null) continue;

            int slot;
            try {
                slot = Integer.parseInt(slotKey);
                if (!(0<=slot && slot<=44)) {
                    StaticUtils.log(ChatColor.RED, "Invalid category slot key '" + slotKey + "'. Must be an integer 0-44. Skipping.");
                    continue;
                }
            } catch (NumberFormatException nfe) {
                StaticUtils.log(ChatColor.RED, "Invalid category slot key '" + slotKey + "'. Must be an integer 0-44. Skipping.");
                continue;
            }

            
            String name = catSec.getString("name", "&cUnnamed Category");
            String lore = catSec.getString("lore", "");
            Material material = StaticUtils.parseMaterial(catSec.getString("material", null));

            // Parse queries
            List<GuiSearchQuery> queries = new ArrayList<>();
            ConfigurationSection queriesSec = catSec.getConfigurationSection("queries");
            if (queriesSec != null) {
                for (String qSlotKey : queriesSec.getKeys(false)) {
                    ConfigurationSection qSec = queriesSec.getConfigurationSection(qSlotKey);
                    if (qSec == null) continue;

                    int qSlot;
                    try {
                        qSlot = Integer.parseInt(qSlotKey);
                        if (!(0<=qSlot && qSlot<=44)) {
                            StaticUtils.log(ChatColor.RED, "Invalid query slot key '" + qSlotKey + "' in category " + slot + ". Must be an integer 0-44. Skipping.");
                            continue;
                        }
                    } catch (NumberFormatException nfe) {
                        StaticUtils.log(ChatColor.RED, "Invalid query slot key '" + qSlotKey + "' in category " + slot + ". Must be an integer 0-44. Skipping.");
                        continue;
                    }

                    String qName = qSec.getString("name", "&cUnnamed Query");
                    String qLore = qSec.getString("lore", "");
                    Material qMat = StaticUtils.parseMaterial(qSec.getString("material", null));
                    String qString = qSec.getString("query", "");

                    queries.add(new GuiSearchQuery(qSlot, qName, qLore, qMat, qString));
                }
            }

            // Sort queries by slot
            queries.sort(Comparator.comparingInt(GuiSearchQuery::getSlot));

            this.searchCategories.add(new GuiSearchCategory(slot, name, lore, material, queries));
        }

        // Sort categories by slot
        this.searchCategories.sort(Comparator.comparingInt(GuiSearchCategory::getSlot));

        StaticUtils.log(ChatColor.GREEN, "Loaded " + this.searchCategories.size() + " GUI search categories from config.yml.");
        return true;
    }

    // Hooks
    public boolean isFloodgateEnabled() {
        return floodgateEnabled;
    }

    // Language
    public String getChatPrefix() {
        return chatPrefix;
    }

    // Shop
    public int getMaxStock() {
        return maxStock;
    }

    public int getMaxBalance() {
        return maxBalance;
    }

    public int getMaxBuyPrice() {
        return maxBuyPrice;
    }

    public int getMaxSellPrice() {
        return maxSellPrice;
    }
    
    public Set<Material> getBaseBlockMaterials() {
        return baseBlockMaterials;
    }

    // Display
    public int getDisplayTickCycle() {
        return displayTickCycle;
    }

    public int getDisplayViewDistance() {
        return displayViewDistance;
    }

    public int getDisplayFocusDistance() {
        return displayFocusDistance;
    }

    public double getDisplayDefaultHeight() {
        return displayDisplayHeight;
    }

    public String getDisplayHoloColor() {
        return displayHoloColor;
    }

    // Gui
    public List<GuiSearchCategory> getSearchCategories() {
        return Collections.unmodifiableList(searchCategories);
    }
}