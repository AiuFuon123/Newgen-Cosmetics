package NewgenCosmetics.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RegionLockUtil {

    private RegionLockUtil() {}

    public static boolean isUnequipLockedHere(Plugin plugin, Player player) {
        if (plugin == null || player == null) return false;

        if (!plugin.getConfig().getBoolean("region-lock.enabled", false)) return false;

        List<String> regionNames = plugin.getConfig().getStringList("region-lock.names");
        if (regionNames == null || regionNames.isEmpty()) return false;

        Set<String> target = new HashSet<>();
        for (String s : regionNames) {
            if (s != null && !s.isBlank()) target.add(s.toLowerCase());
        }
        if (target.isEmpty()) return false;

        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) return false;

        try {
            return isInAnyWorldGuardRegion(player.getLocation(), target);
        } catch (Throwable t) {

            return false;
        }
    }

    public static String getLockMessage(Plugin plugin) {
        String msg = plugin.getConfig().getString("region-lock.message", "&cYou cannot unequip cosmetic here.");
        return Chat.color(msg);
    }

    @SuppressWarnings("unchecked")
    private static boolean isInAnyWorldGuardRegion(Location bukkitLoc, Set<String> targetLower) throws Exception {
        if (bukkitLoc == null) return false;

        Class<?> wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
        Method getInstance = wgClass.getMethod("getInstance");
        Object wg = getInstance.invoke(null);

        Method getPlatform = wgClass.getMethod("getPlatform");
        Object platform = getPlatform.invoke(wg);
        Method getRegionContainer = platform.getClass().getMethod("getRegionContainer");
        Object regionContainer = getRegionContainer.invoke(platform);

        Method createQuery = regionContainer.getClass().getMethod("createQuery");
        Object query = createQuery.invoke(regionContainer);

        Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
        Method adapt = bukkitAdapterClass.getMethod("adapt", Location.class);
        Object weLoc = adapt.invoke(null, bukkitLoc);

        Object applicable;
        Method getApplicableRegions = query.getClass().getMethod("getApplicableRegions", weLoc.getClass());
        applicable = getApplicableRegions.invoke(query, weLoc);

        Method getRegions = applicable.getClass().getMethod("getRegions");
        Set<?> regions = (Set<?>) getRegions.invoke(applicable);
        if (regions == null || regions.isEmpty()) return false;

        for (Object pr : regions) {
            if (pr == null) continue;
            Method getId = pr.getClass().getMethod("getId");
            String id = (String) getId.invoke(pr);
            if (id != null && targetLower.contains(id.toLowerCase())) return true;
        }
        return false;
    }
}
