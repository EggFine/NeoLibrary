package com.blbilink.neoLibrary.bridge;

import com.blbilink.neoLibrary.spi.EconomySpi;
import com.blbilink.neoLibrary.spi.NeoSuiteRegistry;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * 套件共享零硬依赖经济桥接器：
 * <ul>
 *   <li>优先挂载标准 {@code Vault} 经济服务提供器；</li>
 *   <li>若未安装 {@code Vault}，直接通过 {@link NeoSuiteRegistry#economy()} 无反射高速调用 {@code NeoCore} 原生账本！</li>
 * </ul>
 */
public class EconomyBridge implements EconomySpi {

    private volatile Object vaultProvider;
    private volatile Method vaultHas;
    private volatile Method vaultWithdraw;
    private volatile Method vaultDeposit;
    private volatile Method vaultFormat;
    private volatile Method vaultRespSuccess;

    public EconomyBridge() {
        refreshHook();
    }

    public synchronized boolean refreshHook() {
        this.vaultProvider = null;
        if (Bukkit.getServer() != null && Bukkit.getPluginManager().getPlugin("Vault") != null) {
            try {
                Class<?> ecoClass = Class.forName("net.milkbowl.vault.economy.Economy");
                RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration(ecoClass);
                if (rsp != null && rsp.getProvider() != null) {
                    this.vaultHas = ecoClass.getMethod("has", OfflinePlayer.class, double.class);
                    this.vaultWithdraw = ecoClass.getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
                    this.vaultDeposit = ecoClass.getMethod("depositPlayer", OfflinePlayer.class, double.class);
                    this.vaultFormat = ecoClass.getMethod("format", double.class);
                    Class<?> respClass = Class.forName("net.milkbowl.vault.economy.EconomyResponse");
                    this.vaultRespSuccess = respClass.getMethod("transactionSuccess");
                    this.vaultProvider = rsp.getProvider();
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        return NeoSuiteRegistry.economy().isAvailable();
    }

    @Override
    public boolean isAvailable() {
        if (vaultProvider != null) {
            return true;
        }
        if (NeoSuiteRegistry.economy().isAvailable()) {
            return true;
        }
        return refreshHook();
    }

    @Override
    public boolean hasBalance(OfflinePlayer player, double amount) {
        if (amount <= 0.0d) {
            return true;
        }
        if (player == null || !Double.isFinite(amount)) {
            return false;
        }
        if (vaultProvider != null) {
            try {
                return Boolean.TRUE.equals(vaultHas.invoke(vaultProvider, player, amount));
            } catch (Throwable ignored) {
                return false;
            }
        }
        EconomySpi suiteEco = NeoSuiteRegistry.economy();
        if (suiteEco.isAvailable()) {
            return suiteEco.hasBalance(player, amount);
        }
        return refreshHook() && vaultProvider != null && hasBalance(player, amount);
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        if (amount <= 0.0d) {
            return true;
        }
        if (player == null || !Double.isFinite(amount)) {
            return false;
        }
        if (vaultProvider != null) {
            try {
                Object resp = vaultWithdraw.invoke(vaultProvider, player, amount);
                return resp != null && Boolean.TRUE.equals(vaultRespSuccess.invoke(resp));
            } catch (Throwable ignored) {
                return false;
            }
        }
        EconomySpi suiteEco = NeoSuiteRegistry.economy();
        if (suiteEco.isAvailable()) {
            return suiteEco.withdraw(player, amount);
        }
        return false;
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        if (amount <= 0.0d) {
            return true;
        }
        if (player == null || !Double.isFinite(amount)) {
            return false;
        }
        if (vaultProvider != null) {
            try {
                Object resp = vaultDeposit.invoke(vaultProvider, player, amount);
                return resp != null && Boolean.TRUE.equals(vaultRespSuccess.invoke(resp));
            } catch (Throwable ignored) {
                return false;
            }
        }
        EconomySpi suiteEco = NeoSuiteRegistry.economy();
        if (suiteEco.isAvailable()) {
            return suiteEco.deposit(player, amount);
        }
        return false;
    }

    @Override
    public String format(double amount) {
        if (vaultProvider != null) {
            try {
                Object res = vaultFormat.invoke(vaultProvider, amount);
                if (res != null) {
                    return res.toString();
                }
            } catch (Throwable ignored) {
            }
        }
        EconomySpi suiteEco = NeoSuiteRegistry.economy();
        if (suiteEco.isAvailable()) {
            return suiteEco.format(amount);
        }
        return String.format(Locale.ROOT, "%.2f", amount);
    }
}
