package me.ram.bedwarsscoreboardaddon.commands;

import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.game.Game;
import me.ram.bedwarsscoreboardaddon.config.Config;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> suggest = getSuggest(sender, args);
        String last = args[args.length - 1];
        if (suggest != null && !last.isEmpty()) {
            List<String> list = new ArrayList<>();
            suggest.forEach(s -> {
                if (s.startsWith(last)) {
                    list.add(s);
                }
            });
            return list;
        }
        return suggest;
    }

    private List<String> getSuggest(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("help", "shop", "spawner", "edit", "reload", "task", "title", "message", "randomplay");
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("shop")) {
                return Arrays.asList("list", "remove", "set");
            }
            if (args[0].equalsIgnoreCase("spawner")) {
                return Arrays.asList("list", "remove", "add");
            }
            if (args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("task") || args[0].equalsIgnoreCase("title") || args[0].equalsIgnoreCase("message") || args[0].equalsIgnoreCase("randomplay") || args[0].equalsIgnoreCase("teleport")) {
                return getGames();
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("shop") && args[1].equalsIgnoreCase("set")) {
                return Arrays.asList("item", "team");
            }
            if (args[0].equalsIgnoreCase("shop") && args[1].equalsIgnoreCase("list") && sender.hasPermission("bedwarsscoreboardaddon.shop.list")) {
                return getGames();
            }
            if (args[0].equalsIgnoreCase("shop") && args[1].equalsIgnoreCase("remove") && sender.hasPermission("bedwarsscoreboardaddon.shop.remove")) {
                return new ArrayList<>(Config.game_shop_shops.keySet());
            }
            if (args[0].equalsIgnoreCase("spawner") && args[1].equalsIgnoreCase("list")) {
                return getGames();
            }
            if (args[0].equalsIgnoreCase("spawner") && args[1].equalsIgnoreCase("list") && sender.hasPermission("bedwarsscoreboardaddon.spawner.list")) {
                return getGames();
            }
            if (args[0].equalsIgnoreCase("spawner") && args[1].equalsIgnoreCase("add") && sender.hasPermission("bedwarsscoreboardaddon.spawner.add")) {
                return getGames();
            }
            if (args[0].equalsIgnoreCase("spawner") && args[1].equalsIgnoreCase("remove") && sender.hasPermission("bedwarsscoreboardaddon.spawner.remove")) {
                return new ArrayList<>(Config.game_team_spawners.keySet());
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("shop") && args[1].equalsIgnoreCase("set") && (args[2].equalsIgnoreCase("item") || args[2].equalsIgnoreCase("team")) && sender.hasPermission("bedwarsscoreboardaddon.shop.set")) {
                return getGames();
            }
            if (args[0].equalsIgnoreCase("spawner") && args[1].equalsIgnoreCase("add") && sender.hasPermission("bedwarsscoreboardaddon.spawner.add")) {
                String game = args[2];
                return getTeams(game);
            }
        }
        return new ArrayList<>();
    }

    private List<String> getGames() {
        List<String> list = new ArrayList<>();
        BedwarsRel.getInstance().getGameManager().getGames().forEach(game -> {
            list.add(game.getName());
        });
        return list;
    }

    private List<String> getTeams(String g) {
        List<String> list = new ArrayList<>();
        Game game = BedwarsRel.getInstance().getGameManager().getGame(g);
        if (game == null) {
            return list;
        }
        list.addAll(game.getTeams().keySet());
        return list;
    }
}
