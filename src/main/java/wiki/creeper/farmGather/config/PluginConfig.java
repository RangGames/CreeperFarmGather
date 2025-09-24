package wiki.creeper.farmGather.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PluginConfig {
    private final HarvestConfig harvest;
    private final StorageConfig storage;
    private final ComboConfig combo;
    private final XpConfig xp;
    private final ExtraDropConfig extraDrop;
    private final SkillsConfig skills;
    private final ProgressionConfig progression;
    private final WorldConfig world;
    private final UiConfig ui;
    private final ItemIdentityConfig itemIdentity;
    private final CooldownUiConfig cooldownUi;
    private final NotificationsConfig notifications;

    private PluginConfig(Builder builder) {
        this.harvest = builder.harvest;
        this.storage = builder.storage;
        this.combo = builder.combo;
        this.xp = builder.xp;
        this.extraDrop = builder.extraDrop;
        this.skills = builder.skills;
        this.progression = builder.progression;
        this.world = builder.world;
        this.ui = builder.ui;
        this.itemIdentity = builder.itemIdentity;
        this.cooldownUi = builder.cooldownUi;
        this.notifications = builder.notifications;
    }

    public HarvestConfig harvest() {
        return harvest;
    }

    public StorageConfig storage() {
        return storage;
    }

    public ComboConfig combo() {
        return combo;
    }

    public XpConfig xp() {
        return xp;
    }

    public ExtraDropConfig extraDrop() {
        return extraDrop;
    }

    public SkillsConfig skills() {
        return skills;
    }

    public ProgressionConfig progression() {
        return progression;
    }

    public WorldConfig world() {
        return world;
    }

    public UiConfig ui() {
        return ui;
    }

    public ItemIdentityConfig itemIdentity() {
        return itemIdentity;
    }

    public CooldownUiConfig cooldownUi() {
        return cooldownUi;
    }

    public NotificationsConfig notifications() {
        return notifications;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private HarvestConfig harvest;
        private StorageConfig storage;
        private ComboConfig combo;
        private XpConfig xp;
        private ExtraDropConfig extraDrop;
        private SkillsConfig skills;
        private ProgressionConfig progression;
        private WorldConfig world;
        private UiConfig ui;
        private ItemIdentityConfig itemIdentity;
        private CooldownUiConfig cooldownUi;
        private NotificationsConfig notifications;

        public Builder harvest(HarvestConfig harvest) {
            this.harvest = harvest;
            return this;
        }

        public Builder storage(StorageConfig storage) {
            this.storage = storage;
            return this;
        }

        public Builder combo(ComboConfig combo) {
            this.combo = combo;
            return this;
        }

        public Builder xp(XpConfig xp) {
            this.xp = xp;
            return this;
        }

        public Builder extraDrop(ExtraDropConfig extraDrop) {
            this.extraDrop = extraDrop;
            return this;
        }

        public Builder skills(SkillsConfig skills) {
            this.skills = skills;
            return this;
        }

        public Builder progression(ProgressionConfig progression) {
            this.progression = progression;
            return this;
        }

        public Builder world(WorldConfig world) {
            this.world = world;
            return this;
        }

        public Builder ui(UiConfig ui) {
            this.ui = ui;
            return this;
        }

        public Builder itemIdentity(ItemIdentityConfig itemIdentity) {
            this.itemIdentity = itemIdentity;
            return this;
        }

        public Builder cooldownUi(CooldownUiConfig cooldownUi) {
            this.cooldownUi = cooldownUi;
            return this;
        }

        public Builder notifications(NotificationsConfig notifications) {
            this.notifications = notifications;
            return this;
        }

        public PluginConfig build() {
            return new PluginConfig(this);
        }
    }

    public record HarvestConfig(
            List<String> worlds,
            List<String> harvestableTags,
            double actionCooldownSec,
            DropConfig drop,
            AntiMacroConfig antiMacro
    ) {
        public record DropConfig(String mode, double groundMergeRadius, int stackMergeCount) {}

        public record AntiMacroConfig(double viewAngleDeg, double maxDistance, long jitterMs) {}
    }

    public record StorageConfig(
            StorageType type,
            MysqlConfig mysql,
            RedisConfig redis
    ) {
        public enum StorageType {
            MYSQL,
            REDIS,
            HYBRID,
            MEMORY
        }

        public record MysqlConfig(
                String host,
                int port,
                String database,
                String username,
                String password,
                String params,
                PoolConfig pool
        ) {
            public record PoolConfig(
                    int maximumPoolSize,
                    int minimumIdle,
                    long connectionTimeoutMs,
                    long idleTimeoutMs
            ) {}
        }

        public record RedisConfig(
                String host,
                int port,
                String password,
                int database,
                boolean ssl,
                int timeoutMs
        ) {}
    }

    public record ComboConfig(
            double baseWindowSec,
            double perLevelDecayRate,
            double minWindowSec,
            double xpBonusPerStack,
            double xpBonusCap,
            int intRoundingFromStack,
            int intRoundingCap
    ) {}

    public record XpConfig(
            double basePerHarvest,
            GuildBonusConfig guildBonus
    ) {
        public record GuildBonusConfig(
                double baseChance,
                double perMemberInc,
                double chanceCap,
                int recentSec,
                boolean requireSameWorld,
                boolean requireDistance,
                double distance
        ) {}
    }

    public record ExtraDropConfig(
            double base,
            double perCombo,
            double perLevelLog2,
            double hardCap,
            boolean doubletapMultiplies
    ) {}

    public record SkillsConfig(
            EnergyConfig energy,
            SweepConfig sweep,
            FocusConfig focus,
            ShearsConfig shears,
            DoubleTapConfig doubleTap
    ) {
        public record EnergyConfig(int max, double regenPerSec, boolean regenIfAfk) {}

        public record SweepConfig(
                boolean enabled,
                String size,
                double cdBase,
                double cdPerLevelMinus,
                double energyCost,
                int maxTargets,
                int xpFirst,
                int xpOthers
        ) {}

        public record FocusConfig(
                boolean enabled,
                double durationBase,
                double durationPerLevel,
                double cooldown,
                double energyCost,
                double minIntervalSec
        ) {}

        public record ShearsConfig(
                boolean enabled,
                String chainSize,
                double cooldown,
                double energyCostBase,
                double energyCostPerLevelMinus,
                int maxChainTargets,
                double hopDelaySec,
                int xpFirst,
                int xpOthers
        ) {}

        public record DoubleTapConfig(
                boolean enabled,
                double windowSec,
                double chanceBase,
                double chancePerLevel
        ) {}
    }

    public record ProgressionConfig(
            int cap,
            List<PiecewiseEntry> piecewise,
            double globalCostMultiplier
    ) {
        public record PiecewiseEntry(Range range, double scale, String formula) {
            public record Range(int min, int max) {}
        }
    }

    public record WorldConfig(WorldResetConfig reset, List<String> commandWhitelist) {
        public record WorldResetConfig(
                String schedule,
                int preNoticeMin,
                int preKickMin,
                String teleportTarget,
                boolean pregenerate,
                int borderRadius
        ) {}
    }

    public record UiConfig(
            BossBarConfig bossbar,
            boolean actionbarFallback,
            Map<String, String> sounds
    ) {
        public record BossBarConfig(boolean enabled, boolean colorByCombo) {}
    }

    public record ItemIdentityConfig(
            boolean enforceUid,
            boolean ownerLock,
            boolean preventStack,
            boolean preventMerge
    ) {}

    public record CooldownUiConfig(
            CooldownUiMode mode,
            PacketConfig packet,
            HudConfig hud
    ) {
        public enum CooldownUiMode {
            PACKET_COUNT,
            LOCKED_HUD_SLOT,
            DISABLED
        }

        public record PacketConfig(int resendTicks, int clampMax) {}

        public record HudConfig(
                int slot,
                String material,
                String name,
                List<String> lore,
                boolean protect,
                ShowSkill showSkill
        ) {
            public enum ShowSkill {
                LONGEST,
                SWEEP,
                FOCUS,
                SHEARS
            }
        }
    }

    public record NotificationsConfig(
            String harvestDenied,
            String inventoryFull,
            String energyMissing,
            String cooldownActive
    ) {}

    public static ProgressionConfig.PiecewiseEntry.Range parseRange(List<Integer> rangeList) {
        int min = rangeList.get(0);
        int max = rangeList.get(rangeList.size() - 1);
        return new ProgressionConfig.PiecewiseEntry.Range(min, max);
    }

    public static class ConfigLoadException extends RuntimeException {
        public ConfigLoadException(String message) {
            super(message);
        }

        public ConfigLoadException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class BuilderUtil {
        private BuilderUtil() {}

        public static List<ProgressionConfig.PiecewiseEntry> buildPiecewise(List<Map<String, Object>> rawList) {
            List<ProgressionConfig.PiecewiseEntry> result = new ArrayList<>();
            for (Map<String, Object> map : rawList) {
                @SuppressWarnings("unchecked") List<Integer> range = (List<Integer>) map.get("range");
                double scale = ((Number) map.get("scale")).doubleValue();
                String formula = map.get("formula").toString();
                result.add(new ProgressionConfig.PiecewiseEntry(parseRange(range), scale, formula));
            }
            return result;
        }
    }
}
