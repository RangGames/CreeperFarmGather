package wiki.creeper.farmGather.integration;

import org.bukkit.entity.Player;
import wiki.creeper.farmGather.player.PlayerProfile;

public class NoGuildService implements GuildService {
    @Override
    public GuildContext evaluateContext(Player player, PlayerProfile profile) {
        return new GuildContext(false, 0);
    }
}
