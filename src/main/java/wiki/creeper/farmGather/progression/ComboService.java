package wiki.creeper.farmGather.progression;

import java.util.Optional;
import wiki.creeper.farmGather.config.PluginConfig;
import wiki.creeper.farmGather.player.PlayerProfile;

public class ComboService {
    private PluginConfig.ComboConfig comboConfig;

    public ComboService(PluginConfig.ComboConfig comboConfig) {
        this.comboConfig = comboConfig;
    }

    public void reload(PluginConfig.ComboConfig comboConfig) {
        this.comboConfig = comboConfig;
    }

    public ComboWindow computeComboWindow(int level) {
        double base = comboConfig.baseWindowSec();
        double decay = comboConfig.perLevelDecayRate();
        double min = comboConfig.minWindowSec();
        double window = Math.max(base * (1 - decay * level), min);
        return new ComboWindow(window);
    }

    public ComboResult applyCombo(PlayerProfile profile, String blockType, long nowMillis, int level, Optional<Double> overrideWindowSec) {
        double comboWindowSec = overrideWindowSec.orElseGet(() -> computeComboWindow(level).seconds());
        double windowMs = comboWindowSec * 1000.0;

        boolean sameType = blockType.equals(profile.getLastBlockType());
        boolean withinWindow = nowMillis <= profile.getComboExpireAt();
        int nextCombo = (sameType && withinWindow) ? profile.getComboCount() + 1 : 1;

        profile.setLastBlockType(blockType);
        profile.setComboCount(nextCombo);
        profile.setComboExpireAt(nowMillis + (long) windowMs);

        return new ComboResult(nextCombo, comboWindowSec, sameType && withinWindow);
    }

    public record ComboWindow(double seconds) {}

    public record ComboResult(int comboCount, double windowSeconds, boolean continued) {}
}
