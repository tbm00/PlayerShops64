package dev.tbm00.spigot.playershops64.data;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import dev.tbm00.spigot.playershops64.utils.ItemSerializer;

public class ShopDAO {
    private final MySQLConnection mySQL;

    public ShopDAO(MySQLConnection mySQL) {
        this.mySQL = mySQL;
    }

    /**
     * Inserts a new shop into the database.
     */
    public void createShop(Shop shop) {
        final String sql = "INSERT INTO playershops64_shops "
            + "(uuid, owner_uuid, owner_name, world, location, itemstack_b64, stack_size, "
            + "item_stock, money_stock, buy_price, sell_price, last_tx) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = mySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, shop.getUuid().toString());
            ps.setString(2, shop.getOwnerUuid().toString());
            ps.setString(3, shop.getOwnerName());
            bindWorldStrOrNull(ps, 4, shop.getWorld());
            ps.setString(5, serializeLocation(shop.getLocation()));
            ps.setString(6, ItemSerializer.itemStackToBase64(shop.getItemStack()));
            ps.setInt(7, shop.getStackSize());
            ps.setInt(8, shop.getItemStock());
            ps.setBigDecimal(9, shop.getMoneyStock());
            bindBigDecimalOrNull(ps, 10, shop.getBuyPrice());
            bindBigDecimalOrNull(ps, 11, shop.getSellPrice());
            bindTimestampOrNull(ps, 12, shop.getLastTransactionDate());

            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Failed to insert shop: " + e.getMessage());
        }
    }

    /**
     * Retrieves a shop by UUID.
     */
    public Shop getShop(UUID uuid) {
        final String sql = "SELECT * FROM playershops64_shops WHERE uuid = ?";
        try (Connection conn = mySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToShop(rs);
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Failed to fetch shop: " + e.getMessage());
        }
        return null;
    }

    /**
     * Updates an existing shop.
     */
    public void updateShop(Shop shop) {
        final String sql = "UPDATE playershops64_shops SET "
            + "owner_uuid=?, owner_name=?, world=?, location=?, itemstack_b64=?, stack_size=?, "
            + "item_stock=?, money_stock=?, buy_price=?, sell_price=?, last_tx=? "
            + "WHERE uuid=?";

        try (Connection conn = mySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, shop.getOwnerUuid().toString());
            ps.setString(2, shop.getOwnerName());
            bindWorldStrOrNull(ps, 3, shop.getWorld());
            ps.setString(4, serializeLocation(shop.getLocation()));
            ps.setString(5, ItemSerializer.itemStackToBase64(shop.getItemStack()));
            ps.setInt(6, shop.getStackSize());
            ps.setInt(7, shop.getItemStock());
            ps.setBigDecimal(8, shop.getMoneyStock());
            bindBigDecimalOrNull(ps, 9, shop.getBuyPrice());
            bindBigDecimalOrNull(ps, 10, shop.getSellPrice());
            bindTimestampOrNull(ps, 11, shop.getLastTransactionDate());
            ps.setString(12, shop.getUuid().toString());

            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Failed to update shop: " + e.getMessage());
        }
    }

    /**
     * Deletes a shop by UUID.
     */
    public void deleteShop(UUID uuid) {
        final String sql = "DELETE FROM playershops64_shops WHERE uuid=?";
        try (Connection conn = mySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Failed to delete shop: " + e.getMessage());
        }
    }

    /**
     * Maps a SQL row to a Shop object.
     */
    private Shop mapResultSetToShop(ResultSet rs) throws SQLException {
        UUID uuid = UUID.fromString(rs.getString("uuid"));
        UUID ownerUuid = UUID.fromString(rs.getString("owner_uuid"));
        String ownerName = rs.getString("owner_name");
        String worldName = rs.getString("world");
        World world = (worldName == null ? null : Bukkit.getWorld(worldName));
        Location location = deserializeLocation(world, rs.getString("location"));
        ItemStack itemStack = ItemSerializer.itemStackFromBase64(rs.getString("itemstack_b64"));
        int stackSize = rs.getInt("stack_size");
        int itemStock = rs.getInt("item_stock");
        BigDecimal moneyStock = rs.getBigDecimal("money_stock");
        if (moneyStock == null) moneyStock = BigDecimal.ZERO;
        BigDecimal buyPrice = rs.getBigDecimal("buy_price");
        BigDecimal sellPrice = rs.getBigDecimal("sell_price");
        java.util.Date lastTx = toJavaDate(rs.getTimestamp("last_tx"));

        return new Shop(uuid, ownerUuid, ownerName, world, location, itemStack, stackSize, itemStock, moneyStock, buyPrice, sellPrice, lastTx);
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

    private java.util.Date toJavaDate(java.sql.Timestamp d) {
        return d != null ? new java.util.Date(d.getTime()) : null;
    }
    /*
    private java.sql.Timestamp toSqlTimestamp(java.util.Date d) {
        return d != null ? new java.sql.Timestamp(d.getTime()) : null;
    }
    */

    private void bindTimestampOrNull(PreparedStatement ps, int idx, java.util.Date d) throws SQLException {
        if (d == null) ps.setNull(idx, Types.TIMESTAMP); else ps.setTimestamp(idx, new Timestamp(d.getTime()));
    }
    private void bindWorldStrOrNull(PreparedStatement ps, int idx, World w) throws SQLException {
        if (w == null) ps.setNull(idx, Types.VARCHAR); else ps.setString(idx, w.getName());
    }
    private void bindBigDecimalOrNull(PreparedStatement ps, int idx, BigDecimal v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.DECIMAL); else ps.setBigDecimal(idx, v);
    }
}