package NewgenCosmetics.command;

import NewgenCosmetics.gui.AdminManageEditGui;
import NewgenCosmetics.gui.AdminManageListGui;
import NewgenCosmetics.gui.CosmeticAdminGui;
import NewgenCosmetics.gui.CosmeticWardrobeGui;
import NewgenCosmetics.model.CosmeticRecord;
import NewgenCosmetics.service.CosmeticService;
import NewgenCosmetics.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class CosmeticCommand implements CommandExecutor {

    private final Plugin plugin;
    private final CosmeticService service;
    private final CosmeticAdminGui adminGui;
    private final CosmeticWardrobeGui wardrobeGui;

    private final AdminManageListGui manageListGui;
    private final AdminManageEditGui manageEditGui;

    public CosmeticCommand(Plugin plugin,
                           CosmeticService service,
                           CosmeticWardrobeGui wardrobeGui,
                           AdminManageListGui manageListGui,
                           AdminManageEditGui manageEditGui) {
        this.plugin = plugin;
        this.service = service;
        this.adminGui = new CosmeticAdminGui(plugin, service);
        this.wardrobeGui = wardrobeGui;
        this.manageListGui = manageListGui;
        this.manageEditGui = manageEditGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("Only players can use this.");
                return true;
            }
            wardrobeGui.open(p);
            return true;
        }

        String sub = args[0].toLowerCase();


        if (sub.equals("reload")) {
            if (!sender.hasPermission("newgen.cosmetic.reload")) {
                sender.sendMessage(Chat.color("&cYou don't have permission."));
                return true;
            }

            plugin.reloadConfig();


            sender.sendMessage(Chat.color("&aSuccessfully reloaded config.yml."));
            return true;
        }

        if (sub.equals("admin")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("Only players can use this.");
                return true;
            }
            if (!p.hasPermission("newgen.cosmetic.admin")) {
                p.sendMessage(Chat.color("&cYou don't have permission."));
                return true;
            }
            adminGui.open(p);
            return true;
        }

        if (sub.equals("manage")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("Only players can use this.");
                return true;
            }
            if (!p.hasPermission("newgen.cosmetic.admin")) {
                p.sendMessage(Chat.color("&cYou don't have permission."));
                return true;
            }
            manageListGui.open(p, 0);
            return true;
        }

        if (sub.equals("add")) {
            if (!sender.hasPermission("newgen.cosmetic.add")) {
                sender.sendMessage(Chat.color("&cYou don't have permission."));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(Chat.color("&cUsage: /cosmetic add <player> <cosmeticId>"));
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(Chat.color("&cPlayer is offline or the name is incorrect."));
                return true;
            }

            String cosmeticId = args[2];
            if (!service.getDatabase().cosmeticExists(cosmeticId)) {
                sender.sendMessage(Chat.color("&cCosmetic ID does not exist: &f" + cosmeticId));
                return true;
            }

            service.grantToPlayer(target, cosmeticId);

            sender.sendMessage(Chat.color("&aGranted &f" + cosmeticId + " &acho &f" + target.getName()));
            target.sendMessage(Chat.color("&aYou have been granted cosmetic: &f" + cosmeticId));
            return true;
        }

        if (sub.equals("equip")) {
            if (!sender.hasPermission("newgen.cosmetic.equip")) {
                sender.sendMessage(Chat.color("&cYou don't have permission."));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(Chat.color("&cUsage: /cosmetic equip <player> <cosmeticId>"));
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(Chat.color("&cPlayer is offline or the name is incorrect."));
                return true;
            }

            String cosmeticId = args[2];
            CosmeticRecord record = service.getCosmeticRecord(cosmeticId);
            if (record == null) {
                sender.sendMessage(Chat.color("&cCosmetic does not exist: &f" + cosmeticId));
                return true;
            }

            ItemStack wearable = service.buildWearableFromRecord(record);

            service.forceEquipHelmet(target, wearable);
            service.setEquippedCosmeticId(target, cosmeticId);

            sender.sendMessage(Chat.color("&aForce-equipped &f" + cosmeticId + " &acho &f" + target.getName()));
            target.sendMessage(Chat.color("&aYou have been force-equipped with cosmetic: &f" + cosmeticId));
            return true;
        }

        sender.sendMessage(Chat.color("&cInvalid command."));
        sender.sendMessage(Chat.color("&7Usage: &f/cosmetic&7 | &f/cosmetic admin&7 | &f/cosmetic manage&7 | &f/cosmetic reload&7 | &f/cosmetic add <player> <id>&7 | &f/cosmetic equip <player> <id>"));
        return true;
    }
}
