package wiki.creeper.farmGather.ui;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;
import wiki.creeper.farmGather.FarmGather;
import wiki.creeper.farmGather.config.PluginConfig;
import wiki.creeper.farmGather.player.HoeSkillType;
import wiki.creeper.farmGather.skills.SkillManager;
import wiki.creeper.farmGather.util.ItemUtil;
import wiki.creeper.farmGather.util.Text;

public class CooldownUiService implements Listener {
    private static final EnumSet<HoeSkillType> TRACKED_SKILLS = EnumSet.of(HoeSkillType.SWEEP, HoeSkillType.FOCUS, HoeSkillType.SHEARS);

    private final FarmGather plugin;
    private final SkillManager skillManager;

    private PluginConfig.CooldownUiConfig config;
    private PluginConfig.CooldownUiConfig.CooldownUiMode activeMode;
    private ItemCountOverlay overlay;
    private BukkitTask task;
    private final Map<UUID, Integer> lastPacketAmounts = new ConcurrentHashMap<>();

    public CooldownUiService(FarmGather plugin,
                             SkillManager skillManager,
                             PluginConfig.CooldownUiConfig config) {
        this.plugin = plugin;
        this.skillManager = skillManager;
        this.config = config;
        this.overlay = ItemCountOverlay.noop();
        this.activeMode = PluginConfig.CooldownUiConfig.CooldownUiMode.DISABLED;
    }

    public void start() {
        stop();
        evaluateMode();
        int period = Math.max(1, switch (activeMode) {
            case PACKET_COUNT -> config.packet().resendTicks();
            case LOCKED_HUD_SLOT -> config.packet().resendTicks();
            case DISABLED -> 20;
        });
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, period, period);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        lastPacketAmounts.clear();
    }

    public void reload(PluginConfig.CooldownUiConfig config) {
        this.config = config;
        start();
    }

    private void evaluateMode() {
        PluginConfig.CooldownUiConfig.CooldownUiMode requested = config.mode();
        if (requested == PluginConfig.CooldownUiConfig.CooldownUiMode.PACKET_COUNT) {
            Optional<ItemCountOverlay> packetOverlay = tryCreatePacketOverlay();
            if (packetOverlay.isPresent()) {
                this.overlay = packetOverlay.get();
                this.activeMode = PluginConfig.CooldownUiConfig.CooldownUiMode.PACKET_COUNT;
                plugin.getLogger().info("Cooldown UI using PACKET_COUNT mode.");
                return;
            }
            plugin.getLogger().warning("ProtocolLib not detected; falling back to LOCKED_HUD_SLOT cooldown UI.");
            requested = PluginConfig.CooldownUiConfig.CooldownUiMode.LOCKED_HUD_SLOT;
        }

        if (requested == PluginConfig.CooldownUiConfig.CooldownUiMode.LOCKED_HUD_SLOT) {
            this.overlay = ItemCountOverlay.noop();
            this.activeMode = PluginConfig.CooldownUiConfig.CooldownUiMode.LOCKED_HUD_SLOT;
            plugin.getLogger().info("Cooldown UI using LOCKED_HUD_SLOT mode.");
            return;
        }

        this.overlay = ItemCountOverlay.noop();
        this.activeMode = PluginConfig.CooldownUiConfig.CooldownUiMode.DISABLED;
        plugin.getLogger().info("Cooldown UI disabled.");
    }

    private Optional<ItemCountOverlay> tryCreatePacketOverlay() {
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new ProtocolLibItemCountOverlay());
        } catch (NoClassDefFoundError | Exception ex) {
            plugin.getLogger().warning("Failed to initialise ProtocolLib overlay; " + ex.getMessage());
            return Optional.empty();
        }
    }

    private void tick() {
        switch (activeMode) {
            case PACKET_COUNT -> handlePacketMode();
            case LOCKED_HUD_SLOT -> handleHudMode();
            case DISABLED -> {}
        }
    }

    private void handlePacketMode() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePacketForPlayer(player);
        }
    }

    private void handleHudMode() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ensureHudToken(player, computeDisplayAmount(player));
        }
    }

    private void updatePacketForPlayer(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        UUID uuid = player.getUniqueId();
        int slotIndex = 36 + player.getInventory().getHeldItemSlot();
        if (!ItemUtil.isFarmHoe(mainHand, plugin)) {
            Integer previous = lastPacketAmounts.remove(uuid);
            if (previous != null) {
                ItemStack baseline = sanitizeBaseline(mainHand);
                int amount = Math.max(1, baseline.getAmount());
                overlay.update(player, slotIndex, baseline, amount);
            }
            return;
        }

        int amount = computeDisplayAmount(player);
        Integer previous = lastPacketAmounts.get(uuid);
        if (previous != null && previous == amount) {
            return;
        }
        ItemStack template = sanitizeBaseline(mainHand);
        overlay.update(player, slotIndex, template, amount);
        lastPacketAmounts.put(uuid, amount);
    }

    private ItemStack sanitizeBaseline(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return new ItemStack(Material.AIR);
        }
        ItemStack copy = item.clone();
        copy.setAmount(Math.max(1, copy.getAmount()));
        return copy;
    }

    private int computeDisplayAmount(Player player) {
        double remaining = computeRemainingSeconds(player);
        int clamp = Math.max(1, config.packet().clampMax());
        if (remaining <= 0.0) {
            return 1;
        }
        return (int) Math.min(clamp, Math.ceil(remaining));
    }

    private double computeRemainingSeconds(Player player) {
        Map<HoeSkillType, Double> cooldowns = skillManager.snapshotCooldowns(player);
        if (cooldowns.isEmpty()) {
            return 0.0;
        }
        PluginConfig.CooldownUiConfig.HudConfig.ShowSkill show = config.hud().showSkill();
        return switch (show) {
            case LONGEST -> cooldowns.entrySet().stream()
                    .filter(entry -> TRACKED_SKILLS.contains(entry.getKey()))
                    .mapToDouble(Map.Entry::getValue)
                    .max()
                    .orElse(0.0);
            case SWEEP -> cooldowns.getOrDefault(HoeSkillType.SWEEP, 0.0);
            case FOCUS -> cooldowns.getOrDefault(HoeSkillType.FOCUS, 0.0);
            case SHEARS -> cooldowns.getOrDefault(HoeSkillType.SHEARS, 0.0);
        };
    }

    private void ensureHudToken(Player player, int amount) {
        PlayerInventory inventory = player.getInventory();
        int slot = Math.min(8, Math.max(0, config.hud().slot()));
        ItemStack existing = inventory.getItem(slot);
        if (!ItemUtil.isHudToken(existing, plugin)) {
            existing = createHudToken();
        } else {
            existing = existing.clone();
        }
        applyHudPresentation(existing, amount);
        inventory.setItem(slot, existing);
    }

    private ItemStack createHudToken() {
        Material material = Material.matchMaterial(config.hud().material());
        if (material == null || material.isAir()) {
            material = Material.PAPER;
        }
        ItemStack item = new ItemStack(material);
        ItemUtil.markHudToken(item, plugin);
        return item;
    }

    private void applyHudPresentation(ItemStack item, int amount) {
        if (item == null) {
            return;
        }
        item.setAmount(Math.max(1, Math.min(config.packet().clampMax(), amount)));
        var meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        String nameTemplate = config.hud().name();
        meta.displayName(Text.colorize(replaceSec(nameTemplate, amount)));
        java.util.List<String> loreTemplates = config.hud().lore();
        if (!loreTemplates.isEmpty()) {
            java.util.List<net.kyori.adventure.text.Component> loreComponents = new java.util.ArrayList<>();
            for (String line : loreTemplates) {
                loreComponents.add(Text.colorize(replaceSec(line, amount)));
            }
            meta.lore(loreComponents);
        } else {
            meta.lore(null);
        }
        item.setItemMeta(meta);
    }

    private String replaceSec(String template, int seconds) {
        return template.replace("{sec}", String.valueOf(seconds));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (activeMode == PluginConfig.CooldownUiConfig.CooldownUiMode.LOCKED_HUD_SLOT) {
            Bukkit.getScheduler().runTask(plugin, () -> ensureHudToken(event.getPlayer(), computeDisplayAmount(event.getPlayer())));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastPacketAmounts.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (activeMode != PluginConfig.CooldownUiConfig.CooldownUiMode.LOCKED_HUD_SLOT) {
            return;
        }
        if (!config.hud().protect()) {
            return;
        }
        if (ItemUtil.isHudToken(event.getItemDrop().getItemStack(), plugin)) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> ensureHudToken(event.getPlayer(), computeDisplayAmount(event.getPlayer())));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        if (activeMode != PluginConfig.CooldownUiConfig.CooldownUiMode.LOCKED_HUD_SLOT) {
            return;
        }
        event.getDrops().removeIf(stack -> ItemUtil.isHudToken(stack, plugin));
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (activeMode != PluginConfig.CooldownUiConfig.CooldownUiMode.LOCKED_HUD_SLOT) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> ensureHudToken(event.getPlayer(), computeDisplayAmount(event.getPlayer())));
    }

    private interface ItemCountOverlay {
        void update(Player player, int slot, ItemStack template, int amount);

        static ItemCountOverlay noop() {
            return (player, slot, template, amount) -> {};
        }
    }

    private static final class ProtocolLibItemCountOverlay implements ItemCountOverlay {
        private final com.comphenix.protocol.ProtocolManager manager;

        private ProtocolLibItemCountOverlay() {
            this.manager = com.comphenix.protocol.ProtocolLibrary.getProtocolManager();
        }

        @Override
        public void update(Player player, int slot, ItemStack template, int amount) {
            try {
                var packet = manager.createPacket(com.comphenix.protocol.PacketType.Play.Server.SET_SLOT);
                packet.getIntegers().write(0, 0); // container id 0 = player inventory
                packet.getIntegers().write(1, 0);
                packet.getIntegers().write(2, slot);
                ItemStack fake = template.clone();
                fake.setAmount(Math.max(1, amount));
                packet.getItemModifier().write(0, fake);
                manager.sendServerPacket(player, packet);
            } catch (Exception ex) {
                Bukkit.getLogger().warning("Failed to send cooldown overlay packet: " + ex.getMessage());
            }
        }
    }
}
