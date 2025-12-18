package NewgenCosmetics.command;

import NewgenCosmetics.service.CosmeticService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CosmeticTabCompleter implements TabCompleter {

    private final CosmeticService service;

    public CosmeticTabCompleter(CosmeticService service) {
        this.service = service;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            String p = args[0].toLowerCase(Locale.ROOT);
            List<String> base = new ArrayList<>();
            base.add("admin");
            base.add("manage");
            base.add("reload");
            base.add("add");
            base.add("equip");
            return filterPrefix(base, p);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("add")) {
            if (args.length == 2) {
                return filterPrefix(onlinePlayers(), args[1].toLowerCase(Locale.ROOT));
            }
            if (args.length == 3) {
                return suggestCosmeticIds(args[2], 30);
            }
        }

        if (sub.equals("equip")) {
            if (args.length == 2) {
                return filterPrefix(onlinePlayers(), args[1].toLowerCase(Locale.ROOT));
            }
            if (args.length == 3) {
                return suggestCosmeticIds(args[2], 30);
            }
        }

        return List.of();
    }

    private List<String> onlinePlayers() {
        List<String> list = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) list.add(p.getName());
        return list;
    }

    private List<String> suggestCosmeticIds(String prefix, int limit) {
        String p = (prefix == null) ? "" : prefix.trim();
        List<String> res = service.searchCosmeticIdsPrefix(p, limit);
        if (res != null && !res.isEmpty()) return res;
        return filterPrefix(service.getAllCosmeticIds(), p.toLowerCase(Locale.ROOT));
    }

    private List<String> filterPrefix(List<String> input, String prefixLower) {
        if (input == null) return List.of();
        if (prefixLower == null) prefixLower = "";
        String p = prefixLower.toLowerCase(Locale.ROOT);

        List<String> out = new ArrayList<>();
        for (String s : input) {
            if (s == null) continue;
            if (p.isEmpty() || s.toLowerCase(Locale.ROOT).startsWith(p)) out.add(s);
        }
        return out;
    }
}
