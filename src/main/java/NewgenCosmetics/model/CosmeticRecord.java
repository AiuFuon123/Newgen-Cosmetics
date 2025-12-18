package NewgenCosmetics.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record CosmeticRecord(
        String id,
        String ownerUuid,
        String material,
        Integer customModelData,
        String displayName,
        String loreJson,
        String itemBase64,
        long createdAt
) {

    private static final Gson GSON = new Gson();
    private static final Type LORE_TYPE = new TypeToken<List<String>>() {}.getType();

    public static CosmeticRecord fromItem(String id, ItemStack item, UUID owner) {
        ItemMeta meta = item.getItemMeta();

        String displayName = "";
        String loreJson = "";
        Integer cmd = null;

        if (meta != null) {
            if (meta.hasDisplayName()) displayName = meta.getDisplayName();
            if (meta.hasLore() && meta.getLore() != null) {
                loreJson = loreToJson(meta.getLore());
            }
            if (meta.hasCustomModelData()) {
                cmd = meta.getCustomModelData();
            }
        }

        return new CosmeticRecord(
                id,
                owner.toString(),
                item.getType().name(),
                cmd,
                displayName,
                loreJson,
                NewgenCosmetics.util.ItemStacks.toBase64(item),
                System.currentTimeMillis()
        );
    }

    public static String loreToJson(List<String> lore) {
        if (lore == null || lore.isEmpty()) return "";
        return GSON.toJson(lore);
    }

    public static List<String> loreFromJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return GSON.fromJson(json, LORE_TYPE);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
