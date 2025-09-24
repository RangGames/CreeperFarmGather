package wiki.creeper.farmGather;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import wiki.creeper.farmGather.api.FarmGatherAPI;
import wiki.creeper.farmGather.command.FarmGatherCommand;
import wiki.creeper.farmGather.config.ConfigManager;
import wiki.creeper.farmGather.config.PluginConfig;
import wiki.creeper.farmGather.harvest.HarvestListener;
import wiki.creeper.farmGather.harvest.HarvestManager;
import wiki.creeper.farmGather.harvest.HarvestableRegistry;
import wiki.creeper.farmGather.integration.GuildService;
import wiki.creeper.farmGather.integration.NoGuildService;
import wiki.creeper.farmGather.item.ItemIdentityService;
import wiki.creeper.farmGather.player.PlayerConnectionListener;
import wiki.creeper.farmGather.player.ProfileManager;
import wiki.creeper.farmGather.progression.ComboService;
import wiki.creeper.farmGather.progression.ProgressionService;
import wiki.creeper.farmGather.skills.SkillManager;
import wiki.creeper.farmGather.storage.InMemoryPlayerDataStore;
import wiki.creeper.farmGather.storage.JdbcPlayerDataStore;
import wiki.creeper.farmGather.storage.HybridPlayerDataStore;
import wiki.creeper.farmGather.storage.PlayerDataStore;
import wiki.creeper.farmGather.storage.RedisPlayerDataStore;
import wiki.creeper.farmGather.ui.ComboBossBarService;
import wiki.creeper.farmGather.ui.CooldownUiService;
import wiki.creeper.farmGather.world.WorldResetService;
import wiki.creeper.farmGather.world.WorldRuleListener;

public final class FarmGather extends JavaPlugin {

    private ConfigManager configManager;
    private PlayerDataStore playerDataStore;
    private ProfileManager profileManager;
    private ComboService comboService;
    private ProgressionService progressionService;
    private HarvestableRegistry harvestableRegistry;
    private GuildService guildService;
    private HarvestManager harvestManager;
    private SkillManager skillManager;
    private WorldRuleListener worldRuleListener;
    private ComboBossBarService comboBossBarService;
    private WorldResetService worldResetService;
    private ItemIdentityService itemIdentityService;
    private CooldownUiService cooldownUiService;

    @Override
    public void onEnable() {
        FarmGatherAPI.init(this);
        setupConfig();
        setupStorage();
        setupManagers();
        registerListeners();
        registerCommands();

        profileManager.loadOnlinePlayers();
        getServer().getOnlinePlayers().forEach(player -> itemIdentityService.ensureInventoryTagged(player));
        getLogger().info("FarmGather enabled");
    }

    @Override
    public void onDisable() {
        if (profileManager != null) {
            profileManager.flushAllSync();
        }
        if (skillManager != null) {
            skillManager.stop();
        }
        if (comboBossBarService != null) {
            comboBossBarService.stop();
        }
        if (cooldownUiService != null) {
            cooldownUiService.stop();
        }
        if (playerDataStore != null) {
            playerDataStore.close().join();
        }
        FarmGatherAPI.shutdown();
        getLogger().info("FarmGather disabled");
    }

    private void setupConfig() {
        this.configManager = new ConfigManager(this);
        this.configManager.load();
    }

    private void setupStorage() {
        PluginConfig.StorageConfig storageConfig = configManager.getConfig().storage();
        this.playerDataStore = switch (storageConfig.type()) {
            case MYSQL -> createMysqlStore(storageConfig.mysql());
            case REDIS -> createRedisStore(storageConfig.redis());
            case HYBRID -> createHybridStore(storageConfig.mysql(), storageConfig.redis());
            case MEMORY -> new InMemoryPlayerDataStore();
        };
    }

    private PlayerDataStore createMysqlStore(PluginConfig.StorageConfig.MysqlConfig config) {
        JdbcPlayerDataStore store = tryCreateMysqlStore(config);
        if (store == null) {
            getLogger().warning("MySQL storage initialisation failed; using in-memory storage instead.");
            return new InMemoryPlayerDataStore();
        }
        return store;
    }

    private PlayerDataStore createRedisStore(PluginConfig.StorageConfig.RedisConfig config) {
        RedisPlayerDataStore store = tryCreateRedisStore(config);
        if (store == null) {
            getLogger().warning("Redis storage initialisation failed; using in-memory storage instead.");
            return new InMemoryPlayerDataStore();
        }
        return store;
    }

    private PlayerDataStore createHybridStore(PluginConfig.StorageConfig.MysqlConfig mysqlConfig,
                                              PluginConfig.StorageConfig.RedisConfig redisConfig) {
        JdbcPlayerDataStore mysqlStore = tryCreateMysqlStore(mysqlConfig);
        RedisPlayerDataStore redisStore = tryCreateRedisStore(redisConfig);
        if (mysqlStore == null || redisStore == null) {
            getLogger().warning("Hybrid storage initialisation failed; using in-memory storage instead.");
            if (mysqlStore != null) {
                mysqlStore.close().join();
            }
            if (redisStore != null) {
                redisStore.close().join();
            }
            return new InMemoryPlayerDataStore();
        }
        return new HybridPlayerDataStore(mysqlStore, redisStore, getLogger());
    }

    private JdbcPlayerDataStore tryCreateMysqlStore(PluginConfig.StorageConfig.MysqlConfig config) {
        if (config == null) {
            getLogger().warning("MySQL configuration missing; cannot initialise FarmGather data store.");
            return null;
        }
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {
            getLogger().warning("MySQL driver not found; falling back to in-memory storage.");
            return null;
        }

        StringBuilder urlBuilder = new StringBuilder()
                .append("jdbc:mysql://")
                .append(config.host())
                .append(':')
                .append(config.port())
                .append('/')
                .append(config.database());
        if (config.params() != null && !config.params().isBlank()) {
            urlBuilder.append('?').append(config.params());
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(urlBuilder.toString());
        hikariConfig.setUsername(config.username());
        hikariConfig.setPassword(config.password());
        hikariConfig.setMaximumPoolSize(config.pool().maximumPoolSize());
        hikariConfig.setMinimumIdle(config.pool().minimumIdle());
        hikariConfig.setConnectionTimeout(config.pool().connectionTimeoutMs());
        hikariConfig.setIdleTimeout(config.pool().idleTimeoutMs());
        hikariConfig.setPoolName("FarmGather-MySQL");

        try {
            HikariDataSource dataSource = new HikariDataSource(hikariConfig);
            return new JdbcPlayerDataStore(JdbcPlayerDataStore.Dialect.MYSQL, dataSource::getConnection, dataSource::close);
        } catch (Exception ex) {
            getLogger().log(java.util.logging.Level.WARNING, "Failed to initialise MySQL pool", ex);
            return null;
        }
    }

    private RedisPlayerDataStore tryCreateRedisStore(PluginConfig.StorageConfig.RedisConfig config) {
        if (config == null) {
            getLogger().warning("Redis configuration missing; cannot initialise FarmGather data store.");
            return null;
        }

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(16);
        poolConfig.setMinIdle(1);
        poolConfig.setMaxIdle(8);

        DefaultJedisClientConfig.Builder builder = DefaultJedisClientConfig.builder()
                .database(config.database())
                .ssl(config.ssl());
        if (config.password() != null && !config.password().isBlank()) {
            builder.password(config.password());
        }
        if (config.timeoutMs() > 0) {
            builder.connectionTimeoutMillis(config.timeoutMs());
        }
        DefaultJedisClientConfig clientConfig = builder.build();

        try {
            HostAndPort hostAndPort = new HostAndPort(config.host(), config.port());
            JedisPool pool = new JedisPool(poolConfig, hostAndPort, clientConfig);
            return new RedisPlayerDataStore(pool);
        } catch (Exception ex) {
            getLogger().log(java.util.logging.Level.WARNING, "Failed to initialise Redis pool", ex);
            return null;
        }
    }

    private void setupManagers() {
        this.profileManager = new ProfileManager(playerDataStore);
        PluginConfig pluginConfig = configManager.getConfig();
        this.comboService = new ComboService(pluginConfig.combo());
        this.progressionService = new ProgressionService(pluginConfig.progression());
        this.harvestableRegistry = new HarvestableRegistry(this);
        this.guildService = new NoGuildService();
        this.comboBossBarService = new ComboBossBarService(this, progressionService);
        this.comboBossBarService.reload(pluginConfig);
        this.comboBossBarService.start();
        this.harvestManager = new HarvestManager(this, profileManager, comboService, progressionService, harvestableRegistry, guildService, comboBossBarService);
        this.skillManager = new SkillManager(this, profileManager, pluginConfig.skills());
        this.skillManager.start();
        this.worldRuleListener = new WorldRuleListener(this);
        this.worldResetService = new WorldResetService(this, pluginConfig.world());
        this.itemIdentityService = new ItemIdentityService(this, pluginConfig.itemIdentity());
        this.cooldownUiService = new CooldownUiService(this, skillManager, pluginConfig.cooldownUi());
        this.cooldownUiService.start();
    }

    private void registerListeners() {
        registerListener(new PlayerConnectionListener(this, profileManager, comboBossBarService));
        registerListener(new HarvestListener(harvestManager));
        registerListener(skillManager);
        registerListener(worldRuleListener);
        registerListener(itemIdentityService);
        registerListener(cooldownUiService);
    }

    private void registerCommands() {
        var command = getCommand("fg");
        if (command != null) {
            var executor = new FarmGatherCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        } else {
            getLogger().warning("Command /fg is missing from plugin.yml");
        }
    }

    public PluginConfig getPluginConfig() {
        return configManager.getConfig();
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public HarvestManager getHarvestManager() {
        return harvestManager;
    }

    public SkillManager getSkillManager() {
        return skillManager;
    }

    public void reloadPlugin() {
        configManager.reload();
        PluginConfig pluginConfig = configManager.getConfig();
        comboService.reload(pluginConfig.combo());
        progressionService.reload(pluginConfig.progression());
        harvestManager.reload(pluginConfig);
        skillManager.reload(pluginConfig.skills());
        worldRuleListener.reload(pluginConfig);
        comboBossBarService.reload(pluginConfig);
        worldResetService.reload(pluginConfig.world());
        itemIdentityService.reload(pluginConfig.itemIdentity());
        getServer().getOnlinePlayers().forEach(player -> itemIdentityService.ensureInventoryTagged(player));
        cooldownUiService.reload(pluginConfig.cooldownUi());
    }

    private void registerListener(Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, this);
    }

    public ProgressionService getProgressionService() {
        return progressionService;
    }

    public ComboService getComboService() {
        return comboService;
    }

    public ComboBossBarService getComboBossBarService() {
        return comboBossBarService;
    }

    public WorldResetService getWorldResetService() {
        return worldResetService;
    }

    public ItemIdentityService getItemIdentityService() {
        return itemIdentityService;
    }

    public CooldownUiService getCooldownUiService() {
        return cooldownUiService;
    }
}
