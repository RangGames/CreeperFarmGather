package wiki.creeper.farmGather.player;

public class HoeSkill {
    private final HoeSkillType type;
    private int level;

    public HoeSkill(HoeSkillType type, int level) {
        this.type = type;
        this.level = level;
    }

    public HoeSkillType getType() {
        return type;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
