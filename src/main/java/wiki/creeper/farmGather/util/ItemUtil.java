package wiki.creeper.farmGather.util;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import wiki.creeper.farmGather.FarmGather;
import wiki.creeper.farmGather.player.HoeSkill;
import wiki.creeper.farmGather.player.HoeSkillType;

public final class ItemUtil {
    private static final String HOE_TAG = "hoe";
    private static final String UID_TAG = "uid";
    private static final String OWNER_TAG = "owner";
    private static final String HOE_SKILL_TYPE_TAG = "hoe_skill_type";
    private static final String HOE_SKILL_LEVEL_TAG = "hoe_skill_level";
    private static final String HUD_TOKEN_TAG = "hud_token";

    private ItemUtil() {
    }

    public static boolean isFarmHoe(ItemStack item, FarmGather plugin) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        Byte value = container.get(key(plugin, HOE_TAG), PersistentDataType.BYTE);
        return value != null && value == 1;
    }

    public static void applyHoeSkill(ItemStack item,
                                     HoeSkillType type,
                                     int level,
                                     FarmGather plugin,
                                     UUID owner) {
        if (item == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(key(plugin, HOE_TAG), PersistentDataType.BYTE, (byte) 1);
        ensureUidTag(container, plugin);
        container.set(key(plugin, HOE_SKILL_TYPE_TAG), PersistentDataType.STRING, type.name().toLowerCase(Locale.ROOT));
        container.set(key(plugin, HOE_SKILL_LEVEL_TAG), PersistentDataType.INTEGER, level);
        maybeSetOwner(container, owner, plugin);

        Component displayName = Text.colorize(String.format("&b%s 호미 &7(Lv.%d)", localizeSkill(type), level));
        meta.displayName(displayName);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
    }

    public static Optional<HoeSkill> readHoeSkill(ItemStack item, FarmGather plugin) {
        if (item == null || item.getType() == Material.AIR) {
            return Optional.empty();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String typeId = container.get(key(plugin, HOE_SKILL_TYPE_TAG), PersistentDataType.STRING);
        Integer level = container.get(key(plugin, HOE_SKILL_LEVEL_TAG), PersistentDataType.INTEGER);
        if (typeId == null || level == null) {
            return Optional.empty();
        }

        try {
            HoeSkillType type = HoeSkillType.fromKey(typeId);
            if (level <= 0) {
                return Optional.empty();
            }
            return Optional.of(new HoeSkill(type, level));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public static ItemStack createBasicHoe(FarmGather plugin, UUID owner) {
        ItemStack hoe = new ItemStack(Material.IRON_HOE);
        ItemMeta meta = hoe.getItemMeta();
        if (meta != null) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(key(plugin, HOE_TAG), PersistentDataType.BYTE, (byte) 1);
            ensureUidTag(container, plugin);
            maybeSetOwner(container, owner, plugin);
            meta.displayName(Text.colorize("&a기본 채집 호미"));
            meta.lore(List.of(Text.colorize("&7채집 전용 호미입니다.")));
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            hoe.setItemMeta(meta);
        }
        return hoe;
    }

    public static Optional<String> readUid(ItemStack item, FarmGather plugin) {
        if (item == null) {
            return Optional.empty();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        String value = meta.getPersistentDataContainer().get(key(plugin, UID_TAG), PersistentDataType.STRING);
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    public static boolean hasUid(ItemStack item, FarmGather plugin) {
        return readUid(item, plugin).isPresent();
    }

    public static Optional<UUID> readOwner(ItemStack item, FarmGather plugin) {
        if (item == null) {
            return Optional.empty();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        String value = meta.getPersistentDataContainer().get(key(plugin, OWNER_TAG), PersistentDataType.STRING);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public static void ensureOwner(ItemStack item, UUID owner, FarmGather plugin) {
        if (item == null || owner == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        maybeSetOwner(container, owner, plugin);
        item.setItemMeta(meta);
    }

    public static void setUid(ItemStack item, FarmGather plugin, String uid) {
        if (item == null || uid == null || uid.isBlank()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(key(plugin, UID_TAG), PersistentDataType.STRING, uid);
        item.setItemMeta(meta);
    }

    public static void markHudToken(ItemStack item, FarmGather plugin) {
        if (item == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(key(plugin, HUD_TOKEN_TAG), PersistentDataType.BYTE, (byte) 1);
        ensureUidTag(container, plugin);
        item.setItemMeta(meta);
    }

    public static boolean isHudToken(ItemStack item, FarmGather plugin) {
        if (item == null) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        Byte value = meta.getPersistentDataContainer().get(key(plugin, HUD_TOKEN_TAG), PersistentDataType.BYTE);
        return value != null && value == 1;
    }

    private static String localizeSkill(HoeSkillType type) {
        return switch (type) {
            case SWEEP -> "휩쓸기";
            case FOCUS -> "집중";
            case DOUBLETAP -> "더블탭";
            case SHEARS -> "전지가위";
        };
    }

    private static NamespacedKey key(FarmGather plugin, String value) {
        return new NamespacedKey(plugin, value);
    }

    public static void ensureUid(ItemStack item, FarmGather plugin) {
        if (item == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        ensureUidTag(container, plugin);
        item.setItemMeta(meta);
    }

    private static void ensureUidTag(PersistentDataContainer container, FarmGather plugin) {
        if (container.has(key(plugin, UID_TAG), PersistentDataType.STRING)) {
            String current = container.get(key(plugin, UID_TAG), PersistentDataType.STRING);
            if (current != null && !current.isBlank()) {
                return;
            }
        }
        container.set(key(plugin, UID_TAG), PersistentDataType.STRING, UUID.randomUUID().toString());
    }

    private static void maybeSetOwner(PersistentDataContainer container, UUID owner, FarmGather plugin) {
        if (owner == null) {
            return;
        }
        if (!plugin.getPluginConfig().itemIdentity().ownerLock()) {
            return;
        }
        container.set(key(plugin, OWNER_TAG), PersistentDataType.STRING, owner.toString());
    }
}
