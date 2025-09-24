package wiki.creeper.farmGather.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {
    private final JavaPlugin plugin;
    private PluginConfig pluginConfig;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.pluginConfig = fromConfig(plugin.getConfig());
    }

    public PluginConfig getConfig() {
        return pluginConfig;
    }

    private PluginConfig fromConfig(FileConfiguration config) {
        PluginConfig.HarvestConfig harvestConfig = parseHarvest(getSection(config, "harvest"));
        PluginConfig.StorageConfig storageConfig = parseStorage(getSection(config, "storage"));
        PluginConfig.ComboConfig comboConfig = parseCombo(getSection(config, "combo"));
        PluginConfig.XpConfig xpConfig = parseXp(getSection(config, "xp"));
        PluginConfig.ExtraDropConfig extraDropConfig = parseExtraDrop(getSection(config, "extra_drop"));
        PluginConfig.SkillsConfig skillsConfig = parseSkills(getSection(config, "skills"));
        PluginConfig.ProgressionConfig progressionConfig = parseProgression(getSection(config, "progression"));
        PluginConfig.WorldConfig worldConfig = parseWorld(getSection(config, "world"));
        PluginConfig.UiConfig uiConfig = parseUi(getSection(config, "ui"));
        PluginConfig.ItemIdentityConfig itemIdentityConfig = parseItemIdentity(getSection(config, "item_identity"));
        PluginConfig.CooldownUiConfig cooldownUiConfig = parseCooldownUi(getSection(config, "cooldown_ui"));
        PluginConfig.NotificationsConfig notificationsConfig = parseNotifications(getSection(config, "notifications"));

        return PluginConfig.builder()
                .harvest(harvestConfig)
                .storage(storageConfig)
                .combo(comboConfig)
                .xp(xpConfig)
                .extraDrop(extraDropConfig)
                .skills(skillsConfig)
                .progression(progressionConfig)
                .world(worldConfig)
                .ui(uiConfig)
                .itemIdentity(itemIdentityConfig)
                .cooldownUi(cooldownUiConfig)
                .notifications(notificationsConfig)
                .build();
    }

    private PluginConfig.HarvestConfig parseHarvest(ConfigurationSection section) {
        List<String> worlds = section.getStringList("worlds");
        List<String> harvestableTags = section.getStringList("harvestable_tags");
        double actionCooldown = section.getDouble("action_cooldown_sec");

        ConfigurationSection dropSection = getSection(section, "drop");
        PluginConfig.HarvestConfig.DropConfig dropConfig = new PluginConfig.HarvestConfig.DropConfig(
                dropSection.getString("mode", "VIRTUAL"),
                dropSection.getDouble("ground_merge_radius"),
                dropSection.getInt("stack_merge_count")
        );

        ConfigurationSection antiMacroSection = getSection(section, "anti_macro");
        PluginConfig.HarvestConfig.AntiMacroConfig antiMacroConfig = new PluginConfig.HarvestConfig.AntiMacroConfig(
                antiMacroSection.getDouble("view_angle_deg"),
                antiMacroSection.getDouble("max_distance"),
                antiMacroSection.getLong("jitter_ms")
        );

        return new PluginConfig.HarvestConfig(worlds, harvestableTags, actionCooldown, dropConfig, antiMacroConfig);
    }

    private PluginConfig.ComboConfig parseCombo(ConfigurationSection section) {
        return new PluginConfig.ComboConfig(
                section.getDouble("base_window_sec"),
                section.getDouble("per_level_decay_rate"),
                section.getDouble("min_window_sec"),
                section.getDouble("xp_bonus_per_stack"),
                section.getDouble("xp_bonus_cap"),
                section.getInt("int_rounding_from_stack"),
                section.getInt("int_rounding_cap")
        );
    }

    private PluginConfig.XpConfig parseXp(ConfigurationSection section) {
        double base = section.getDouble("base_per_harvest");
        ConfigurationSection guildSection = getSection(section, "guild_bonus");
        PluginConfig.XpConfig.GuildBonusConfig guildBonusConfig = new PluginConfig.XpConfig.GuildBonusConfig(
                guildSection.getDouble("base_chance"),
                guildSection.getDouble("per_member_inc"),
                guildSection.getDouble("chance_cap"),
                guildSection.getInt("recent_sec"),
                guildSection.getBoolean("require_same_world"),
                guildSection.getBoolean("require_distance"),
                guildSection.getDouble("distance")
        );
        return new PluginConfig.XpConfig(base, guildBonusConfig);
    }

    private PluginConfig.ExtraDropConfig parseExtraDrop(ConfigurationSection section) {
        return new PluginConfig.ExtraDropConfig(
                section.getDouble("base"),
                section.getDouble("per_combo"),
                section.getDouble("per_level_log2"),
                section.getDouble("hard_cap"),
                section.getBoolean("doubletap_multiplies")
        );
    }

    private PluginConfig.SkillsConfig parseSkills(ConfigurationSection section) {
        ConfigurationSection energySection = getSection(section, "energy");
        PluginConfig.SkillsConfig.EnergyConfig energyConfig = new PluginConfig.SkillsConfig.EnergyConfig(
                energySection.getInt("max"),
                energySection.getDouble("regen_per_sec"),
                energySection.getBoolean("regen_if_afk")
        );

        ConfigurationSection sweepSection = getSection(section, "sweep");
        PluginConfig.SkillsConfig.SweepConfig sweepConfig = new PluginConfig.SkillsConfig.SweepConfig(
                sweepSection.getBoolean("enabled"),
                sweepSection.getString("size"),
                sweepSection.getDouble("cd_base"),
                sweepSection.getDouble("cd_per_level_minus"),
                sweepSection.getDouble("energy_cost"),
                sweepSection.getInt("max_targets"),
                sweepSection.getInt("xp_first"),
                sweepSection.getInt("xp_others")
        );

        ConfigurationSection focusSection = getSection(section, "focus");
        PluginConfig.SkillsConfig.FocusConfig focusConfig = new PluginConfig.SkillsConfig.FocusConfig(
                focusSection.getBoolean("enabled"),
                focusSection.getDouble("duration_base"),
                focusSection.getDouble("duration_per_level"),
                focusSection.getDouble("cooldown"),
                focusSection.getDouble("energy_cost"),
                focusSection.getDouble("min_interval_sec")
        );

        ConfigurationSection shearsSection = getSection(section, "shears");
        PluginConfig.SkillsConfig.ShearsConfig shearsConfig = new PluginConfig.SkillsConfig.ShearsConfig(
                shearsSection.getBoolean("enabled"),
                shearsSection.getString("chain_size"),
                shearsSection.getDouble("cooldown"),
                shearsSection.getDouble("energy_cost_base"),
                shearsSection.getDouble("energy_cost_per_level_minus"),
                shearsSection.getInt("max_chain_targets"),
                shearsSection.getDouble("hop_delay_sec"),
                shearsSection.getInt("xp_first"),
                shearsSection.getInt("xp_others")
        );

        ConfigurationSection doubleTapSection = getSection(section, "doubletap");
        PluginConfig.SkillsConfig.DoubleTapConfig doubleTapConfig = new PluginConfig.SkillsConfig.DoubleTapConfig(
                doubleTapSection.getBoolean("enabled"),
                doubleTapSection.getDouble("window_sec"),
                doubleTapSection.getDouble("chance_base"),
                doubleTapSection.getDouble("chance_per_level")
        );

        return new PluginConfig.SkillsConfig(energyConfig, sweepConfig, focusConfig, shearsConfig, doubleTapConfig);
    }

    private PluginConfig.ProgressionConfig parseProgression(ConfigurationSection section) {
        int cap = section.getInt("cap");
        List<Map<String, Object>> rawPiecewise = new java.util.ArrayList<>();
        for (Map<?, ?> raw : section.getMapList("piecewise")) {
            Map<String, Object> converted = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                converted.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            rawPiecewise.add(converted);
        }
        List<PluginConfig.ProgressionConfig.PiecewiseEntry> entries = PluginConfig.BuilderUtil.buildPiecewise(rawPiecewise);
        double globalMultiplier = section.getDouble("global_cost_multiplier");
        return new PluginConfig.ProgressionConfig(cap, entries, globalMultiplier);
    }

    private PluginConfig.WorldConfig parseWorld(ConfigurationSection section) {
        ConfigurationSection resetSection = getSection(section, "reset");
        PluginConfig.WorldConfig.WorldResetConfig resetConfig = new PluginConfig.WorldConfig.WorldResetConfig(
                resetSection.getString("schedule"),
                resetSection.getInt("pre_notice_min"),
                resetSection.getInt("pre_kick_min"),
                resetSection.getString("teleport_target"),
                resetSection.getBoolean("pregenerate"),
                resetSection.getInt("border_radius")
        );
        List<String> whitelist = section.getStringList("command_whitelist");
        whitelist.replaceAll(cmd -> cmd.toLowerCase(java.util.Locale.ROOT));
        return new PluginConfig.WorldConfig(resetConfig, whitelist);
    }

    private PluginConfig.UiConfig parseUi(ConfigurationSection section) {
        ConfigurationSection bossbarSection = getSection(section, "bossbar");
        PluginConfig.UiConfig.BossBarConfig bossBarConfig = new PluginConfig.UiConfig.BossBarConfig(
                bossbarSection.getBoolean("enabled"),
                bossbarSection.getBoolean("color_by_combo")
        );
        boolean actionbarFallback = section.getBoolean("actionbar_fallback");
        Map<String, Object> rawSounds = getSection(section, "sounds").getValues(false);
        return new PluginConfig.UiConfig(bossBarConfig, actionbarFallback, rawSounds.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, entry -> String.valueOf(entry.getValue()))));
    }

    private PluginConfig.ItemIdentityConfig parseItemIdentity(ConfigurationSection section) {
        return new PluginConfig.ItemIdentityConfig(
                section.getBoolean("enforce_uid", true),
                section.getBoolean("owner_lock", false),
                section.getBoolean("prevent_stack", true),
                section.getBoolean("prevent_merge", true)
        );
    }

    private PluginConfig.CooldownUiConfig parseCooldownUi(ConfigurationSection section) {
        String modeValue = section.getString("mode", "PACKET_COUNT");
        PluginConfig.CooldownUiConfig.CooldownUiMode mode;
        try {
            mode = PluginConfig.CooldownUiConfig.CooldownUiMode.valueOf(modeValue.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new PluginConfig.ConfigLoadException("Unknown cooldown_ui.mode: " + modeValue, ex);
        }

        ConfigurationSection packetSection = getSection(section, "packet");
        PluginConfig.CooldownUiConfig.PacketConfig packetConfig = new PluginConfig.CooldownUiConfig.PacketConfig(
                packetSection.getInt("resend_ticks", 2),
                packetSection.getInt("clamp_max", 64)
        );

        ConfigurationSection hudSection = getSection(section, "hud");
        String hudModeValue = hudSection.getString("show_skill", "LONGEST");
        PluginConfig.CooldownUiConfig.HudConfig.ShowSkill showSkill;
        try {
            showSkill = PluginConfig.CooldownUiConfig.HudConfig.ShowSkill.valueOf(hudModeValue.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new PluginConfig.ConfigLoadException("Unknown cooldown_ui.hud.show_skill: " + hudModeValue, ex);
        }
        PluginConfig.CooldownUiConfig.HudConfig hudConfig = new PluginConfig.CooldownUiConfig.HudConfig(
                hudSection.getInt("slot", 8),
                hudSection.getString("material", "PAPER"),
                hudSection.getString("name", "&e스킬 쿨타임"),
                hudSection.getStringList("lore"),
                hudSection.getBoolean("protect", true),
                showSkill
        );

        return new PluginConfig.CooldownUiConfig(mode, packetConfig, hudConfig);
    }

    private PluginConfig.NotificationsConfig parseNotifications(ConfigurationSection section) {
        return new PluginConfig.NotificationsConfig(
                section.getString("harvest_denied"),
                section.getString("inventory_full"),
                section.getString("energy_missing"),
                section.getString("cooldown_active")
        );
    }

    private PluginConfig.StorageConfig parseStorage(ConfigurationSection section) {
        String typeString = section.getString("type", "MEMORY");
        PluginConfig.StorageConfig.StorageType type;
        try {
            type = PluginConfig.StorageConfig.StorageType.valueOf(typeString.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new PluginConfig.ConfigLoadException("Unknown storage type: " + typeString, ex);
        }

        ConfigurationSection mysqlSection = getSection(section, "mysql");
        ConfigurationSection poolSection = mysqlSection.getConfigurationSection("pool");
        PluginConfig.StorageConfig.MysqlConfig.PoolConfig poolConfig = new PluginConfig.StorageConfig.MysqlConfig.PoolConfig(
                poolSection != null ? poolSection.getInt("maximum_pool_size", 10) : 10,
                poolSection != null ? poolSection.getInt("minimum_idle", 2) : 2,
                poolSection != null ? poolSection.getLong("connection_timeout_ms", 30_000L) : 30_000L,
                poolSection != null ? poolSection.getLong("idle_timeout_ms", 600_000L) : 600_000L
        );
        PluginConfig.StorageConfig.MysqlConfig mysqlConfig = new PluginConfig.StorageConfig.MysqlConfig(
                mysqlSection.getString("host", "localhost"),
                mysqlSection.getInt("port", 3306),
                mysqlSection.getString("database", "farmgather"),
                mysqlSection.getString("username", "root"),
                mysqlSection.getString("password", ""),
                mysqlSection.getString("params", "useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8"),
                poolConfig
        );

        ConfigurationSection redisSection = getSection(section, "redis");
        PluginConfig.StorageConfig.RedisConfig redisConfig = new PluginConfig.StorageConfig.RedisConfig(
                redisSection.getString("host", "localhost"),
                redisSection.getInt("port", 6379),
                redisSection.getString("password", ""),
                redisSection.getInt("database", 0),
                redisSection.getBoolean("ssl", false),
                redisSection.getInt("timeout_ms", 2000)
        );

        return new PluginConfig.StorageConfig(type, mysqlConfig, redisConfig);
    }

    private ConfigurationSection getSection(ConfigurationSection root, String path) {
        ConfigurationSection section = root.getConfigurationSection(path);
        if (section == null) {
            String prefix = root.getCurrentPath();
            if (prefix == null || prefix.isBlank()) {
                prefix = "<root>";
            }
            throw new PluginConfig.ConfigLoadException("Missing configuration section: " + prefix + "." + path);
        }
        return section;
    }
}
