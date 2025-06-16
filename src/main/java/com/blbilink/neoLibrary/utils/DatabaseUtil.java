package com.blbilink.neoLibrary.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * @author EggFine
 * @version 1.0.0
 */
public class DatabaseUtil {

    private final Plugin plugin;
    private final ExecutorService databaseExecutor;
    private HikariDataSource dataSource;
    private boolean initialized = false;

    // 默认配置常量
    private static final int DEFAULT_MAX_POOL_SIZE = 10;
    private static final int DEFAULT_MIN_IDLE = 2;
    private static final long DEFAULT_CONNECTION_TIMEOUT = 30000; // 30秒
    private static final long DEFAULT_IDLE_TIMEOUT = 600000; // 10分钟
    private static final long DEFAULT_MAX_LIFETIME = 1800000; // 30分钟

    public enum DatabaseType {
        SQLITE("org.sqlite.JDBC", "jdbc:sqlite:"),
        MYSQL("com.mysql.cj.jdbc.Driver", "jdbc:mysql://"),
        POSTGRESQL("org.postgresql.Driver", "jdbc:postgresql://"),
        MARIADB("org.mariadb.jdbc.Driver", "jdbc:mariadb://");

        private final String driverClass;
        private final String urlPrefix;

        DatabaseType(String driverClass, String urlPrefix) {
            this.driverClass = driverClass;
            this.urlPrefix = urlPrefix;
        }

        public String getDriverClass() { return driverClass; }
        public String getUrlPrefix() { return urlPrefix; }
    }

    public static class DatabaseConfig {
        private DatabaseType type;
        private String host = "localhost";
        private int port = 3306;
        private String database;
        private String username;
        private String password;
        private String filePath;

        private int maxPoolSize = DEFAULT_MAX_POOL_SIZE;
        private int minIdle = DEFAULT_MIN_IDLE;
        private long connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
        private long idleTimeout = DEFAULT_IDLE_TIMEOUT;
        private long maxLifetime = DEFAULT_MAX_LIFETIME;
        
        private boolean useSSL = false;
        private boolean allowPublicKeyRetrieval = false;

        private DatabaseConfig() {}

        public static DatabaseConfig create() { return new DatabaseConfig(); }

        public DatabaseConfig type(DatabaseType type) { this.type = type; return this; }
        public DatabaseConfig host(String host) { this.host = host; return this; }
        public DatabaseConfig port(int port) { this.port = port; return this; }
        public DatabaseConfig database(String database) { this.database = database; return this; }
        public DatabaseConfig username(String username) { this.username = username; return this; }
        public DatabaseConfig password(String password) { this.password = password; return this; }
        public DatabaseConfig filePath(String filePath) { this.filePath = filePath; return this; }
        public DatabaseConfig maxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; return this; }
        public DatabaseConfig minIdle(int minIdle) { this.minIdle = minIdle; return this; }
        public DatabaseConfig connectionTimeout(long connectionTimeout) { this.connectionTimeout = connectionTimeout; return this; }
        public DatabaseConfig idleTimeout(long idleTimeout) { this.idleTimeout = idleTimeout; return this; }
        public DatabaseConfig maxLifetime(long maxLifetime) { this.maxLifetime = maxLifetime; return this; }
        public DatabaseConfig useSSL(boolean useSSL) { this.useSSL = useSSL; return this; }
        public DatabaseConfig allowPublicKeyRetrieval(boolean allowPublicKeyRetrieval) { this.allowPublicKeyRetrieval = allowPublicKeyRetrieval; return this; }
        
        // ... (所有 getter 方法保持不变)
        public DatabaseType getType() { return type; }
        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getDatabase() { return database; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getFilePath() { return filePath; }
        public int getMaxPoolSize() { return maxPoolSize; }
        public int getMinIdle() { return minIdle; }
        public long getConnectionTimeout() { return connectionTimeout; }
        public long getIdleTimeout() { return idleTimeout; }
        public long getMaxLifetime() { return maxLifetime; }
        public boolean isUseSSL() { return useSSL; }
        public boolean isAllowPublicKeyRetrieval() { return allowPublicKeyRetrieval; }
    }
    
    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public DatabaseUtil(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin instance cannot be null");
        // 创建一个固定大小的线程池专门用于数据库操作
        this.databaseExecutor = Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors()),
                (r) -> {
                    Thread t = new Thread(r, plugin.getName() + "-Database-Worker");
                    t.setDaemon(true);
                    return t;
                }
        );
    }

    /**
     * 从配置文件初始化数据库连接。
     * @param configSection 数据库配置段
     * @return 初始化成功返回 true
     */
    public boolean initialize(ConfigurationSection configSection) {
        try {
            DatabaseConfig config = loadConfigFromSection(configSection);
            return initialize(config);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to parse database configuration", e);
            return false;
        }
    }
    
    /**
     * 使用配置对象初始化数据库连接。
     * @param dbConfig 数据库配置
     * @return 初始化成功返回 true
     */
    public boolean initialize(DatabaseConfig dbConfig) {
        if (initialized) {
            plugin.getLogger().warning("Database is already initialized!");
            return true;
        }

        try {
            // 检查驱动是否存在
            Class.forName(dbConfig.getType().getDriverClass());

            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setPoolName(plugin.getName() + "-DB-Pool");
            
            // 设置 JDBC URL
            hikariConfig.setJdbcUrl(buildJdbcUrl(dbConfig));

            // 设置凭据 (如果不是SQLite)
            if (dbConfig.getType() != DatabaseType.SQLITE) {
                hikariConfig.setUsername(dbConfig.getUsername());
                hikariConfig.setPassword(dbConfig.getPassword());
            }

            // 应用连接池配置
            hikariConfig.setMaximumPoolSize(dbConfig.getMaxPoolSize());
            hikariConfig.setMinimumIdle(dbConfig.getMinIdle());
            hikariConfig.setConnectionTimeout(dbConfig.getConnectionTimeout());
            hikariConfig.setIdleTimeout(dbConfig.getIdleTimeout());
            hikariConfig.setMaxLifetime(dbConfig.getMaxLifetime());

            // 针对不同数据库的推荐优化参数
            switch (dbConfig.getType()) {
                case MYSQL:
                case MARIADB:
                    hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
                    hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
                    hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                    hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
                    hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
                    hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
                    hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
                    hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
                    hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
                    hikariConfig.addDataSourceProperty("maintainTimeStats", "false");
                    break;
                case POSTGRESQL:
                    hikariConfig.addDataSourceProperty("prepareThreshold", "5");
                    hikariConfig.addDataSourceProperty("preparedStatementCacheQueries", "256");
                    hikariConfig.addDataSourceProperty("preparedStatementCacheSizeMiB", "5");
                    hikariConfig.addDataSourceProperty("databaseMetadataCacheFields", "65536");
                    hikariConfig.addDataSourceProperty("databaseMetadataCacheFieldsMiB", "5");
                    break;
            }

            this.dataSource = new HikariDataSource(hikariConfig);
            this.initialized = true;
            plugin.getLogger().info("Database connection pool initialized successfully! Type: " + dbConfig.getType().name());
            
            return true;
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("Database driver not found: " + dbConfig.getType().getDriverClass());
            plugin.getLogger().severe("Please add the required database driver to your server's lib folder or shade it into your plugin.");
            return false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database connection pool", e);
            close();
            return false;
        }
    }

    private DatabaseConfig loadConfigFromSection(ConfigurationSection section) {
        DatabaseConfig config = DatabaseConfig.create();
     
        String typeStr = section.getString("type", "SQLITE").toUpperCase();
        try {
            config.type(DatabaseType.valueOf(typeStr));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid database type '" + typeStr + "', using SQLITE as default");
            config.type(DatabaseType.SQLITE);
        }
        
        config.host(section.getString("host", "localhost"))
              .port(section.getInt("port", getDefaultPort(config.getType())))
              .database(section.getString("database", ""))
              .username(section.getString("username", ""))
              .password(section.getString("password", ""));

        if (config.getType() == DatabaseType.SQLITE) {
            String filePath = section.getString("file", "database.db");
            if (!filePath.startsWith("/") && !filePath.contains(":")) {
                filePath = new File(plugin.getDataFolder(), filePath).getAbsolutePath();
            }
            config.filePath(filePath);
        }

        ConfigurationSection poolSection = section.getConfigurationSection("pool");
        if (poolSection != null) {
            config.maxPoolSize(poolSection.getInt("max-size", DEFAULT_MAX_POOL_SIZE))
                  .minIdle(poolSection.getInt("min-idle", DEFAULT_MIN_IDLE))
                  .connectionTimeout(poolSection.getLong("connection-timeout", DEFAULT_CONNECTION_TIMEOUT))
                  .idleTimeout(poolSection.getLong("idle-timeout", DEFAULT_IDLE_TIMEOUT))
                  .maxLifetime(poolSection.getLong("max-lifetime", DEFAULT_MAX_LIFETIME));
        }
        
        config.useSSL(section.getBoolean("use-ssl", false))
              .allowPublicKeyRetrieval(section.getBoolean("allow-public-key-retrieval", false));
        
        return config;
    }

    private int getDefaultPort(DatabaseType type) {
        switch (type) {
            case MYSQL:
            case MARIADB:
                return 3306;
            case POSTGRESQL:
                return 5432;
            default:
                return 0;
        }
    }

    private String buildJdbcUrl(DatabaseConfig config) {
        if (config.getType() == DatabaseType.SQLITE) {
            return config.getType().getUrlPrefix() + config.getFilePath();
        }
        
        StringBuilder url = new StringBuilder(config.getType().getUrlPrefix());
        url.append(config.getHost()).append(":").append(config.getPort()).append("/").append(config.getDatabase());
        
        List<String> params = new ArrayList<>();
        params.add("useUnicode=true");
        params.add("characterEncoding=utf8");
        
        if (config.getType() == DatabaseType.MYSQL || config.getType() == DatabaseType.MARIADB) {
            params.add("useSSL=" + config.isUseSSL());
            params.add("allowPublicKeyRetrieval=" + config.isAllowPublicKeyRetrieval());
            params.add("serverTimezone=UTC");
        }
        
        return url.append("?").append(String.join("&", params)).toString();
    }
    
    /**
     * 从连接池获取一个数据库连接。
     * <p><b>重要:</b> 请务必在 try-with-resources 语句中使用此方法来确保连接被正确关闭和归还。
     * <pre>{@code
     * try (Connection conn = databaseUtil.getConnection()) {
     * // ... 执行数据库操作 ...
     * } catch (SQLException e) {
     * // ... 异常处理 ...
     * }
     * }</pre>
     *
     * @return 数据库连接
     * @throws SQLException 如果获取连接失败或数据库未初始化
     */
    public Connection getConnection() throws SQLException {
        if (!initialized || dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Database is not initialized or has been closed.");
        }
        return dataSource.getConnection();
    }

    public <T> CompletableFuture<T> executeQueryAsync(String sql, Function<ResultSet, T> resultMapper, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
                setParameters(stmt, params);
                try (ResultSet rs = stmt.executeQuery()) {
                    return resultMapper.apply(rs);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error executing async query: " + sql, e);
                throw new RuntimeException(e); // 在 CompletableFuture 中重新抛出为未检查异常
            }
        }, databaseExecutor);
    }
    
    public CompletableFuture<Void> executeQueryAsync(String sql, Consumer<ResultSet> resultHandler, Object... params) {
        return CompletableFuture.runAsync(() -> {
             try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
                setParameters(stmt, params);
                try (ResultSet rs = stmt.executeQuery()) {
                    resultHandler.accept(rs);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error executing async query: " + sql, e);
                throw new RuntimeException(e); // 保持与其他异步方法的一致性
            }
        }, databaseExecutor);
    }

    public int executeUpdate(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            setParameters(stmt, params);
            return stmt.executeUpdate();
        }
    }

    public CompletableFuture<Integer> executeUpdateAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeUpdate(sql, params);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error executing async update: " + sql, e);
                throw new RuntimeException(e);
            }
        }, databaseExecutor);
    }

    public boolean executeTransaction(Function<Connection, Boolean> transactionFunction) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (transactionFunction.apply(conn)) {
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
            } catch (Exception e) {
                conn.rollback();
                plugin.getLogger().log(Level.SEVERE, "Transaction failed, rolling back", e);
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get connection for transaction", e);
            return false;
        }
    }
    
    private void setParameters(PreparedStatement stmt, Object[] params) throws SQLException {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
        }
    }
    
    public boolean isInitialized() {
        return initialized && dataSource != null && !dataSource.isClosed();
    }
    
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection pool closed.");
        }
        if (databaseExecutor != null && !databaseExecutor.isShutdown()) {
            databaseExecutor.shutdown();
        }
        initialized = false;
    }
}
