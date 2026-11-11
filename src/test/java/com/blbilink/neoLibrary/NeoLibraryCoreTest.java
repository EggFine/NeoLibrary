package com.blbilink.neoLibrary;

import com.blbilink.neoLibrary.gui.ConfigCategory;
import com.blbilink.neoLibrary.gui.ConfigEntry;
import com.blbilink.neoLibrary.gui.ConfigGui;
import com.blbilink.neoLibrary.gui.ConfigGuiManager;
import com.blbilink.neoLibrary.utils.DatabaseUtil;
import com.blbilink.neoLibrary.utils.FileUtil;
import com.blbilink.neoLibrary.utils.I18n;
import com.blbilink.neoLibrary.utils.UpdateUtil;
import com.blbilink.neoLibrary.utils.VersionUtil;
import com.blbilink.neoLibrary.utils.YmlUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class NeoLibraryCoreTest {

    @AfterEach
    void tearDown() {
        I18n.setGlobalFallbackForTesting(null);
    }

    private Plugin createStubPlugin(File dataFolder) {
        Logger logger = Logger.getLogger("NeoLibraryTest");
        return (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[]{Plugin.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getDataFolder" -> dataFolder;
                    case "getLogger" -> logger;
                    case "getName" -> "NeoLibraryTest";
                    case "getResource" -> NeoLibraryCoreTest.class.getClassLoader().getResourceAsStream((String) args[0]);
                    case "saveResource" -> {
                        String resourcePath = (String) args[0];
                        boolean replace = (Boolean) args[1];
                        File outFile = new File(dataFolder, resourcePath);
                        if (!outFile.exists() || replace) {
                            outFile.getParentFile().mkdirs();
                            try (InputStream in = NeoLibraryCoreTest.class.getClassLoader().getResourceAsStream(resourcePath)) {
                                assertNotNull(in, "Missing resource: " + resourcePath);
                                Files.copy(in, outFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                        yield null;
                    }
                    default -> null;
                }
        );
    }

    @Test
    void testVersionUtilSemVerSnapshotAndUpdateUtilSelection() {
        assertTrue(VersionUtil.isVersionNewer("1.0.0", "1.0.0-SNAPSHOT"));
        assertTrue(VersionUtil.isVersionNewer("1.0.0.2", "1.0.0.1"));
        assertTrue(VersionUtil.isVersionNewer("v2.0.0", "1.9.9"));
        assertTrue(VersionUtil.isVersionNewer("1.3.0", "1.2"));
        assertFalse(VersionUtil.isVersionNewer("1.2.0", "1.2"));
        assertFalse(VersionUtil.isVersionNewer("1.0.0-SNAPSHOT", "1.0.0"));
        assertEquals(
                VersionUtil.isVersionNewer("2.0.0", "1.9.9"),
                YmlUtil.isVersionNewer("2.0.0", "1.9.9")
        );

        assertEquals("2.0.0", UpdateUtil.selectLatestCompatibleVersion(List.of("v2.0.0", "1.0.0"), "1.21.8"));
        assertEquals("1.1.0", UpdateUtil.selectLatestCompatibleVersion(
                List.of("1.2.0 (MC: 1.22)", "1.1.0 (MC: 1.21)", "1.0.0"),
                "1.21.4"
        ));
    }

    @Test
    void testAllTwelveLanguageFilesSchemaCamelCaseAndCompletion(@TempDir Path tempDir) throws Exception {
        try (InputStream cfgIn = getClass().getClassLoader().getResourceAsStream("config.yml")) {
            assertNotNull(cfgIn);
            YamlConfiguration cfgYml = YamlConfiguration.loadConfiguration(new InputStreamReader(cfgIn, StandardCharsets.UTF_8));
            assertEquals("1.0.0", cfgYml.getString("version"));
            assertEquals("zh_CN", cfgYml.getString("language"));
        }

        Set<String> baselineKeys = null;
        for (String locale : NeoLibrary.SUPPORTED_LANGUAGES) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("languages/" + locale + ".yml")) {
                assertNotNull(in, "Missing NeoLibrary language file: " + locale);
                YamlConfiguration yml = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
                assertEquals("1.0.0", yml.getString("version"), "Version mismatch in " + locale);
                Set<String> keys = yml.getKeys(true);
                if (baselineKeys == null) {
                    baselineKeys = keys;
                } else {
                    assertEquals(baselineKeys, keys, "Key schema mismatch in locale: " + locale);
                }
                for (String key : keys) {
                    assertFalse(key.contains("_"), "Key should be camelCase without underscores: " + key);
                    for (String segment : key.split("\\.")) {
                        assertTrue(Character.isLowerCase(segment.charAt(0)), "Key segment must start lowercase: " + key);
                    }
                    if (yml.isString(key)) {
                        String val = yml.getString(key, "");
                        assertFalse(val.contains("{0}") || val.contains("{1}"),
                                "Language value must use %s instead of {0}: " + locale + ":" + key);
                    }
                }
            }
        }

        File langDir = new File(tempDir.toFile(), "languages");
        assertTrue(langDir.mkdirs());
        File zhFile = new File(langDir, "zh_CN.yml");
        Files.writeString(zhFile.toPath(), """
                version: "0.9.0"
                languageName: "我的自定义中文名"
                """);

        Plugin plugin = createStubPlugin(tempDir.toFile());
        FileUtil.completeLanguageFile(plugin, false, "languages/zh_CN.yml");
        YamlConfiguration updatedZh = YamlConfiguration.loadConfiguration(zhFile);
        assertEquals("1.0.0", updatedZh.getString("version"));
        assertEquals("我的自定义中文名", updatedZh.getString("languageName"));
        assertNotNull(updatedZh.getString("common.noPermission"));
        assertNotNull(updatedZh.getString("gui.previousPage"));
    }

    @Test
    void testGlobalSharedI18nInheritanceForDownstreamPlugins() throws Exception {
        YamlConfiguration globalZh = new YamlConfiguration();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("languages/ja_JP.yml")) {
            assertNotNull(in);
            globalZh.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        I18n globalI18n = new I18n("&8[&fNeo&bLib&8] &f", "ja_JP", globalZh);
        I18n.setGlobalFallbackForTesting(globalI18n);

        // 下游插件仅定义自身业务词条，不重复定义 common.* / gui.* / updateChecker.*
        YamlConfiguration pluginLang = new YamlConfiguration();
        pluginLang.set("login.welcome", "&aようこそ %s!");
        I18n downstream = new I18n("&8[&fNeo&bLogin&8] &f", "ja_JP", pluginLang);

        assertEquals("§8[§fNeo§bLogin§8] §f§aようこそ Steve!", downstream.as("login.welcome", true, "Steve"));
        // 自动继承 NeoLibrary 的 common.* 与 gui.*
        assertTrue(downstream.hasKey("common.noPermission"));
        assertTrue(downstream.as("common.noPermission", true).startsWith("§8[§fNeo§bLogin§8] §f"));
        assertFalse(downstream.as("common.noPermission", false).contains("Missing key"));
        // 兼容旧版 command.noPermission / login.onlyPlayer 自动委托至 common.*
        assertEquals(downstream.as("common.noPermission", false), downstream.as("command.noPermission", false));
        assertEquals(downstream.as("common.onlyPlayer", false), downstream.as("login.onlyPlayer", false));
        assertEquals(downstream.as("gui.previousPage", false), globalI18n.as("gui.previousPage", false));
    }

    @Test
    void testConfigGuiAutoIntrospectionPaginationAndSearch() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("version", "1.0.0");
        cfg.set("prefix", "&8[&fTest&8] ");
        cfg.set("checkUpdate", true);
        cfg.set("database.type", "sqlite");
        cfg.set("database.password", "super_secret_123");
        cfg.set("database.pool.maxSize", 10);
        cfg.set("database.pool.minIdle", 2);
        cfg.set("database.pool.connectionTimeout", 30000);
        cfg.set("database.pool.idleTimeout", 600000);
        cfg.set("notLoggedInPlayerLimit.enabled", true);
        cfg.set("notLoggedInPlayerLimit.type.move", true);
        cfg.set("notLoggedInPlayerLimit.type.chat", true);
        cfg.set("notLoggedInPlayerLimit.type.commandWhitelist", List.of("/login", "/reg"));

        AtomicBoolean saved = new AtomicBoolean(false);
        AtomicBoolean reloaded = new AtomicBoolean(false);

        YamlConfiguration i18nYml = new YamlConfiguration();
        i18nYml.set("guiConfig.item.notLoggedInPlayerLimit.type.move", "&b禁止移动限制");
        I18n pluginI18n = new I18n("[Test] ", "zh_CN", i18nYml);

        ConfigGui gui = ConfigGui.fromFileConfiguration("TestPlugin", cfg, () -> saved.set(true), () -> pluginI18n)
                .customizeEntry("database.type", e -> e.options("sqlite", "mysql", "mariadb", "postgresql"))
                .customizeEntry("database.pool.maxSize", e -> e.range(1, 64).step(1, 5))
                .onReload(() -> reloaded.set(true));

        // 顶层 general + database + notLoggedInPlayerLimit 共 3 个分类（version 已自动跳过）
        assertEquals(3, gui.getCategories().size());
        assertNull(gui.findEntry("version"));

        // 密码字段应自动标记为 masked
        ConfigEntry pwdEntry = gui.findEntry("database.password");
        assertNotNull(pwdEntry);
        assertTrue(pwdEntry.isMasked());
        // 搜索敏感密码真实内容不应泄露匹配
        assertTrue(gui.search("super_secret_123").isEmpty(), "Masked values must not be searchable by secret value");

        // database.pool (>=4 个子键) 应自动生成嵌套子分类
        ConfigCategory dbCat = gui.findCategory("database");
        assertNotNull(dbCat);
        assertEquals(1, dbCat.getSubCategories().size());
        assertEquals("database.pool", dbCat.getSubCategories().get(0).getId());

        // notLoggedInPlayerLimit.type 作为唯一 type 子节应平铺展示
        ConfigCategory limitCat = gui.findCategory("notLoggedInPlayerLimit");
        assertNotNull(limitCat);
        assertTrue(limitCat.getSubCategories().isEmpty());
        assertNotNull(limitCat.findEntry("notLoggedInPlayerLimit.type.move"));

        // 搜索支持本地化中文名称、键名以及子分类递归检索
        assertEquals(1, gui.search("禁止移动").size());
        assertEquals(1, gui.search("maxSize").size());
        assertEquals(ConfigEntry.EntryType.ENUM, gui.findEntry("database.type").getType());
        assertEquals(64.0, gui.findEntry("database.pool.maxSize").clamp(999));
        assertEquals(1.0, gui.findEntry("database.pool.maxSize").clamp(-5));

        // 分页计算验证（每页 36 项）
        assertEquals(1, ConfigGuiManager.calculateTotalPages(0));
        assertEquals(1, ConfigGuiManager.calculateTotalPages(36));
        assertEquals(2, ConfigGuiManager.calculateTotalPages(37));
        assertEquals(3, ConfigGuiManager.calculateTotalPages(73));

        gui.saveAndReload();
        assertTrue(saved.get() && reloaded.get());
    }

    @Test
    void testDatabaseUtilConfigParsingAndDriverSpecificJdbcUrls(@TempDir Path tempDir) {
        Plugin plugin = createStubPlugin(tempDir.toFile());
        DatabaseUtil dbUtil = new DatabaseUtil(plugin);
        try {
            YamlConfiguration mysqlCfg = new YamlConfiguration();
            mysqlCfg.set("database.type", "MYSQL");
            mysqlCfg.set("database.host", "db.example.com");
            mysqlCfg.set("database.port", 3307);
            mysqlCfg.set("database.database", "neologin");
            mysqlCfg.set("database.useSsl", true);
            mysqlCfg.set("database.allowPublicKeyRetrieval", true);
            mysqlCfg.set("database.pool.maxSize", 24);

            DatabaseUtil.DatabaseConfig parsedMysql = dbUtil.parseConfig(mysqlCfg.getConfigurationSection("database"));
            String mysqlUrl = dbUtil.buildJdbcUrl(parsedMysql);
            assertTrue(mysqlUrl.startsWith("jdbc:mysql://db.example.com:3307/neologin?"));
            assertTrue(mysqlUrl.contains("useSSL=true"));
            assertEquals(24, parsedMysql.getMaxPoolSize());

            YamlConfiguration mariaCfg = new YamlConfiguration();
            mariaCfg.set("database.type", "MARIADB");
            mariaCfg.set("database.database", "neodb");
            mariaCfg.set("database.use-ssl", false);
            DatabaseUtil.DatabaseConfig parsedMaria = dbUtil.parseConfig(mariaCfg.getConfigurationSection("database"));
            String mariaUrl = dbUtil.buildJdbcUrl(parsedMaria);
            assertTrue(mariaUrl.startsWith("jdbc:mariadb://localhost:3306/neodb?"));
            assertTrue(mariaUrl.contains("sslMode=disable"));
            assertFalse(mariaUrl.contains("useSSL="));

            YamlConfiguration pgCfg = new YamlConfiguration();
            pgCfg.set("database.type", "POSTGRESQL");
            pgCfg.set("database.database", "neodb");
            pgCfg.set("database.useSsl", true);
            DatabaseUtil.DatabaseConfig parsedPg = dbUtil.parseConfig(pgCfg.getConfigurationSection("database"));
            String pgUrl = dbUtil.buildJdbcUrl(parsedPg);
            assertTrue(pgUrl.startsWith("jdbc:postgresql://localhost:5432/neodb?"));
            assertTrue(pgUrl.contains("ssl=true") && pgUrl.contains("sslmode=require"));
            assertFalse(pgUrl.contains("useUnicode"));
        } finally {
            dbUtil.close();
        }
    }
}
