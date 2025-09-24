package wiki.creeper.farmGather.storage;

import java.util.Map;
import wiki.creeper.farmGather.player.HoeSkill;
import wiki.creeper.farmGather.player.HoeSkillType;
import wiki.creeper.farmGather.player.PlayerProfile;

final class ProfileSkillCodec {
    private ProfileSkillCodec() {
    }

    static String encode(Map<HoeSkillType, HoeSkill> skills) {
        if (skills.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (HoeSkill skill : skills.values()) {
            if (skill.getLevel() <= 0) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(skill.getType().name()).append(':').append(skill.getLevel());
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    static void decode(String encoded, PlayerProfile profile) {
        profile.clearHoeSkills();
        if (encoded == null || encoded.isBlank()) {
            return;
        }
        String[] tokens = encoded.split(",");
        for (String token : tokens) {
            String[] pair = token.split(":");
            if (pair.length != 2) {
                continue;
            }
            try {
                HoeSkillType type = HoeSkillType.fromKey(pair[0]);
                int level = Integer.parseInt(pair[1]);
                if (level > 0) {
                    profile.setSkill(new HoeSkill(type, level));
                }
            } catch (IllegalArgumentException ignored) {
                // ignore malformed entries
            }
        }
    }
}
