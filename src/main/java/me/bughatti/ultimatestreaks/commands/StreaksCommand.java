package me.bughatti.ultimatestreaks.commands;

import me.bughatti.ultimatestreaks.UltimateStreaks;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StreaksCommand implements CommandExecutor {

    private final UltimateStreaks plugin;

    public StreaksCommand(UltimateStreaks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "help":
                sendHelp(sender);
                return true;

            case "reload":
                return handleReload(sender);

            case "claim":
                return handleClaim(sender, args);

            case "give":
                return handleGive(sender, args);

            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().colorize("&8&m----------&r &b&lUltimateStreaks &8&m----------"));
        sender.sendMessage(plugin.getConfigManager().colorize("&e/ultimatestreaks help &7- Muestra esta ayuda"));
        sender.sendMessage(plugin.getConfigManager().colorize("&e/ultimatestreaks reload &7- Recarga la configuración"));
        sender.sendMessage(plugin.getConfigManager().colorize("&e/ultimatestreaks claim <streak_day_X> &7- Reclama tu racha diaria"));
        sender.sendMessage(plugin.getConfigManager().colorize("&e/ultimatestreaks give <player> <streak_day_X> &7- Le da una racha a otro jugador"));
        sender.sendMessage(plugin.getConfigManager().colorize("&8&m--------------------------------------------"));
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("ultimatestreaks.reload")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }
        plugin.getConfigManager().reload();
        sender.sendMessage(plugin.getConfigManager().getMessage("reload-success"));
        return true;
    }

    private boolean handleClaim(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando solo puede ser usado por jugadores.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("ultimatestreaks.claim")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        // Abre el menú de rachas en lugar de reclamar directo por comando,
        // salvo que quieras reclamo directo por texto (ver nota abajo)
        plugin.getGuiManager().openMenu(player, 1);
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ultimatestreaks.give")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cUso: /ultimatestreaks give <player> <streak_day_X>"));
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cEl jugador &e" + args[1] + " &cno está online."));
            return true;
        }

        String dayArg = args[2].replace("streak_day_", "").replace("day_", "");
        int day;
        try {
            day = Integer.parseInt(dayArg);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getConfigManager().getMessage("invalid-day"));
            return true;
        }

        if (day < 1 || day > plugin.getConfigManager().getMaxStreakDays()) {
            sender.sendMessage(plugin.getConfigManager().getMessage("invalid-day"));
            return true;
        }

        plugin.getDataManager().give(target, day);

        String giveMsg = plugin.getConfigManager().getMessage("give-success")
                .replace("{day}", String.valueOf(day))
                .replace("{player}", target.getName());
        sender.sendMessage(giveMsg);

        String receiveMsg = plugin.getConfigManager().getMessage("give-received")
                .replace("{day}", String.valueOf(day));
        target.sendMessage(receiveMsg);

        return true;
    }
  }
