package dev.tbm00.spigot.playershops64;

import dev.tbm00.spigot.playershops64.data.MySQLConnection;
import dev.tbm00.spigot.playershops64.data.ShopDAO;
import dev.tbm00.spigot.playershops64.hook.VaultHook;

public class ShopHandler {
    private final PlayerShops64 javaPlugin;
    private final ShopDAO dao;
    private final VaultHook economy;

    public ShopHandler(PlayerShops64 javaPlugin, MySQLConnection db, VaultHook economy) {
        this.javaPlugin = javaPlugin;
        this.dao = new ShopDAO(db);
        this.economy = economy;
    }
}
