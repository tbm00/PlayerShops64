package dev.tbm00.papermc.playershops64.data.structure;

import org.bukkit.Material;

public class GuiSearchQuery {
    private int slot;
    private String name;
    private String lore;
    private Material material;
    private String query;

    public GuiSearchQuery(int slot, String name, String lore, Material material, String query) {
        this.slot = slot;
        this.lore = lore;
        this.name = name;
        this.material = material;
        this.query = query;
    }

    // --- Getters ---
    public int getSlot() {
        return slot;
    }

    public String getName() {
        return name;
    }

    public String getLore() {
        return lore;
    }

    public Material getMaterial() {
        return material;
    }

    public String getQuery() {
        return query;
    }

    // --- Setters ---
    public void setSlot(int slot) {
        this.slot = slot;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLore(String lore) {
        this.lore = lore;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
