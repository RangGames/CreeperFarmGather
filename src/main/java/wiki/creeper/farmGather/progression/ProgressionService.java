package wiki.creeper.farmGather.progression;

import java.util.ArrayList;
import java.util.List;
import wiki.creeper.farmGather.config.PluginConfig;
import wiki.creeper.farmGather.player.PlayerProfile;

public class ProgressionService {
    private PluginConfig.ProgressionConfig config;

    public ProgressionService(PluginConfig.ProgressionConfig config) {
        this.config = config;
    }

    public void reload(PluginConfig.ProgressionConfig config) {
        this.config = config;
    }

    public PluginConfig.ProgressionConfig getConfig() {
        return config;
    }

    public LevelUpResult addXp(PlayerProfile profile, double amount) {
        if (amount <= 0) {
            return LevelUpResult.noChange(profile.getLevel(), profile.getXp());
        }

        int level = profile.getLevel();
        double xp = profile.getXp() + amount;
        List<Integer> levelUps = new ArrayList<>();

        while (level < config.cap()) {
            double xpNeeded = getXpToNextLevel(level);
            if (xp < xpNeeded) {
                break;
            }
            xp -= xpNeeded;
            level += 1;
            levelUps.add(level);
        }

        if (level >= config.cap()) {
            level = config.cap();
            xp = 0;
        }

        profile.setLevel(level);
        profile.setXp(xp);

        return new LevelUpResult(level, xp, levelUps);
    }

    public double getXpToNextLevel(int level) {
        if (level >= config.cap()) {
            return Double.POSITIVE_INFINITY;
        }

        for (PluginConfig.ProgressionConfig.PiecewiseEntry entry : config.piecewise()) {
            PluginConfig.ProgressionConfig.PiecewiseEntry.Range range = entry.range();
            if (level >= range.min() && level <= range.max()) {
                double baseXp = evaluateFormula(entry.formula(), level);
                baseXp *= entry.scale();
                baseXp *= config.globalCostMultiplier();
                return baseXp;
            }
        }

        throw new IllegalStateException("No XP formula defined for level " + level);
    }

    private double evaluateFormula(String formula, int level) {
        if (formula.contains("(L-100)")) {
            double delta = level - 100;
            return 95_375 + 600 * delta + 18 * delta * delta;
        }
        if (formula.contains("L^2")) {
            return 375 + 50 * level + 9 * level * level;
        }
        throw new IllegalArgumentException("Unsupported progression formula: " + formula);
    }

    public record LevelUpResult(int level, double remainingXp, List<Integer> leveledTo) {
        public boolean leveledUp() {
            return !leveledTo.isEmpty();
        }

        public static LevelUpResult noChange(int level, double xp) {
            return new LevelUpResult(level, xp, List.of());
        }
    }
}
