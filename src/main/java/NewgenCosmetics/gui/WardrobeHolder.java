package NewgenCosmetics.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class WardrobeHolder implements InventoryHolder {

    private final int page;
    private final boolean adminViewAll;

    public WardrobeHolder(int page, boolean adminViewAll) {
        this.page = page;
        this.adminViewAll = adminViewAll;
    }

    public int getPage() {
        return page;
    }

    public boolean isAdminViewAll() {
        return adminViewAll;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
