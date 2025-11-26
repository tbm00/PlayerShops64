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

    public static SortType fromString(String string) {
        String lowercase = string.toLowerCase().replace("_", "");
        switch (lowercase) {
            case "unsorted":
            case "none":
                return UNSORTED;
            case "material":
            case "mat":
                return MATERIAL;
            case "buyprice":
            case "buy":
                return BUY_PRICE;
            case "sellprice":
            case "sell":
                return SELL_PRICE;
            case "stock":
                return STOCK;
            case "balance":
                return BALANCE;
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
