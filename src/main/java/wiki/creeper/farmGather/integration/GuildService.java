package wiki.creeper.farmGather.integration;

import org.bukkit.entity.Player;
import wiki.creeper.farmGather.player.PlayerProfile;

public interface GuildService {
    GuildContext evaluateContext(Player player, PlayerProfile profile);

    record GuildContext(boolean sameWorldActive, int activeMembers) {}
}
