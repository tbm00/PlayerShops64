package dev.tbm00.papermc.playershops64.data;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import dev.tbm00.papermc.playershops64.data.structure.Shop;
import dev.tbm00.papermc.playershops64.utils.ItemSerializer;
import dev.tbm00.papermc.playershops64.utils.StaticUtils;

public class ShopDAO {
    private final MySQLConnection mySQL;

    public ShopDAO(MySQLConnection mySQL) {
        this.mySQL = mySQL;
        StaticUtils.log(ChatColor.GREEN, "ShopDAO initialized.");
    }

    public List<Shop> getAllShopsFromSql() {
        final String sql = "SELECT * FROM "+StaticUtils.TBL_SHOPS;
        List<Shop> out = new ArrayList<>();
        try (Connection conn = mySQL.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                try {
                    Shop s = mapResultSetToShop(rs);
                    out.add(s);
                } catch (Exception ex) {
                    StaticUtils.log(ChatColor.RED, "Failed to map shop row: " + ex.getMessage());
                }
            }
        } catch (SQLException e) {
            StaticUtils.log(ChatColor.RED, "Failed to fetch all shops: " + e.getMessage());
        }
        return out;
    }

    /**
     * Retrieves a shop by UUID.
     */
    public Shop getShopFromSql(UUID uuid) {
        final String sql = "SELECT * FROM "+StaticUtils.TBL_SHOPS+" WHERE uuid = ?";
        try (Connection conn = mySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToShop(rs);
            }
        } catch (SQLException e) {
            StaticUtils.log(ChatColor.RED, "Failed to fetch shop: " + e.getMessage());
        }
        return null;
    }

    /**
     * Adds or updates a shop.
     */
    public boolean upsertShopToSql(Shop shop) {
        if (shop.getWorld()==null) {
            StaticUtils.log(ChatColor.RED, shop.getOwnerName() + "'s shop's ("+shop.getUuid()+") world is unloaded/null, so the shop was not upserted in SQL!");
            return false;
        } else if (shop.getLocation()==null) {
            StaticUtils.log(ChatColor.RED, shop.getOwnerName() + "'s shop's ("+shop.getUuid()+") location is null, so the shop was not upserted in SQL!");
            return false;
        }

        final String sql = "INSERT INTO "+StaticUtils.TBL_SHOPS+" " +
            "(uuid, owner_uuid, owner_name, world, location, itemstack_b64, stack_size, item_stock, money_stock, " +
            " buy_price, sell_price, last_tx, inf_money, inf_stock, description, display_height, base_material, assistants) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE " +
            " owner_uuid=VALUES(owner_uuid), " +
            " owner_name=VALUES(owner_name), " +
            " world=VALUES(world), " +
            " location=VALUES(location), " +
            " itemstack_b64=VALUES(itemstack_b64), " +
            " stack_size=VALUES(stack_size), " +
            " item_stock=VALUES(item_stock), " +
            " money_stock=VALUES(money_stock), " +
            " buy_price=VALUES(buy_price), " +
            " sell_price=VALUES(sell_price), " +
            " last_tx=VALUES(last_tx), " +
            " inf_money=VALUES(inf_money), " +
            " inf_stock=VALUES(inf_stock), " +
            " description=VALUES(description)," +
            " display_height=VALUES(display_height)," +
            " base_material=VALUES(base_material), " +
            " assistants=VALUES(assistants)";

        try (Connection conn = mySQL.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, shop.getUuid().toString());
            bindStringOrNull(ps, 2, shop.getOwnerUuid() != null ? shop.getOwnerUuid().toString() : null);
            bindStringOrNull(ps, 3, shop.getOwnerName());
            ps.setString(4, shop.getWorld().getName());
            ps.setString(5, serializeLocation(shop.getLocation()));
            ps.setString(6, ItemSerializer.itemStackToBase64(shop.getItemStack()));
            ps.setInt(7, shop.getStackSize());
            ps.setInt(8, shop.getItemStock());
            ps.setBigDecimal(9, shop.getMoneyStock());
            bindBigDecimalOrNull(ps, 10, shop.getBuyPrice());
            bindBigDecimalOrNull(ps, 11, shop.getSellPrice());
            bindTimestampOrNull(ps, 12, shop.getLastTransactionDate());
            ps.setBoolean(13, shop.hasInfiniteMoney());
            ps.setBoolean(14, shop.hasInfiniteStock());
            bindStringOrNull(ps, 15, shop.getDescription());
            ps.setInt(16, shop.getDisplayHeight());
            ps.setString(17, materialToString(shop.getBaseMaterial()));
            ps.setString(18, serializeAssistants(shop.getAssistants()));

            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            StaticUtils.log(ChatColor.RED, "Failed to upsert shop: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a shop by UUID.
     */
    public boolean deleteShopFromSql(UUID uuid) {
        final String sql = "DELETE FROM "+StaticUtils.TBL_SHOPS+" WHERE uuid=?";
        try (Connection conn = mySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            StaticUtils.log(ChatColor.RED, "Failed to delete shop: " + e.getMessage());
            return false;
        }
    }

    /**
     * Maps a SQL row to a Shop object.
     */
    private Shop mapResultSetToShop(ResultSet rs) throws SQLException {
        UUID uuid = UUID.fromString(rs.getString("uuid"));
        String ownerUuidStr = rs.getString("owner_uuid");
        UUID ownerUuid = null;
        if (ownerUuidStr != null && !ownerUuidStr.isBlank()) ownerUuid = UUID.fromString(ownerUuidStr);
        String ownerName = rs.getString("owner_name");
        String worldName = rs.getString("world");
        World world = (worldName == null) ? null : Bukkit.getWorld(worldName);
        Location location = deserializeLocation(world, rs.getString("location"));
        String raw = rs.getString("itemstack_b64");
        ItemStack itemStack = (raw==null || raw.isBlank()) ? null : ItemSerializer.itemStackFromBase64(raw);
        int stackSize = rs.getInt("stack_size");
        int itemStock = rs.getInt("item_stock");
        BigDecimal moneyStock = rs.getBigDecimal("money_stock");
        if (moneyStock == null) moneyStock = BigDecimal.ZERO;
        BigDecimal buyPrice = rs.getBigDecimal("buy_price");
        BigDecimal sellPrice = rs.getBigDecimal("sell_price");
        java.util.Date lastTx = toJavaDate(rs.getTimestamp("last_tx"));
        boolean infiniteMoney = rs.getBoolean("inf_money");
        boolean infiniteStock = rs.getBoolean("inf_stock");
        String description = rs.getString("description");
        int displayHeight = rs.getInt("display_height");
        Material baseMaterial = stringToMaterial(rs.getString("base_material"));
        Set<UUID> assistants = deserializeAssistants(rs.getString("assistants"));

        return new Shop(uuid, ownerUuid, ownerName, world, location, itemStack, stackSize, itemStock, moneyStock, buyPrice, sellPrice, lastTx, infiniteMoney, infiniteStock, description, displayHeight, baseMaterial, assistants, null);
    }

    // --- Helper methods ---

    private String serializeLocation(Location loc) {
        return loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location deserializeLocation(World world, String locStr) {
        if (locStr == null || locStr.isEmpty()) return null;

        String[] parts = locStr.split(",");
        if (parts.length != 3) return null;

        int x = Integer.parseInt(parts[0]);
        int y = Integer.parseInt(parts[1]);
        int z = Integer.parseInt(parts[2]);
        return new Location(world, x, y, z);
    }

    private String serializeAssistants(Set<UUID> assistants) {
        if (assistants == null || assistants.isEmpty()) return "";
        return assistants.stream().map(UUID::toString).collect(Collectors.joining(","));
    }

    private Set<UUID> deserializeAssistants(String raw) {
        Set<UUID> out = new HashSet<>();
        if (raw == null || raw.isEmpty()) return out;
        String[] parts = raw.split(",");
        for (String p : parts) {
            String t = p.trim();
            if (t.isEmpty()) continue;
            try {
                out.add(UUID.fromString(t));
            } catch (Exception e) {
                StaticUtils.log(ChatColor.RED, "Bad assistant UUID: " + t);
                e.printStackTrace();
            }
        }
        return out;
    }

    private Material stringToMaterial(String s) {
        if (s == null || s.isEmpty()) return null;
        
        try { return Material.valueOf(s); }
        catch (Exception ex) {
            StaticUtils.log(ChatColor.RED, "Invalid base material: " + ex.getMessage());
            return null;
        }
    }

    private String materialToString(Material m) {
        return (m == null) ? null : m.name();
    }

    private java.util.Date toJavaDate(java.sql.Timestamp d) {
        return d != null ? new java.util.Date(d.getTime()) : null;
    }

    /*
    private java.sql.Timestamp toSqlTimestamp(java.util.Date d) {
        return d != null ? new java.sql.Timestamp(d.getTime()) : null;
    }
    */

    private void bindTimestampOrNull(PreparedStatement ps, int idx, java.util.Date d) throws SQLException {
        if (d == null) ps.setNull(idx, Types.TIMESTAMP); 
        else ps.setTimestamp(idx, new Timestamp(d.getTime()));
    }

    private void bindBigDecimalOrNull(PreparedStatement ps, int idx, BigDecimal v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.DECIMAL); 
        else ps.setBigDecimal(idx, v);
    }

    private void bindStringOrNull(PreparedStatement ps, int idx, String s) throws SQLException {
        if (s == null) ps.setNull(idx, Types.VARCHAR); 
        else ps.setString(idx, s);
    }
}