package dev.tbm00.papermc.playershops64.data.enums;

public enum SortType {
    UNSORTED,
    MATERIAL,
    BUY_PRICE,
    SELL_PRICE,
    STOCK,
    BALANCE;

    public static String toString(SortType sortType) {
        switch (sortType) {
            case UNSORTED:
                return "Unsorted";
            case MATERIAL:
                return "Material";
            case BUY_PRICE:
                return "Buy Price";
            case SELL_PRICE:
                return "Sell Price";
            case STOCK:
                return "Stock";
            case BALANCE:
                return "Balance";
            default:
                return null;
        }
    }

    public static SortType nextType(SortType sortType) {
        switch (sortType) {
            case UNSORTED:
                return MATERIAL;
            case MATERIAL:
                return BUY_PRICE;
            case BUY_PRICE:
                return SELL_PRICE;
            case SELL_PRICE:
                return STOCK;
            case STOCK:
                return BALANCE;
            case BALANCE:
                return UNSORTED;
            default:
                return null;
        }
    }
}
