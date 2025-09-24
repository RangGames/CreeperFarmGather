package wiki.creeper.farmGather.harvest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.bukkit.event.inventory.InventoryType;
import wiki.creeper.farmGather.FarmGather;
import wiki.creeper.farmGather.api.event.FarmGatherHarvestEvent;
import wiki.creeper.farmGather.config.PluginConfig;
import wiki.creeper.farmGather.integration.GuildService;
import wiki.creeper.farmGather.player.HoeSkill;
import wiki.creeper.farmGather.player.HoeSkillType;
import wiki.creeper.farmGather.player.PlayerProfile;
import wiki.creeper.farmGather.player.ProfileManager;
import wiki.creeper.farmGather.progression.ComboService;
import wiki.creeper.farmGather.progression.ProgressionService;
import wiki.creeper.farmGather.ui.ComboBossBarService;
import wiki.creeper.farmGather.util.ItemUtil;
import wiki.creeper.farmGather.util.Text;

public class HarvestManager {
    private final FarmGather plugin;
    private final ProfileManager profileManager;
    private final ComboService comboService;
    private final ProgressionService progressionService;
    private final HarvestableRegistry harvestableRegistry;
    private final GuildService guildService;
    private final ComboBossBarService bossBarService;

    private PluginConfig config;
    private Set<String> harvestWorlds = new HashSet<>();
    private DropMode dropMode = DropMode.VIRTUAL;

    public HarvestManager(FarmGather plugin,
                          ProfileManager profileManager,
                          ComboService comboService,
                          ProgressionService progressionService,
                          HarvestableRegistry harvestableRegistry,
                          GuildService guildService,
                          ComboBossBarService bossBarService) {
        this.plugin = plugin;
        this.profileManager = profileManager;
        this.comboService = comboService;
        this.progressionService = progressionService;
        this.harvestableRegistry = harvestableRegistry;
        this.guildService = guildService;
        this.bossBarService = bossBarService;
        reload(plugin.getPluginConfig());
    }

    public void reload(PluginConfig config) {
        this.config = config;
        this.harvestWorlds = new HashSet<>(config.harvest().worlds());
        this.harvestableRegistry.rebuild(config.harvest().harvestableTags());
        this.dropMode = DropMode.valueOf(config.harvest().drop().mode().toUpperCase(Locale.ROOT));
    }

    public HarvestResult attemptHarvest(Player player, Block block) {
        PlayerProfile profile = profileManager.getProfile(player);
        if (profile == null) {
            return HarvestResult.failure(HarvestResult.FailReason.PROFILE_NOT_LOADED);
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!ItemUtil.isFarmHoe(tool, plugin)) {
            return HarvestResult.failure(HarvestResult.FailReason.INVALID_TOOL);
        }

        if (!harvestWorlds.contains(block.getWorld().getName())) {
            sendNotification(player, config.notifications().harvestDenied());
            return HarvestResult.failure(HarvestResult.FailReason.WRONG_WORLD);
        }

        Material blockType = block.getType();
        if (!harvestableRegistry.isHarvestable(blockType)) {
            return HarvestResult.failure(HarvestResult.FailReason.NOT_HARVESTABLE);
        }

        if (isPlayerBusy(player)) {
            return HarvestResult.failure(HarvestResult.FailReason.BUSY_STATE);
        }

        long now = System.currentTimeMillis();
        if (profile.getActionCooldownEnd() > 0 && now >= profile.getActionCooldownEnd()) {
            profile.setActionCooldownEnd(0);
        }
        if (now < profile.getActionCooldownEnd()) {
            sendNotification(player, config.notifications().cooldownActive());
            return HarvestResult.failure(HarvestResult.FailReason.ACTION_COOLDOWN);
        }

        Optional<Double> comboOverride = Optional.empty();
        if (profile.getComboOverrideUntil() > now) {
            comboOverride = Optional.of(profile.getComboOverrideWindow());
        } else if (profile.getComboOverrideUntil() != 0) {
            profile.clearComboOverride();
        }

        String blockKey = blockType.name();
        long evalTime = now;

        String previousBlockType = profile.getLastBlockType();
        long previousHarvest = profile.getLastHarvestAt();
        int previousComboCount = profile.getComboCount();
        long previousComboExpireAt = profile.getComboExpireAt();

        ComboService.ComboResult comboResult = comboService.applyCombo(profile, blockKey, evalTime, profile.getLevel(), comboOverride);
        int comboCount = comboResult.comboCount();
        double comboWindowSeconds = comboResult.windowSeconds();

        double xpBonusPercent = Math.min(Math.max(0, comboCount - 1) * config.combo().xpBonusPerStack(), config.combo().xpBonusCap());
        double xpBase = config.xp().basePerHarvest();
        int xpGained = (int) Math.round(xpBase * (1 + xpBonusPercent));

        boolean guildXpBonus = false;
        GuildService.GuildContext guildContext = guildService.evaluateContext(player, profile);
        if (guildContext.sameWorldActive()) {
            PluginConfig.XpConfig.GuildBonusConfig guildBonusConfig = config.xp().guildBonus();
            double chance = guildBonusConfig.baseChance() + guildBonusConfig.perMemberInc() * guildContext.activeMembers();
            chance = Math.min(chance, guildBonusConfig.chanceCap());
            if (ThreadLocalRandom.current().nextDouble() < chance) {
                xpGained += 1;
                guildXpBonus = true;
            }
        }

        boolean extraDrop = computeExtraDrop(profile.getLevel(), comboCount);

        HoeSkill doubleTapSkill = profile.getSkill(HoeSkillType.DOUBLETAP);
        boolean doubleTapTriggered = false;
        double doubleTapMultiplier = 1.0;
        PluginConfig.SkillsConfig.DoubleTapConfig doubleTapConfig = config.skills().doubleTap();
        if (doubleTapSkill != null && doubleTapConfig.enabled() && doubleTapSkill.getLevel() > 0) {
            double windowSec = doubleTapConfig.windowSec();
            if (blockKey.equals(previousBlockType) && (now - previousHarvest) <= windowSec * 1000) {
                double chance = doubleTapConfig.chanceBase() + doubleTapConfig.chancePerLevel() * (doubleTapSkill.getLevel() - 1);
                chance = Math.min(chance, 0.50);
                if (ThreadLocalRandom.current().nextDouble() < chance) {
                    doubleTapTriggered = true;
                    if (config.extraDrop().doubletapMultiplies()) {
                        doubleTapMultiplier = 1.3 + 0.05 * (doubleTapSkill.getLevel() - 1);
                    } else {
                        doubleTapMultiplier = 2.0;
                    }
                }
            }
        }

        List<ItemStack> preliminaryDrops = buildDrops(player, block, tool, extraDrop, doubleTapTriggered, doubleTapMultiplier);

        FarmGatherHarvestEvent harvestEvent = new FarmGatherHarvestEvent(
                player,
                profile,
                block,
                comboCount,
                comboWindowSeconds,
                xpBonusPercent,
                xpGained,
                guildXpBonus,
                extraDrop,
                doubleTapTriggered,
                doubleTapMultiplier,
                preliminaryDrops
        );

        plugin.getServer().getPluginManager().callEvent(harvestEvent);

        if (harvestEvent.isCancelled()) {
            profile.setComboCount(previousComboCount);
            profile.setComboExpireAt(previousComboExpireAt);
            profile.setLastBlockType(previousBlockType);
            return HarvestResult.failure(HarvestResult.FailReason.CANCELLED);
        }

        comboCount = harvestEvent.getComboCount();
        comboWindowSeconds = harvestEvent.getComboWindowSeconds();
        xpBonusPercent = harvestEvent.getXpBonusPercent();
        xpGained = Math.max(0, harvestEvent.getXpGained());
        guildXpBonus = harvestEvent.isGuildBonusApplied();
        extraDrop = harvestEvent.isExtraDrop();
        doubleTapTriggered = harvestEvent.isDoubleTapTriggered();
        doubleTapMultiplier = Math.max(1.0, harvestEvent.getDoubleTapMultiplier());
        List<ItemStack> finalDrops = cloneDrops(harvestEvent.getDrops());

        profile.setComboCount(comboCount);
        profile.setComboExpireAt(now + (long) (Math.max(comboWindowSeconds, 0) * 1000L));

        boolean inventoryFull = distributeDrops(player, finalDrops);

        block.setType(Material.AIR, false);

        ProgressionService.LevelUpResult levelUpResult = xpGained > 0
                ? progressionService.addXp(profile, xpGained)
                : ProgressionService.LevelUpResult.noChange(profile.getLevel(), profile.getXp());

        profile.setLastHarvestAt(now);
        long cooldownMillis = (long) (config.harvest().actionCooldownSec() * 1000);
        profile.setActionCooldownEnd(now + cooldownMillis);
        profile.setLastGuildHarvestAt(now);

        bossBarService.handleHarvest(player, profile, comboCount, comboWindowSeconds, xpBonusPercent, xpGained);

        if (inventoryFull) {
            sendNotification(player, config.notifications().inventoryFull());
        }

        return new HarvestResult(
                true,
                null,
                comboCount,
                comboWindowSeconds,
                xpGained,
                guildXpBonus,
                extraDrop,
                doubleTapTriggered,
                doubleTapMultiplier,
                finalDrops,
                levelUpResult
        );
    }

    private boolean computeExtraDrop(int level, int combo) {
        PluginConfig.ExtraDropConfig extra = config.extraDrop();
        double chance = extra.base() + extra.perCombo() * combo + extra.perLevelLog2() * log2(level + 1);
        chance = Math.min(chance, extra.hardCap());
        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    private double log2(int value) {
        return Math.log(value) / Math.log(2);
    }

    private List<ItemStack> buildDrops(Player player, Block block, ItemStack tool, boolean extraDrop, boolean doubleTapTriggered, double doubleTapMultiplier) {
        List<ItemStack> drops = new ArrayList<>(block.getDrops(tool, player));
        if (extraDrop && !drops.isEmpty()) {
            ItemStack template = drops.get(0).clone();
            template.setAmount(1);
            drops.add(template);
        }

        if (doubleTapTriggered && doubleTapMultiplier > 1.0) {
            for (int i = 0; i < drops.size(); i++) {
                ItemStack stack = drops.get(i);
                int newAmount = (int) Math.ceil(stack.getAmount() * doubleTapMultiplier);
                if (newAmount < 1) {
                    newAmount = 1;
                }
                ItemStack clone = stack.clone();
                clone.setAmount(newAmount);
                drops.set(i, clone);
            }
        } else {
            for (int i = 0; i < drops.size(); i++) {
                drops.set(i, drops.get(i).clone());
            }
        }
        return drops;
    }

    private boolean distributeDrops(Player player, List<ItemStack> drops) {
        if (drops.isEmpty()) {
            return false;
        }

        if (dropMode == DropMode.GROUND) {
            dropToGround(player, drops);
            return false;
        }

        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(drops.toArray(ItemStack[]::new));
        if (leftovers.isEmpty()) {
            return false;
        }

        dropToGround(player, leftovers.values().stream().toList());
        return true;
    }

    private List<ItemStack> cloneDrops(List<ItemStack> drops) {
        List<ItemStack> copy = new ArrayList<>();
        if (drops == null) {
            return copy;
        }
        for (ItemStack stack : drops) {
            if (stack == null) {
                continue;
            }
            copy.add(stack.clone());
        }
        return copy;
    }

    private void dropToGround(Player player, List<ItemStack> stacks) {
        Location location = player.getLocation().add(0, 0.5, 0);
        for (ItemStack stack : stacks) {
            Item item = player.getWorld().dropItem(location, stack, entity -> {
                entity.setCanPlayerPickup(true);
                entity.setOwner(player.getUniqueId());
                entity.setPickupDelay(10);
            });
            item.setVelocity(new Vector(0, 0.1, 0));
            item.setThrower(player.getUniqueId());
            item.setGlowing(false);
            item.setUnlimitedLifetime(false);
            item.setCustomNameVisible(false);
            item.setTicksLived(1);
            item.setGravity(true);
        }
    }

    private void sendNotification(Player player, String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        player.sendMessage(Text.colorize(message));
    }

    private boolean isPlayerBusy(Player player) {
        if (player.isSleeping() || player.isDead() || player.isConversing()) {
            return true;
        }
        InventoryType openType = player.getOpenInventory().getType();
        return openType != InventoryType.CRAFTING && openType != InventoryType.CREATIVE;
    }

    private enum DropMode {
        VIRTUAL,
        GROUND
    }
}
