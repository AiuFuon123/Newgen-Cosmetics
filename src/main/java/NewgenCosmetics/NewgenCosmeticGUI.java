package NewgenCosmetics;

import NewgenCosmetics.command.CosmeticCommand;
import NewgenCosmetics.command.CosmeticTabCompleter;
import NewgenCosmetics.db.DatabaseManager;
import NewgenCosmetics.gui.*;
import NewgenCosmetics.listener.*;
import NewgenCosmetics.service.CosmeticService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class NewgenCosmeticGUI extends JavaPlugin {

    private DatabaseManager databaseManager;
    private CosmeticService cosmeticService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getServer().getConsoleSender().sendMessage(
                org.bukkit.ChatColor.translateAlternateColorCodes('&',
                        "&f~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                                "&a =================================\n" +
                                "&e|                                 |\n" +
                                "&e|        Plugin by AiuFuon        |\n" +
                                "&e|         NewgenCosmetics         |\n" +
                                "&e|      Version: 1.0.0 Release     |\n" +
                                "&e|                                 |\n" +
                                "&a =================================\n" +
                                "&bYou can use this plugin to create cosmetics\n" +
                                "&bInspired by Hoplite.gg\n" +
                                "&bEach cosmetic can be any material, but it will still carry armor stats\n" +
                                "&bAnd it can be equipped/unequipped freely\n" +
                                "&a=============================================================================================================\n" +
                                "&a███   ██ ███████ ██     ██  ██████  ███████ ███   ██       ███████ ████████ ██    ██ ███████  ██████  ██████\n" +
                                "&a████  ██ ██      ██     ██ ██       ██      ████  ██       ██         ██    ██    ██ ██    ██   ██   ██    ██\n" +
                                "&a██ ██ ██ █████   ██  █  ██ ██  ████ █████   ██ ██ ██       ███████    ██    ██    ██ ██    ██   ██   ██    ██\n" +
                                "&a██  ████ ██      ██ ███ ██ ██    ██ ██      ██  ████            ██    ██    ██    ██ ██    ██   ██   ██    ██\n" +
                                "&a██   ███ ███████  ███ ███   ██████  ███████ ██   ███       ███████    ██     ██████  ███████  ██████  ██████\n" +
                                "&a=============================================================================================================\n" +
                                "&bBy the way join my Discord Server for more info!\n" +
                                "&a(\\_/)\n" +
                                "&a('w')\n" +
                                "&a(>  )> &eLink: discord.gg/newgenmc\n" +
                                "&a U.U\n" +
                                "&f~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
                )
        );

        databaseManager = new DatabaseManager(this);
        databaseManager.init();

        cosmeticService = new CosmeticService(this, databaseManager);

        CosmeticWardrobeGui wardrobeGui = new CosmeticWardrobeGui(this, cosmeticService);

        AdminManageListGui manageListGui = new AdminManageListGui(this, cosmeticService);
        AdminManageEditGui manageEditGui = new AdminManageEditGui(this, cosmeticService);

        Bukkit.getPluginManager().registerEvents(new GuiListener(cosmeticService), this);
        Bukkit.getPluginManager().registerEvents(new CosmeticEquipListener(cosmeticService), this);
        Bukkit.getPluginManager().registerEvents(new WardrobeListener(wardrobeGui, cosmeticService), this);
        Bukkit.getPluginManager().registerEvents(new JoinEquipListener(this, cosmeticService), this);

        Bukkit.getPluginManager().registerEvents(new AdminManageListener(cosmeticService, manageListGui, manageEditGui), this);

        var cmd = getCommand("cosmetic");
        if (cmd != null) {
            cmd.setExecutor(new CosmeticCommand(this, cosmeticService, wardrobeGui, manageListGui, manageEditGui));
            cmd.setTabCompleter(new CosmeticTabCompleter(cosmeticService));
        }

        getLogger().info("Enabled NewgenCosmetics");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) databaseManager.close();
    }
}
