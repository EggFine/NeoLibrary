package com.blbilink.neoLibrary;

import com.blbilink.neoLibrary.utils.DatabaseUtil;
import com.blbilink.neoLibrary.utils.FileUtil;
import com.blbilink.neoLibrary.utils.I18n;
import com.blbilink.neoLibrary.utils.UpdateUtil;
import com.blbilink.neoLibrary.utils.VersionUtil;
import com.blbilink.neoLibrary.utils.YmlUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
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
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class NeoLibraryCoreTest {

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

        // 标准 SemVer 大版本（如 2.0.0）不应被误判为 Minecraft 版本而过滤掉
        assertEquals("2.0.0", UpdateUtil.selectLatestCompatibleVersion(List.of("v2.0.0", "1.0.0"), "1.21.8"));
        // 显式带 (MC: x.y.z) 标签的版本应按服务端 MC 版本正确筛选
        assertEquals("1.1.0", UpdateUtil.selectLatestCompatibleVersion(
                List.of("1.2.0 (MC: 1.22)", "1.1.0 (MC: 1.21)", "1.0.0"),
                "1.21.4"
        ));
    }

    @Test
    void testLanguageFilesCamelCaseAndNonDestructiveCompletion(@TempDir Path tempDir) throws Exception {
        try (InputStream cfgIn = getClass().getClassLoader().getResourceAsStream("config.yml")) {
            assertNotNull(cfgIn);
            YamlConfiguration cfgYml = YamlConfiguration.loadConfiguration(new InputStreamReader(cfgIn, StandardCharsets.UTF_8));
            assertEquals("1.0.0", cfgYml.getString("version"));
        }

        for (String locale : new String[]{"zh_CN", "en_US"}) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("languages/" + locale + ".yml")) {
                assertNotNull(in, "Missing language file: " + locale);
                YamlConfiguration yml = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
                assertEquals("1.0.0", yml.getString("version"));
                for (String key : yml.getKeys(true)) {
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

        // 验证语言文件升级时保留服主自定义翻译，仅补全缺失键并推进版本号
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
        assertEquals("我的自定义中文名", updatedZh.getString("languageName"), "Custom translation must be preserved");
        assertNotNull(updatedZh.getString("updateChecker.found"), "Missing keys must be completed");

        I18n i18n = new I18n(plugin, "&8[&fNeo&bLib&8] &f", "zh_CN");
        i18n.loadLanguage();

        String foundMsg = i18n.as("updateChecker.found", true, "1.1.0", "1.0.0");
        assertTrue(foundMsg.startsWith("§8[§fNeo§bLib§8] §f"));
        assertTrue(foundMsg.contains("1.1.0") && foundMsg.contains("1.0.0"));
        assertEquals(
                i18n.as("updateChecker.found", false, "2.0", "1.0"),
                i18n.as("UpdateChecker.Found", false, "2.0", "1.0")
        );
    }

    @Test
    void testGetMessageOrDefaultFallbackWhenDownstreamPluginOmitsLibraryKeys(@TempDir Path tempDir) throws Exception {
        File langDir = new File(tempDir.toFile(), "languages");
        assertTrue(langDir.mkdirs());
        Files.writeString(new File(langDir, "zh_CN.yml").toPath(), """
                version: "1.0.0"
                languageName: "简体中文"
                loadedLanguage: "已加载: %s"
                customPlugin:
                  hello: "你好 %s"
                """);

        Plugin plugin = createStubPlugin(tempDir.toFile());
        I18n downstreamI18n = new I18n(plugin, "[Test] ", "zh_CN");
        downstreamI18n.loadLanguage();

        String fallbackMsg = I18n.getMessageOrDefault(downstreamI18n, "configUtil.localVersion", "1.0.0", "1.1.0");
        assertFalse(fallbackMsg.contains("Missing key"), "Must fall back to DEFAULT_MESSAGES instead of Missing key");
        assertTrue(fallbackMsg.contains("1.0.0") && fallbackMsg.contains("1.1.0"));
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
            assertFalse(mariaUrl.contains("useSSL="), "MariaDB 3.x should use sslMode instead of removed useSSL");

            YamlConfiguration pgCfg = new YamlConfiguration();
            pgCfg.set("database.type", "POSTGRESQL");
            pgCfg.set("database.database", "neodb");
            pgCfg.set("database.useSsl", true);
            DatabaseUtil.DatabaseConfig parsedPg = dbUtil.parseConfig(pgCfg.getConfigurationSection("database"));
            String pgUrl = dbUtil.buildJdbcUrl(parsedPg);
            assertTrue(pgUrl.startsWith("jdbc:postgresql://localhost:5432/neodb?"));
            assertTrue(pgUrl.contains("ssl=true") && pgUrl.contains("sslmode=require"));
            assertFalse(pgUrl.contains("useUnicode"), "PostgreSQL JDBC must not receive MySQL useUnicode params");
        } finally {
            dbUtil.close();
        }
    }
}
