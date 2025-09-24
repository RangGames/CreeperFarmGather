package wiki.creeper.farmGather.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import wiki.creeper.farmGather.player.HoeSkill;
import wiki.creeper.farmGather.player.HoeSkillType;
import wiki.creeper.farmGather.player.PlayerProfile;

public class JdbcPlayerDataStore implements PlayerDataStore {
    public enum Dialect {
        MYSQL
    }

    @FunctionalInterface
    public interface ConnectionProvider {
        Connection get() throws SQLException;
    }

    private static final String INSERT_COLUMNS = "uuid, level, xp, mastery, energy, last_energy_tick, last_harvest_at, action_cooldown_end, last_block_type, combo_count, combo_expire_at, combo_override_window, combo_override_until, last_yaw, last_pitch, last_target_distance, hoe_skills, guild_id, last_guild_harvest_at";

    private final Dialect dialect;
    private final ConnectionProvider connectionProvider;
    private final ExecutorService executor;
    private final String upsertSql;
    private final Runnable closeHook;

    public JdbcPlayerDataStore(Dialect dialect, ConnectionProvider connectionProvider) {
        this(dialect, connectionProvider, () -> {});
    }

    public JdbcPlayerDataStore(Dialect dialect, ConnectionProvider connectionProvider, Runnable closeHook) {
        this.dialect = dialect;
        this.connectionProvider = connectionProvider;
        this.closeHook = closeHook == null ? () -> {} : closeHook;
        this.executor = Executors.newSingleThreadExecutor(createThreadFactory());
        this.upsertSql = buildUpsertSql();
        initialize();
    }

    private ThreadFactory createThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, "FarmGather-DB");
            thread.setDaemon(true);
            return thread;
        };
    }

    private void initialize() {
        try (Connection connection = connectionProvider.get(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS player_profiles (" +
                    "uuid VARCHAR(36) PRIMARY KEY," +
                    "level INTEGER NOT NULL," +
                    "xp DOUBLE NOT NULL," +
                    "mastery DOUBLE NOT NULL," +
                    "energy DOUBLE NOT NULL," +
                    "last_energy_tick BIGINT NOT NULL," +
                    "last_harvest_at BIGINT NOT NULL," +
                    "action_cooldown_end BIGINT NOT NULL," +
                    "last_block_type TEXT," +
                    "combo_count INTEGER NOT NULL," +
                    "combo_expire_at BIGINT NOT NULL," +
                    "combo_override_window DOUBLE NOT NULL," +
                    "combo_override_until BIGINT NOT NULL," +
                    "last_yaw REAL NOT NULL," +
                    "last_pitch REAL NOT NULL," +
                    "last_target_distance DOUBLE NOT NULL," +
                    "hoe_skills TEXT," +
                    "guild_id TEXT," +
                    "last_guild_harvest_at BIGINT NOT NULL" +
                    ")");
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to initialize player data store", ex);
        }
    }

    private String buildUpsertSql() {
        String placeholders = "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?";
        String insert = "INSERT INTO player_profiles (" + INSERT_COLUMNS + ") VALUES (" + placeholders + ")";
        return insert + " ON DUPLICATE KEY UPDATE " +
                "level=VALUES(level)," +
                "xp=VALUES(xp)," +
                "mastery=VALUES(mastery)," +
                "energy=VALUES(energy)," +
                "last_energy_tick=VALUES(last_energy_tick)," +
                "last_harvest_at=VALUES(last_harvest_at)," +
                "action_cooldown_end=VALUES(action_cooldown_end)," +
                "last_block_type=VALUES(last_block_type)," +
                "combo_count=VALUES(combo_count)," +
                "combo_expire_at=VALUES(combo_expire_at)," +
                "combo_override_window=VALUES(combo_override_window)," +
                "combo_override_until=VALUES(combo_override_until)," +
                "last_yaw=VALUES(last_yaw)," +
                "last_pitch=VALUES(last_pitch)," +
                "last_target_distance=VALUES(last_target_distance)," +
                "hoe_skills=VALUES(hoe_skills)," +
                "guild_id=VALUES(guild_id)," +
                "last_guild_harvest_at=VALUES(last_guild_harvest_at)";
    }

    @Override
    public CompletableFuture<Optional<PlayerProfile>> loadProfile(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = connectionProvider.get();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT " + INSERT_COLUMNS + " FROM player_profiles WHERE uuid = ?")) {
                statement.setString(1, uuid.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapProfile(uuid, resultSet));
                    }
                    return Optional.empty();
                }
            } catch (SQLException ex) {
                throw new CompletionException(ex);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> saveProfile(PlayerProfile profile) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = connectionProvider.get();
                 PreparedStatement statement = connection.prepareStatement(upsertSql)) {
                bindProfile(statement, profile);
                statement.executeUpdate();
            } catch (SQLException ex) {
                throw new CompletionException(ex);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> close() {
        executor.shutdownNow();
        try {
            closeHook.run();
        } catch (Exception ex) {
            throw new CompletionException(ex);
        }
        return CompletableFuture.completedFuture(null);
    }

    private PlayerProfile mapProfile(UUID uuid, ResultSet resultSet) throws SQLException {
        PlayerProfile profile = new PlayerProfile(uuid);
        profile.setLevel(resultSet.getInt("level"));
        profile.setXp(resultSet.getDouble("xp"));
        profile.setMastery(resultSet.getDouble("mastery"));
        profile.setEnergy(resultSet.getDouble("energy"));
        profile.setLastEnergyTick(resultSet.getLong("last_energy_tick"));
        profile.setLastHarvestAt(resultSet.getLong("last_harvest_at"));
        profile.setActionCooldownEnd(resultSet.getLong("action_cooldown_end"));
        profile.setLastBlockType(resultSet.getString("last_block_type"));
        profile.setComboCount(resultSet.getInt("combo_count"));
        profile.setComboExpireAt(resultSet.getLong("combo_expire_at"));

        double comboOverrideWindow = resultSet.getDouble("combo_override_window");
        long comboOverrideUntil = resultSet.getLong("combo_override_until");
        if (comboOverrideWindow > 0 && comboOverrideUntil > 0) {
            profile.setComboOverride(comboOverrideWindow, comboOverrideUntil);
        } else {
            profile.clearComboOverride();
        }

        profile.setLastYaw(resultSet.getFloat("last_yaw"));
        profile.setLastPitch(resultSet.getFloat("last_pitch"));
        profile.setLastTargetDistance(resultSet.getDouble("last_target_distance"));
        ProfileSkillCodec.decode(resultSet.getString("hoe_skills"), profile);
        profile.setGuildId(resultSet.getString("guild_id"));
        profile.setLastGuildHarvestAt(resultSet.getLong("last_guild_harvest_at"));
        return profile;
    }

    private void bindProfile(PreparedStatement statement, PlayerProfile profile) throws SQLException {
        statement.setString(1, profile.getUuid().toString());
        statement.setInt(2, profile.getLevel());
        statement.setDouble(3, profile.getXp());
        statement.setDouble(4, profile.getMastery());
        statement.setDouble(5, profile.getEnergy());
        statement.setLong(6, profile.getLastEnergyTick());
        statement.setLong(7, profile.getLastHarvestAt());
        statement.setLong(8, profile.getActionCooldownEnd());
        statement.setString(9, profile.getLastBlockType());
        statement.setInt(10, profile.getComboCount());
        statement.setLong(11, profile.getComboExpireAt());
        statement.setDouble(12, profile.getComboOverrideWindow());
        statement.setLong(13, profile.getComboOverrideUntil());
        statement.setFloat(14, profile.getLastYaw());
        statement.setFloat(15, profile.getLastPitch());
        statement.setDouble(16, profile.getLastTargetDistance());
        statement.setString(17, ProfileSkillCodec.encode(profile.getHoeSkills()));
        statement.setString(18, profile.getGuildId());
        statement.setLong(19, profile.getLastGuildHarvestAt());
    }
}
