package wiki.creeper.farmGather.player;

public enum HoeSkillType {
    SWEEP,
    FOCUS,
    DOUBLETAP,
    SHEARS;

    public static HoeSkillType fromKey(String key) {
        for (HoeSkillType type : values()) {
            if (type.name().equalsIgnoreCase(key)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown hoe skill type: " + key);
    }
}
