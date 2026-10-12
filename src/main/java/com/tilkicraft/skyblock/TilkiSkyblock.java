package com.tilkicraft.skyblock;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class TilkiSkyblock extends JavaPlugin implements CommandExecutor, TabCompleter {
    final Map<UUID, Island> islands = new LinkedHashMap<>();
    final Map<UUID, UUID> invitations = new HashMap<>();
    Economy economy;
    World world;
    Menus menus;
    private File dataFile;

    @Override public void onEnable() {
        saveDefaultConfig();
        economy = new Economy(this);
        dataFile = new File(getDataFolder(), "islands.yml");
        loadIslands();
        world = Bukkit.createWorld(new WorldCreator(getConfig().getString("world", "tilki_skyblock"))
            .environment(World.Environment.NORMAL).generator(new VoidGenerator()));
        if (world == null) { getLogger().severe("Ada dünyası oluşturulamadı."); Bukkit.getPluginManager().disablePlugin(this); return; }
        world.setSpawnLocation(0, 80, 0);
        menus = new Menus(this);
        Objects.requireNonNull(getCommand("is")).setExecutor(this);
        Objects.requireNonNull(getCommand("is")).setTabCompleter(this);
        Bukkit.getPluginManager().registerEvents(new IslandListener(this), this);
        Bukkit.getPluginManager().registerEvents(menus, this);
        Bukkit.getScheduler().runTaskTimer(this, this::saveIslands, 600L, 600L);
    }

    @Override public void onDisable() { saveIslands(); }

    private void loadIslands() {
        if (!dataFile.isFile()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        var section = yaml.getConfigurationSection("islands");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            var child = section.getConfigurationSection(key);
            if (child == null) continue;
            try { Island island = Island.load(child); islands.put(island.owner, island); }
            catch (IllegalArgumentException e) { getLogger().warning("Geçersiz ada kaydı: " + key); }
        }
    }

    void saveIslands() {
        if (dataFile == null) return;
        YamlConfiguration yaml = new YamlConfiguration();
        for (Island island : islands.values()) island.save(yaml.createSection("islands." + island.owner));
        try { yaml.save(dataFile); } catch (IOException e) { getLogger().severe("Ada verileri kaydedilemedi: " + e.getMessage()); }
    }

    Island ownIsland(UUID id) {
        for (Island island : islands.values()) if (island.isMember(id)) return island;
        return null;
    }

    Island islandAt(Location location) {
        if (location.getWorld() != world) return null;
        int spacing = getConfig().getInt("island-spacing", 256);
        for (Island island : islands.values()) {
            int x = island.index * spacing;
            if (Math.abs(location.getBlockX() - x) <= 110 && Math.abs(location.getBlockZ()) <= 110) return island;
        }
        return null;
    }

    int radius(Island island) { return intLevel("border-radii", island.borderLevel, 25); }
    int capacity(Island island) { return intLevel("storage-capacities", island.storageLevel, 10000); }
    int memberLimit(Island island) { return intLevel("member-limits", island.memberLevel, 2); }
    int pistonLimit(Island island) { return intLevel("piston-limits", island.pistonLevel, 500); }
    int intLevel(String path, int level, int fallback) {
        List<Integer> values = getConfig().getIntegerList(path);
        return values.isEmpty() ? fallback : values.get(Math.min(Math.max(level, 0), values.size() - 1));
    }

    void createIsland(org.bukkit.entity.Player player) {
        if (ownIsland(player.getUniqueId()) != null) { player.sendMessage("§cZaten bir adaya kayıtlısın."); return; }
        int index = islands.values().stream().mapToInt(i -> i.index).max().orElse(-1) + 1;
        Island island = new Island(player.getUniqueId(), index);
        int center = index * getConfig().getInt("island-spacing", 256);
        world.getChunkAt(center >> 4, 0).load();
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            world.getBlockAt(center + dx, 78, dz).setType(Material.DIRT);
            world.getBlockAt(center + dx, 79, dz).setType(Material.GRASS_BLOCK);
        }
        world.getBlockAt(center, 77, 0).setType(Material.BEDROCK);
        for (int y = 80; y <= 83; y++) world.getBlockAt(center + 1, y, 1).setType(Material.OAK_LOG);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if (Math.abs(dx) + Math.abs(dz) <= 3) world.getBlockAt(center + 1 + dx, 84, 1 + dz).setType(Material.OAK_LEAVES);
        }
        world.getBlockAt(center + 1, 85, 1).setType(Material.OAK_LEAVES);
        world.getBlockAt(center - 1, 80, -1).setType(Material.CHEST);
        Chest chest = (Chest) world.getBlockAt(center - 1, 80, -1).getState();
        chest.getBlockInventory().addItem(new ItemStack(Material.LAVA_BUCKET), new ItemStack(Material.WATER_BUCKET),
            new ItemStack(Material.OAK_SAPLING), new ItemStack(Material.ICE, 2),
            new ItemStack(Material.MELON_SEEDS), new ItemStack(Material.PUMPKIN_SEEDS));
        islands.put(island.owner, island);
        saveIslands();
        player.teleport(home(island));
        player.sendMessage("§aKlasik SkyBlock adan kuruldu! §7/is ile menüyü açabilirsin.");
    }

    Location home(Island island) { return new Location(world, island.index * getConfig().getInt("island-spacing", 256) - 1.5, 80, 1.5, 0, 0); }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof org.bukkit.entity.Player player)) { sender.sendMessage("Bu komut oyuncular içindir."); return true; }
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        Island island = ownIsland(player.getUniqueId());
        if (sub.equals("create") && args.length != 1) { player.sendMessage("§cKullanım: /is create (ada adı gerekmez)"); return true; }
        if (sub.isEmpty() || sub.equals("create")) {
            if (island == null) createIsland(player);
            else if (sub.isEmpty()) menus.openMain(player, island);
            else player.sendMessage("§cZaten bir adaya kayıtlısın.");
            return true;
        }
        if (sub.equals("join") && args.length == 1) {
            UUID owner = invitations.remove(player.getUniqueId());
            Island target = owner == null ? null : islands.get(owner);
            if (island != null || target == null) { player.sendMessage("§cKatılabileceğin bir ada daveti yok."); return true; }
            if (target.members.size() >= memberLimit(target)) { player.sendMessage("§cBu adanın üye sınırı dolu."); return true; }
            target.members.put(player.getUniqueId(), Island.Role.MEMBER);
            saveIslands(); player.teleport(home(target)); player.sendMessage("§aAdaya katıldın."); return true;
        }
        if (island == null) { player.sendMessage("§cÖnce /is ile ada kur veya davet kabul et."); return true; }
        switch (sub) {
            case "home" -> player.teleport(home(island));
            case "invite" -> {
                if (!island.canManage(player.getUniqueId()) || args.length != 2) { player.sendMessage("§cKullanım: /is invite <oyuncu>"); break; }
                var target = Bukkit.getPlayerExact(args[1]);
                if (target == null || ownIsland(target.getUniqueId()) != null || target.equals(player)) { player.sendMessage("§cOyuncu çevrimdışı veya zaten bir adada."); break; }
                if (island.members.size() >= memberLimit(island)) { player.sendMessage("§cÜye sınırı dolu."); break; }
                invitations.put(target.getUniqueId(), island.owner);
                target.sendMessage("§a" + player.getName() + " seni adasına davet etti. Kabul: §e/is join");
                player.sendMessage("§aDavet gönderildi.");
            }
            case "leave" -> {
                if (island.owner.equals(player.getUniqueId())) { player.sendMessage("§cAda sahibi adadan ayrılamaz."); break; }
                island.members.remove(player.getUniqueId()); saveIslands(); player.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
                player.sendMessage("§eAdadan ayrıldın.");
            }
            case "kick" -> {
                if (!island.canManage(player.getUniqueId()) || args.length != 2) { player.sendMessage("§cKullanım: /is kick <oyuncu>"); break; }
                var target = Bukkit.getOfflinePlayer(args[1]);
                Island.Role role = island.members.get(target.getUniqueId());
                if (role == null || role == Island.Role.OWNER || (role == Island.Role.OFFICER && !island.owner.equals(player.getUniqueId()))) { player.sendMessage("§cBu üyeyi çıkaramazsın."); break; }
                island.members.remove(target.getUniqueId()); saveIslands();
                if (target.isOnline()) target.getPlayer().teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
                player.sendMessage("§aÜye çıkarıldı.");
            }
            default -> player.sendMessage("§e/is, /is create, /is home, /is invite <oyuncu>, /is join, /is leave, /is kick <oyuncu>");
        }
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("create", "home", "invite", "join", "leave", "kick").stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        if (args.length == 2 && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("kick"))) return Bukkit.getOnlinePlayers().stream().map(org.bukkit.entity.Player::getName).filter(s -> s.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        return List.of();
    }

    private static final class VoidGenerator extends ChunkGenerator {
        @Override public void generateNoise(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) { }
    }
}
