package com.blbilink.neoLibrary.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 套件共享无损物品栏与药水状态二进制编解码器（GZIP + {@link BukkitObjectOutputStream} + Base64）。
 * <p>
 * 与 {@code NeoCore} 及 {@code NeoWorld} 既有数据库列保持 100% 字节级向前/向后兼容，完整保留 1.21+ 物品组件、
 * 自定义模型数据、附魔、盔甲纹饰、潜影盒内容物与 {@code PersistentDataContainer} 标签。
 * </p>
 */
public final class ItemStateCodec {

    private static final int MAX_SERIALIZED_SLOTS = 256;

    private ItemStateCodec() {
    }

    /**
     * 无头单元测试与运行时双重安全的空物品判定。
     */
    public static boolean isNullOrAir(ItemStack item) {
        if (item == null || item.getAmount() <= 0) {
            return true;
        }
        Material type = item.getType();
        if (type == Material.AIR || type == Material.CAVE_AIR || type == Material.VOID_AIR) {
            return true;
        }
        return Bukkit.getServer() != null && type.isAir();
    }

    public static int countNonEmpty(ItemStack[] items) {
        if (items == null) {
            return 0;
        }
        int count = 0;
        for (ItemStack item : items) {
            if (!isNullOrAir(item)) {
                count++;
            }
        }
        return count;
    }

    public static ItemStack[] cloneArray(ItemStack[] source, int length) {
        ItemStack[] copy = new ItemStack[Math.max(0, length)];
        if (source == null) {
            return copy;
        }
        int limit = Math.min(source.length, copy.length);
        for (int i = 0; i < limit; i++) {
            ItemStack item = source[i];
            copy[i] = isNullOrAir(item) ? null : item.clone();
        }
        return copy;
    }

    public static ItemStack[] trimArray(ItemStack[] source, int exactLen) {
        int safeLen = Math.max(0, exactLen);
        if (source == null) {
            return new ItemStack[safeLen];
        }
        if (source.length == safeLen) {
            return source;
        }
        ItemStack[] resized = new ItemStack[safeLen];
        System.arraycopy(source, 0, resized, 0, Math.min(source.length, safeLen));
        return resized;
    }

    /**
     * 将物品数组压缩序列化为 Base64 字符串；若全为空槽位则返回空字符串以节省存储体积。
     */
    public static String encodeItems(ItemStack[] items) {
        if (items == null || items.length == 0 || countNonEmpty(items) == 0) {
            return "";
        }
        int len = Math.min(items.length, MAX_SERIALIZED_SLOTS);
        try {
            ByteArrayOutputStream rawOut = new ByteArrayOutputStream(256);
            try (GZIPOutputStream gzipOut = new GZIPOutputStream(rawOut);
                 BukkitObjectOutputStream objOut = new BukkitObjectOutputStream(gzipOut)) {
                objOut.writeInt(len);
                for (int i = 0; i < len; i++) {
                    ItemStack item = items[i];
                    objOut.writeObject(isNullOrAir(item) ? null : item.clone());
                }
            }
            return Base64.getEncoder().encodeToString(rawOut.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    public static String serializeItems(ItemStack[] items) {
        return encodeItems(items);
    }

    /**
     * 从 Base64 字符串反序列化为指定长度的物品数组；输入为空或损坏时安全返回空槽数组。
     */
    public static ItemStack[] decodeItems(String base64, int expectedLength) {
        int safeLen = Math.max(0, Math.min(expectedLength, MAX_SERIALIZED_SLOTS));
        ItemStack[] result = new ItemStack[safeLen];
        if (base64 == null || base64.isBlank() || safeLen == 0) {
            return result;
        }
        try {
            byte[] compressed = Base64.getDecoder().decode(base64.trim());
            try (ByteArrayInputStream rawIn = new ByteArrayInputStream(compressed);
                 GZIPInputStream gzipIn = new GZIPInputStream(rawIn);
                 BukkitObjectInputStream objIn = new BukkitObjectInputStream(gzipIn)) {
                int storedLen = Math.max(0, Math.min(objIn.readInt(), MAX_SERIALIZED_SLOTS));
                for (int i = 0; i < storedLen; i++) {
                    Object rawObj = objIn.readObject();
                    if (i < safeLen && rawObj instanceof ItemStack stack && !isNullOrAir(stack)) {
                        result[i] = stack;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    public static ItemStack[] deserializeItems(String base64, int expectedLength) {
        return decodeItems(base64, expectedLength);
    }

    public static String encodeCompactList(List<ItemStack> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        List<ItemStack> nonNull = new ArrayList<>();
        for (ItemStack item : items) {
            if (!isNullOrAir(item) && nonNull.size() < MAX_SERIALIZED_SLOTS) {
                nonNull.add(item.clone());
            }
        }
        if (nonNull.isEmpty()) {
            return "";
        }
        return encodeItems(nonNull.toArray(new ItemStack[0]));
    }

    public static String serializeCompactList(List<ItemStack> items) {
        return encodeCompactList(items);
    }

    public static List<ItemStack> decodeCompactList(String base64) {
        List<ItemStack> list = new ArrayList<>();
        if (base64 == null || base64.isBlank()) {
            return list;
        }
        ItemStack[] decoded = decodeItems(base64, MAX_SERIALIZED_SLOTS);
        for (ItemStack stack : decoded) {
            if (!isNullOrAir(stack)) {
                list.add(stack);
            }
        }
        return list;
    }

    public static List<ItemStack> deserializeCompactList(String base64) {
        return decodeCompactList(base64);
    }

    public static String encodeSingle(ItemStack item) {
        if (isNullOrAir(item)) {
            return "";
        }
        return encodeItems(new ItemStack[]{item});
    }

    public static String serializeSingle(ItemStack item) {
        return encodeSingle(item);
    }

    public static ItemStack decodeSingle(String base64) {
        if (base64 == null || base64.isBlank()) {
            return null;
        }
        return decodeItems(base64, 1)[0];
    }

    public static ItemStack deserializeSingle(String base64) {
        return decodeSingle(base64);
    }

    public static String encodePotions(Collection<PotionEffect> effects) {
        if (effects == null || effects.isEmpty()) {
            return "";
        }
        List<String> entries = new ArrayList<>(effects.size());
        for (PotionEffect effect : effects) {
            if (effect == null || effect.getType() == null) {
                continue;
            }
            String key = effect.getType().getKey().toString();
            entries.add(String.format(
                    Locale.ROOT,
                    "%s;%d;%d;%b;%b;%b",
                    key,
                    effect.getDuration(),
                    effect.getAmplifier(),
                    effect.isAmbient(),
                    effect.hasParticles(),
                    effect.hasIcon()
            ));
        }
        return String.join("|", entries);
    }

    public static List<PotionEffect> decodePotions(String raw) {
        List<PotionEffect> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return result;
        }
        for (String token : raw.split("\\|")) {
            String[] parts = token.split(";");
            if (parts.length < 6) {
                continue;
            }
            try {
                NamespacedKey nsKey = NamespacedKey.fromString(parts[0]);
                PotionEffectType type = nsKey != null ? Registry.EFFECT.get(nsKey) : null;
                if (type == null) {
                    continue;
                }
                int duration = Integer.parseInt(parts[1]);
                int amplifier = Integer.parseInt(parts[2]);
                boolean ambient = Boolean.parseBoolean(parts[3]);
                boolean particles = Boolean.parseBoolean(parts[4]);
                boolean icon = Boolean.parseBoolean(parts[5]);
                result.add(new PotionEffect(type, duration, amplifier, ambient, particles, icon));
            } catch (Exception ignored) {
            }
        }
        return result;
    }
}
