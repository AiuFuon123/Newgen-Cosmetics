# Newgen-Cosmetics

A lightweight cosmetics plugin inspired by Hoplite.gg.  
Create and manage head cosmetics with armor stats, let players equip them through an in-game wardrobe GUI, and keep everything stored safely in `data.db` (SQLite) with optional Redis caching.

**Author:** AiuFuon  
**Version:** 1.0.0

---

## Features

- **Cosmetic Wardrobe GUI** (`/cosmetic`)
- **Admin Cosmetics Manager GUI** (create, edit, manage cosmetics)
- **Cosmetics can be any material** (items are used as head cosmetics)
- **Armor stats support** (armor + toughness applied from config)
- **Persistent storage**
  - SQLite (`data.db`)
  - Optional **Redis caching** (toggle in config)
- **Region lock (WorldGuard)**
  - Prevent unequipping cosmetics in specific regions (e.g. `spawn`)
- **Safe equip behavior**
  - Clicking an already-equipped cosmetic does **nothing**
  - Only changes when the player selects a different cosmetic

---

## Requirements

- **Paper/Spigot** 1.21.8 (recommended: latest Paper)
- Java **17+** (recommended: Java 21)
- (Optional) **Redis** server if you enable redis
- (Optional) **WorldGuard** if you enable region-lock

---

## Installation

1. Put `Newgen-Cosmetics.jar` into your server `plugins/` folder  
2. Restart the server  
3. Edit `plugins/Newgen-Cosmetics/config.yml` to your liking  
4. Restart again to apply changes

---

## Commands

| Command | Description |
|--------|-------------|
| `/cosmetic` | Open the Cosmetic Wardrobe GUI |
| `/cosmetic admin` | Open the Admin Cosmetics manager (admin only) |

> Exact subcommands may differ depending on your build, but `/cosmetic` is the main entry point.

---

## Permissions

| Permission | Description |
|-----------|-------------|
| `newgen.cosmetic.admin` | Access admin features (manager GUIs, creation/editing) |

---

## Configuration

Main config file: `plugins/Newgen-Cosmetics/config.yml`

Key sections:
- `gui.*` — titles, sizes, slots, button items, lore
- `database.file` — SQLite file name (default: `data.db`)
- `item.armor` / `item.toughness` — stats applied for cosmetics
- `redis.*` — Redis settings (optional)
- `region-lock.*` — WorldGuard regions where unequipping is blocked

### Redis key prefix

If you change `redis.key-prefix`, the plugin will use a different namespace in Redis.  
This means old cached data will not be read anymore (acts like a fresh cache).  
Use a stable value such as: `newgen:cosmetics:`

---

## Storage

- **SQLite:** `plugins/Newgen-Cosmetics/data.db`
- If SQLite WAL mode is used, you may also see:
  - `data.db-wal`
  - `data.db-shm`

When backing up, stop the server and copy all DB files together.

---

## Support & Community

Discord: `discord.gg/newgenmc`

---

## License

This project is provided as-is for your server use.  
If you plan to redistribute or sell modified builds, please credit **AiuFuon** and keep the original project name references.
