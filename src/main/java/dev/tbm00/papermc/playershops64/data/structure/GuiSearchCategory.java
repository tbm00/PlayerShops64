package dev.tbm00.papermc.playershops64.data.structure;

import java.util.List;

import org.bukkit.Material;

public class GuiSearchCategory {
    private int slot;
    private String name;
    private String lore;
    private Material material;
    private List<GuiSearchQuery> queries;

    public GuiSearchCategory(int slot, String name, String lore, Material material, List<GuiSearchQuery> queries) {
        this.slot = slot;
        this.lore = lore;
        this.name = name;
        this.material = material;
        this.queries = queries;
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

    public List<GuiSearchQuery> getQueries() {
        return queries;
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

    public void setQueries(List<GuiSearchQuery> queries) {
        this.queries = queries;
    }
}
