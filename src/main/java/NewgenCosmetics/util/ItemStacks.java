package NewgenCosmetics.util;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public final class ItemStacks {
    private ItemStacks() {}

    public static String toBase64(ItemStack item) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BukkitObjectOutputStream oos = new BukkitObjectOutputStream(baos)) {
            oos.writeObject(item);
            oos.flush();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize item", e);
        }
    }

    public static ItemStack fromBase64(String data) {
        byte[] bytes = Base64.getDecoder().decode(data);
        try (BukkitObjectInputStream ois = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (ItemStack) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Failed to deserialize item", e);
        }
    }

    public static String toJsonArray(List<String> lines) {
        if (lines == null) lines = List.of();
        return "[" + lines.stream()
                .map(s -> s == null ? "" : s)
                .map(ItemStacks::jsonEscape)
                .map(s -> "\"" + s + "\"")
                .collect(Collectors.joining(",")) + "]";
    }

    private static String jsonEscape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static void applyHeadArmorAttributes(Plugin plugin, ItemMeta meta, double armor, double toughness) {
        meta.removeAttributeModifier(Attribute.ARMOR);
        meta.removeAttributeModifier(Attribute.ARMOR_TOUGHNESS);

        AttributeModifier armorMod = new AttributeModifier(
                new NamespacedKey(plugin, "newgen_cosmetic_armor"),
                armor,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.HEAD
        );

        AttributeModifier toughMod = new AttributeModifier(
                new NamespacedKey(plugin, "newgen_cosmetic_toughness"),
                toughness,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.HEAD
        );

        meta.addAttributeModifier(Attribute.ARMOR, armorMod);
        meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, toughMod);
    }
}
