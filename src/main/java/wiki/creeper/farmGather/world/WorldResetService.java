package wiki.creeper.farmGather.world;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import wiki.creeper.farmGather.FarmGather;
import wiki.creeper.farmGather.config.PluginConfig;
import wiki.creeper.farmGather.util.Text;

public class WorldResetService {
    private final FarmGather plugin;
    private PluginConfig.WorldConfig worldConfig;

    public WorldResetService(FarmGather plugin, PluginConfig.WorldConfig worldConfig) {
        this.plugin = plugin;
        this.worldConfig = worldConfig;
    }

    public void reload(PluginConfig.WorldConfig config) {
        this.worldConfig = config;
    }

    public void resetNow(CommandSender sender) {
        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        scheduler.runTask(plugin, () -> performReset(sender));
    }

    private void performReset(CommandSender sender) {
        PluginConfig.WorldConfig.WorldResetConfig resetConfig = worldConfig.reset();
        List<String> worlds = plugin.getPluginConfig().harvest().worlds();
        if (worlds.isEmpty()) {
            sender.sendMessage(Text.colorize("&c리셋할 채집 월드가 없습니다."));
            return;
        }

        Location fallback = parseTeleport(resetConfig.teleportTarget());
        List<String> failed = new ArrayList<>();

        for (String worldName : worlds) {
            World world = Bukkit.getWorld(worldName);
            Path worldFolder = plugin.getServer().getWorldContainer().toPath().resolve(worldName);
            if (world != null) {
                teleportPlayers(world, fallback);
                boolean unloaded = Bukkit.unloadWorld(world, false);
                if (!unloaded) {
                    plugin.getLogger().warning("Failed to unload world " + worldName);
                    failed.add(worldName);
                    continue;
                }
            }

            try {
                deleteWorldFolder(worldFolder);
            } catch (IOException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to delete world folder " + worldName, ex);
                failed.add(worldName);
                continue;
            }

            WorldCreator creator = WorldCreator.name(worldName);
            World newWorld = creator.createWorld();
            if (newWorld == null) {
                plugin.getLogger().warning("Failed to recreate world " + worldName);
                failed.add(worldName);
                continue;
            }
            if (resetConfig.pregenerate()) {
                plugin.getLogger().info("Pregenerate enabled but not implemented; skipping for world " + worldName);
            }
        }

        if (failed.isEmpty()) {
            sender.sendMessage(Text.colorize("&a채집 월드 리셋이 완료되었습니다."));
        } else {
            sender.sendMessage(Text.colorize("&c일부 월드 리셋에 실패했습니다: " + String.join(", ", failed)));
        }
    }

    private void teleportPlayers(World world, Location fallback) {
        for (Player player : world.getPlayers()) {
            if (fallback != null) {
                player.teleport(fallback);
            } else {
                player.teleport(world.getSpawnLocation());
            }
            player.sendMessage(Text.colorize("&e월드 리셋으로 인해 이동되었습니다."));
        }
    }

    private void deleteWorldFolder(Path worldFolder) throws IOException {
        if (!Files.exists(worldFolder)) {
            return;
        }
        List<Path> paths = Files.walk(worldFolder)
                .sorted((a, b) -> Integer.compare(b.getNameCount(), a.getNameCount()))
                .toList();
        for (Path path : paths) {
            Files.deleteIfExists(path);
        }
    }

    private Location parseTeleport(String spec) {
        if (spec == null || spec.isBlank()) {
            return null;
        }
        try {
            String[] parts = spec.split(":");
            if (parts.length != 4) {
                return null;
            }
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) {
                return null;
            }
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            return new Location(world, x, y, z);
        } catch (NumberFormatException ex) {
            plugin.getLogger().log(Level.WARNING, "Invalid teleport target: " + spec, ex);
            return null;
        }
    }
}
