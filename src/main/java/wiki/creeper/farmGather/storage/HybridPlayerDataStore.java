package wiki.creeper.farmGather.storage;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import wiki.creeper.farmGather.player.PlayerProfile;

public class HybridPlayerDataStore implements PlayerDataStore {
    private final PlayerDataStore primary;
    private final RedisPlayerDataStore cache;
    private final Logger logger;

    public HybridPlayerDataStore(PlayerDataStore primary, RedisPlayerDataStore cache, Logger logger) {
        this.primary = primary;
        this.cache = cache;
        this.logger = logger;
    }

    @Override
    public CompletableFuture<Optional<PlayerProfile>> loadProfile(UUID uuid) {
        return cache.loadProfile(uuid)
                .exceptionally(throwable -> {
                    logger.log(Level.WARNING, "Failed to load FarmGather profile from Redis cache", throwable);
                    return Optional.empty();
                })
                .thenCompose(optional -> {
                    if (optional.isPresent()) {
                        return CompletableFuture.completedFuture(optional);
                    }
                    return primary.loadProfile(uuid).thenCompose(primaryResult -> {
                        if (primaryResult.isEmpty()) {
                            return CompletableFuture.completedFuture(Optional.empty());
                        }
                        PlayerProfile profile = primaryResult.get();
                        return cache.saveProfile(profile)
                                .exceptionally(throwable -> {
                                    logger.log(Level.WARNING, "Failed to cache FarmGather profile into Redis", throwable);
                                    return null;
                                })
                                .thenApply(ignored -> Optional.of(profile));
                    });
                });
    }

    @Override
    public CompletableFuture<Void> saveProfile(PlayerProfile profile) {
        CompletableFuture<Void> primaryFuture = primary.saveProfile(profile);
        CompletableFuture<Void> cacheFuture = cache.saveProfile(profile)
                .exceptionally(throwable -> {
                    logger.log(Level.WARNING, "Failed to cache FarmGather profile into Redis", throwable);
                    return null;
                });
        return primaryFuture.thenCombine(cacheFuture, (a, b) -> null);
    }

    @Override
    public CompletableFuture<Void> close() {
        return primary.close()
                .exceptionally(throwable -> {
                    logger.log(Level.WARNING, "Failed to close primary data store", throwable);
                    return null;
                })
                .thenCompose(v -> cache.close().exceptionally(throwable -> {
                    logger.log(Level.WARNING, "Failed to close Redis data store", throwable);
                    return null;
                }));
    }
}
