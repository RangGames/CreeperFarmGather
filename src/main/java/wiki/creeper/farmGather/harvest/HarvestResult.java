package wiki.creeper.farmGather.harvest;

import java.util.List;
import org.bukkit.inventory.ItemStack;
import wiki.creeper.farmGather.progression.ProgressionService;

public record HarvestResult(
        boolean success,
        FailReason failReason,
        int comboCount,
        double comboWindowSec,
        int xpGained,
        boolean guildXpBonus,
        boolean extraDropGranted,
        boolean doubleTapTriggered,
        double doubleTapMultiplier,
        List<ItemStack> grantedItems,
        ProgressionService.LevelUpResult levelUpResult
) {
    public static HarvestResult failure(FailReason reason) {
        return new HarvestResult(false, reason, 0, 0, 0, false, false, false, 1.0, List.of(), ProgressionService.LevelUpResult.noChange(0, 0));
    }

    public enum FailReason {
        PROFILE_NOT_LOADED,
        WRONG_WORLD,
        INVALID_TOOL,
        NOT_HARVESTABLE,
        ACTION_COOLDOWN,
        BUSY_STATE,
        OUT_OF_RANGE,
        VIEW_ANGLE,
        CANCELLED
    }
}
