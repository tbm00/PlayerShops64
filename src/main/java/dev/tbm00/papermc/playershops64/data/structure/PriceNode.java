package dev.tbm00.papermc.playershops64.data.structure;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public final class PriceNode implements Comparable<PriceNode> {
    private final BigDecimal price;
    private final UUID shopUuid;

    public PriceNode(BigDecimal price, UUID shopUuid) {
        if (price == null || shopUuid == null) throw new NullPointerException("price/shopUuid");
        this.price = StaticUtils.normalizeBigDecimal(price);
        this.shopUuid = shopUuid;
    }

    public BigDecimal getPrice() { return price; }
    public UUID getUuid() { return shopUuid; }

    // higher price first -- ties break by UUID
    @Override
    public int compareTo(PriceNode o) {
        int c = this.price.compareTo(o.price);
        if (c != 0) return c;
        return this.shopUuid.compareTo(o.shopUuid);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PriceNode)) return false;
        PriceNode other = (PriceNode) obj;
        return price.compareTo(other.price) == 0 && shopUuid.equals(other.shopUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(price.stripTrailingZeros(), shopUuid);
    }

    @Override
    public String toString() {
        return "PriorityNode{price=" + price + ", shopUuid=" + shopUuid + "}";
    }
}
