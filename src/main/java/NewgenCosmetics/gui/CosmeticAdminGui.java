package NewgenCosmetics.gui;

import NewgenCosmetics.service.CosmeticService;
import NewgenCosmetics.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class CosmeticAdminGui {

    private final CosmeticService cosmeticService;

    private final String title;
    private final int size;
    private final int inputSlot;
    private final int saveSlot;
    private final int infoSlot;

    public CosmeticAdminGui(Plugin plugin, CosmeticService cosmeticService) {
        this.cosmeticService = cosmeticService;

        FileConfiguration cfg = plugin.getConfig();
        this.title = Chat.color(cfg.getString("gui.admin-title", "&dCosmetic Admin"));
        this.size = clamp(cfg.getInt("gui.admin-size", 27), 9, 54);
        this.inputSlot = clamp(cfg.getInt("gui.admin-input-slot", 13), 0, size - 1);
        this.saveSlot = clamp(cfg.getInt("gui.admin-save-slot", 22), 0, size - 1);
        this.infoSlot = clamp(cfg.getInt("gui.admin-info-slot", 4), 0, size - 1);
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(player, size, title);

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        fm.setDisplayName(Chat.color("&7"));
        filler.setItemMeta(fm);
        for (int i = 0; i < size; i++) inv.setItem(i, filler);

        inv.setItem(inputSlot, null);

        ItemStack save = new ItemStack(Material.LIME_DYE);
        ItemMeta sm = save.getItemMeta();
        sm.setDisplayName(Chat.color("&a&lSAVE COSMETIC"));
        sm.setLore(List.of(
                Chat.color("&7Place the item in the middle slot, then click &aSAVE&7."),
                Chat.color("&7The cosmetic ID will be generated from the item name (slug)."),
                Chat.color("&7Then it will be saved to &fdata.db&7 and the item will be returned (helmet +3 armor, +2 toughness).")
        ));
        save.setItemMeta(sm);
        inv.setItem(saveSlot, save);

        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName(Chat.color("&dInstructions"));
        im.setLore(List.of(
                Chat.color("&7Input slot: &f" + inputSlot + "&7."),
                Chat.color("&7Click &aSAVE&7 to save."),
                Chat.color("&7Closing the GUI: unsaved items will be returned.")
        ));
        info.setItemMeta(im);
        inv.setItem(infoSlot, info);

        cosmeticService.setAdminGuiSlots(inputSlot, saveSlot);
        cosmeticService.tagAdminGui(inv);

        player.openInventory(inv);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
