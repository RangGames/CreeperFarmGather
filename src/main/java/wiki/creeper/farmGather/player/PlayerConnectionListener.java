package wiki.creeper.farmGather.player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitScheduler;
import wiki.creeper.farmGather.FarmGather;
import wiki.creeper.farmGather.ui.ComboBossBarService;

public class PlayerConnectionListener implements Listener {
    private final FarmGather plugin;
    private final ProfileManager profileManager;
    private final ComboBossBarService bossBarService;

    public PlayerConnectionListener(FarmGather plugin, ProfileManager profileManager, ComboBossBarService bossBarService) {
        this.plugin = plugin;
        this.profileManager = profileManager;
        this.bossBarService = bossBarService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        scheduler.runTaskAsynchronously(plugin, () -> profileManager.loadProfile(event.getPlayer().getUniqueId()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        bossBarService.clear(event.getPlayer());
        scheduler.runTaskAsynchronously(plugin, () -> profileManager.unloadProfile(event.getPlayer().getUniqueId()).join());
    }
}
