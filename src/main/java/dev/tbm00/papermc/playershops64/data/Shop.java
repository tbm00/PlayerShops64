package dev.tbm00.papermc.playershops64.data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class Shop {
    // on SQL
    private UUID uuid; // generated when the shop is created (shop base block is placed)
    private UUID ownerUuid; // minecraft player's uuid
    private String ownerName; // minecraft player's username
    private World world; // stored in SQL as String (using world.getName())
    private Location location; // stored in SQL as String (using "x,y,z" formatting)
    private ItemStack itemStack; // serialized and stored in SQL as Base64
    private int stackSize; // minimum 1, should never be null
    private int itemStock; // minimum 0, should never be null
    private BigDecimal moneyStock; // minimum 0, should never be null
    private BigDecimal buyPrice; // null to disable buying from the shop
    private BigDecimal sellPrice; // null to disable selling to the shop
    private Date lastTransactionDate; // null if no transactions ever
    private boolean infiniteMoney; // only enabled by admins
    private boolean infiniteStock; // only enabled by admins
    private String description; // null if not set
    private int displayHeight; // -5 thru 5, 0 default
    private Material baseMaterial; // default lectern (will have a predefined set to select from via gui)

    // NOT on SQL
    private UUID currentEditor; // null for none

    /**
     * Constructs a Shop with all properties initialized.
     */
    public Shop(UUID uuid,
                UUID ownerUuid,
                String ownerName,
                World world,
                Location location,
                ItemStack itemStack,
                int stackSize,
                int itemStock,
                BigDecimal moneyStock,
                BigDecimal buyPrice,
                BigDecimal sellPrice,
                Date lastTransactionDate,
                boolean infiniteMoney,
                boolean infiniteStock,
                String description,
                int displayHeight,
                Material baseMaterial,
                UUID currentEditor
                ) {
        this.uuid = uuid;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.world = world;
        this.location = location;
        this.itemStack = itemStack;
        this.stackSize = (stackSize>0) ? stackSize : 1;
        this.itemStock = (itemStock>=0) ? itemStock : 0;
        this.moneyStock = (moneyStock!=null && moneyStock.compareTo(BigDecimal.ZERO)>=0) ? StaticUtils.normalizeBigDecimal(moneyStock) : BigDecimal.ZERO;
        this.buyPrice = (buyPrice!=null && buyPrice.compareTo(BigDecimal.ZERO)>=0) ? StaticUtils.normalizeBigDecimal(buyPrice) : null;
        this.sellPrice = (sellPrice!=null && sellPrice.compareTo(BigDecimal.ZERO)>=0) ? StaticUtils.normalizeBigDecimal(sellPrice) : null;
        this.lastTransactionDate = lastTransactionDate;
        this.infiniteMoney = infiniteMoney;
        this.infiniteStock = infiniteStock;
        this.description = description;
        this.displayHeight = displayHeight;
        this.baseMaterial = baseMaterial;
        this.currentEditor = currentEditor;
    }

    // --- Getters ---
    public UUID getUuid() {
        return uuid;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public World getWorld() {
        return world;
    }

    public Location getLocation() {
        return location;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public int getStackSize() {
        return stackSize;
    }

    public int getItemStock() {
        return itemStock;
    }

    public BigDecimal getMoneyStock() {
        return moneyStock;
    }

    public BigDecimal getBuyPrice() {
        return buyPrice;
    }

    public BigDecimal getSellPrice() {
        return sellPrice;
    }

    public Date getLastTransactionDate() {
        return lastTransactionDate;
    }

    public boolean hasInfiniteMoney() {
        return infiniteMoney;
    }

    public boolean hasInfiniteStock() {
        return infiniteStock;
    }

    public String getDescription() {
        return description;
    }

    public UUID getCurrentEditor() {
        return currentEditor;
    }

    public int getDisplayHeight() {
        if (displayHeight < -5 || displayHeight > 5) {
            displayHeight = 0;
            return displayHeight;
        }

        return displayHeight;
    }

    public Material getBaseMaterial() {
        if (baseMaterial == null) baseMaterial = Material.LECTERN;
        return baseMaterial;
    }

    // --- Setters ---
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public void setStackSize(int stackSize) {
        this.stackSize = (stackSize > 0) ? stackSize : 1;
    }

    public void setItemStock(int itemStock) {
        this.itemStock = (itemStock > 0) ? itemStock : 0;
    }

    public void setMoneyStock(BigDecimal moneyStock) {
        if (moneyStock != null && moneyStock.compareTo(BigDecimal.ZERO) > 0) { 
            this.moneyStock = StaticUtils.normalizeBigDecimal(moneyStock);
        } else {
            this.moneyStock = BigDecimal.ZERO;
        }
    }

    public void setBuyPrice(BigDecimal buyPrice) {
        if (buyPrice != null && buyPrice.compareTo(BigDecimal.ZERO) >= 0) {
            this.buyPrice = StaticUtils.normalizeBigDecimal(buyPrice);
        } else {
            this.buyPrice = null;
        }
    }

    public void setSellPrice(BigDecimal sellPrice) {
        if (sellPrice != null && sellPrice.compareTo(BigDecimal.ZERO) >= 0) {
            this.sellPrice = StaticUtils.normalizeBigDecimal(sellPrice);
        } else {
            this.sellPrice = null;
        }
    }

    public void setLastTransactionDate(Date lastTransactionDate) {
        this.lastTransactionDate = lastTransactionDate;
    }

    public void setInfiniteMoney(boolean infiniteMoney) {
        this.infiniteMoney = infiniteMoney;
    }

    public void setInfiniteStock(boolean infiniteStock) {
        this.infiniteStock = infiniteStock;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDisplayHeight(int displayHeight) {
        if (displayHeight < -5) {
            displayHeight = -5;
        } else if (displayHeight > 5) {
            displayHeight = 5;
        }
        this.displayHeight = displayHeight;
    }

    public void setBaseMaterial(Material baseMaterial) {
        if (baseMaterial == null) baseMaterial = Material.LECTERN;
        this.baseMaterial = baseMaterial;
    }

    public void setCurrentEditor(UUID currentEditor) {
        this.currentEditor = currentEditor;
    }
}
