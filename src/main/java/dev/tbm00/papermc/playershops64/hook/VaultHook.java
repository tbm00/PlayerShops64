package dev.tbm00.papermc.playershops64.hook;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import dev.tbm00.papermc.playershops64.PlayerShops64;

public class VaultHook {
    public Economy pl;
    
    public VaultHook(PlayerShops64 javaPlugin) {
        RegisteredServiceProvider<Economy> rsp = javaPlugin.getServer().getServicesManager().getRegistration(Economy.class);
        pl = rsp.getProvider();
    }

    public double getBalance(Player player) {
        return pl.getBalance(player);
    } public double getBalance(OfflinePlayer player) {
        return pl.getBalance(player);
    }

    public boolean hasMoney(Player player, double amount) {
        double bal = pl.getBalance(player);
        if (bal>=amount) return true;
        else return false;
    } public boolean hasMoney(OfflinePlayer player, double amount) {
        double bal = pl.getBalance(player);
        if (bal>=amount) return true;
        else return false;
    }

    public boolean removeMoney(Player player, double amount) {
        if (amount <= 0) return true;

        EconomyResponse r = pl.withdrawPlayer(player, amount);
        if (r.transactionSuccess()) return true;
        else return false;
    } public boolean removeMoney(OfflinePlayer player, double amount) {
        if (amount <= 0) return true;

        EconomyResponse r = pl.withdrawPlayer(player, amount);
        if (r.transactionSuccess()) return true;
        else return false;
    }

    public boolean giveMoney(Player player, double amount) {
        if (amount <= 0) return true;

        EconomyResponse r = pl.depositPlayer(player, amount);
        if (r.transactionSuccess()) return true;
        else return false;
    }public boolean giveMoney(OfflinePlayer player, double amount) {
        if (amount <= 0) return true;

        EconomyResponse r = pl.depositPlayer(player, amount);
        if (r.transactionSuccess()) return true;
        else return false;
    }
}