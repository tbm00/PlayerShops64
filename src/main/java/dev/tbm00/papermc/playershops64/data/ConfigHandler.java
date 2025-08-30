package dev.tbm00.papermc.playershops64.data;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import dev.tbm00.papermc.playershops64.PlayerShops64;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class ConfigHandler {
    private final PlayerShops64 javaPlugin;

    private String chatPrefix;
    
    private int displayTickCycle = 5;
    private int displayViewDistance = 16;
    private int displayFocusDistance = 5;
    private double displayGlassScale = 1.0;
    private double displayItemScale = 1.0;
    private String displayHoloColor = "60,0,0,0";

    /**
     * Constructs a ConfigHandler instance.
     * Loads configuration values for the plugin.
     *
     * @param javaPlugin the main plugin instance
     */
    public ConfigHandler(PlayerShops64 javaPlugin) {
        this.javaPlugin = javaPlugin;
        try {
            loadLanguageSection();
            loadDisplaySection();
        } catch (Exception e) {
            StaticUtils.log(ChatColor.RED, "Caught exception loading config: " + e.getMessage());
        }
    }

    /**
     * Loads the "lang" section of the configuration.
     */
    private void loadLanguageSection() {
        ConfigurationSection section = javaPlugin.getConfig().getConfigurationSection("lang");
        if (section!=null)
            chatPrefix = section.contains("prefix") ? section.getString("prefix") : null;
    }

    /**
     * Loads the "display" section of the configuration.
     */
    private void loadDisplaySection() {
        ConfigurationSection section = javaPlugin.getConfig().getConfigurationSection("display");
        if (section != null) {
            displayTickCycle = section.getInt("tick-cycle", 5);
            displayViewDistance = section.getInt("view-distance", 16);
            displayFocusDistance = section.getInt("focus-distance", 5);
            displayGlassScale = section.getDouble("glass-scale", 1.0);
            displayItemScale = section.getDouble("item-scale", 1.0);
            displayHoloColor = section.getString("holo-color", "60,0,0,0");
        }
    }

    // Language
    public String getChatPrefix() {
        return chatPrefix;
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

    public double getDisplayGlassScale() {
        return displayGlassScale;
    }

    public double getDisplayItemScale() {
        return displayItemScale;
    }

    public String getDisplayHoloColor() {
        return displayHoloColor;
    }

}