package me.ram.bedwarsscoreboardaddon.commands;

import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.Team;
import io.github.bedwarsrel.game.TeamColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;

public class BedwarsRelCommandTabCompleter implements TabCompleter {

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
            return Arrays.asList("help", "setspawner", "addgame", "start", "stop", "addteam", "save", "setregion", "join", "setspawn", "setlobby", "settarget", "setbed", "leave", "reload", "setmainlobby", "list", "regionname", "removeteam", "removegame", "clearspawner", "gametime", "stats", "setminplayers", "setgameblock", "setbuilder", "setautobalance", "kick", "addteamjoin", "addholo", "removeholo", "debugpaste", "itemspaste");
        }
        List<String> games = new ArrayList<>();
        BedwarsRel.getInstance().getGameManager().getGames().forEach(game -> {
            games.add(game.getName());
        });
        if (args.length == 2) {
            String cmd = args[0].toLowerCase();
            final Set<String> setupCommands = new HashSet<>(Arrays.asList(
                    "setspawner", "start", "addteam", "save", "setregion", "setspawn",
                    "setlobby", "settarget", "setbed", "setmainlobby", "regionname",
                    "removeteam", "removegame", "clearspawner", "gametime",
                    "setminplayers", "setgameblock", "setbuilder", "setautobalance",
                    "addteamjoin"
            ));
            switch (cmd) {
                case "join":
                    return games;
                case "stats":
                    if (sender.hasPermission("bw.base")) {
                        return null;
                    }
                    return null;
                case "kick":
                    if (sender.hasPermission("bw.kick")) {
                        return null;
                    }
                    return null;
                case "stop":
                    if (sender.hasPermission("bw.base")) {
                        return games;
                    }
                    return null;
            }
            if (setupCommands.contains(cmd) && sender.hasPermission("bw.setup")) {
                return games;
            }
        }
        if (args.length == 3) {
            Game game = BedwarsRel.getInstance().getGameManager().getGame(args[1]);
            List<String> teams = new ArrayList<>();
            if (game != null) {
                for (Team team : game.getTeams().values()) {
                    teams.add(team.getName());
                }
            }
            if (args[0].equalsIgnoreCase("setspawner")) {
                if (sender.hasPermission("bw.setup")) {
                    List<String> list = new ArrayList<>(BedwarsRel.getInstance().getConfig().getConfigurationSection("resource").getKeys(false));
                    return list;
                }
            } else if (args[0].equalsIgnoreCase("setregion")) {
                if (sender.hasPermission("bw.setup")) {
                    return Arrays.asList("loc1", "loc2");
                }
            } else if (args[0].equalsIgnoreCase("setspawn")) {
                if (sender.hasPermission("bw.setup")) {
                    return teams;
                }
            } else if (args[0].equalsIgnoreCase("settarget")) {
                if (sender.hasPermission("bw.setup")) {
                    return teams;
                }
            } else if (args[0].equalsIgnoreCase("setbed")) {
                if (sender.hasPermission("bw.setup")) {
                    return teams;
                }
            } else if (args[0].equalsIgnoreCase("removeteam")) {
                if (sender.hasPermission("bw.setup")) {
                    return teams;
                }
            } else if (args[0].equalsIgnoreCase("setgameblock")) {
                if (sender.hasPermission("bw.setup")) {
                    List<String> list = new ArrayList<>();
                    for (Material type : Material.values()) {
                        list.add(type.name());
                    }
                    return list;
                }
            } else if (args[0].equalsIgnoreCase("setbuilder")) {
                if (sender.hasPermission("bw.setup")) {
                    return null;
                }
            } else if (args[0].equalsIgnoreCase("addteamjoin")) {
                if (sender.hasPermission("bw.setup")) {
                    return teams;
                }
            }
        }
        if (args.length == 4) {
            if (args[0].equalsIgnoreCase("addteam")) {
                if (sender.hasPermission("bw.setup")) {
                    List<String> list = new ArrayList<>();
                    for (TeamColor teamColor : TeamColor.values()) {
                        list.add(teamColor.name());
                    }
                    return list;
                }
            }
        }
        return new ArrayList<>();
    }
}
