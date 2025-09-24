package wiki.creeper.farmGather.storage;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import wiki.creeper.farmGather.player.PlayerProfile;

public class InMemoryPlayerDataStore implements PlayerDataStore {
    private final Map<UUID, PlayerProfile> profiles = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<Optional<PlayerProfile>> loadProfile(UUID uuid) {
        return CompletableFuture.completedFuture(Optional.ofNullable(profiles.get(uuid)));
    }

    @Override
    public CompletableFuture<Void> saveProfile(PlayerProfile profile) {
        profiles.put(profile.getUuid(), profile);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> close() {
        profiles.clear();
        return CompletableFuture.completedFuture(null);
    }
}
