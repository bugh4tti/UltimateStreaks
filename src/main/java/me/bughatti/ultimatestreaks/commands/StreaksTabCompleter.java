package me.bughatti.ultimatestreaks.commands;

import me.bughatti.ultimatestreaks.UltimateStreaks;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StreaksTabCompleter implements TabCompleter {

    private final UltimateStreaks plugin;
    private static final List<String> SUBCOMMANDS = List.of("help", "reload", "claim", "give");

    public StreaksTabCompleter(UltimateStreaks plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], SUBCOMMANDS, completions);
            Collections.sort(completions);
            return completions;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();

            if (sub.equals("give")) {
                List<String> playerNames = new ArrayList<>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    playerNames.add(player.getName());
                }
                StringUtil.copyPartialMatches(args[1], playerNames, completions);
                Collections.sort(completions);
                return completions;
            }

            if (sub.equals("claim")) {
                StringUtil.copyPartialMatches(args[1], getAllStreakDays(), completions);
                Collections.sort(completions);
                return completions;
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();

            if (sub.equals("give")) {
                StringUtil.copyPartialMatches(args[2], getAllStreakDays(), completions);
                Collections.sort(completions);
                return completions;
            }
        }

        return completions;
    }

    /**
     * Genera streak_day_1 a streak_day_50 (según max-streak-days en config.yml)
     */
    private List<String> getAllStreakDays() {
        List<String> days = new ArrayList<>();
        int maxDays = plugin.getConfigManager().getMaxStreakDays();
        for (int i = 1; i <= maxDays; i++) {
            days.add("streak_day_" + i);
        }
        return days;
    }
                                      }
