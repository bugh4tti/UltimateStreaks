package me.bughatti.ultimatestreaks.managers;

import me.bughatti.ultimatestreaks.UltimateStreaks;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class GUIManager implements Listener {

    private static final int MENU_SIZE = 54;
    private static final List<Integer> DEFAULT_DAY_SLOTS = List.of(
            9, 10, 19, 28, 37, 38, 39, 30, 21, 12, 13, 14, 23, 32, 41, 42, 43, 34, 25, 16, 17);
    private static final List<Integer> DEFAULT_PAGE_SLOTS = List.of(1, 2, 3, 5, 6, 7);
    private static final int DEFAULT_PROGRESS_SLOT = 49;

    private final UltimateStreaks plugin;

    private enum DayState {
        LOCKED, UNLOCKED, CLAIMED
    }

    /**
     * Identifica los menús de este plugin (más seguro que comparar el título).
     */
    private static class StreaksHolder implements InventoryHolder {
        private final int page;
        private Inventory inventory;

        StreaksHolder(int page) {
            this.page = page;
        }

        int getPage() {
            return page;
        }

        void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public GUIManager(UltimateStreaks plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    //  Apertura del menú
    // ------------------------------------------------------------------

    public void openMenu(Player player, int page) {
        int totalPages = getTotalPages();
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        List<Integer> daySlots = getDaySlots();
        int perPage = daySlots.size();
        int maxDays = plugin.getConfigManager().getMaxStreakDays();

        String title = plugin.getConfigManager().getGuiTitle(page)
                .replace("{pages}", String.valueOf(totalPages));

        StreaksHolder holder = new StreaksHolder(page);
        Inventory inv = Bukkit.createInventory(holder, MENU_SIZE, title);
        holder.setInventory(inv);

        // Relleno
        ItemStack filler = buildFiller();
        for (int i = 0; i < MENU_SIZE; i++) {
            inv.setItem(i, filler);
        }

        int effectiveDay = getEffectiveDay(player);
        boolean canClaim = plugin.getDataManager().canClaim(player.getUniqueId());

        // Días (camino en serpiente)
        int startDay = (page - 1) * perPage + 1;
        for (int i = 0; i < perPage; i++) {
            int day = startDay + i;
            if (day > maxDays) break;
            setItem(inv, daySlots.get(i), buildDayItem(day, effectiveDay, canClaim, maxDays));
        }

        // Botones de página (1, 2, 3...)
        List<Integer> pageSlots = getPageSlots();
        int buttons = Math.min(totalPages, pageSlots.size());
        for (int p = 1; p <= buttons; p++) {
            setItem(inv, pageSlots.get(p - 1), buildPageButton(p, perPage, maxDays, totalPages));
        }

        // Flechas
        ConfigurationSection prev = plugin.getConfig().getConfigurationSection("gui.previous-page-item");
        if (prev != null && page > 1) {
            setItem(inv, prev.getInt("slot", 48), buildNavItem(prev));
        }
        ConfigurationSection next = plugin.getConfig().getConfigurationSection("gui.next-page-item");
        if (next != null && page < totalPages) {
            setItem(inv, next.getInt("slot", 50), buildNavItem(next));
        }

        // Mi Progreso
        int progressSlot = plugin.getConfig().getInt("gui.layout.progress-slot", DEFAULT_PROGRESS_SLOT);
        setItem(inv, progressSlot, buildProgressItem(player, effectiveDay, canClaim, maxDays));

        player.openInventory(inv);
    }

    // ------------------------------------------------------------------
    //  Construcción de ítems
    // ------------------------------------------------------------------

    private ItemStack buildFiller() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("gui.filler-item");

        Material material = Material.GRAY_STAINED_GLASS_PANE;
        String name = " ";
        if (section != null) {
            material = parseMaterial(section.getString("material"), Material.GRAY_STAINED_GLASS_PANE);
            name = section.getString("name", " ");
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().colorize(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildDayItem(int day, int effectiveDay, boolean canClaim, int maxDays) {
        DayState state = getDayState(day, effectiveDay, canClaim);

        ConfigurationSection stateSection = plugin.getConfig()
                .getConfigurationSection("gui.states." + state.name().toLowerCase(Locale.ROOT));

        Material material = Material.GRAY_DYE;
        List<String> footer = new ArrayList<>();
        if (stateSection != null) {
            material = parseMaterial(stateSection.getString("material"), Material.GRAY_DYE);
            footer = stateSection.getStringList("footer");
        }

        String name = plugin.getConfig().getString("gui.day-item.name", "&fDía {day}");
        List<String> template = plugin.getConfig().getStringList("gui.day-item.lore");

        ConfigurationSection daySection = plugin.getConfigManager().getStreakSection("day_" + day);
        List<String> rewards = daySection != null
                ? daySection.getStringList("rewards-lore")
                : new ArrayList<>();

        List<String> lore = new ArrayList<>();
        for (String line : template) {
            String trimmed = line.trim();
            if (trimmed.equals("{rewards}")) {
                lore.addAll(rewards);
            } else if (trimmed.equals("{status}")) {
                lore.addAll(footer);
            } else {
                lore.add(line);
            }
        }

        String[] placeholders = {
                "{day}", String.valueOf(day),
                "{current}", String.valueOf(effectiveDay),
                "{max}", String.valueOf(maxDays)
        };

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().colorize(replace(name, placeholders)));
            meta.setLore(plugin.getConfigManager().colorizeList(replaceAll(lore, placeholders)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildPageButton(int page, int perPage, int maxDays, int totalPages) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("gui.page-button");

        int from = (page - 1) * perPage + 1;
        int to = Math.min(page * perPage, maxDays);

        Material material = Material.PAPER;
        String name = "&fPágina {page}";
        List<String> lore = new ArrayList<>();
        String textureUrl = null;

        if (section != null) {
            material = parseMaterial(section.getString("material"), Material.PAPER);
            name = section.getString("name", name);
            lore = section.getStringList("lore");
            textureUrl = section.getString("texture-urls." + page);
        }

        ItemStack item;
        if (textureUrl != null && !textureUrl.isBlank()) {
            item = buildTexturedHead(textureUrl);
        } else {
            item = new ItemStack(material, Math.min(page, 64));
        }

        String[] placeholders = {
                "{page}", String.valueOf(page),
                "{pages}", String.valueOf(totalPages),
                "{from}", String.valueOf(from),
                "{to}", String.valueOf(to)
        };

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().colorize(replace(name, placeholders)));
            meta.setLore(plugin.getConfigManager().colorizeList(replaceAll(lore, placeholders)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildTexturedHead(String textureUrl) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = head.getItemMeta();
        if (rawMeta instanceof SkullMeta skullMeta) {
            try {
                PlayerProfile profile = Bukkit.createPlayerProfile(
                        UUID.nameUUIDFromBytes(textureUrl.getBytes()));
                PlayerTextures textures = profile.getTextures();
                textures.setSkin(new URL(textureUrl));
                profile.setTextures(textures);
                skullMeta.setOwnerProfile(profile);
                head.setItemMeta(skullMeta);
            } catch (MalformedURLException e) {
                plugin.getLogger().warning("URL de textura inválida: " + textureUrl);
            }
        }
        return head;
    }

    private ItemStack buildNavItem(ConfigurationSection section) {
        ItemStack item = new ItemStack(parseMaterial(section.getString("material"), Material.ARROW));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().colorize(section.getString("name", "")));
            meta.setLore(plugin.getConfigManager().colorizeList(section.getStringList("lore")));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildProgressItem(Player player, int effectiveDay, boolean canClaim, int maxDays) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = item.getItemMeta();
        if (rawMeta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(player);
            item.setItemMeta(skullMeta);
        }

        int claimed = Math.max(0, effectiveDay - 1);
        int remaining = Math.max(0, maxDays - claimed);
        String time = canClaim
                ? plugin.getConfigManager().getMessageNoPrefix("time-available")
                : plugin.getDataManager().getTimeUntilNextClaim(player.getUniqueId());

        String[] placeholders = {
                "{current}", String.valueOf(effectiveDay),
                "{claimed}", String.valueOf(claimed),
                "{remaining}", String.valueOf(remaining),
                "{max}", String.valueOf(maxDays),
                "{time}", time
        };

        String name = plugin.getConfig().getString("gui.progress-item.name", "&e&lMi Progreso");
        List<String> lore = plugin.getConfig().getStringList("gui.progress-item.lore");

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().colorize(replace(name, placeholders)));
            meta.setLore(plugin.getConfigManager().colorizeList(replaceAll(lore, placeholders)));
            item.setItemMeta(meta);
        }
        return item;
    }

    // ------------------------------------------------------------------
    //  Eventos
    // ------------------------------------------------------------------

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof StreaksHolder holder)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(top)) return;

        int slot = event.getRawSlot();
        int page = holder.getPage();
        int totalPages = getTotalPages();

        // Flechas
        ConfigurationSection prev = plugin.getConfig().getConfigurationSection("gui.previous-page-item");
        if (prev != null && page > 1 && slot == prev.getInt("slot", 48)) {
            openMenu(player, page - 1);
            return;
        }
        ConfigurationSection next = plugin.getConfig().getConfigurationSection("gui.next-page-item");
        if (next != null && page < totalPages && slot == next.getInt("slot", 50)) {
            openMenu(player, page + 1);
            return;
        }

        // Botones de página
        int pageIndex = getPageSlots().indexOf(slot);
        if (pageIndex >= 0 && pageIndex + 1 <= totalPages) {
            if (pageIndex + 1 != page) {
                openMenu(player, pageIndex + 1);
            }
            return;
        }

        // Días
        List<Integer> daySlots = getDaySlots();
        int slotIndex = daySlots.indexOf(slot);
        if (slotIndex < 0) return;

        int day = (page - 1) * daySlots.size() + slotIndex + 1;
        int maxDays = plugin.getConfigManager().getMaxStreakDays();
        if (day > maxDays) return;

        int effectiveDay = getEffectiveDay(player);
        boolean canClaim = plugin.getDataManager().canClaim(player.getUniqueId());
        DayState state = getDayState(day, effectiveDay, canClaim);

        switch (state) {
            case UNLOCKED -> {
                claimDay(player);
                openMenu(player, page);
            }
            case CLAIMED -> player.sendMessage(
                    plugin.getConfigManager().getMessage("day-claimed")
                            .replace("{day}", String.valueOf(day)));
            case LOCKED -> {
                if (day == effectiveDay) {
                    // Es el día que toca, pero todavía está en cooldown
                    player.sendMessage(plugin.getConfigManager().getMessage("already-claimed")
                            .replace("{time}", plugin.getDataManager().getTimeUntilNextClaim(player.getUniqueId())));
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("day-locked"));
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof StreaksHolder) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------
    //  Lógica
    // ------------------------------------------------------------------

    private void claimDay(Player player) {
        // processClaim devuelve el día realmente otorgado (considera si se rompió la racha)
        int day = plugin.getDataManager().processClaim(player);

        ConfigurationSection section = plugin.getConfigManager().getStreakSection("day_" + day);
        if (section != null) {
            for (String cmd : section.getStringList("command-rewards")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", player.getName()));
            }
        }

        player.sendMessage(plugin.getConfigManager().getMessage("claim-success")
                .replace("{day}", String.valueOf(day)));
    }

    /**
     * Día que le toca al jugador, considerando si la racha se rompió
     * (misma regla que DataManager#processClaim).
     */
    private int getEffectiveDay(Player player) {
        UUID uuid = player.getUniqueId();
        int current = plugin.getDataManager().getCurrentDay(uuid);
        long last = plugin.getDataManager().getLastClaim(uuid);

        if (last != 0L) {
            long breakLimit = TimeUnit.HOURS.toMillis(2L * plugin.getConfigManager().getStreakResetHours());
            if (System.currentTimeMillis() - last > breakLimit) {
                return 1;
            }
        }
        return current;
    }

    private DayState getDayState(int day, int effectiveDay, boolean canClaim) {
        if (day < effectiveDay) return DayState.CLAIMED;
        if (day == effectiveDay && canClaim) return DayState.UNLOCKED;
        return DayState.LOCKED;
    }

    private int getTotalPages() {
        int perPage = getDaySlots().size();
        int maxDays = plugin.getConfigManager().getMaxStreakDays();
        return Math.max(1, (int) Math.ceil(maxDays / (double) perPage));
    }

    // ------------------------------------------------------------------
    //  Utilidades
    // ------------------------------------------------------------------

    private List<Integer> getDaySlots() {
        List<Integer> list = plugin.getConfig().getIntegerList("gui.layout.day-slots");
        return list.isEmpty() ? DEFAULT_DAY_SLOTS : list;
    }

    private List<Integer> getPageSlots() {
        List<Integer> list = plugin.getConfig().getIntegerList("gui.layout.page-button-slots");
        return list.isEmpty() ? DEFAULT_PAGE_SLOTS : list;
    }

    private void setItem(Inventory inv, int slot, ItemStack item) {
        if (slot >= 0 && slot < inv.getSize()) {
            inv.setItem(slot, item);
        }
    }

    private Material parseMaterial(String name, Material fallback) {
        if (name == null) return fallback;
        Material material = Material.matchMaterial(name);
        return material != null ? material : fallback;
    }

    private String replace(String text, String... pairs) {
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            text = text.replace(pairs[i], pairs[i + 1]);
        }
        return text;
    }

    private List<String> replaceAll(List<String> lines, String... pairs) {
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            result.add(replace(line, pairs));
        }
        return result;
    }
            }
