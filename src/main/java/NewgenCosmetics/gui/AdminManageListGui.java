package NewgenCosmetics.gui;

import NewgenCosmetics.model.CosmeticRecord;
import NewgenCosmetics.service.CosmeticService;
import NewgenCosmetics.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class AdminManageListGui {

    private final Plugin plugin;
    private final CosmeticService service;

    private final NamespacedKey manageIdKey;


    private static final int SIZE = 45;

    private static final int CLOSE_SLOT = 0;
    private static final int INFO_SLOT  = 4;
    private static final int RELOAD_SLOT = 8;

    private static final int PREV_SLOT = 36;
    private static final int PAGE_SLOT = 40;
    private static final int NEXT_SLOT = 44;


    private static final int[] COSMETIC_SLOTS;
    static {
        COSMETIC_SLOTS = new int[27];
        int idx = 0;
        for (int i = 9; i <= 35; i++) COSMETIC_SLOTS[idx++] = i;
    }

    public AdminManageListGui(Plugin plugin, CosmeticService service) {
        this.plugin = plugin;
        this.service = service;
        this.manageIdKey = new NamespacedKey(plugin, "manage_cosmetic_id");
    }

    public void open(Player admin, int page) {
        int safePage = Math.max(0, page);
        Inventory inv = Bukkit.createInventory(new ManageListHolder(safePage), SIZE, Chat.color("&cCosmetics &8| &fManage"));


        ItemStack bar = pane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 36; i < SIZE; i++) inv.setItem(i, bar);

        inv.setItem(CLOSE_SLOT, button(Material.BARRIER, "&cClose", List.of("&7Close GUI")));
        inv.setItem(INFO_SLOT, button(Material.BOOK, "&eHow to use", List.of(
                "&7Left click cosmetic: &aEdit/Replace",
                "&7Shift + Right click: &cDelete",
                "&7Edit: put new item -> Save"
        )));
        inv.setItem(RELOAD_SLOT, button(Material.COMPARATOR, "&bRefresh", List.of("&7Reload this page")));

        List<String> ids = service.getAllCosmeticIds();
        int perPage = COSMETIC_SLOTS.length;

        int from = safePage * perPage;
        int to = Math.min(from + perPage, ids.size());

        int slotIndex = 0;
        for (int i = from; i < to; i++) {
            String id = ids.get(i);
            CosmeticRecord record = service.getCosmeticRecord(id);
            if (record == null) continue;

            ItemStack icon = service.buildWearableFromRecord(record);

            ItemMeta meta = icon.getItemMeta();
            if (meta == null) continue;


            List<String> lore = new ArrayList<>();
            lore.add(Chat.color("&8ID: &f" + id));
            lore.add(Chat.color("&aLeft: &fEdit"));
            lore.add(Chat.color("&cShift+Right: &fDelete"));

            meta.setLore(lore);
            meta.getPersistentDataContainer().set(manageIdKey, PersistentDataType.STRING, id);
            icon.setItemMeta(meta);

            inv.setItem(COSMETIC_SLOTS[slotIndex++], icon);
            if (slotIndex >= COSMETIC_SLOTS.length) break;
        }

        boolean hasPrev = safePage > 0;
        boolean hasNext = (safePage + 1) * perPage < ids.size();

        inv.setItem(PREV_SLOT, hasPrev
                ? button(Material.ARROW, "&aPrev", List.of("&7Previous page"))
                : button(Material.BARRIER, "&7Prev", List.of("&cNo previous page")));

        inv.setItem(NEXT_SLOT, hasNext
                ? button(Material.ARROW, "&aNext", List.of("&7Next page"))
                : button(Material.BARRIER, "&7Next", List.of("&cNo next page")));

        int totalPages = Math.max(1, (int) Math.ceil(ids.size() / (double) perPage));
        inv.setItem(PAGE_SLOT, button(Material.PAPER, "&fPage &e" + (safePage + 1) + "&7/&e" + totalPages,
                List.of("&7Total cosmetics: &f" + ids.size())));

        admin.openInventory(inv);
    }

    public NamespacedKey getManageIdKey() { return manageIdKey; }

    public boolean isManageList(Inventory top) {
        return top != null && top.getHolder() instanceof ManageListHolder;
    }

    public ManageListHolder getHolder(Inventory top) {
        return top != null && top.getHolder() instanceof ManageListHolder h ? h : null;
    }

    public int getCloseSlot() { return CLOSE_SLOT; }
    public int getInfoSlot() { return INFO_SLOT; }
    public int getReloadSlot() { return RELOAD_SLOT; }

    public int getPrevSlot() { return PREV_SLOT; }
    public int getNextSlot() { return NEXT_SLOT; }

    public static class ManageListHolder implements InventoryHolder {
        private final int page;
        public ManageListHolder(int page) { this.page = page; }
        public int getPage() { return page; }
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
