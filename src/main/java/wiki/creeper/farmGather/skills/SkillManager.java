package wiki.creeper.farmGather.skills;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import wiki.creeper.farmGather.FarmGather;
import wiki.creeper.farmGather.config.PluginConfig;
import wiki.creeper.farmGather.player.HoeSkill;
import wiki.creeper.farmGather.player.HoeSkillType;
import wiki.creeper.farmGather.player.PlayerProfile;
import wiki.creeper.farmGather.player.ProfileManager;
import wiki.creeper.farmGather.util.ItemUtil;
import wiki.creeper.farmGather.util.Text;

public class SkillManager implements Listener {
    private final FarmGather plugin;
    private final ProfileManager profileManager;
    private final Map<UUID, PlayerSkillState> states = new ConcurrentHashMap<>();

    private PluginConfig.SkillsConfig config;
    private BukkitTask energyTask;

    public SkillManager(FarmGather plugin, ProfileManager profileManager, PluginConfig.SkillsConfig config) {
        this.plugin = plugin;
        this.profileManager = profileManager;
        this.config = config;
    }

    public void start() {
        stop();
        long periodTicks = 20L;
        this.energyTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickEnergy, periodTicks, periodTicks);
    }

    public void stop() {
        if (energyTask != null) {
            energyTask.cancel();
            energyTask = null;
        }
    }

    public void reload(PluginConfig.SkillsConfig config) {
        this.config = config;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        states.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> refreshActiveHoeSkill(event.getPlayer()));
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> refreshActiveHoeSkill(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLeftClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        if (!ItemUtil.isFarmHoe(player.getInventory().getItemInMainHand(), plugin)) {
            return;
        }
        event.setCancelled(true);
        PlayerProfile profile = profileManager.getProfile(player);
        if (profile == null) {
            return;
        }

        refreshActiveHoeSkill(player, profile);

        HoeSkill focusSkill = profile.getSkill(HoeSkillType.FOCUS);
        if (focusSkill != null && config.focus().enabled()) {
            handleFocus(player, profile, focusSkill);
        }
    }

    private void refreshActiveHoeSkill(Player player) {
        PlayerProfile profile = profileManager.getProfile(player);
        if (profile == null) {
            return;
        }
        refreshActiveHoeSkill(player, profile);
    }

    private void refreshActiveHoeSkill(Player player, PlayerProfile profile) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (!ItemUtil.isFarmHoe(mainHand, plugin)) {
            return;
        }
        ItemUtil.readHoeSkill(mainHand, plugin).ifPresent(profile::setSkill);
    }

    private void handleFocus(Player player, PlayerProfile profile, HoeSkill skill) {
        PlayerSkillState state = states.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerSkillState());
        long now = System.currentTimeMillis();
        long cooldownEnd = state.getCooldownEnd(HoeSkillType.FOCUS);
        if (now < cooldownEnd) {
            long remaining = cooldownEnd - now;
            player.sendActionBar(Text.colorize("&c" + (remaining / 1000 + 1) + "초 후 사용 가능"));
            return;
        }

        double energyCost = config.focus().energyCost();
        if (profile.getEnergy() < energyCost) {
            player.sendActionBar(Text.colorize(plugin.getPluginConfig().notifications().energyMissing()));
            return;
        }

        profile.setEnergy(Math.max(0, profile.getEnergy() - energyCost));
        profile.setLastEnergyTick(now);

        double duration = config.focus().durationBase() + (skill.getLevel() - 1) * config.focus().durationPerLevel();
        long focusUntil = now + (long) (duration * 1000);
        state.setFocusActiveUntil(focusUntil);
        profile.setComboOverride(3.0, focusUntil);

        long cooldownMillis = (long) (config.focus().cooldown() * 1000);
        state.setCooldownEnd(HoeSkillType.FOCUS, now + cooldownMillis);

        player.sendActionBar(Text.colorize("&b집중 스킬 발동!"));
    }

    private void tickEnergy() {
        double maxEnergy = config.energy().max();
        double regenAmount = config.energy().regenPerSec();
        boolean regenIfAfk = config.energy().regenIfAfk();
        long now = System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = profileManager.getProfile(player);
            if (profile == null) {
                continue;
            }
            double current = profile.getEnergy();
            if (current >= maxEnergy) {
                continue;
            }
            long lastAction = profile.getLastHarvestAt();
            if (!regenIfAfk && lastAction != 0 && (now - lastAction) > 60_000) {
                continue;
            }
            double next = Math.min(maxEnergy, current + regenAmount);
            profile.setEnergy(next);
            profile.setLastEnergyTick(now);
        }
    }
}
