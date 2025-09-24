package wiki.creeper.farmGather.harvest;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import wiki.creeper.farmGather.FarmGather;

public class HarvestableRegistry {
    private final FarmGather plugin;
    private final Set<Material> harvestableMaterials = new HashSet<>();

    public HarvestableRegistry(FarmGather plugin) {
        this.plugin = plugin;
    }

    public void rebuild(List<String> entries) {
        harvestableMaterials.clear();
        for (String entry : entries) {
            if (entry.startsWith("#")) {
                registerTag(entry.substring(1));
            } else {
                registerMaterial(entry);
            }
        }
        plugin.getLogger().info("Loaded " + harvestableMaterials.size() + " harvestable materials");
    }

    public boolean isHarvestable(Material material) {
        return harvestableMaterials.contains(material);
    }

    public Set<Material> getHarvestableMaterials() {
        return Collections.unmodifiableSet(harvestableMaterials);
    }

    private void registerTag(String keyString) {
        NamespacedKey key = NamespacedKey.fromString(keyString);
        if (key == null) {
            plugin.getLogger().warning("Invalid tag key: " + keyString);
            return;
        }
        Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, key, Material.class);
        if (tag == null) {
            tag = Bukkit.getTag(Tag.REGISTRY_ITEMS, key, Material.class);
        }
        if (tag == null) {
            plugin.getLogger().warning("Unknown material tag: #" + keyString);
            return;
        }
        harvestableMaterials.addAll(tag.getValues());
    }

    private void registerMaterial(String materialName) {
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            plugin.getLogger().warning("Unknown material: " + materialName);
            return;
        }
        harvestableMaterials.add(material);
    }
}
