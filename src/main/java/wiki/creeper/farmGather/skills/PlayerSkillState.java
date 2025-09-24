package wiki.creeper.farmGather.skills;

import java.util.EnumMap;
import java.util.Map;
import wiki.creeper.farmGather.player.HoeSkillType;

class PlayerSkillState {
    private final Map<HoeSkillType, Long> cooldowns = new EnumMap<>(HoeSkillType.class);
    private long focusActiveUntil;

    public long getCooldownEnd(HoeSkillType type) {
        return cooldowns.getOrDefault(type, 0L);
    }

    public void setCooldownEnd(HoeSkillType type, long end) {
        cooldowns.put(type, end);
    }

    public long getFocusActiveUntil() {
        return focusActiveUntil;
    }

    public void setFocusActiveUntil(long focusActiveUntil) {
        this.focusActiveUntil = focusActiveUntil;
    }
}
