package wiki.creeper.farmGather.harvest;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import wiki.creeper.farmGather.util.Text;

public class HarvestListener implements Listener {
    private final HarvestManager harvestManager;

    public HarvestListener(HarvestManager harvestManager) {
        this.harvestManager = harvestManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        HarvestResult result = harvestManager.attemptHarvest(event.getPlayer(), block);
        if (!result.success()) {
            return;
        }

        event.setCancelled(true);
    }
}
