package dev.tbm00.papermc.playershops64.data.structure;

import java.math.BigDecimal;

import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class ItemPrice {
    // on SQL
    private String itemStackBase64;
    private BigDecimal averagePrice;
    private Integer quantitySold;

    /**
     * Constructs a Shop with all properties initialized.
     */
    public ItemPrice(String itemStackBase64, BigDecimal averagePrice, Integer quantitySold) {
        this.itemStackBase64 = itemStackBase64;
        this.averagePrice = averagePrice;
        this.quantitySold = quantitySold;
    }

    // --- Getters ---
    public String getItemStackBase64() {
        return itemStackBase64;
    }

    public BigDecimal getAveragePrice() {
        return averagePrice;
    }

    public Integer getQuantiySold() {
        return quantitySold;
    }

    // --- Setters ---
    public void setItemStackBase64(String itemStackBase64) {
        this.itemStackBase64 = itemStackBase64;
    }

    public void setAveragePrice(BigDecimal averagePrice) {
        this.averagePrice = StaticUtils.normalizeBigDecimal(averagePrice);;
    }

    public void setQuantitySold(Integer quantitySold) {
        this.quantitySold = quantitySold;
    }
}