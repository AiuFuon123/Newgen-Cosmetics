package NewgenCosmetics.gui;

import NewgenCosmetics.model.CosmeticRecord;
import NewgenCosmetics.service.CosmeticService;
import NewgenCosmetics.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class CosmeticWardrobeGui {

    private final Plugin plugin;
    private final CosmeticService service;

    private final NamespacedKey wardrobeItemIdKey;
    private final NamespacedKey cosmeticIdKey;


    private String title;
    private int size;

    private int closeSlot;
    private int infoSlot;

    private int[] cosmeticSlots;
    private boolean[] prevSlotMask;
    private boolean[] nextSlotMask;

    private int perPage;

    private List<String> extraLoreColored;

    private ItemStack fillerTemplate;
    private ItemStack closeTemplate;
    private ItemStack infoTemplate;
    private ItemStack prevEnabledTemplate;
    private ItemStack prevDisabledTemplate;
    private ItemStack nextEnabledTemplate;
    private ItemStack nextDisabledTemplate;

    public CosmeticWardrobeGui(Plugin plugin, CosmeticService service) {
        this.plugin = plugin;
        this.service = service;
        this.wardrobeItemIdKey = new NamespacedKey(plugin, "wardrobe_cosmetic_id");
        this.cosmeticIdKey = new NamespacedKey(plugin, "cosmetic_id");
        reloadCache();
    }


    private void reloadCache() {
        FileConfiguration cfg = plugin.getConfig();

        this.title = Chat.color(cfg.getString("gui.wardrobe.title", "&dCosmetic Wardrobe"));
        this.size = clamp(cfg.getInt("gui.wardrobe.size", 54), 9, 54);

        this.closeSlot = clamp(cfg.getInt("gui.wardrobe.close-button.slot", 0), 0, size - 1);
        this.infoSlot  = clamp(cfg.getInt("gui.wardrobe.info-button.slot", 8), 0, size - 1);

        List<Integer> list = cfg.getIntegerList("gui.wardrobe.cosmetic-slots");
        if (list == null || list.isEmpty()) {
            list = new ArrayList<>();
            for (int i = 9; i <= 35; i++) list.add(i);
        }

        this.cosmeticSlots = list.stream()
                .filter(s -> s >= 0 && s < size)
                .distinct()
                .mapToInt(Integer::intValue)
                .toArray();

        this.perPage = cosmeticSlots.length;

        this.extraLoreColored = new ArrayList<>();
        for (String line : cfg.getStringList("gui.wardrobe.extra-lore")) {
            this.extraLoreColored.add(Chat.color(line));
        }

        this.fillerTemplate = buildConfigItem(cfg, "gui.wardrobe.filler");
        this.closeTemplate  = buildConfigItem(cfg, "gui.wardrobe.close-button");
        this.infoTemplate   = buildConfigItem(cfg, "gui.wardrobe.info-button");

        this.prevEnabledTemplate  = buildConfigItem(cfg, "gui.wardrobe.nav.prev-enabled");
        this.prevDisabledTemplate = buildConfigItem(cfg, "gui.wardrobe.nav.prev-disabled");
        this.nextEnabledTemplate  = buildConfigItem(cfg, "gui.wardrobe.nav.next-enabled");
        this.nextDisabledTemplate = buildConfigItem(cfg, "gui.wardrobe.nav.next-disabled");

        this.prevSlotMask = new boolean[size];
        this.nextSlotMask = new boolean[size];

        for (int s : cfg.getIntegerList("gui.wardrobe.nav.prev-slots")) {
            if (s >= 0 && s < size) prevSlotMask[s] = true;
        }
        for (int s : cfg.getIntegerList("gui.wardrobe.nav.next-slots")) {
            if (s >= 0 && s < size) nextSlotMask[s] = true;
        }
    }


    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        WardrobeHolder holder = new WardrobeHolder(Math.max(0, page), player.hasPermission("newgen.cosmetic.admin"));
        Inventory inv = Bukkit.createInventory(holder, size, title);


        ItemStack filler = fillerTemplate.clone();
        for (int i = 18; i < size; i++) inv.setItem(i, filler);

        inv.setItem(closeSlot, closeTemplate.clone());
        inv.setItem(infoSlot, infoTemplate.clone());

        List<String> ids = holder.isAdminViewAll()
                ? service.getAllCosmeticIds()
                : service.getPlayerGrantedCosmeticIds(player);

        int from = page * perPage;
        int to = Math.min(from + perPage, ids.size());

        for (int s : cosmeticSlots) inv.setItem(s, null);


        String equippedId = getEquippedCosmeticId(player);

        if (from < ids.size()) {
            int idx = 0;
            for (int i = from; i < to; i++) {
                String id = ids.get(i);
                CosmeticRecord record = service.getCosmeticRecord(id);
                if (record == null) continue;

                ItemStack icon = service.buildWearableFromRecord(record);
                ItemMeta meta = icon.getItemMeta();
                if (meta == null) continue;

                List<String> lore = meta.hasLore() && meta.getLore() != null
                        ? new ArrayList<>(meta.getLore())
                        : new ArrayList<>();


                List<String> extra = new ArrayList<>(extraLoreColored);


                if (equippedId != null && equippedId.equals(id)) {
                    if (!extra.isEmpty()) {
                        extra.set(0, Chat.color("&aEquipped"));
                    }
                }

                lore.addAll(extra);
                meta.setLore(lore);

                meta.getPersistentDataContainer().set(
                        wardrobeItemIdKey,
                        PersistentDataType.STRING,
                        id
                );
                icon.setItemMeta(meta);

                while (idx < cosmeticSlots.length && inv.getItem(cosmeticSlots[idx]) != null) idx++;
                if (idx >= cosmeticSlots.length) break;

                inv.setItem(cosmeticSlots[idx], icon);
                idx++;
            }
        }

        boolean hasPrev = page > 0;
        boolean hasNext = (page + 1) * perPage < ids.size();

        ItemStack prev = (hasPrev ? prevEnabledTemplate : prevDisabledTemplate).clone();
        ItemStack next = (hasNext ? nextEnabledTemplate : nextDisabledTemplate).clone();

        for (int i = 0; i < size; i++) {
            if (prevSlotMask[i]) inv.setItem(i, prev);
            else if (nextSlotMask[i]) inv.setItem(i, next);
        }

        player.openInventory(inv);
    }


    private String getEquippedCosmeticId(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet == null || helmet.getType().isAir()) return null;

        ItemMeta meta = helmet.getItemMeta();
        if (meta == null) return null;

        return meta.getPersistentDataContainer().get(
                cosmeticIdKey,
                PersistentDataType.STRING
        );
    }

    public boolean isWardrobeInventory(Inventory top) {
        return top != null && top.getHolder() instanceof WardrobeHolder;
    }

    public WardrobeHolder getHolder(Inventory top) {
        return top != null && top.getHolder() instanceof WardrobeHolder h ? h : null;
    }

    public NamespacedKey getWardrobeItemIdKey() {
        return wardrobeItemIdKey;
    }

    public int getCloseSlot() { return closeSlot; }
    public int getInfoSlot() { return infoSlot; }

    public boolean isPrevSlot(int slot) {
        return slot >= 0 && slot < prevSlotMask.length && prevSlotMask[slot];
    }

    public boolean isNextSlot(int slot) {
        return slot >= 0 && slot < nextSlotMask.length && nextSlotMask[slot];
    }

    public boolean hasNextPage(Player player, int page) {
        List<String> ids = player.hasPermission("newgen.cosmetic.admin")
                ? service.getAllCosmeticIds()
                : service.getPlayerGrantedCosmeticIds(player);
        return (page + 1) * perPage < ids.size();
    }

    private ItemStack buildConfigItem(FileConfiguration cfg, String path) {
        Material mat = Material.matchMaterial(cfg.getString(path + ".material", "STONE"));
        if (mat == null) mat = Material.STONE;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Chat.color(cfg.getString(path + ".name", "&fItem")));

            List<String> lore = cfg.getStringList(path + ".lore");
            if (lore != null && !lore.isEmpty()) {
                List<String> colored = new ArrayList<>();
                for (String s : lore) colored.add(Chat.color(s));
                meta.setLore(colored);
            }

            int cmd = cfg.getInt(path + ".custom-model-data", 0);
            if (cmd > 0) meta.setCustomModelData(cmd);

            item.setItemMeta(meta);
        }
        return item;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
