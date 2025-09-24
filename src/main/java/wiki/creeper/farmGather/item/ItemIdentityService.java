package wiki.creeper.farmGather.item;

import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import wiki.creeper.farmGather.FarmGather;
import wiki.creeper.farmGather.config.PluginConfig;
import wiki.creeper.farmGather.util.ItemUtil;

public class ItemIdentityService implements Listener {
    private final FarmGather plugin;
    private PluginConfig.ItemIdentityConfig config;

    public ItemIdentityService(FarmGather plugin, PluginConfig.ItemIdentityConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void reload(PluginConfig.ItemIdentityConfig config) {
        this.config = config;
    }

    public void ensureInventoryTagged(Player player) {
        if (player == null || !config.enforceUid()) {
            return;
        }
        boolean changed = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (shouldEnsureUid(item)) {
                ItemUtil.ensureUid(item, plugin);
                changed = true;
            }
        }
        if (changed) {
            plugin.getServer().getScheduler().runTask(plugin, player::updateInventory);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> ensureInventoryTagged(event.getPlayer()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // no-op for now, but slot reserved for future clean-up if required.
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        boolean hudProtect = hudProtectEnabled();
        boolean preventStack = config.preventStack();
        if (!hudProtect && !preventStack) {
            return;
        }

        if (hudProtect && handleHudInteraction(event, player)) {
            return;
        }

        if (!preventStack) {
            return;
        }

        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        boolean trackedCursor = isTracked(cursor);
        boolean trackedCurrent = isTracked(current);

        if (event.getClick() == ClickType.NUMBER_KEY) {
            ItemStack hotbar = player.getInventory().getItem(event.getHotbarButton());
            if (isTracked(hotbar) || trackedCurrent) {
                cancelWithUpdate(event);
                return;
            }
        }

        if (event.getClick() == ClickType.SWAP_OFFHAND) {
            PlayerInventory inventory = player.getInventory();
            ItemStack offhand = inventory.getItemInOffHand();
            ItemStack mainHand = inventory.getItemInMainHand();
            if (isTracked(offhand) || isTracked(mainHand)) {
                cancelWithUpdate(event);
                return;
            }
        }

        if (!trackedCursor && !trackedCurrent) {
            return;
        }

        if (event.getClick() == ClickType.DOUBLE_CLICK && trackedCursor) {
            cancelWithUpdate(event);
            return;
        }

        if (event.getClickedInventory() != null
                && event.getClickedInventory() == event.getView().getTopInventory()
                && isRestrictedInput(event.getView().getTopInventory().getType())
                && (trackedCursor || trackedCurrent)) {
            cancelWithUpdate(event);
            return;
        }

        if (event.isShiftClick()) {
            cancelWithUpdate(event);
            return;
        }

        InventoryAction action = event.getAction();
        if (action == InventoryAction.COLLECT_TO_CURSOR || action == InventoryAction.MOVE_TO_OTHER_INVENTORY
                || action == InventoryAction.HOTBAR_MOVE_AND_READD || action == InventoryAction.HOTBAR_SWAP) {
            cancelWithUpdate(event);
            return;
        }

        if (trackedCursor && !isEmpty(current)) {
            cancelWithUpdate(event);
            return;
        }

        if (trackedCurrent && !isEmpty(cursor)) {
            cancelWithUpdate(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        boolean hudProtect = hudProtectEnabled();
        boolean preventStack = config.preventStack();
        if (!hudProtect && !preventStack) {
            return;
        }

        if (hudProtect && handleHudDrag(event)) {
            return;
        }

        if (!preventStack) {
            return;
        }
        if (isTracked(event.getCursor()) || isTracked(event.getOldCursor())) {
            cancelDrag(event);
            return;
        }
        if (event.getNewItems().values().stream().anyMatch(this::isTracked)) {
            cancelDrag(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemMerge(org.bukkit.event.entity.ItemMergeEvent event) {
        if (!config.preventMerge()) {
            return;
        }
        if (isTracked(event.getEntity().getItemStack()) || isTracked(event.getTarget().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!config.ownerLock()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Optional<java.util.UUID> owner = ItemUtil.readOwner(event.getItem().getItemStack(), plugin);
        if (owner.isPresent() && !owner.get().equals(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (!config.preventStack()) {
            return;
        }
        if (isTracked(event.getItem())) {
            event.setCancelled(true);
        }
    }

    private void cancelWithUpdate(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getWhoClicked() instanceof Player player) {
            plugin.getServer().getScheduler().runTask(plugin, player::updateInventory);
        }
    }

    private void cancelDrag(InventoryDragEvent event) {
        event.setCancelled(true);
        if (event.getWhoClicked() instanceof Player player) {
            plugin.getServer().getScheduler().runTask(plugin, player::updateInventory);
        }
    }

    private boolean hudProtectEnabled() {
        PluginConfig.CooldownUiConfig cooldownUi = plugin.getPluginConfig().cooldownUi();
        if (cooldownUi == null || cooldownUi.hud() == null) {
            return false;
        }
        return cooldownUi.mode() == PluginConfig.CooldownUiConfig.CooldownUiMode.LOCKED_HUD_SLOT
                && cooldownUi.hud().protect();
    }

    private int getHudSlot() {
        PluginConfig.CooldownUiConfig cooldownUi = plugin.getPluginConfig().cooldownUi();
        if (cooldownUi == null || cooldownUi.hud() == null) {
            return 8;
        }
        int configured = cooldownUi.hud().slot();
        if (configured < 0) {
            return 0;
        }
        return Math.min(8, configured);
    }

    private boolean handleHudInteraction(InventoryClickEvent event, Player player) {
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        boolean involvesHud = ItemUtil.isHudToken(cursor, plugin) || ItemUtil.isHudToken(current, plugin);

        if (!involvesHud && event.getClick() == ClickType.NUMBER_KEY) {
            ItemStack hotbar = player.getInventory().getItem(event.getHotbarButton());
            involvesHud = ItemUtil.isHudToken(hotbar, plugin);
        }

        if (!involvesHud && event.getClick() == ClickType.SWAP_OFFHAND) {
            PlayerInventory inventory = player.getInventory();
            involvesHud = ItemUtil.isHudToken(inventory.getItemInOffHand(), plugin)
                    || ItemUtil.isHudToken(inventory.getItemInMainHand(), plugin);
        }

        if (!involvesHud) {
            if (event.getSlotType() == SlotType.QUICKBAR && event.getSlot() == getHudSlot()) {
                involvesHud = true;
            } else if (isHudRawSlot(event.getView(), event.getRawSlot())) {
                involvesHud = true;
            }
        }

        if (involvesHud) {
            cancelWithUpdate(event);
            return true;
        }
        return false;
    }

    private boolean handleHudDrag(InventoryDragEvent event) {
        if (event.getNewItems().values().stream().anyMatch(item -> ItemUtil.isHudToken(item, plugin))) {
            cancelDrag(event);
            return true;
        }
        InventoryView view = event.getView();
        for (int rawSlot : event.getRawSlots()) {
            if (isHudRawSlot(view, rawSlot)) {
                cancelDrag(event);
                return true;
            }
        }
        return false;
    }

    private boolean isHudRawSlot(InventoryView view, int rawSlot) {
        if (rawSlot < 0) {
            return false;
        }
        if (view.getSlotType(rawSlot) != SlotType.QUICKBAR) {
            return false;
        }
        return view.convertSlot(rawSlot) == getHudSlot();
    }

    private boolean shouldEnsureUid(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        if (!ItemUtil.hasUid(item, plugin)
                && (ItemUtil.isFarmHoe(item, plugin) || ItemUtil.isHudToken(item, plugin))) {
            return true;
        }
        return false;
    }

    private boolean isTracked(ItemStack item) {
        return ItemUtil.hasUid(item, plugin);
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir() || item.getAmount() <= 0;
    }

    private boolean isRestrictedInput(InventoryType type) {
        return type == InventoryType.ANVIL
                || type == InventoryType.GRINDSTONE
                || type == InventoryType.SMITHING
                || type == InventoryType.WORKBENCH
                || type == InventoryType.CRAFTING;
    }
}
