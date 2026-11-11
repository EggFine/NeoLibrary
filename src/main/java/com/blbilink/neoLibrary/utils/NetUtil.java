package com.blbilink.neoLibrary.utils;

import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Locale;

/**
 * 套件共享网络地址解析与规范化工具（统一处理 IPv4/IPv6、代理端口剥离与脱敏展示）。
 */
public final class NetUtil {

    public static final String UNKNOWN_IP = "unknown";

    private NetUtil() {
    }

    /**
     * 安全提取在线玩家的真实连接 IP 字符串（无反向 DNS 阻塞）。
     */
    public static String extractPlayerIp(Player player) {
        if (player == null) {
            return UNKNOWN_IP;
        }
        InetSocketAddress socketAddress = player.getAddress();
        if (socketAddress == null) {
            return UNKNOWN_IP;
        }
        InetAddress inet = socketAddress.getAddress();
        if (inet != null) {
            return normalizeIp(inet.getHostAddress());
        }
        return normalizeIp(socketAddress.getHostString());
    }

    /**
     * 规范化原始 IP 字符串（剥离前导斜杠、IPv6 网卡作用域后缀并转为小写）。
     */
    public static String normalizeIp(String rawIp) {
        if (rawIp == null) {
            return UNKNOWN_IP;
        }
        String ip = rawIp.trim();
        if (ip.startsWith("/")) {
            ip = ip.substring(1);
        }
        if (ip.startsWith("[") && ip.contains("]")) {
            ip = ip.substring(1, ip.indexOf(']'));
        } else {
            int firstColon = ip.indexOf(':');
            int lastColon = ip.lastIndexOf(':');
            if (firstColon > 0 && firstColon == lastColon && ip.contains(".")) {
                ip = ip.substring(0, firstColon);
            }
        }
        int zoneIdx = ip.indexOf('%');
        if (zoneIdx > 0) {
            ip = ip.substring(0, zoneIdx);
        }
        ip = ip.trim();
        if (ip.isEmpty()) {
            return UNKNOWN_IP;
        }
        return ip.toLowerCase(Locale.ROOT);
    }

    /**
     * 对 IP 地址末尾段脱敏（供低权限稽查展示）。
     */
    public static String maskIp(String ip) {
        String clean = normalizeIp(ip);
        if (UNKNOWN_IP.equals(clean)) {
            return clean;
        }
        int lastDot = clean.lastIndexOf('.');
        if (lastDot > 0) {
            return clean.substring(0, lastDot + 1) + "***";
        }
        int lastColon = clean.lastIndexOf(':');
        if (lastColon > 0) {
            return clean.substring(0, lastColon + 1) + "****";
        }
        return "***";
    }
}
