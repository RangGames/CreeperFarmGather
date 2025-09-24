package wiki.creeper.farmGather.player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import wiki.creeper.farmGather.storage.PlayerDataStore;

public class ProfileManager {
    private final PlayerDataStore dataStore;
    private final Map<UUID, PlayerProfile> profiles = new ConcurrentHashMap<>();

    public ProfileManager(PlayerDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public CompletableFuture<PlayerProfile> loadProfile(UUID uuid) {
        return dataStore.loadProfile(uuid)
                .thenApply(optional -> {
                    PlayerProfile profile = optional.orElseGet(() -> new PlayerProfile(uuid));
                    profiles.put(uuid, profile);
                    return profile;
                });
    }

    public CompletableFuture<Void> unloadProfile(UUID uuid) {
        PlayerProfile profile = profiles.remove(uuid);
        if (profile == null) {
            return CompletableFuture.completedFuture(null);
        }
        return dataStore.saveProfile(profile);
    }

    public CompletableFuture<Void> flush(UUID uuid) {
        PlayerProfile profile = profiles.get(uuid);
        if (profile == null) {
            return CompletableFuture.completedFuture(null);
        }
        return dataStore.saveProfile(profile);
    }

    public PlayerProfile getProfile(UUID uuid) {
        return profiles.get(uuid);
    }

    public PlayerProfile getProfile(Player player) {
        return getProfile(player.getUniqueId());
    }

    public void flushAllSync() {
        profiles.values().forEach(profile -> dataStore.saveProfile(profile).join());
    }

    public CompletableFuture<Void> close() {
        profiles.values().forEach(profile -> dataStore.saveProfile(profile).join());
        return dataStore.close();
    }

    public void loadOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadProfile(player.getUniqueId());
        }
    }
}
