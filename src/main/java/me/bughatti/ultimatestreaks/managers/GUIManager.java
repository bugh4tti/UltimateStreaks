package me.bughatti.ultimatestreaks.managers;

import me.bughatti.ultimatestreaks.UltimateStreaks;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GUIManager implements Listener {

    private final UltimateStreaks plugin;
    private final Map<UUID, Integer> openPages = new HashMap<>();

    private static final int ITEMS_PER_PAGE = 45; // slots 0-44, dejamos la fila de abajo para las flechas

    public GUIManager(UltimateStreaks plugin) {
        this.plugin = plugin;
    }

    public void openMenu(Player player, int page) {
        int maxDays = plugin.getConfigManager().getMaxStreakDays();
        int size = plugin.getConfigManager().getGuiSize();

        Inventory inv = Bukkit.createInventory(null, size, plugin.getConfigManager().getGuiTitle(page));

        int startDay = (page - 1) * ITEMS_PER_PAGE + 1;
        int endDay = Math.min(startDay + ITEMS_PER_PAGE - 1, maxDays);

        int slot = 0;
        for (int day = startDay; day <= endDay; day++) {
            ItemStack item = buildDayItem(player, day);
            if (item != null) {
                inv.setItem(slot, item);
            }
            slot++;
        }

        // Flecha anterior
        if (page > 1) {
            ConfigurationSection prevSection = plugin.getConfig().getConfigurationSection("gui.previous-page-item");
            if (prevSection != null) {
                inv.setItem(prevSection.getInt("slot", 45), buildNavItem(prevSection));
            }
        }

        // Flecha siguiente
        if (endDay < maxDays) {
            ConfigurationSection nextSection = plugin.getConfig().getConfigurationSection("gui.next-page-item");
            if (nextSection != null) {
                inv.setItem(nextSection.getInt("slot", 53), buildNavItem(nextSection));
            }
        }

        openPages.put(player.getUniqueId(), page);
        player.openInventory(inv);
    }

    private ItemStack buildNavItem(ConfigurationSection section) {
        Material material = Material.matchMaterial(section.getString("material", "ARROW"));
        if (material == null) material = Material.ARROW;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().colorize(section.getString("name", "")));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildDayItem(Player player, int day) {
        String dayKey = "day_" + day;
        ConfigurationSection section = plugin.getConfigManager().getStreakSection(dayKey);
        if (section == null) return null;

        Material material = Material.matchMaterial(section.getString("material", "STONE"));
        if (material == null) material = Material.STONE;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().colorize(section.getString("name", "&fDía " + day)));

            List<String> lore = plugin.getConfigManager().colorizeList(section.getStringList("lore"));

            int currentDay = plugin.getDataManager().getCurrentDay(player.getUniqueId());
            boolean canClaim = plugin.getDataManager().canClaim(player.getUniqueId());

            if (day < currentDay || (day == currentDay && !canClaim && plugin.getDataManager().getLastClaim(player.getUniqueId()) != 0)) {
                lore.add(plugin.getConfigManager().colorize("&8✔ Reclamado"));
            } else if (day == currentDay && canClaim) {
                lore.add(plugin.getConfigManager().colorize("&a✔ Disponible para reclamar"));
            } else {
                lore.add(plugin.getConfigManager().colorize("&c✘ Bloqueado"));
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String title = event.getView().getTitle();
        String expectedPrefix = plugin.getConfigManager().colorize("&8&lRachas");
        if (!title.startsWith(stripToBase(expectedPrefix))) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int page = openPages.getOrDefault(player.getUniqueId(), 1);
        int slot = event.getRawSlot();

        ConfigurationSection nextSection = plugin.getConfig().getConfigurationSection("gui.next-page-item");
        ConfigurationSection prevSection = plugin.getConfig().getConfigurationSection("gui.previous-page-item");

        if (nextSection != null && slot == nextSection.getInt("slot", 53)) {
            openMenu(player, page + 1);
            return;
        }

        if (prevSection != null && slot == prevSection.getInt("slot", 45)) {
            openMenu(player, page - 1);
            return;
        }

        int startDay = (page - 1) * ITEMS_PER_PAGE + 1;
        int day = startDay + slot;

        if (day > plugin.getConfigManager().getMaxStreakDays()) return;

        int currentDay = plugin.getDataManager().getCurrentDay(player.getUniqueId());
        boolean canClaim = plugin.getDataManager().canClaim(player.getUniqueId());

        if (day == currentDay && canClaim) {
            claimDay(player, day);
            openMenu(player, page);
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("already-claimed")
                    .replace("{time}", plugin.getDataManager().getTimeUntilNextClaim(player.getUniqueId())));
        }
    }

    private void claimDay(Player player, int day) {
        ConfigurationSection section = plugin.getConfigManager().getStreakSection("day_" + day);
        if (section != null) {
            List<String> commands = section.getStringList("command-rewards");
            for (String cmd : commands) {
                String parsed = cmd.replace("{player}", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
            }
        }

        plugin.getDataManager().processClaim(player);

        player.sendMessage(plugin.getConfigManager().getMessage("claim-success")
                .replace("{day}", String.valueOf(day)));
    }

    private String stripToBase(String colorized) {
        // Compara solo el inicio del título para no depender de la página exacta
        int idx = colorized.indexOf("Rachas");
        return idx >= 0 ? colorized.substring(0, idx + 6) : colorized;
    }
              }
