package NewgenCosmetics.redis;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import redis.clients.jedis.*;

import java.util.*;

public class RedisCache {

    private final Plugin plugin;
    private JedisPool pool;

    private final boolean enabled;
    private final String prefix;
    private final int ttlSeconds;

    private final String KEY_ALL_IDS_Z;
    private final String KEY_ALL_IDS_SET;

    public RedisCache(Plugin plugin) {
        this.plugin = plugin;

        FileConfiguration cfg = plugin.getConfig();
        this.enabled = cfg.getBoolean("redis.enabled", false);

        this.prefix = cfg.getString("redis.key-prefix", "newgen:cosmetic:");
        this.ttlSeconds = Math.max(0, cfg.getInt("redis.cache-ttl-seconds", 86400));

        this.KEY_ALL_IDS_Z = prefix + "cosmetics:ids:z";
        this.KEY_ALL_IDS_SET = prefix + "cosmetics:ids:set";

        if (!enabled) return;

        String host = cfg.getString("redis.host", "127.0.0.1");
        int port = cfg.getInt("redis.port", 6379);
        String username = cfg.getString("redis.username", "");
        String password = cfg.getString("redis.password", "");
        int database = cfg.getInt("redis.database", 0);
        boolean ssl = cfg.getBoolean("redis.ssl", false);
        int timeoutMs = Math.max(250, cfg.getInt("redis.timeout-ms", 1500));

        DefaultJedisClientConfig.Builder c = DefaultJedisClientConfig.builder()
                .timeoutMillis(timeoutMs)
                .database(database)
                .ssl(ssl);

        if (username != null && !username.isBlank()) c.user(username);
        if (password != null && !password.isBlank()) c.password(password);

        pool = new JedisPool(new HostAndPort(host, port), c.build());
    }

    public boolean isEnabled() {
        return enabled && pool != null;
    }

    public void testConnectionLog() {
        if (!isEnabled()) {
            plugin.getLogger().info("Redis cache disabled.");
            return;
        }
        try (Jedis j = pool.getResource()) {
            plugin.getLogger().info("Redis cache enabled. PING=" + j.ping());
        } catch (Exception e) {
            plugin.getLogger().severe("Redis connect failed -> disabling cache. Error: " + e.getMessage());
            close();
        }
    }

    public void close() {
        if (pool != null) {
            try { pool.close(); } catch (Exception ignored) {}
            pool = null;
        }
    }

    private String keyCosmeticHash(String id) { return prefix + "cosmetic:" + id; }
    private String keyPlayerAccessSet(String uuid) { return prefix + "access:" + uuid; }
    private String keyEquipped(String uuid) { return prefix + "equipped:" + uuid; }

    private void applyTTL(Jedis j, String key) {
        if (ttlSeconds > 0) j.expire(key, ttlSeconds);
    }

    public void cacheAddCosmeticId(String id) {
        if (!isEnabled() || id == null || id.isBlank()) return;
        try (Jedis j = pool.getResource()) {
            j.sadd(KEY_ALL_IDS_SET, id);
            applyTTL(j, KEY_ALL_IDS_SET);

            j.zadd(KEY_ALL_IDS_Z, 0, id);
            applyTTL(j, KEY_ALL_IDS_Z);
        } catch (Exception ignored) {}
    }

    public List<String> cacheGetAllCosmeticIdsIfPresent() {
        if (!isEnabled()) return null;
        try (Jedis j = pool.getResource()) {
            if (!j.exists(KEY_ALL_IDS_SET)) return null;
            Set<String> s = j.smembers(KEY_ALL_IDS_SET);
            if (s == null) return null;
            List<String> out = new ArrayList<>(s);
            out.sort(String::compareTo);
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    public void cacheSetAllCosmeticIds(Collection<String> ids) {
        if (!isEnabled()) return;
        try (Jedis j = pool.getResource()) {
            j.del(KEY_ALL_IDS_SET);
            j.del(KEY_ALL_IDS_Z);

            if (ids != null && !ids.isEmpty()) {
                Pipeline p = j.pipelined();
                for (String id : ids) {
                    p.sadd(KEY_ALL_IDS_SET, id);
                    p.zadd(KEY_ALL_IDS_Z, 0, id);
                }
                p.sync();
                applyTTL(j, KEY_ALL_IDS_SET);
                applyTTL(j, KEY_ALL_IDS_Z);
            }
        } catch (Exception ignored) {}
    }

    public List<String> cacheSearchCosmeticIdsPrefix(String prefixStr, int limit) {
        if (!isEnabled()) return null;
        String p = (prefixStr == null) ? "" : prefixStr;
        p = p.replace("%", "").replace("_", "");
        int lim = Math.max(1, Math.min(limit, 100));

        try (Jedis j = pool.getResource()) {
            if (!j.exists(KEY_ALL_IDS_Z)) return null;

            String min = "[" + p;
            String max = "[" + p + "\uFFFF";
            List<String> res = j.zrangeByLex(KEY_ALL_IDS_Z, min, max, 0, lim);
            return (res == null) ? new ArrayList<>() : res;
        } catch (Exception e) {
            return null;
        }
    }

    public Map<String, String> cacheGetCosmeticHash(String id) {
        if (!isEnabled()) return null;
        try (Jedis j = pool.getResource()) {
            String key = keyCosmeticHash(id);
            if (!j.exists(key)) return null;
            Map<String, String> m = j.hgetAll(key);
            return (m == null || m.isEmpty()) ? null : m;
        } catch (Exception e) {
            return null;
        }
    }

    public void cacheSetCosmeticHash(String id, Map<String, String> fields) {
        if (!isEnabled()) return;
        if (id == null || id.isBlank()) return;
        if (fields == null || fields.isEmpty()) return;

        try (Jedis j = pool.getResource()) {
            String key = keyCosmeticHash(id);
            j.hset(key, fields);
            applyTTL(j, key);

            j.sadd(KEY_ALL_IDS_SET, id);
            j.zadd(KEY_ALL_IDS_Z, 0, id);
            applyTTL(j, KEY_ALL_IDS_SET);
            applyTTL(j, KEY_ALL_IDS_Z);
        } catch (Exception ignored) {}
    }

    public void cacheRemoveCosmetic(String id) {
        if (!isEnabled() || id == null || id.isBlank()) return;
        try (Jedis j = pool.getResource()) {
            j.del(keyCosmeticHash(id));
            j.srem(KEY_ALL_IDS_SET, id);
            j.zrem(KEY_ALL_IDS_Z, id);
        } catch (Exception ignored) {}
    }

    public Boolean cachePlayerHasCosmetic(String uuid, String cosmeticId) {
        if (!isEnabled()) return null;
        try (Jedis j = pool.getResource()) {
            String key = keyPlayerAccessSet(uuid);
            if (!j.exists(key)) return null;
            return j.sismember(key, cosmeticId);
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> cacheGetPlayerCosmeticsIfPresent(String uuid) {
        if (!isEnabled()) return null;
        try (Jedis j = pool.getResource()) {
            String key = keyPlayerAccessSet(uuid);
            if (!j.exists(key)) return null;
            Set<String> s = j.smembers(key);
            if (s == null) return null;
            List<String> out = new ArrayList<>(s);
            out.sort(String::compareTo);
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    public void cacheSetPlayerCosmetics(String uuid, Collection<String> ids) {
        if (!isEnabled()) return;
        try (Jedis j = pool.getResource()) {
            String key = keyPlayerAccessSet(uuid);
            j.del(key);
            if (ids != null && !ids.isEmpty()) {
                j.sadd(key, ids.toArray(new String[0]));
                applyTTL(j, key);
            }
        } catch (Exception ignored) {}
    }

    public void cacheGrantPlayerCosmetic(String uuid, String cosmeticId) {
        if (!isEnabled()) return;
        try (Jedis j = pool.getResource()) {
            String key = keyPlayerAccessSet(uuid);
            j.sadd(key, cosmeticId);
            applyTTL(j, key);
        } catch (Exception ignored) {}
    }

    public void cacheSetEquippedCosmetic(String uuid, String cosmeticId) {
        if (!isEnabled()) return;
        if (uuid == null || uuid.isBlank()) return;

        try (Jedis j = pool.getResource()) {
            String key = keyEquipped(uuid);
            if (cosmeticId == null || cosmeticId.isBlank()) {
                j.del(key);
            } else {
                j.set(key, cosmeticId);
                applyTTL(j, key);
            }
        } catch (Exception ignored) {}
    }

    public String cacheGetEquippedCosmetic(String uuid) {
        if (!isEnabled()) return null;
        if (uuid == null || uuid.isBlank()) return null;

        try (Jedis j = pool.getResource()) {
            return j.get(keyEquipped(uuid));
        } catch (Exception ignored) {
            return null;
        }
    }
}
