package NewgenCosmetics.listener;

import NewgenCosmetics.model.CosmeticRecord;
import NewgenCosmetics.service.CosmeticService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

public class JoinEquipListener implements Listener {

    private final Plugin plugin;
    private final CosmeticService service;

    public JoinEquipListener(Plugin plugin, CosmeticService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();


        Bukkit.getScheduler().runTask(plugin, () -> {
            String equippedId = service.getEquippedCosmeticId(player);
            if (equippedId == null || equippedId.isBlank()) return;


            if (!player.hasPermission("newgen.cosmetic.admin") && !service.playerHasCosmetic(player, equippedId)) {
                return;
            }

            CosmeticRecord record = service.getCosmeticRecord(equippedId);
            if (record == null) return;

            service.forceEquipHelmet(player, service.buildWearableFromRecord(record));
        });
    }
}
