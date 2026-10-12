package com.tilkicraft.skyblock;

import java.util.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

final class IslandListener implements Listener {
    private final TilkiSkyblock plugin;
    IslandListener(TilkiSkyblock plugin) { this.plugin = plugin; }

    private boolean allowed(Player player, Block block) {
        if (block.getWorld() != plugin.world) return true;
        if (player.hasPermission("tilkiskyblock.admin")) return true;
        Island island = plugin.islandAt(block.getLocation());
        if (island == null || !island.isMember(player.getUniqueId())) return false;
        int center = island.index * plugin.getConfig().getInt("island-spacing", 256);
        return Math.abs(block.getX() - center) <= plugin.radius(island) && Math.abs(block.getZ()) <= plugin.radius(island);
    }

    @EventHandler(ignoreCancelled = true) public void onPlace(BlockPlaceEvent event) {
        if (!allowed(event.getPlayer(), event.getBlockPlaced())) { event.setCancelled(true); event.getPlayer().sendMessage("§cBu alanda blok koyamazsın."); return; }
        Island island = plugin.islandAt(event.getBlockPlaced().getLocation());
        if (island == null || !isPiston(event.getBlockPlaced().getType())) return;
        if (island.pistonCount >= plugin.pistonLimit(island)) { event.setCancelled(true); event.getPlayer().sendMessage("§cPiston sınırına ulaştın."); return; }
        island.pistonCount++; plugin.saveIslands();
    }

    @EventHandler(ignoreCancelled = true) public void onBreak(BlockBreakEvent event) {
        if (!allowed(event.getPlayer(), event.getBlock())) { event.setCancelled(true); event.getPlayer().sendMessage("§cBu alanda blok kıramazsın."); return; }
        Island island = plugin.islandAt(event.getBlock().getLocation());
        if (island == null) return;
        if (isPiston(event.getBlock().getType())) island.pistonCount = Math.max(0, island.pistonCount - 1);
        island.level++;
    }

    @EventHandler(ignoreCancelled = true) public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block != null && !allowed(event.getPlayer(), block)) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true) public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!allowed(event.getPlayer(), event.getBlockClicked().getRelative(event.getBlockFace()))) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true) public void onBucketFill(PlayerBucketFillEvent event) {
        if (!allowed(event.getPlayer(), event.getBlockClicked())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true) public void onEntityExplode(EntityExplodeEvent event) {
        if (event.getLocation().getWorld() == plugin.world) event.blockList().clear();
    }

    @EventHandler(ignoreCancelled = true) public void onBlockExplode(BlockExplodeEvent event) {
        if (event.getBlock().getWorld() == plugin.world) event.blockList().clear();
    }

    @EventHandler(ignoreCancelled = true) public void onDrop(BlockDropItemEvent event) {
        Island island = plugin.islandAt(event.getBlockState().getLocation());
        if (island == null || !island.autoHarvest) return;
        Iterator<Item> iterator = event.getItems().iterator();
        while (iterator.hasNext()) {
            Item item = iterator.next();
            int remaining = store(island, item.getItemStack());
            if (remaining == 0) iterator.remove();
            else {
                ItemStack stack = item.getItemStack();
                stack.setAmount(remaining);
                item.setItemStack(stack);
            }
        }
    }

    @EventHandler(ignoreCancelled = true) public void onItemSpawn(ItemSpawnEvent event) {
        Island island = plugin.islandAt(event.getLocation());
        if (island == null || !island.chestCollect) return;
        Item item = event.getEntity();
        ItemStack stack = item.getItemStack();
        int remaining = store(island, stack);
        if (remaining == 0) event.setCancelled(true);
        else if (remaining < stack.getAmount()) { stack.setAmount(remaining); item.setItemStack(stack); }
    }

    private int store(Island island, ItemStack stack) {
        if (stack == null || stack.getType().isAir() || stack.hasItemMeta()) return stack == null ? 0 : stack.getAmount();
        Material material = island.autoSmelt ? smelt(stack.getType()) : stack.getType();
        int amount = stack.getAmount();
        long room = Math.max(0, (long) plugin.capacity(island) - island.totalStored());
        if (room < amount && island.autoSell) {
            sellToMakeRoom(island, amount - room);
            room = Math.max(0, (long) plugin.capacity(island) - island.totalStored());
        }
        int collected = (int) Math.min(room, amount);
        if (collected > 0) {
            island.storage.merge(material, (long) collected, Long::sum);
        }
        return amount - collected;
    }

    private void sellToMakeRoom(Island island, long needed) {
        if (!plugin.economy.available()) return;
        for (var entry : new ArrayList<>(island.storage.entrySet())) {
            if (needed <= 0) break;
            double price = plugin.getConfig().getDouble("sell-prices." + entry.getKey().name());
            if (price <= 0 || entry.getValue() <= 0) continue;
            long count = Math.min(entry.getValue(), needed);
            if (!plugin.economy.deposit(Bukkit.getOfflinePlayer(island.owner), count * price)) continue;
            island.storage.put(entry.getKey(), entry.getValue() - count);
            needed -= count;
        }
    }

    private Material smelt(Material material) {
        return switch (material) {
            case RAW_IRON -> Material.IRON_INGOT;
            case RAW_GOLD -> Material.GOLD_INGOT;
            case RAW_COPPER -> Material.COPPER_INGOT;
            case COBBLESTONE -> Material.STONE;
            case SAND -> Material.GLASS;
            case CLAY_BALL -> Material.BRICK;
            case BEEF -> Material.COOKED_BEEF;
            case CHICKEN -> Material.COOKED_CHICKEN;
            case PORKCHOP -> Material.COOKED_PORKCHOP;
            case MUTTON -> Material.COOKED_MUTTON;
            case COD -> Material.COOKED_COD;
            case SALMON -> Material.COOKED_SALMON;
            case POTATO -> Material.BAKED_POTATO;
            default -> material;
        };
    }

    @EventHandler(ignoreCancelled = true) public void onGenerator(BlockFormEvent event) {
        if (event.getNewState().getType() != Material.COBBLESTONE) return;
        Island island = plugin.islandAt(event.getBlock().getLocation());
        if (island == null || island.generatorLevel == 0) return;
        double roll = Math.random();
        Material result = island.generatorLevel >= 3 && roll < 0.02 ? Material.DIAMOND_ORE
            : island.generatorLevel >= 2 && roll < 0.08 ? Material.GOLD_ORE
            : roll < 0.20 ? Material.IRON_ORE : Material.COBBLESTONE;
        event.getNewState().setType(result);
    }

    @EventHandler public void onChat(AsyncPlayerChatEvent event) {
        Island island = plugin.ownIsland(event.getPlayer().getUniqueId());
        long level = island == null ? 0 : island.level;
        event.setFormat("§c[Lv." + level + "]§r " + event.getFormat());
    }

    private boolean isPiston(Material material) { return material == Material.PISTON || material == Material.STICKY_PISTON; }
}
