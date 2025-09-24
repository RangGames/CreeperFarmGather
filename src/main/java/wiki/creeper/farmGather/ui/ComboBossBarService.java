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

        Component text = buildText(profile, comboCount, xpBonusPercent, xpGained);
        double windowMillis = Math.max(comboWindowSec * 1000.0, 1);
        long expireAt = profile.getComboExpireAt();

        if (uiConfig.bossbar().enabled()) {
            entries.compute(player.getUniqueId(), (uuid, existing) -> {
                BossBar bossBar = existing != null ? existing.bossBar() : createBossBar(comboCount);
                bossBar.name(text);
                bossBar.color(pickColor(comboCount));
                bossBar.progress((float) progress(expireAt, windowMillis));
                player.showBossBar(bossBar);
                return new BossBarEntry(uuid, bossBar, expireAt, windowMillis);
            });
            if (uiConfig.actionbarFallback()) {
                player.sendActionBar(text);
            }
        } else {
            clear(player);
            if (uiConfig.actionbarFallback()) {
                player.sendActionBar(text);
            }
        }

    }

    private Component buildText(PlayerProfile profile, int comboCount, double xpBonusPercent, int xpGained) {
        int level = profile.getLevel();
        double currentXp = profile.getXp();
        double xpToNext;
        try {
            xpToNext = progressionService.getXpToNextLevel(level);
        } catch (IllegalStateException ignored) {
            xpToNext = Double.POSITIVE_INFINITY;
        }
        String xpSection;
        if (Double.isInfinite(xpToNext)) {
            xpSection = "&aMAX";
        } else {
            xpSection = String.format("&a%d&7/%d", (int) Math.round(currentXp), (int) Math.round(xpToNext));
        }
        double percent = Math.max(0, xpBonusPercent) * 100.0;
        return Text.colorize(String.format("&e%d콤보 &7[+%.0f%%보너스] %s &a[+%d경험치]",
                comboCount,
                percent,
                xpSection,
                xpGained));
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

    private double progress(long expireAt, double windowMillis) {
        long now = System.currentTimeMillis();
        double remaining = expireAt - now;
        if (remaining <= 0) {
            return 0.0;
        }
        return Math.min(1.0, remaining / windowMillis);
    }

    private void tick() {
        entries.entrySet().removeIf(entry -> {
            UUID uuid = entry.getKey();
            BossBarEntry value = entry.getValue();
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                return true;
            }
            double progress = progress(value.expireAt(), value.windowMillis());
            value.bossBar().progress((float) progress);
            if (progress <= 0.0) {
                player.hideBossBar(value.bossBar());
                return true;
            }
            return false;
        });
    }

    private record BossBarEntry(UUID playerId, BossBar bossBar, long expireAt, double windowMillis) {}
}
