package com.tilkicraft.skyblock;

import java.util.*;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

final class Island {
    enum Role { MEMBER, OFFICER, OWNER }

    final UUID owner;
    final int index;
    final Map<UUID, Role> members = new LinkedHashMap<>();
    final Map<Material, Long> storage = new EnumMap<>(Material.class);
    int storageLevel, borderLevel, generatorLevel, memberLevel, pistonLevel;
    int pistonCount;
    boolean chestCollect = true, autoSell, autoSmelt, autoHarvest;
    long level;

    Island(UUID owner, int index) {
        this.owner = owner;
        this.index = index;
        members.put(owner, Role.OWNER);
    }

    static Island load(ConfigurationSection section) {
        Island island = new Island(UUID.fromString(section.getString("owner")), section.getInt("index"));
        island.storageLevel = section.getInt("storage-level");
        island.borderLevel = section.getInt("border-level");
        island.generatorLevel = section.getInt("generator-level");
        island.memberLevel = section.getInt("member-level");
        island.pistonLevel = section.getInt("piston-level");
        island.pistonCount = section.getInt("piston-count");
        island.chestCollect = section.getBoolean("chest-collect", true);
        island.autoSell = section.getBoolean("auto-sell");
        island.autoSmelt = section.getBoolean("auto-smelt");
        island.autoHarvest = section.getBoolean("auto-harvest");
        island.level = section.getLong("level");
        ConfigurationSection members = section.getConfigurationSection("members");
        if (members != null) for (String id : members.getKeys(false)) {
            try { island.members.put(UUID.fromString(id), Role.valueOf(members.getString(id))); }
            catch (IllegalArgumentException ignored) { }
        }
        island.members.put(island.owner, Role.OWNER);
        ConfigurationSection storage = section.getConfigurationSection("storage");
        if (storage != null) for (String name : storage.getKeys(false)) {
            Material material = Material.matchMaterial(name);
            if (material != null && storage.getLong(name) > 0) island.storage.put(material, storage.getLong(name));
        }
        return island;
    }

    void save(ConfigurationSection section) {
        section.set("owner", owner.toString()); section.set("index", index);
        section.set("storage-level", storageLevel); section.set("border-level", borderLevel);
        section.set("generator-level", generatorLevel); section.set("member-level", memberLevel);
        section.set("piston-level", pistonLevel); section.set("piston-count", pistonCount);
        section.set("chest-collect", chestCollect); section.set("auto-sell", autoSell);
        section.set("auto-smelt", autoSmelt); section.set("auto-harvest", autoHarvest);
        section.set("level", level);
        for (var entry : members.entrySet()) section.set("members." + entry.getKey(), entry.getValue().name());
        for (var entry : storage.entrySet()) section.set("storage." + entry.getKey().name(), entry.getValue());
    }

    boolean isMember(UUID id) { return members.containsKey(id); }
    boolean canManage(UUID id) { return members.get(id) == Role.OWNER || members.get(id) == Role.OFFICER; }
    long totalStored() { return storage.values().stream().mapToLong(Long::longValue).sum(); }
}
