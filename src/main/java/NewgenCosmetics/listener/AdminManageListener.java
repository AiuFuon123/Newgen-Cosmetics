package NewgenCosmetics.listener;

import NewgenCosmetics.gui.AdminManageEditGui;
import NewgenCosmetics.gui.AdminManageListGui;
import NewgenCosmetics.gui.AdminManageEditGui.ManageEditHolder;
import NewgenCosmetics.gui.AdminManageListGui.ManageListHolder;
import NewgenCosmetics.service.CosmeticService;
import NewgenCosmetics.util.Chat;
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

public class AdminManageListener implements Listener {

    private final CosmeticService service;
    private final AdminManageListGui listGui;
    private final AdminManageEditGui editGui;

    public AdminManageListener(CosmeticService service, AdminManageListGui listGui, AdminManageEditGui editGui) {
        this.service = service;
        this.listGui = listGui;
        this.editGui = editGui;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!listGui.isManageList(top) && !editGui.isEdit(top)) return;


        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(e.getWhoClicked() instanceof Player player)) return;

        if (listGui.isManageList(top)) {
            e.setCancelled(true);
            handleListClick(e, player, top);
            return;
        }

        if (editGui.isEdit(top)) {
            int raw = e.getRawSlot();
            if (raw == editGui.getInputSlot()) {

                e.setCancelled(false);
                return;
            }
            e.setCancelled(true);
            handleEditClick(e, player, top);
        }
    }

    private void handleListClick(InventoryClickEvent e, Player admin, Inventory top) {
        int raw = e.getRawSlot();
        if (raw >= top.getSize()) return;

        ManageListHolder holder = listGui.getHolder(top);
        int page = holder != null ? holder.getPage() : 0;

        if (raw == listGui.getCloseSlot()) {
            admin.closeInventory();
            return;
        }

        if (raw == listGui.getReloadSlot()) {
            listGui.open(admin, page);
            return;
        }

        if (raw == listGui.getPrevSlot()) {
            if (page > 0) listGui.open(admin, page - 1);
            return;
        }

        if (raw == listGui.getNextSlot()) {
            listGui.open(admin, page + 1);
            return;
        }

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String id = meta.getPersistentDataContainer().get(listGui.getManageIdKey(), PersistentDataType.STRING);
        if (id == null || id.isBlank()) return;


        if (e.isShiftClick() && e.isRightClick()) {
            boolean ok = service.adminDeleteCosmetic(id);
            if (ok) admin.sendMessage(Chat.color("&aDeleted cosmetic: &f" + id));
            else admin.sendMessage(Chat.color("&cDelete failed: &f" + id));
            listGui.open(admin, page);
            return;
        }


        if (e.isLeftClick()) {
            editGui.open(admin, id, page);
        }
    }

    private void handleEditClick(InventoryClickEvent e, Player admin, Inventory top) {
        int raw = e.getRawSlot();
        if (raw >= top.getSize()) return;

        ManageEditHolder holder = editGui.getHolder(top);
        if (holder == null) return;

        String cosmeticId = holder.getCosmeticId();
        int returnPage = holder.getReturnPage();

        if (raw == editGui.getBackSlot()) {
            listGui.open(admin, returnPage);
            return;
        }

        if (raw == editGui.getSaveSlot()) {
            ItemStack input = top.getItem(editGui.getInputSlot());
            if (input == null || input.getType().isAir()) {
                admin.sendMessage(Chat.color("&cPlease place the new item into slot 13."));
                return;
            }

            boolean ok = service.adminUpdateCosmeticItem(cosmeticId, input);
            if (ok) {
                admin.sendMessage(Chat.color("&aUpdated cosmetic: &f" + cosmeticId));
                editGui.open(admin, cosmeticId, returnPage);
            } else {
                admin.sendMessage(Chat.color("&cUpdate failed: &f" + cosmeticId));
            }
            return;
        }

        if (raw == editGui.getDeleteSlot()) {
            if (!e.isShiftClick()) {
                admin.sendMessage(Chat.color("&cShift-click to confirm deletion."));
                return;
            }
            boolean ok = service.adminDeleteCosmetic(cosmeticId);
            if (ok) admin.sendMessage(Chat.color("&aDeleted cosmetic: &f" + cosmeticId));
            else admin.sendMessage(Chat.color("&cDelete failed: &f" + cosmeticId));
            listGui.open(admin, returnPage);
        }
    }
}
