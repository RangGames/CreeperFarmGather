package wiki.creeper.farmGather.api.event;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wiki.creeper.farmGather.player.PlayerProfile;

public class FarmGatherHarvestEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final PlayerProfile profile;
    private final Block block;
    private ItemStack tool;
    private String hoeUid;
    private int comboCount;
    private double comboWindowSeconds;
    private double xpBonusPercent;
    private int xpGained;
    private boolean guildBonusApplied;
    private boolean extraDrop;
    private boolean doubleTapTriggered;
    private double doubleTapMultiplier;
    private List<ItemStack> drops;
    private boolean cancelled;

    public FarmGatherHarvestEvent(@NotNull Player player,
                                  @NotNull PlayerProfile profile,
                                  @NotNull Block block,
                                  @Nullable ItemStack tool,
                                  @Nullable String hoeUid,
                                  int comboCount,
                                  double comboWindowSeconds,
                                  double xpBonusPercent,
                                  int xpGained,
                                  boolean guildBonusApplied,
                                  boolean extraDrop,
                                  boolean doubleTapTriggered,
                                  double doubleTapMultiplier,
                                  @NotNull List<ItemStack> drops) {
        super(player);
        this.profile = profile;
        this.block = block;
        setTool(tool);
        this.hoeUid = hoeUid;
        this.comboCount = comboCount;
        this.comboWindowSeconds = comboWindowSeconds;
        this.xpBonusPercent = xpBonusPercent;
        this.xpGained = xpGained;
        this.guildBonusApplied = guildBonusApplied;
        this.extraDrop = extraDrop;
        this.doubleTapTriggered = doubleTapTriggered;
        this.doubleTapMultiplier = doubleTapMultiplier;
        setDrops(drops);
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    public PlayerProfile getProfile() {
        return profile;
    }

    public Block getBlock() {
        return block;
    }

    public ItemStack getTool() {
        return tool == null ? null : tool.clone();
    }

    public void setTool(@Nullable ItemStack tool) {
        this.tool = tool == null ? null : tool.clone();
    }

    public String getHoeUid() {
        return hoeUid;
    }

    public void setHoeUid(@Nullable String hoeUid) {
        this.hoeUid = hoeUid;
    }

    public int getComboCount() {
        return comboCount;
    }

    public void setComboCount(int comboCount) {
        this.comboCount = comboCount;
    }

    public double getComboWindowSeconds() {
        return comboWindowSeconds;
    }

    public void setComboWindowSeconds(double comboWindowSeconds) {
        this.comboWindowSeconds = comboWindowSeconds;
    }

    public double getXpBonusPercent() {
        return xpBonusPercent;
    }

    public void setXpBonusPercent(double xpBonusPercent) {
        this.xpBonusPercent = xpBonusPercent;
    }

    public int getXpGained() {
        return xpGained;
    }

    public void setXpGained(int xpGained) {
        this.xpGained = xpGained;
    }

    public boolean isGuildBonusApplied() {
        return guildBonusApplied;
    }

    public void setGuildBonusApplied(boolean guildBonusApplied) {
        this.guildBonusApplied = guildBonusApplied;
    }

    public boolean isExtraDrop() {
        return extraDrop;
    }

    public void setExtraDrop(boolean extraDrop) {
        this.extraDrop = extraDrop;
    }

    public boolean isDoubleTapTriggered() {
        return doubleTapTriggered;
    }

    public void setDoubleTapTriggered(boolean doubleTapTriggered) {
        this.doubleTapTriggered = doubleTapTriggered;
    }

    public double getDoubleTapMultiplier() {
        return doubleTapMultiplier;
    }

    public void setDoubleTapMultiplier(double doubleTapMultiplier) {
        this.doubleTapMultiplier = doubleTapMultiplier;
    }

    public List<ItemStack> getDrops() {
        return drops;
    }

    public void setDrops(@NotNull List<ItemStack> drops) {
        this.drops = new ArrayList<>(drops.size());
        for (ItemStack stack : drops) {
            if (stack == null) {
                continue;
            }
            this.drops.add(stack.clone());
        }
    }
}
