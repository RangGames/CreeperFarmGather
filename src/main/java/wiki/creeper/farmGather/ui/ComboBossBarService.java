package wiki.creeper.farmGather.ui;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import wiki.creeper.farmGather.FarmGather;
import wiki.creeper.farmGather.config.PluginConfig;
import wiki.creeper.farmGather.player.PlayerProfile;
import wiki.creeper.farmGather.progression.ProgressionService;
import wiki.creeper.farmGather.util.Text;

public class ComboBossBarService {
    private final FarmGather plugin;
    private final ProgressionService progressionService;
    private final Map<UUID, BossBarEntry> entries = new ConcurrentHashMap<>();

    private PluginConfig.UiConfig uiConfig;
    private double actionCooldownSec;
    private BukkitTask task;

    public ComboBossBarService(FarmGather plugin, ProgressionService progressionService) {
        this.plugin = plugin;
        this.progressionService = progressionService;
    }

    public void start() {
        stop();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 5L, 5L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        entries.values().forEach(entry -> {
            Player player = Bukkit.getPlayer(entry.playerId());
            if (player != null) {
                player.hideBossBar(entry.bossBar());
            }
        });
        entries.clear();
    }

    public void reload(@NotNull PluginConfig config) {
        this.uiConfig = config.ui();
        this.actionCooldownSec = Math.max(0.0, config.harvest().actionCooldownSec());
        if (uiConfig == null || !uiConfig.bossbar().enabled()) {
            entries.values().forEach(entry -> {
                Player player = Bukkit.getPlayer(entry.playerId());
                if (player != null) {
                    player.hideBossBar(entry.bossBar());
                }
            });
            entries.clear();
        }
    }

    public void clear(Player player) {
        BossBarEntry entry = entries.remove(player.getUniqueId());
        if (entry != null) {
            player.hideBossBar(entry.bossBar());
        }
    }

    public void handleHarvest(Player player,
                              PlayerProfile profile,
                              int comboCount,
                              double comboWindowSec,
                              double xpBonusPercent,
                              int xpGained) {
        if (uiConfig == null) {
            return;
        }
        PluginConfig.UiConfig.BossBarConfig bossbar = uiConfig.bossbar();
        if (bossbar == null || !bossbar.enabled()) {
            clear(player);
            return;
        }

        Component title = formatTitle(profile, comboCount, xpBonusPercent, xpGained);
        double windowMillis = Math.max(comboWindowSec * 1000.0, 1);
        long expireAt = profile.getComboExpireAt();
        long actionCooldownEnd = profile.getActionCooldownEnd();
        long now = System.currentTimeMillis();
        DisplayStage stage = determineStage(bossbar.mode(), actionCooldownEnd, now);

        entries.compute(player.getUniqueId(), (uuid, existing) -> {
            BossBar bossBar = existing != null ? existing.bossBar() : createBossBar(comboCount);
            bossBar.name(title);
            bossBar.color(pickColor(comboCount));
            double progressValue = stage == DisplayStage.COOLDOWN
                    ? cooldownProgress(now, actionCooldownEnd)
                    : comboProgress(expireAt, windowMillis, now);
            bossBar.progress(clampProgress(progressValue));
            player.showBossBar(bossBar);
            return new BossBarEntry(uuid, bossBar, expireAt, windowMillis, actionCooldownEnd, stage);
        });
    }

    private Component formatTitle(PlayerProfile profile, int comboCount, double xpBonusPercent, int xpGained) {
        String template = uiConfig.bossbar().titleFormat();
        int level = profile.getLevel();
        double currentXp = profile.getXp();
        double xpToNext;
        try {
            xpToNext = progressionService.getXpToNextLevel(level);
        } catch (IllegalStateException ignored) {
            xpToNext = Double.POSITIVE_INFINITY;
        }
        String nextDisplay = Double.isInfinite(xpToNext)
                ? "MAX"
                : Integer.toString((int) Math.round(xpToNext));
        String formatted = template
                .replace("{combo}", Integer.toString(comboCount))
                .replace("{bonus}", String.format("%.0f", Math.max(0, xpBonusPercent) * 100.0))
                .replace("{gain}", Integer.toString(Math.max(0, xpGained)))
                .replace("{level}", Integer.toString(level))
                .replace("{cur}", Integer.toString((int) Math.round(currentXp)))
                .replace("{next}", nextDisplay);
        return Text.colorize(formatted);
    }

    private BossBar createBossBar(int comboCount) {
        return BossBar.bossBar(Component.empty(), 1.0f, pickColor(comboCount), BossBar.Overlay.PROGRESS);
    }

    private BossBar.Color pickColor(int comboCount) {
        if (!uiConfig.bossbar().colorByCombo()) {
            return BossBar.Color.YELLOW;
        }
        if (comboCount >= 10) {
            return BossBar.Color.PURPLE;
        }
        if (comboCount >= 5) {
            return BossBar.Color.GREEN;
        }
        return BossBar.Color.YELLOW;
    }

    private DisplayStage determineStage(PluginConfig.UiConfig.BossBarConfig.BossBarMode mode,
                                        long actionCooldownEnd,
                                        long now) {
        if (mode == PluginConfig.UiConfig.BossBarConfig.BossBarMode.COOLDOWN_THEN_COMBO
                && actionCooldownEnd > now) {
            return DisplayStage.COOLDOWN;
        }
        return DisplayStage.COMBO;
    }

    private double cooldownProgress(long now, long actionCooldownEnd) {
        if (actionCooldownSec <= 0.0) {
            return 1.0;
        }
        double remaining = actionCooldownEnd - now;
        if (remaining <= 0) {
            return 1.0;
        }
        double totalMillis = actionCooldownSec * 1000.0;
        return 1.0 - Math.min(1.0, remaining / totalMillis);
    }

    private double comboProgress(long expireAt, double windowMillis, long now) {
        double remaining = expireAt - now;
        if (remaining <= 0) {
            return 0.0;
        }
        return Math.min(1.0, remaining / windowMillis);
    }

    private float clampProgress(double value) {
        if (value <= 0.0) {
            return 0.0f;
        }
        if (value >= 1.0) {
            return 1.0f;
        }
        return (float) value;
    }

    private void tick() {
        long now = System.currentTimeMillis();
        var iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, BossBarEntry> mapEntry = iterator.next();
            UUID uuid = mapEntry.getKey();
            BossBarEntry entry = mapEntry.getValue();
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                iterator.remove();
                continue;
            }

            BossBarEntry current = entry;
            double progressValue;
            if (current.stage() == DisplayStage.COOLDOWN
                    && uiConfig != null
                    && uiConfig.bossbar().mode() == PluginConfig.UiConfig.BossBarConfig.BossBarMode.COOLDOWN_THEN_COMBO) {
                if (current.actionCooldownEnd() <= now) {
                    current = current.withStage(DisplayStage.COMBO);
                    mapEntry.setValue(current);
                    progressValue = comboProgress(current.expireAt(), current.windowMillis(), now);
                } else {
                    progressValue = cooldownProgress(now, current.actionCooldownEnd());
                }
            } else {
                progressValue = comboProgress(current.expireAt(), current.windowMillis(), now);
            }

            current.bossBar().progress(clampProgress(progressValue));

            boolean shouldRemove = (current.stage() == DisplayStage.COMBO && progressValue <= 0.0)
                    || uiConfig == null
                    || !uiConfig.bossbar().enabled();
            if (shouldRemove) {
                player.hideBossBar(current.bossBar());
                iterator.remove();
            }
        }
    }

    private record BossBarEntry(UUID playerId,
                                BossBar bossBar,
                                long expireAt,
                                double windowMillis,
                                long actionCooldownEnd,
                                DisplayStage stage) {
        BossBarEntry withStage(DisplayStage newStage) {
            return new BossBarEntry(playerId, bossBar, expireAt, windowMillis, actionCooldownEnd, newStage);
        }
    }

    private enum DisplayStage {
        COOLDOWN,
        COMBO
    }
}
