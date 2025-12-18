package NewgenCosmetics.listener;

import NewgenCosmetics.gui.CosmeticWardrobeGui;
import NewgenCosmetics.gui.WardrobeHolder;
import NewgenCosmetics.service.CosmeticService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class WardrobeListener implements Listener {

    private final CosmeticWardrobeGui wardrobeGui;
    private final CosmeticService service;

    public WardrobeListener(CosmeticWardrobeGui wardrobeGui, CosmeticService service) {
        this.wardrobeGui = wardrobeGui;
        this.service = service;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!wardrobeGui.isWardrobeInventory(top)) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!wardrobeGui.isWardrobeInventory(top)) return;

        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) return;

        int raw = e.getRawSlot();
        if (raw >= top.getSize()) return;

        WardrobeHolder holder = wardrobeGui.getHolder(top);
        int page = holder != null ? holder.getPage() : 0;

        if (raw == wardrobeGui.getCloseSlot()) {
            player.closeInventory();
            return;
        }

        if (raw == wardrobeGui.getInfoSlot()) {
            return;
        }

        if (wardrobeGui.isPrevSlot(raw)) {
            if (page > 0) wardrobeGui.open(player, page - 1);
            return;
        }

        if (wardrobeGui.isNextSlot(raw)) {
            if (wardrobeGui.hasNextPage(player, page)) wardrobeGui.open(player, page + 1);
            return;
        }

        if (!e.isLeftClick()) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String cosmeticId = meta.getPersistentDataContainer().get(
                wardrobeGui.getWardrobeItemIdKey(),
                PersistentDataType.STRING
        );
        if (cosmeticId == null || cosmeticId.isBlank()) return;

        if (!player.hasPermission("newgen.cosmetic.admin") && !service.playerHasCosmetic(player, cosmeticId)) {
            return;
        }

        String equipped = service.getEquippedCosmeticId(player);

        if (equipped != null && cosmeticId.equals(equipped)) {
            return;
        }

        service.toggleEquipCosmetic(player, cosmeticId);
        wardrobeGui.open(player, page);
    }
}
