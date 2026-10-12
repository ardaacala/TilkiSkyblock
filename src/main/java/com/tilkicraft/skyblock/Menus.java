package com.tilkicraft.skyblock;

import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

final class Menus implements Listener {
    private final TilkiSkyblock plugin;
    private enum Page { MAIN, MANAGEMENT, ADDONS, UPGRADES, MEMBERS, STORAGE, CONVERT }
    private static final Material[] STORAGE_ITEMS = {
        Material.WHEAT, Material.SWEET_BERRIES, Material.SUGAR_CANE, Material.GLOW_BERRIES, Material.CACTUS,
        Material.BAMBOO, Material.KELP, Material.POTATO, Material.BEETROOT, Material.PUMPKIN,
        Material.MELON, Material.MELON_SLICE, Material.WHEAT_SEEDS, Material.CARROT, Material.SLIME_BALL,
        Material.BONE, Material.ROTTEN_FLESH, Material.STRING, Material.SPIDER_EYE, Material.EGG,
        Material.IRON_INGOT, Material.GOLD_INGOT, Material.COPPER_INGOT, Material.RAW_COPPER,
        Material.DIAMOND, Material.EMERALD, Material.LAPIS_LAZULI, Material.REDSTONE, Material.COAL,
        Material.COBBLESTONE, Material.STONE, Material.OAK_LOG, Material.APPLE, Material.LEATHER,
        Material.RAW_IRON, Material.RAW_GOLD, Material.BEEF, Material.CHICKEN, Material.MUTTON,
        Material.PORKCHOP, Material.SALMON, Material.COD, Material.PUFFERFISH, Material.TROPICAL_FISH,
        Material.INK_SAC, Material.GUNPOWDER, Material.ENDER_PEARL, Material.NETHER_WART
    };
    private static final Material[] CONVERT_INPUTS = {
        Material.IRON_INGOT, Material.GOLD_INGOT, Material.EMERALD, Material.DIAMOND,
        Material.NETHERITE_INGOT, Material.RAW_IRON, Material.RAW_GOLD, Material.RAW_COPPER,
        Material.COAL, Material.LAPIS_LAZULI, Material.REDSTONE, Material.COPPER_INGOT
    };
    private static final Material[] CONVERT_OUTPUTS = {
        Material.IRON_BLOCK, Material.GOLD_BLOCK, Material.EMERALD_BLOCK, Material.DIAMOND_BLOCK,
        Material.NETHERITE_BLOCK, Material.RAW_IRON_BLOCK, Material.RAW_GOLD_BLOCK, Material.RAW_COPPER_BLOCK,
        Material.COAL_BLOCK, Material.LAPIS_BLOCK, Material.REDSTONE_BLOCK, Material.COPPER_BLOCK
    };

    private static final class MenuHolder implements InventoryHolder {
        final Page page; final UUID islandOwner; final int pageNumber;
        final Map<Integer, UUID> memberSlots = new HashMap<>();
        Inventory inventory;
        MenuHolder(Page page, UUID islandOwner, int pageNumber) { this.page = page; this.islandOwner = islandOwner; this.pageNumber = pageNumber; }
        @Override public Inventory getInventory() { return inventory; }
    }

    Menus(TilkiSkyblock plugin) { this.plugin = plugin; }

    private Inventory inventory(Page page, Island island, int size, String title, int pageNumber) {
        MenuHolder holder = new MenuHolder(page, island.owner, pageNumber);
        holder.inventory = Bukkit.createInventory(holder, size, title);
        return holder.inventory;
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        stack.setItemMeta(meta);
        return stack;
    }

    private void fill(Inventory inv) {
        ItemStack pane = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane);
    }

    void openMain(Player player, Island island) {
        Inventory inv = inventory(Page.MAIN, island, 27, "§6TilkiCraft §8• §fAda", 0); fill(inv);
        inv.setItem(4, item(Material.REDSTONE, "§eEklentiler", "§7Otomatik satış, pişirme ve hasat"));
        inv.setItem(8, item(Material.IRON_BLOCK, "§aBlok Dönüştürme", "§7Depodaki cevherlerden blok üret"));
        inv.setItem(10, item(Material.HOPPER, "§aÇiftçi Deposu", "§7Dolu: §f" + island.totalStored() + "§7/§f" + plugin.capacity(island)));
        inv.setItem(13, item(Material.BEACON, "§6Yönetim Paneli", "§7Ada seçenekleri ve üyeler"));
        inv.setItem(16, item(Material.DIAMOND_PICKAXE, "§aYükseltmeler", "§7Sınır, jeneratör, üye ve piston"));
        player.openInventory(inv);
    }

    private void openManagement(Player player, Island island) {
        Inventory inv = inventory(Page.MANAGEMENT, island, 27, "§6Yönetim Paneli", 0); fill(inv);
        inv.setItem(10, item(Material.CHEST, "§eSandık Oto Toplama", "§7Durum: " + state(island.chestCollect), "§7Yerdeki eşyaları çiftçi toplar"));
        inv.setItem(13, item(Material.BARREL, "§eÇiftçi Deposu", "§7Kapasite: §f" + plugin.capacity(island),
            "§7Sonraki seviye: §f" + nextPrice("storage-upgrade-prices", island.storageLevel), "§aSatın almak için tıkla"));
        inv.setItem(16, item(Material.PLAYER_HEAD, "§eÜye Paneli", "§7Kayıtlı oyuncular ve yetkiler"));
        inv.setItem(22, back()); player.openInventory(inv);
    }

    private void openAddons(Player player, Island island) {
        Inventory inv = inventory(Page.ADDONS, island, 27, "§6Eklentiler", 0); fill(inv);
        inv.setItem(10, item(Material.GOLD_INGOT, "§eDolan Stoğu Oto Sat", "§7Durum: " + state(island.autoSell), "§7Vault ve satış fiyatı gerekir"));
        inv.setItem(13, item(Material.FURNACE, "§eOtomatik Pişirme", "§7Durum: " + state(island.autoSmelt), "§7Toplanan uygun eşyaları pişirir"));
        inv.setItem(16, item(Material.DIAMOND_HOE, "§eOtomatik Hasat", "§7Durum: " + state(island.autoHarvest), "§7Kırılan blokların düşenlerini depolar"));
        inv.setItem(22, back()); player.openInventory(inv);
    }

    private void openUpgrades(Player player, Island island) {
        Inventory inv = inventory(Page.UPGRADES, island, 27, "§6Yükseltmeler", 0); fill(inv);
        upgrade(inv, 10, Material.GRASS_BLOCK, "Sınır", island.borderLevel, "border-radii", "border-upgrade-prices");
        upgrade(inv, 12, Material.COBBLESTONE, "Jeneratör", island.generatorLevel, null, "generator-upgrade-prices");
        upgrade(inv, 14, Material.PLAYER_HEAD, "Üye", island.memberLevel, "member-limits", "member-upgrade-prices");
        upgrade(inv, 16, Material.PISTON, "Piston", island.pistonLevel, "piston-limits", "piston-upgrade-prices");
        inv.setItem(22, back()); player.openInventory(inv);
    }

    private void upgrade(Inventory inv, int slot, Material material, String name, int level, String values, String prices) {
        String current = values == null ? "Kademe " + (level + 1) : String.valueOf(plugin.intLevel(values, level, 0));
        inv.setItem(slot, item(material, "§a" + name + " Yükseltmesi", "§7Şu an: §f" + current,
            "§7Sonraki fiyat: §e" + nextPrice(prices, level), "§aSatın almak için tıkla"));
    }

    private String nextPrice(String path, int level) {
        List<Integer> prices = plugin.getConfig().getIntegerList(path);
        return level + 1 >= prices.size() ? "MAKSİMUM" : prices.get(level + 1) + " oyun parası";
    }

    private void openMembers(Player player, Island island) {
        Inventory inv = inventory(Page.MEMBERS, island, 54, "§6Üye Paneli", 0); fill(inv);
        MenuHolder holder = (MenuHolder) inv.getHolder();
        int slot = 10;
        for (var entry : island.members.entrySet()) {
            if (slot >= 44) break;
            UUID id = entry.getKey();
            String name = Bukkit.getOfflinePlayer(id).getName();
            inv.setItem(slot, item(Material.PLAYER_HEAD, "§e" + (name == null ? id.toString() : name),
                "§7Yetki: §f" + entry.getValue(), "§aSol tık: yükselt", "§cSağ tık: düşür"));
            holder.memberSlots.put(slot++, id);
        }
        inv.setItem(49, back()); player.openInventory(inv);
    }

    private void openStorage(Player player, Island island, int page) {
        List<Material> available = storageItems(island);
        int pages = (available.size() + 27) / 28;
        int safePage = Math.max(0, Math.min(page, pages - 1));
        Inventory inv = inventory(Page.STORAGE, island, 54, "§6Çiftçi Deposu §8• §f" + (safePage + 1), safePage); fill(inv);
        inv.setItem(4, item(Material.BARREL, "§eDepo", "§7" + island.totalStored() + "/" + plugin.capacity(island)));
        for (int i = 0; i < 28 && safePage * 28 + i < available.size(); i++) {
            Material material = available.get(safePage * 28 + i);
            int slot = 10 + (i / 7) * 9 + i % 7;
            inv.setItem(slot, item(material, "§f" + material.name().replace('_', ' '),
                "§7Stok: §e" + island.storage.getOrDefault(material, 0L), "§aSol tık: 64 al", "§eSağ tık: 1 al"));
        }
        inv.setItem(45, item(Material.ARROW, "§eÖnceki sayfa"));
        inv.setItem(49, back()); inv.setItem(53, item(Material.ARROW, "§eSonraki sayfa"));
        player.openInventory(inv);
    }

    private void openConvert(Player player, Island island) {
        Inventory inv = inventory(Page.CONVERT, island, 27, "§aBlok Dönüştürme", 0); fill(inv);
        for (int i = 0; i < CONVERT_INPUTS.length; i++) {
            int slot = i < 7 ? 10 + i : 19 + i - 7;
            Material input = CONVERT_INPUTS[i];
            inv.setItem(slot, item(CONVERT_OUTPUTS[i], "§a" + CONVERT_OUTPUTS[i].name().replace('_', ' '),
                "§7Depodaki " + input.name().replace('_', ' ') + ": §f" + island.storage.getOrDefault(input, 0L),
                "§710 cevher → 1 blok (9 + %10 vergi yuvarlaması)", "§7En çok 64 blok; almak için tıkla"));
        }
        inv.setItem(26, back()); player.openInventory(inv);
    }

    private ItemStack back() { return item(Material.BARRIER, "§cGeri"); }
    private String state(boolean enabled) { return enabled ? "§aAçık" : "§cKapalı"; }

    private boolean pay(Player player, Island island, String prices, int level) {
        List<Integer> values = plugin.getConfig().getIntegerList(prices);
        if (level + 1 >= values.size()) { player.sendMessage("§eBu yükseltme maksimum seviyede."); return false; }
        if (!island.canManage(player.getUniqueId())) { player.sendMessage("§cBunu yalnızca ada yöneticisi yapabilir."); return false; }
        if (!plugin.economy.available()) { player.sendMessage("§cVault ekonomisi bağlı değil."); return false; }
        int price = values.get(level + 1);
        if (!plugin.economy.withdraw(player, price)) { player.sendMessage("§cYetersiz bakiye veya ödeme başarısız."); return false; }
        player.sendMessage("§aYükseltme satın alındı: §e" + price + " oyun parası"); return true;
    }

    @EventHandler public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() != event.getView().getTopInventory()) return;
        int slot = event.getRawSlot();
        Island island = plugin.islands.get(holder.islandOwner);
        if (island == null || !island.isMember(player.getUniqueId())) { player.closeInventory(); return; }
        if ((slot == 22 && holder.page != Page.MAIN && holder.page != Page.STORAGE && holder.page != Page.MEMBERS && holder.page != Page.CONVERT) || (slot == 26 && holder.page == Page.CONVERT)) { openMain(player, island); return; }
        if (slot == 49 && (holder.page == Page.STORAGE || holder.page == Page.MEMBERS)) { openMain(player, island); return; }
        switch (holder.page) {
            case MAIN -> {
                if (slot == 4) openAddons(player, island);
                if (slot == 8) openConvert(player, island);
                if (slot == 10) openStorage(player, island, 0);
                if (slot == 13) openManagement(player, island);
                if (slot == 16) openUpgrades(player, island);
            }
            case MANAGEMENT -> {
                if (slot == 10 && island.canManage(player.getUniqueId())) island.chestCollect = !island.chestCollect;
                if (slot == 13 && pay(player, island, "storage-upgrade-prices", island.storageLevel)) island.storageLevel++;
                if (slot == 16) { openMembers(player, island); return; }
                if (slot == 10 || slot == 13) openManagement(player, island);
            }
            case ADDONS -> {
                if (!island.canManage(player.getUniqueId())) { player.sendMessage("§cYönetici yetkisi gerekli."); return; }
                if (slot == 10) {
                    if (!island.autoSell && !plugin.economy.available()) { player.sendMessage("§cVault ekonomisi bağlı değil."); return; }
                    island.autoSell = !island.autoSell;
                }
                if (slot == 13) island.autoSmelt = !island.autoSmelt;
                if (slot == 16) island.autoHarvest = !island.autoHarvest;
                if (slot == 10 || slot == 13 || slot == 16) openAddons(player, island);
            }
            case UPGRADES -> {
                if (slot == 10 && pay(player, island, "border-upgrade-prices", island.borderLevel)) island.borderLevel++;
                if (slot == 12 && pay(player, island, "generator-upgrade-prices", island.generatorLevel)) island.generatorLevel++;
                if (slot == 14 && pay(player, island, "member-upgrade-prices", island.memberLevel)) island.memberLevel++;
                if (slot == 16 && pay(player, island, "piston-upgrade-prices", island.pistonLevel)) island.pistonLevel++;
                if (slot == 10 || slot == 12 || slot == 14 || slot == 16) openUpgrades(player, island);
            }
            case MEMBERS -> {
                UUID member = holder.memberSlots.get(slot);
                if (member == null || !island.owner.equals(player.getUniqueId()) || member.equals(island.owner)) return;
                Island.Role role = island.members.get(member);
                if (event.isLeftClick() && role == Island.Role.MEMBER) island.members.put(member, Island.Role.OFFICER);
                if (event.isRightClick() && role == Island.Role.OFFICER) island.members.put(member, Island.Role.MEMBER);
                openMembers(player, island);
            }
            case STORAGE -> {
                if (slot == 45) { openStorage(player, island, holder.pageNumber - 1); return; }
                if (slot == 53) { openStorage(player, island, holder.pageNumber + 1); return; }
                int row = (slot - 10) / 9, col = (slot - 10) % 9;
                if (slot < 10 || row < 0 || row > 3 || col > 6) return;
                int index = holder.pageNumber * 28 + row * 7 + col;
                List<Material> available = storageItems(island);
                if (index >= available.size()) return;
                Material material = available.get(index);
                long stored = island.storage.getOrDefault(material, 0L);
                int count = (int) Math.min(stored, event.isRightClick() ? 1 : 64);
                if (count == 0) return;
                ItemStack stack = new ItemStack(material, count);
                if (!canFit(player, stack)) { player.sendMessage("§cEnvanterin dolu."); return; }
                player.getInventory().addItem(stack);
                island.storage.put(material, stored - count); openStorage(player, island, holder.pageNumber);
            }
            case CONVERT -> {
                int index = slot >= 10 && slot <= 16 ? slot - 10 : slot >= 19 && slot <= 23 ? slot - 19 + 7 : -1;
                if (index < 0 || index >= CONVERT_INPUTS.length) return;
                Material input = CONVERT_INPUTS[index], output = CONVERT_OUTPUTS[index];
                long stored = island.storage.getOrDefault(input, 0L);
                int blocks = (int) Math.min(64, stored / 10);
                if (blocks == 0) { player.sendMessage("§cEn az 10 malzeme gerekli."); return; }
                ItemStack result = new ItemStack(output, blocks);
                if (!canFit(player, result)) { player.sendMessage("§cEnvanterin dolu."); return; }
                player.getInventory().addItem(result);
                island.storage.put(input, stored - blocks * 10L); openConvert(player, island);
            }
        }
        plugin.saveIslands();
    }

    @EventHandler public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) event.setCancelled(true);
    }

    private boolean canFit(Player player, ItemStack stack) {
        int room = 0;
        for (ItemStack existing : player.getInventory().getStorageContents()) {
            if (existing == null || existing.getType().isAir()) room += stack.getMaxStackSize();
            else if (existing.isSimilar(stack)) room += Math.max(0, existing.getMaxStackSize() - existing.getAmount());
            if (room >= stack.getAmount()) return true;
        }
        return false;
    }

    private List<Material> storageItems(Island island) {
        LinkedHashSet<Material> materials = new LinkedHashSet<>(Arrays.asList(STORAGE_ITEMS));
        island.storage.keySet().stream().filter(material -> !materials.contains(material))
            .sorted(Comparator.comparing(Enum::name)).forEach(materials::add);
        return new ArrayList<>(materials);
    }
}
