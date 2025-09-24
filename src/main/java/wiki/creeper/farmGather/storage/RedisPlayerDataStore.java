package wiki.creeper.farmGather.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import wiki.creeper.farmGather.player.PlayerProfile;

public class RedisPlayerDataStore implements PlayerDataStore {
    private static final String KEY_PREFIX = "farmgather:profile:";

    private final JedisPool pool;
    private final ExecutorService executor;

    public RedisPlayerDataStore(JedisPool pool) {
        this.pool = pool;
        this.executor = Executors.newFixedThreadPool(2, createThreadFactory());
    }

    private ThreadFactory createThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, "FarmGather-Redis");
            thread.setDaemon(true);
            return thread;
        };
    }

    @Override
    public CompletableFuture<Optional<PlayerProfile>> loadProfile(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Jedis jedis = pool.getResource()) {
                Map<String, String> data = jedis.hgetAll(key(uuid));
                if (data == null || data.isEmpty()) {
                    return Optional.empty();
                }
                PlayerProfile profile = new PlayerProfile(uuid);
                applyProfile(profile, data);
                return Optional.of(profile);
            } catch (Exception ex) {
                throw new CompletionException(ex);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> saveProfile(PlayerProfile profile) {
        return CompletableFuture.runAsync(() -> {
            try (Jedis jedis = pool.getResource()) {
                Map<String, String> payload = serializeProfile(profile);
                jedis.del(key(profile.getUuid()));
                jedis.hset(key(profile.getUuid()), payload);
            } catch (Exception ex) {
                throw new CompletionException(ex);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> close() {
        executor.shutdownNow();
        pool.close();
        return CompletableFuture.completedFuture(null);
    }

    private String key(UUID uuid) {
        return KEY_PREFIX + uuid;
    }

    private Map<String, String> serializeProfile(PlayerProfile profile) {
        Map<String, String> map = new HashMap<>();
        map.put("uuid", profile.getUuid().toString());
        map.put("level", String.valueOf(profile.getLevel()));
        map.put("xp", String.valueOf(profile.getXp()));
        map.put("mastery", String.valueOf(profile.getMastery()));
        map.put("energy", String.valueOf(profile.getEnergy()));
        map.put("last_energy_tick", String.valueOf(profile.getLastEnergyTick()));
        map.put("last_harvest_at", String.valueOf(profile.getLastHarvestAt()));
        map.put("action_cooldown_end", String.valueOf(profile.getActionCooldownEnd()));
        if (profile.getLastBlockType() != null) {
            map.put("last_block_type", profile.getLastBlockType());
        }
        map.put("combo_count", String.valueOf(profile.getComboCount()));
        map.put("combo_expire_at", String.valueOf(profile.getComboExpireAt()));
        map.put("combo_override_window", String.valueOf(profile.getComboOverrideWindow()));
        map.put("combo_override_until", String.valueOf(profile.getComboOverrideUntil()));
        map.put("last_yaw", String.valueOf(profile.getLastYaw()));
        map.put("last_pitch", String.valueOf(profile.getLastPitch()));
        map.put("last_target_distance", String.valueOf(profile.getLastTargetDistance()));
        String encodedSkills = ProfileSkillCodec.encode(profile.getHoeSkills());
        if (encodedSkills != null) {
            map.put("hoe_skills", encodedSkills);
        }
        if (profile.getGuildId() != null) {
            map.put("guild_id", profile.getGuildId());
        }
        map.put("last_guild_harvest_at", String.valueOf(profile.getLastGuildHarvestAt()));
        return map;
    }

    private void applyProfile(PlayerProfile profile, Map<String, String> data) {
        profile.setLevel(parseInt(data.get("level"), profile.getLevel()));
        profile.setXp(parseDouble(data.get("xp"), profile.getXp()));
        profile.setMastery(parseDouble(data.get("mastery"), profile.getMastery()));
        profile.setEnergy(parseDouble(data.get("energy"), profile.getEnergy()));
        profile.setLastEnergyTick(parseLong(data.get("last_energy_tick"), profile.getLastEnergyTick()));
        profile.setLastHarvestAt(parseLong(data.get("last_harvest_at"), profile.getLastHarvestAt()));
        profile.setActionCooldownEnd(parseLong(data.get("action_cooldown_end"), profile.getActionCooldownEnd()));
        profile.setLastBlockType(data.get("last_block_type"));
        profile.setComboCount(parseInt(data.get("combo_count"), profile.getComboCount()));
        profile.setComboExpireAt(parseLong(data.get("combo_expire_at"), profile.getComboExpireAt()));
        double overrideWindow = parseDouble(data.get("combo_override_window"), 0.0);
        long overrideUntil = parseLong(data.get("combo_override_until"), 0L);
        if (overrideWindow > 0 && overrideUntil > 0) {
            profile.setComboOverride(overrideWindow, overrideUntil);
        } else {
            profile.clearComboOverride();
        }
        profile.setLastYaw((float) parseDouble(data.get("last_yaw"), profile.getLastYaw()));
        profile.setLastPitch((float) parseDouble(data.get("last_pitch"), profile.getLastPitch()));
        profile.setLastTargetDistance(parseDouble(data.get("last_target_distance"), profile.getLastTargetDistance()));
        ProfileSkillCodec.decode(data.get("hoe_skills"), profile);
        profile.setGuildId(data.get("guild_id"));
        profile.setLastGuildHarvestAt(parseLong(data.get("last_guild_harvest_at"), profile.getLastGuildHarvestAt()));
    }

    private int parseInt(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private long parseLong(String value, long fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private double parseDouble(String value, double fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
