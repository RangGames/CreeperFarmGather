package wiki.creeper.farmGather.player;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class PlayerProfile {
    private final UUID uuid;
    private int level;
    private double xp;
    private double mastery;

    private double energy;
    private long lastEnergyTick;

    private long lastHarvestAt;
    private long actionCooldownEnd;

    private String lastBlockType;
    private int comboCount;
    private long comboExpireAt;

    private double comboOverrideWindow;
    private long comboOverrideUntil;

    private float lastYaw;
    private float lastPitch;
    private double lastTargetDistance;

    private final Map<HoeSkillType, HoeSkill> hoeSkills = new EnumMap<>(HoeSkillType.class);

    private String guildId;
    private long lastGuildHarvestAt;

    public PlayerProfile(UUID uuid) {
        this.uuid = uuid;
        this.level = 1;
        this.energy = 100.0;
        this.lastEnergyTick = Instant.now().toEpochMilli();
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public double getXp() {
        return xp;
    }

    public void setXp(double xp) {
        this.xp = xp;
    }

    public void addXp(double amount) {
        this.xp += amount;
    }

    public double getMastery() {
        return mastery;
    }

    public void setMastery(double mastery) {
        this.mastery = mastery;
    }

    public double getEnergy() {
        return energy;
    }

    public void setEnergy(double energy) {
        this.energy = energy;
    }

    public long getLastEnergyTick() {
        return lastEnergyTick;
    }

    public void setLastEnergyTick(long lastEnergyTick) {
        this.lastEnergyTick = lastEnergyTick;
    }

    public long getLastHarvestAt() {
        return lastHarvestAt;
    }

    public void setLastHarvestAt(long lastHarvestAt) {
        this.lastHarvestAt = lastHarvestAt;
    }

    public long getActionCooldownEnd() {
        return actionCooldownEnd;
    }

    public void setActionCooldownEnd(long actionCooldownEnd) {
        this.actionCooldownEnd = actionCooldownEnd;
    }

    public String getLastBlockType() {
        return lastBlockType;
    }

    public void setLastBlockType(String lastBlockType) {
        this.lastBlockType = lastBlockType;
    }

    public int getComboCount() {
        return comboCount;
    }

    public void setComboCount(int comboCount) {
        this.comboCount = comboCount;
    }

    public long getComboExpireAt() {
        return comboExpireAt;
    }

    public void setComboExpireAt(long comboExpireAt) {
        this.comboExpireAt = comboExpireAt;
    }

    public float getLastYaw() {
        return lastYaw;
    }

    public void setLastYaw(float lastYaw) {
        this.lastYaw = lastYaw;
    }

    public float getLastPitch() {
        return lastPitch;
    }

    public void setLastPitch(float lastPitch) {
        this.lastPitch = lastPitch;
    }

    public double getLastTargetDistance() {
        return lastTargetDistance;
    }

    public void setLastTargetDistance(double lastTargetDistance) {
        this.lastTargetDistance = lastTargetDistance;
    }

    public HoeSkill getSkill(HoeSkillType type) {
        return hoeSkills.get(type);
    }

    public void setSkill(HoeSkill skill) {
        hoeSkills.put(skill.getType(), skill);
    }

    public Map<HoeSkillType, HoeSkill> getHoeSkills() {
        return hoeSkills;
    }

    public void clearHoeSkills() {
        hoeSkills.clear();
    }

    public String getGuildId() {
        return guildId;
    }

    public void setGuildId(String guildId) {
        this.guildId = guildId;
    }

    public long getLastGuildHarvestAt() {
        return lastGuildHarvestAt;
    }

    public void setLastGuildHarvestAt(long lastGuildHarvestAt) {
        this.lastGuildHarvestAt = lastGuildHarvestAt;
    }

    public void setComboOverride(double windowSeconds, long untilMillis) {
        this.comboOverrideWindow = windowSeconds;
        this.comboOverrideUntil = untilMillis;
    }

    public double getComboOverrideWindow() {
        return comboOverrideWindow;
    }

    public long getComboOverrideUntil() {
        return comboOverrideUntil;
    }

    public void clearComboOverride() {
        this.comboOverrideWindow = 0;
        this.comboOverrideUntil = 0;
    }
}
