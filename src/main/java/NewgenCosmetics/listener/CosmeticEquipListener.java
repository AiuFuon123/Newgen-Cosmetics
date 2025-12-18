package NewgenCosmetics.listener;

import NewgenCosmetics.service.CosmeticService;
import NewgenCosmetics.util.RegionLockUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class CosmeticEquipListener implements Listener {

    private final CosmeticService service;

    public CosmeticEquipListener(CosmeticService service) {
        this.service = service;
    }

    private static final int HELMET_SLOT = 39;

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHelmetClick(InventoryClickEvent e) {
        if (e.getSlot() != HELMET_SLOT) return;
        if (!(e.getWhoClicked() instanceof Player player)) return;

        ItemStack cursor = e.getCursor();
        ItemStack current = e.getCurrentItem();

        boolean cursorCos = service.isCosmeticItem(cursor);
        boolean currentCos = service.isCosmeticItem(current);

        if (!cursorCos && !currentCos) return;

        boolean locked = RegionLockUtil.isUnequipLockedHere(service.getPlugin(), player);

        boolean cursorAir = (cursor == null || cursor.getType().isAir());
        boolean cursorHasItem = (cursor != null && !cursor.getType().isAir());

        if (currentCos && cursorAir) {
            if (locked) {
                e.setCancelled(true);
                return;
            }
            service.setEquippedCosmeticId(player, null);
            return;
        }

        if (currentCos && cursorHasItem && !cursorCos) {
            if (locked) {
                e.setCancelled(true);
                return;
            }
            service.setEquippedCosmeticId(player, null);
            return;
        }

        if (cursorCos) {
            e.setCancelled(true);

            String id = service.getCosmeticIdFromItem(cursor);

            service.forceEquipHelmet(player, cursor.clone());
            e.setCursor(null);

            if (id != null) service.setEquippedCosmeticId(player, id);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHelmetDrag(InventoryDragEvent e) {
        if (!e.getInventorySlots().contains(HELMET_SLOT)) return;

        ItemStack cursor = e.getOldCursor();
        if (service.isCosmeticItem(cursor)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRightClick(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (!e.getAction().isRightClick()) return;

        ItemStack item = e.getItem();
        if (!service.isCosmeticItem(item)) return;

        e.setCancelled(true);

        Player player = e.getPlayer();
        String id = service.getCosmeticIdFromItem(item);
        if (id == null) return;

        String equipped = service.getEquippedCosmeticId(player);

        if (id.equals(equipped)) {
            if (RegionLockUtil.isUnequipLockedHere(service.getPlugin(), player)) {

                return;
            }
            service.unequipCosmetic(player);
            return;
        }

        service.forceEquipHelmet(player, item.clone());
        service.setEquippedCosmeticId(player, id);
    }
}
