package NewgenCosmetics.db;

import NewgenCosmetics.model.CosmeticRecord;
import NewgenCosmetics.redis.RedisCache;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.*;
import java.util.*;

public class DatabaseManager {

    private final Plugin plugin;
    private Connection connection;

    private RedisCache redis;

    public DatabaseManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public void init() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) dataFolder.mkdirs();

            String fileName = plugin.getConfig().getString("database.file", "data.db");
            File dbFile = new File(dataFolder, fileName);

            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            try (Statement st = connection.createStatement()) {
                st.executeUpdate("PRAGMA journal_mode=WAL;");
                st.executeUpdate("PRAGMA synchronous=NORMAL;");

                st.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS cosmetics (" +
                                "id TEXT PRIMARY KEY," +
                                "owner_uuid TEXT NOT NULL," +
                                "material TEXT NOT NULL," +
                                "custom_model_data INTEGER," +
                                "display_name TEXT," +
                                "lore_json TEXT," +
                                "item_base64 TEXT NOT NULL," +
                                "created_at INTEGER NOT NULL" +
                                ");"
                );
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_cosmetics_owner ON cosmetics(owner_uuid);");

                st.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS cosmetic_access (" +
                                "player_uuid TEXT NOT NULL," +
                                "cosmetic_id TEXT NOT NULL," +
                                "granted_at INTEGER NOT NULL," +
                                "PRIMARY KEY(player_uuid, cosmetic_id)" +
                                ");"
                );
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_access_player ON cosmetic_access(player_uuid);");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_access_cosmetic ON cosmetic_access(cosmetic_id);");
            }

            plugin.getLogger().info("SQLite ready: " + dbFile.getName());

            redis = new RedisCache(plugin);
            redis.testConnectionLog();

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to init database: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }


    public void insertCosmetic(CosmeticRecord r) {
        final String sql =
                "INSERT INTO cosmetics (id, owner_uuid, material, custom_model_data, display_name, lore_json, item_base64, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, r.id());
            ps.setString(2, r.ownerUuid());
            ps.setString(3, r.material());

            if (r.customModelData() == null) ps.setNull(4, Types.INTEGER);
            else ps.setInt(4, r.customModelData());

            ps.setString(5, r.displayName());
            ps.setString(6, r.loreJson());
            ps.setString(7, r.itemBase64());
            ps.setLong(8, r.createdAt());

            ps.executeUpdate();

            if (redis != null && redis.isEnabled()) {
                redis.cacheAddCosmeticId(r.id());
                redis.cacheSetCosmeticHash(r.id(), recordToMap(r));
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to insert cosmetic: " + e.getMessage());
        }
    }

    public boolean cosmeticExists(String cosmeticId) {
        if (redis != null && redis.isEnabled()) {
            Map<String, String> m = redis.cacheGetCosmeticHash(cosmeticId);
            if (m != null) return true;
        }

        final String sql = "SELECT 1 FROM cosmetics WHERE id = ? LIMIT 1;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, cosmeticId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("cosmeticExists failed: " + e.getMessage());
            return false;
        }
    }

    public CosmeticRecord getCosmeticById(String cosmeticId) {
        if (redis != null && redis.isEnabled()) {
            Map<String, String> m = redis.cacheGetCosmeticHash(cosmeticId);
            if (m != null) {
                CosmeticRecord r = mapToRecord(m);
                if (r != null) return r;
            }
        }

        final String sql =
                "SELECT id, owner_uuid, material, custom_model_data, display_name, lore_json, item_base64, created_at " +
                        "FROM cosmetics WHERE id = ? LIMIT 1;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, cosmeticId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                Integer cmd = null;
                int cmdVal = rs.getInt("custom_model_data");
                if (!rs.wasNull()) cmd = cmdVal;

                CosmeticRecord r = new CosmeticRecord(
                        rs.getString("id"),
                        rs.getString("owner_uuid"),
                        rs.getString("material"),
                        cmd,
                        rs.getString("display_name"),
                        rs.getString("lore_json"),
                        rs.getString("item_base64"),
                        rs.getLong("created_at")
                );

                if (redis != null && redis.isEnabled()) {
                    redis.cacheSetCosmeticHash(r.id(), recordToMap(r));
                    redis.cacheAddCosmeticId(r.id());
                }

                return r;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("getCosmeticById failed: " + e.getMessage());
            return null;
        }
    }

    public List<String> getAllCosmeticIds() {
        if (redis != null && redis.isEnabled()) {
            List<String> cached = redis.cacheGetAllCosmeticIdsIfPresent();
            if (cached != null) return cached;
        }

        List<String> out = new ArrayList<>();
        final String sql = "SELECT id FROM cosmetics ORDER BY id ASC;";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(rs.getString("id"));
        } catch (SQLException e) {
            plugin.getLogger().severe("getAllCosmeticIds failed: " + e.getMessage());
        }

        if (redis != null && redis.isEnabled()) redis.cacheSetAllCosmeticIds(out);
        return out;
    }

    public List<String> searchCosmeticIdsPrefix(String prefix, int limit) {
        if (redis != null && redis.isEnabled()) {
            List<String> cached = redis.cacheSearchCosmeticIdsPrefix(prefix, limit);
            if (cached != null) return cached;
        }

        List<String> out = new ArrayList<>();
        final String sql = "SELECT id FROM cosmetics WHERE id LIKE ? ORDER BY id ASC LIMIT ?;";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            String p = (prefix == null) ? "" : prefix;
            p = p.replace("%", "").replace("_", "");
            ps.setString(1, p + "%");
            ps.setInt(2, Math.max(1, Math.min(limit, 100)));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getString("id"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("searchCosmeticIdsPrefix failed: " + e.getMessage());
        }

        return out;
    }


    public void grantCosmeticToPlayer(String playerUuid, String cosmeticId) {
        final String sql = "INSERT OR IGNORE INTO cosmetic_access(player_uuid, cosmetic_id, granted_at) VALUES(?, ?, ?);";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            ps.setString(2, cosmeticId);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();

            if (redis != null && redis.isEnabled()) {
                redis.cacheGrantPlayerCosmetic(playerUuid, cosmeticId);
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("grantCosmeticToPlayer failed: " + e.getMessage());
        }
    }

    public boolean playerHasCosmetic(String playerUuid, String cosmeticId) {
        if (redis != null && redis.isEnabled()) {
            Boolean has = redis.cachePlayerHasCosmetic(playerUuid, cosmeticId);
            if (has != null) return has;
        }

        final String sql = "SELECT 1 FROM cosmetic_access WHERE player_uuid = ? AND cosmetic_id = ? LIMIT 1;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            ps.setString(2, cosmeticId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("playerHasCosmetic failed: " + e.getMessage());
            return false;
        }
    }

    public List<String> getCosmeticsOfPlayer(String playerUuid) {
        if (redis != null && redis.isEnabled()) {
            List<String> cached = redis.cacheGetPlayerCosmeticsIfPresent(playerUuid);
            if (cached != null) return cached;
        }

        List<String> out = new ArrayList<>();
        final String sql = "SELECT cosmetic_id FROM cosmetic_access WHERE player_uuid = ? ORDER BY cosmetic_id ASC;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getString("cosmetic_id"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("getCosmeticsOfPlayer failed: " + e.getMessage());
        }

        if (redis != null && redis.isEnabled()) redis.cacheSetPlayerCosmetics(playerUuid, out);
        return out;
    }


    public int deleteAccessByCosmeticId(String cosmeticId) {
        final String sql = "DELETE FROM cosmetic_access WHERE cosmetic_id = ?;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, cosmeticId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("deleteAccessByCosmeticId failed: " + e.getMessage());
            return 0;
        }
    }


    public void setEquippedCosmeticId(String playerUuid, String cosmeticId) {
        if (redis == null || !redis.isEnabled()) return;
        redis.cacheSetEquippedCosmetic(playerUuid, cosmeticId);
    }

    public String getEquippedCosmeticId(String playerUuid) {
        if (redis == null || !redis.isEnabled()) return null;
        return redis.cacheGetEquippedCosmetic(playerUuid);
    }


    public boolean updateCosmeticKeepId(String cosmeticId,
                                        String material,
                                        Integer customModelData,
                                        String displayName,
                                        String loreJson,
                                        String itemBase64) {

        CosmeticRecord old = getCosmeticById(cosmeticId);
        if (old == null) return false;

        final String sql =
                "UPDATE cosmetics SET material=?, custom_model_data=?, display_name=?, lore_json=?, item_base64=? WHERE id=?;";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, material);

            if (customModelData == null) ps.setNull(2, Types.INTEGER);
            else ps.setInt(2, customModelData);

            ps.setString(3, displayName);
            ps.setString(4, loreJson);
            ps.setString(5, itemBase64);
            ps.setString(6, cosmeticId);

            int updated = ps.executeUpdate();
            if (updated <= 0) return false;


            if (redis != null && redis.isEnabled()) {
                CosmeticRecord newer = new CosmeticRecord(
                        old.id(),
                        old.ownerUuid(),
                        material,
                        customModelData,
                        displayName,
                        loreJson,
                        itemBase64,
                        old.createdAt()
                );
                redis.cacheSetCosmeticHash(cosmeticId, recordToMap(newer));
                redis.cacheAddCosmeticId(cosmeticId);
            }

            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("updateCosmeticKeepId failed: " + e.getMessage());
            return false;
        }
    }


    public boolean deleteCosmeticFully(String cosmeticId) {

        deleteAccessByCosmeticId(cosmeticId);

        final String sql = "DELETE FROM cosmetics WHERE id = ?;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, cosmeticId);
            int deleted = ps.executeUpdate();
            if (deleted <= 0) return false;

            if (redis != null && redis.isEnabled()) {
                redis.cacheRemoveCosmetic(cosmeticId);


            }
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("deleteCosmeticFully failed: " + e.getMessage());
            return false;
        }
    }

    public void close() {
        if (redis != null) {
            redis.close();
            redis = null;
        }
        if (connection == null) return;
        try { connection.close(); } catch (SQLException ignored) {}
    }


    private static Map<String, String> recordToMap(CosmeticRecord r) {
        Map<String, String> m = new HashMap<>();
        m.put("id", r.id());
        m.put("owner_uuid", r.ownerUuid());
        m.put("material", r.material());
        m.put("custom_model_data", r.customModelData() == null ? "" : String.valueOf(r.customModelData()));
        m.put("display_name", r.displayName() == null ? "" : r.displayName());
        m.put("lore_json", r.loreJson() == null ? "" : r.loreJson());
        m.put("item_base64", r.itemBase64());
        m.put("created_at", String.valueOf(r.createdAt()));
        return m;
    }

    private static CosmeticRecord mapToRecord(Map<String, String> m) {
        try {
            String id = m.get("id");
            String owner = m.get("owner_uuid");
            String material = m.get("material");
            String cmdStr = m.getOrDefault("custom_model_data", "");
            Integer cmd = (cmdStr == null || cmdStr.isBlank()) ? null : Integer.parseInt(cmdStr);
            String dn = m.getOrDefault("display_name", "");
            String lj = m.getOrDefault("lore_json", "");
            String b64 = m.get("item_base64");
            long created = Long.parseLong(m.getOrDefault("created_at", "0"));

            if (id == null || owner == null || material == null || b64 == null) return null;
            return new CosmeticRecord(id, owner, material, cmd, dn, lj, b64, created);
        } catch (Exception e) {
            return null;
        }
    }
}
