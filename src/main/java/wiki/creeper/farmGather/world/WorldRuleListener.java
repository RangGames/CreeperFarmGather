package wiki.creeper.farmGather.world;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import wiki.creeper.farmGather.FarmGather;
import wiki.creeper.farmGather.config.PluginConfig;
import wiki.creeper.farmGather.util.ItemUtil;
import wiki.creeper.farmGather.util.Text;

public class WorldRuleListener implements Listener {
    private final FarmGather plugin;
    private final Set<String> restrictedWorlds = new HashSet<>();
    private final Set<String> commandWhitelist = new HashSet<>();

    public WorldRuleListener(FarmGather plugin) {
        this.plugin = plugin;
        reload(plugin.getPluginConfig());
    }

    public void reload(@NotNull PluginConfig config) {
        restrictedWorlds.clear();
        restrictedWorlds.addAll(config.harvest().worlds());
        commandWhitelist.clear();
        for (String entry : config.world().commandWhitelist()) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String normalized = entry.trim().toLowerCase(Locale.ROOT);
            if (!normalized.startsWith("/")) {
                normalized = "/" + normalized;
            }
            commandWhitelist.add(normalized);
        }
    }

    private boolean inRestrictedWorld(org.bukkit.World world) {
        return restrictedWorlds.contains(world.getName());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (inRestrictedWorld(event.getBlock().getWorld())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Text.colorize("&c이 월드에서는 블록을 설치할 수 없습니다."));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (inRestrictedWorld(event.getBlock().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }
        if (!inRestrictedWorld(event.getClickedBlock().getWorld())) {
            return;
        }

        boolean isHoe = ItemUtil.isFarmHoe(event.getPlayer().getInventory().getItemInMainHand(), plugin);
        switch (event.getAction()) {
            case RIGHT_CLICK_BLOCK -> {
                if (!isHoe) {
                    event.setCancelled(true);
                }
            }
            case LEFT_CLICK_BLOCK -> {
                if (!isHoe) {
                    event.setCancelled(true);
                }
            }
            default -> {
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (inRestrictedWorld(event.getBlock().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (inRestrictedWorld(event.getBlock().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        if (inRestrictedWorld(event.getBlock().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (event.getEntered() instanceof org.bukkit.entity.Player player && inRestrictedWorld(event.getVehicle().getWorld())) {
            event.setCancelled(true);
            player.sendMessage(Text.colorize("&c채집 월드에서는 탈것을 이용할 수 없습니다."));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent event) {
        if (event.getWorld() != null && inRestrictedWorld(event.getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!inRestrictedWorld(event.getPlayer().getWorld())) {
            return;
        }
        if (event.getPlayer().hasPermission("farmgather.bypass.worldrule")) {
            return;
        }
        String message = event.getMessage();
        if (message == null || message.isBlank()) {
            return;
        }
        String label = message.split("\\s+")[0].toLowerCase(Locale.ROOT);
        if (!label.startsWith("/")) {
            label = "/" + label;
        }
        if (!commandWhitelist.contains(label)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Text.colorize("&c채집 월드에서는 해당 명령을 사용할 수 없습니다."));
        }
    }
}
