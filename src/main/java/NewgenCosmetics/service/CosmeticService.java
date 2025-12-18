package NewgenCosmetics.service;

import NewgenCosmetics.db.DatabaseManager;
import NewgenCosmetics.model.CosmeticRecord;
import NewgenCosmetics.util.ItemStacks;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class CosmeticService {

    private final Plugin plugin;
    private final DatabaseManager db;

    private final NamespacedKey adminGuiKey;
    private final NamespacedKey cosmeticIdKey;

    private int adminInputSlot = 13;
    private int adminSaveSlot = 22;

    private final double armor;
    private final double toughness;

    public CosmeticService(Plugin plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;

        this.adminGuiKey = new NamespacedKey(plugin, "admin_gui_marker");
        this.cosmeticIdKey = new NamespacedKey(plugin, "cosmetic_id");

        this.armor = plugin.getConfig().getDouble("item.armor", 3.0);
        this.toughness = plugin.getConfig().getDouble("item.toughness", 2.0);
    }

    public Plugin getPlugin() { return plugin; }
    public DatabaseManager getDatabase() { return db; }
    public NamespacedKey getCosmeticIdKey() { return cosmeticIdKey; }


    public void setAdminGuiSlots(int inputSlot, int saveSlot) {
        this.adminInputSlot = inputSlot;
        this.adminSaveSlot = saveSlot;
    }

    public int getAdminInputSlot() { return adminInputSlot; }
    public int getAdminSaveSlot() { return adminSaveSlot; }

    public void tagAdminGui(Inventory inv) {
        ItemStack save = inv.getItem(adminSaveSlot);
        if (save == null) return;
        ItemMeta meta = save.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(adminGuiKey, PersistentDataType.BYTE, (byte) 1);
        save.setItemMeta(meta);
        inv.setItem(adminSaveSlot, save);
    }

    public boolean isAdminGui(Inventory inv) {
        if (inv == null) return false;
        ItemStack save = inv.getItem(adminSaveSlot);
        if (save == null || save.getType().isAir()) return false;
        ItemMeta meta = save.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(adminGuiKey, PersistentDataType.BYTE);
    }

    public String normalizeCosmeticId(ItemStack item) {
        ItemMeta meta = item.getItemMeta();

        String base = null;
        if (meta != null && meta.hasDisplayName()) base = ChatColor.stripColor(meta.getDisplayName());
        if (base == null || base.isBlank()) base = item.getType().name();

        String slug = base.toLowerCase()
                .replaceAll("[^a-z0-9\\s_-]", "")
                .trim()
                .replaceAll("\\s+", "_");

        if (slug.isBlank()) slug = "cosmetic";

        String tryId = slug;
        int i = 2;
        while (db.cosmeticExists(tryId)) {
            tryId = slug + "_" + i++;
        }
        return tryId;
    }

    public ItemStack saveAsCosmeticWithId(ItemStack original, Player saver, String cosmeticId) {
        CosmeticRecord record = CosmeticRecord.fromItem(cosmeticId, original, saver.getUniqueId());
        db.insertCosmetic(record);

        ItemStack wearable = original.clone();
        ItemMeta meta = wearable.getItemMeta();

        if (meta != null) {
            meta.getPersistentDataContainer().set(cosmeticIdKey, PersistentDataType.STRING, cosmeticId);
            ItemStacks.applyHeadArmorAttributes(plugin, meta, armor, toughness);
            wearable.setItemMeta(meta);
        }
        return wearable;
    }

    public ItemStack buildWearableFromRecord(CosmeticRecord record) {
        ItemStack item = ItemStacks.fromBase64(record.itemBase64());
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.getPersistentDataContainer().set(cosmeticIdKey, PersistentDataType.STRING, record.id());
            ItemStacks.applyHeadArmorAttributes(plugin, meta, armor, toughness);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isCosmeticItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(cosmeticIdKey, PersistentDataType.STRING);
    }

    public String getCosmeticIdFromItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(cosmeticIdKey, PersistentDataType.STRING);
    }

    public void forceEquipHelmet(Player player, ItemStack newHelmet) {
        if (player == null) return;
        if (newHelmet == null || newHelmet.getType().isAir()) return;

        player.getInventory().setHelmet(null);
        player.getInventory().setHelmet(newHelmet);
    }

    public void unequipCosmetic(Player player) {
        if (player == null) return;
        player.getInventory().setHelmet(null);
        setEquippedCosmeticId(player, null);
    }

    public void toggleEquipCosmetic(Player player, String cosmeticId) {
        if (player == null) return;
        if (cosmeticId == null || cosmeticId.isBlank()) return;

        String equipped = getEquippedCosmeticId(player);
        if (cosmeticId.equals(equipped)) {
            unequipCosmetic(player);
            return;
        }

        CosmeticRecord record = getCosmeticRecord(cosmeticId);
        if (record == null) return;

        forceEquipHelmet(player, buildWearableFromRecord(record));
        setEquippedCosmeticId(player, cosmeticId);
    }

    public void setEquippedCosmeticId(Player player, String cosmeticId) {
        if (player == null) return;
        db.setEquippedCosmeticId(player.getUniqueId().toString(), cosmeticId);
    }

    public String getEquippedCosmeticId(Player player) {
        if (player == null) return null;
        return db.getEquippedCosmeticId(player.getUniqueId().toString());
    }

    public void grantToPlayer(Player player, String cosmeticId) {
        db.grantCosmeticToPlayer(player.getUniqueId().toString(), cosmeticId);
    }

    public boolean playerHasCosmetic(Player player, String cosmeticId) {
        return db.playerHasCosmetic(player.getUniqueId().toString(), cosmeticId);
    }

    public List<String> getPlayerGrantedCosmeticIds(Player player) {
        return db.getCosmeticsOfPlayer(player.getUniqueId().toString());
    }

    public CosmeticRecord getCosmeticRecord(String id) {
        return db.getCosmeticById(id);
    }

    public List<String> getAllCosmeticIds() {
        return db.getAllCosmeticIds();
    }

    public List<String> searchCosmeticIdsPrefix(String prefix, int limit) {
        return db.searchCosmeticIdsPrefix(prefix, limit);
    }

    public void giveOrDrop(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        var leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) leftover.values().forEach(it -> player.getWorld().dropItemNaturally(player.getLocation(), it));
    }

    public boolean adminUpdateCosmeticItem(String cosmeticId, ItemStack newItem) {
        if (cosmeticId == null || cosmeticId.isBlank()) return false;
        if (newItem == null || newItem.getType().isAir()) return false;

        String material = newItem.getType().name();
        ItemMeta meta = newItem.getItemMeta();

        Integer cmd = null;
        String displayName = "";
        String loreJson = "";

        if (meta != null) {
            if (meta.hasCustomModelData()) cmd = meta.getCustomModelData();
            if (meta.hasDisplayName()) displayName = meta.getDisplayName();
            if (meta.hasLore() && meta.getLore() != null) {
                loreJson = CosmeticRecord.loreToJson(meta.getLore());
            }
        }

        String base64 = ItemStacks.toBase64(newItem);
        return db.updateCosmeticKeepId(cosmeticId, material, cmd, displayName, loreJson, base64);
    }

    public boolean adminDeleteCosmetic(String cosmeticId) {
        if (cosmeticId == null || cosmeticId.isBlank()) return false;
        return db.deleteCosmeticFully(cosmeticId);
    }
}
