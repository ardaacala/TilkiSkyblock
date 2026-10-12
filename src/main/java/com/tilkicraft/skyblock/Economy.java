package com.tilkicraft.skyblock;

import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

final class Economy {
    private final TilkiSkyblock plugin;
    Economy(TilkiSkyblock plugin) { this.plugin = plugin; }

    private Object provider() {
        try {
            Plugin vault = Bukkit.getPluginManager().getPlugin("Vault");
            if (vault == null || !vault.isEnabled()) return null;
            Class<?> type = vault.getClass().getClassLoader().loadClass("net.milkbowl.vault.economy.Economy");
            @SuppressWarnings({"rawtypes", "unchecked"}) RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration((Class) type);
            return registration == null ? null : registration.getProvider();
        } catch (ReflectiveOperationException e) { plugin.getLogger().warning("Vault ekonomi bağlantısı kurulamadı: " + e.getMessage()); return null; }
    }

    boolean available() { return provider() != null; }

    boolean withdraw(OfflinePlayer player, double amount) {
        Object service = provider();
        if (service == null || amount < 0) return false;
        try {
            Method has = service.getClass().getMethod("has", OfflinePlayer.class, double.class);
            if (!Boolean.TRUE.equals(has.invoke(service, player, amount))) return false;
            Object result = service.getClass().getMethod("withdrawPlayer", OfflinePlayer.class, double.class).invoke(service, player, amount);
            return Boolean.TRUE.equals(result.getClass().getMethod("transactionSuccess").invoke(result));
        } catch (ReflectiveOperationException e) { plugin.getLogger().warning("Ekonomi ödeme hatası: " + e.getMessage()); return false; }
    }

    boolean deposit(OfflinePlayer player, double amount) {
        Object service = provider();
        if (service == null || amount <= 0) return false;
        try {
            Object result = service.getClass().getMethod("depositPlayer", OfflinePlayer.class, double.class).invoke(service, player, amount);
            return Boolean.TRUE.equals(result.getClass().getMethod("transactionSuccess").invoke(result));
        } catch (ReflectiveOperationException e) { plugin.getLogger().warning("Ekonomi yatırma hatası: " + e.getMessage()); return false; }
    }
}
