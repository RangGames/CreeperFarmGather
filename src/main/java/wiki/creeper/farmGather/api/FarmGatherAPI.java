package wiki.creeper.farmGather.api;

import java.util.UUID;
import org.bukkit.entity.Player;
import wiki.creeper.farmGather.FarmGather;
import wiki.creeper.farmGather.harvest.HarvestManager;
import wiki.creeper.farmGather.item.ItemIdentityService;
import wiki.creeper.farmGather.player.PlayerProfile;
import wiki.creeper.farmGather.player.ProfileManager;
import wiki.creeper.farmGather.progression.ComboService;
import wiki.creeper.farmGather.progression.ProgressionService;
import wiki.creeper.farmGather.skills.SkillManager;
import wiki.creeper.farmGather.ui.CooldownUiService;

public final class FarmGatherAPI {
    private static FarmGather plugin;

    private FarmGatherAPI() {
    }

    public static void init(FarmGather instance) {
        plugin = instance;
    }

    public static void shutdown() {
        plugin = null;
    }

    public static boolean isReady() {
        return plugin != null;
    }

    public static FarmGather plugin() {
        ensureReady();
        return plugin;
    }

    public static ProfileManager profiles() {
        ensureReady();
        return plugin.getProfileManager();
    }

    public static PlayerProfile profile(Player player) {
        ensureReady();
        return plugin.getProfileManager().getProfile(player);
    }

    public static PlayerProfile profile(UUID uuid) {
        ensureReady();
        return plugin.getProfileManager().getProfile(uuid);
    }

    public static ProgressionService progression() {
        ensureReady();
        return plugin.getProgressionService();
    }

    public static ComboService combos() {
        ensureReady();
        return plugin.getComboService();
    }

    public static HarvestManager harvests() {
        ensureReady();
        return plugin.getHarvestManager();
    }

    public static SkillManager skills() {
        ensureReady();
        return plugin.getSkillManager();
    }

    public static ItemIdentityService identity() {
        ensureReady();
        return plugin.getItemIdentityService();
    }

    public static CooldownUiService cooldownUi() {
        ensureReady();
        return plugin.getCooldownUiService();
    }

    private static void ensureReady() {
        if (plugin == null) {
            throw new IllegalStateException("FarmGatherAPI has not been initialised yet.");
        }
    }
}
