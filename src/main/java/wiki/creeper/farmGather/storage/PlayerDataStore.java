package wiki.creeper.farmGather.storage;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import wiki.creeper.farmGather.player.PlayerProfile;

public interface PlayerDataStore {
    CompletableFuture<Optional<PlayerProfile>> loadProfile(UUID uuid);

    CompletableFuture<Void> saveProfile(PlayerProfile profile);

    CompletableFuture<Void> close();
}
