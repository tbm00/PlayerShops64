package dev.tbm00.papermc.playershops64.data.enums;

public enum AdjustAttribute {
    BUY_PRICE,
    SELL_PRICE,
    STOCK,
    BALANCE,
    DESCRIPTION,
    DISPLAY_HEIGHT,
    ASSISTANT,
    BASE_MATERIAL,
    TRANSACTION;

    public static String toString(AdjustAttribute adjustAttribute) {
        switch (adjustAttribute) {
            case BUY_PRICE:
                return "Buy Price";
            case SELL_PRICE:
                return "Sell Price";
            case STOCK:
                return "Stock";
            case BALANCE:
                return "Balance";
            case DESCRIPTION:
                return "Description";
            case DISPLAY_HEIGHT:
                return "Display Height";
            case ASSISTANT:
                return "Assistant";
            case BASE_MATERIAL:
                return "Material";
            case TRANSACTION:
                return "Transaction";
            default:
                return null;
        }
    }
}

