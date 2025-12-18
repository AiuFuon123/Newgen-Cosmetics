package NewgenCosmetics.listener;

import NewgenCosmetics.service.CosmeticService;
import NewgenCosmetics.util.Chat;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GuiListener implements Listener {

    private final CosmeticService service;

    public GuiListener(CosmeticService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!service.isAdminGui(top)) return;

        int input = service.getAdminInputSlot();
        boolean touchesOther = e.getRawSlots().stream().anyMatch(s -> s < top.getSize() && s != input);
        if (touchesOther) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!service.isAdminGui(top)) return;

        if (!(e.getWhoClicked() instanceof Player player)) return;

        int raw = e.getRawSlot();
        if (raw >= top.getSize()) return;

        int input = service.getAdminInputSlot();
        int save = service.getAdminSaveSlot();


        if (raw != input && raw != save) {
            e.setCancelled(true);
            return;
        }


        if (raw == input) return;


        e.setCancelled(true);

        ItemStack item = top.getItem(input);
        if (item == null || item.getType().isAir()) {
            player.sendMessage(Chat.color("&cPlease place an item into the input slot first."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        String id = service.normalizeCosmeticId(item);
        service.saveAsCosmeticWithId(item, player, id);


        top.setItem(input, null);

        player.sendMessage(Chat.color("&aSaved cosmetic: &f" + id));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.6f);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!service.isAdminGui(top)) return;
        if (!(e.getPlayer() instanceof Player player)) return;

        ItemStack remain = top.getItem(service.getAdminInputSlot());
        if (remain != null && !remain.getType().isAir()) {
            top.setItem(service.getAdminInputSlot(), null);
            service.giveOrDrop(player, remain);
        }
    }
}
