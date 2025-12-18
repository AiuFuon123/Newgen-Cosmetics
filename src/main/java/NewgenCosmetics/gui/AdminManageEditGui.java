package NewgenCosmetics.gui;

import NewgenCosmetics.model.CosmeticRecord;
import NewgenCosmetics.service.CosmeticService;
import NewgenCosmetics.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class AdminManageEditGui {

    private final Plugin plugin;
    private final CosmeticService service;

    private static final int SIZE = 27;

    private static final int BACK_SLOT = 0;
    private static final int ID_SLOT = 4;

    private static final int PREVIEW_SLOT = 11;
    private static final int INPUT_SLOT = 13;
    private static final int SAVE_SLOT = 15;

    private static final int DELETE_SLOT = 26;

    public AdminManageEditGui(Plugin plugin, CosmeticService service) {
        this.plugin = plugin;
        this.service = service;
    }

    public void open(Player admin, String cosmeticId, int returnPage) {
        CosmeticRecord record = service.getCosmeticRecord(cosmeticId);
        if (record == null) {
            admin.sendMessage(Chat.color("&cCosmetic does not exist: &f" + cosmeticId));
            return;
        }

        Inventory inv = Bukkit.createInventory(new ManageEditHolder(cosmeticId, returnPage), SIZE,
                Chat.color("&cEdit &8| &f" + cosmeticId));


        ItemStack border = pane(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) inv.setItem(i, border);

        inv.setItem(BACK_SLOT, button(Material.ARROW, "&aBack", List.of("&7Return to list")));
        inv.setItem(ID_SLOT, button(Material.NAME_TAG, "&eCosmetic ID", List.of("&f" + cosmeticId)));

        ItemStack current = service.buildWearableFromRecord(record);
        inv.setItem(PREVIEW_SLOT, current);

        inv.setItem(INPUT_SLOT, null);

        inv.setItem(SAVE_SLOT, button(Material.LIME_CONCRETE, "&aSave", List.of(
                "&7Put new item in slot &f13",
                "&7Click to replace cosmetic data"
        )));

        inv.setItem(DELETE_SLOT, button(Material.RED_CONCRETE, "&cDelete", List.of(
                "&7Shift-click to confirm",
                "&cDelete from database"
        )));

        admin.openInventory(inv);
    }

    public boolean isEdit(Inventory top) {
        return top != null && top.getHolder() instanceof ManageEditHolder;
    }

    public ManageEditHolder getHolder(Inventory top) {
        return top != null && top.getHolder() instanceof ManageEditHolder h ? h : null;
    }

    public int getBackSlot() { return BACK_SLOT; }
    public int getPreviewSlot() { return PREVIEW_SLOT; }
    public int getInputSlot() { return INPUT_SLOT; }
    public int getSaveSlot() { return SAVE_SLOT; }
    public int getDeleteSlot() { return DELETE_SLOT; }

    public static class ManageEditHolder implements InventoryHolder {
        private final String cosmeticId;
        private final int returnPage;
        public ManageEditHolder(String cosmeticId, int returnPage) {
            this.cosmeticId = cosmeticId;
            this.returnPage = returnPage;
        }
        public String getCosmeticId() { return cosmeticId; }
        public int getReturnPage() { return returnPage; }
        @Override public Inventory getInventory() { return null; }
    }

    private static ItemStack pane(Material mat) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            it.setItemMeta(meta);
        }
        return it;
    }

    private static ItemStack button(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Chat.color(name));
            if (lore != null && !lore.isEmpty()) {
                List<String> out = new ArrayList<>();
                for (String s : lore) out.add(Chat.color(s));
                meta.setLore(out);
            }
            it.setItemMeta(meta);
        }
        return it;
    }
}
