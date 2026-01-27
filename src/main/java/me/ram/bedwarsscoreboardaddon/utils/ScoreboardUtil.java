package me.ram.bedwarsscoreboardaddon.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.Team;
import lombok.Getter;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.config.Config;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Scoreboard;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardUtil {

    @Getter
    private static final Map<Player, Scoreboard> scoreboards = new ConcurrentHashMap<>();
    private static final Map<Player, Map<Player, Integer>> playerHealth = new ConcurrentHashMap<>();
    private static final DecimalFormat decimalFormat = new DecimalFormat("##");

    public static void removePlayer(Player player) {
        scoreboards.remove(player);
        playerHealth.remove(player);
    }

    private static List<String> getNormalizedLines(List<String> lines) {
        List<String> normalizedLines = new ArrayList<>(lines.size());
        Set<String> seenLines = new HashSet<>(lines.size());

        for (String line : lines) {
            String processedLine = line != null && line.length() > 40 ? line.substring(0, 40) : line;

            if (!seenLines.contains(processedLine)) {
                normalizedLines.add(processedLine);
                seenLines.add(processedLine);
            }
        }

        // 填充到至少15行
        while (normalizedLines.size() < 15) {
            normalizedLines.add(0, null);
        }

        return normalizedLines;
    }

    private static String[] toElementArray(String title, List<String> lines) {
        List<String> result = new ArrayList<>();
        result.add(title != null && title.length() > 32 ? title.substring(0, 32) : (title != null ? title : "BedWars"));
        result.addAll(getNormalizedLines(lines));
        return result.toArray(new String[0]);
    }

    public static void setLobbyScoreboard(Player player, String title, List<String> lines, Game game) {
        String[] elements = toElementArray(title, lines);
        Scoreboard scoreboard = initializeScoreboard(player);

        try {
            updateScoreboardObjective(scoreboard, "bwsba-lobby", elements);
            setupTeamsForScoreboard(player, game, scoreboard);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Scoreboard initializeScoreboard(Player player) {
        Scoreboard currentScoreboard = player.getScoreboard();

        if (currentScoreboard == null ||
                currentScoreboard == Bukkit.getScoreboardManager().getMainScoreboard() ||
                currentScoreboard.getObjectives().size() != 1) {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            return player.getScoreboard();
        }

        return currentScoreboard;
    }

    private static void updateScoreboardObjective(Scoreboard scoreboard, String objectiveName, String[] elements) {
        org.bukkit.scoreboard.Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null) {
            objective = scoreboard.registerNewObjective(objectiveName, "dummy");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        // 设置显示名
        objective.setDisplayName(elements[0]);

        // 更新分数
        updateScores(scoreboard, objectiveName, elements);

        // 清理不需要的条目
        cleanupEntries(scoreboard, objectiveName, elements);
    }

    private static void updateScores(Scoreboard scoreboard, String objectiveName, String[] elements) {
        for (int i = 1; i < elements.length; i++) {
            if (elements[i] != null) {
                int expectedScore = 16 - i;
                org.bukkit.scoreboard.Score score = scoreboard.getObjective(DisplaySlot.SIDEBAR).getScore(elements[i]);

                if (score.getScore() != expectedScore) {
                    score.setScore(expectedScore);

                    // 清理冲突的条目
                    for (String entry : scoreboard.getEntries()) {
                        org.bukkit.scoreboard.Score otherScore = scoreboard.getObjective(objectiveName).getScore(entry);
                        if (otherScore.getScore() == expectedScore && !entry.equals(elements[i])) {
                            scoreboard.resetScores(entry);
                        }
                    }
                }
            }
        }
    }

    private static void cleanupEntries(Scoreboard scoreboard, String objectiveName, String[] elements) {
        Set<String> validEntries = new HashSet<>();
        for (String element : elements) {
            if (element != null) {
                validEntries.add(element);
            }
        }

        for (String entry : scoreboard.getEntries()) {
            if (!validEntries.contains(entry)) {
                scoreboard.resetScores(entry);
            }
        }
    }

    private static void setupTeamsForScoreboard(Player player, Game game, Scoreboard scoreboard) {
        for (Team bedwarsTeam : game.getTeams().values()) {
            String teamId = game.getName() + ":" + bedwarsTeam.getName();
            org.bukkit.scoreboard.Team bukkitTeam = scoreboard.getTeam(teamId);

            if (bukkitTeam == null) {
                bukkitTeam = scoreboard.registerNewTeam(teamId);
            }

            configureTeamTags(bukkitTeam, player, bedwarsTeam);
            configurePlayerListNames(player, bedwarsTeam);
        }
    }

    private static void configureTeamTags(org.bukkit.scoreboard.Team bukkitTeam, Player player, Team bedwarsTeam) {
        String color = bedwarsTeam.getChatColor().toString();
        String colorInitials = bedwarsTeam.getChatColor().name().substring(0, 1);
        String colorName = upperInitials(bedwarsTeam.getChatColor().name());
        String teamInitials = bedwarsTeam.getName().substring(0, 1);
        String teamName = bedwarsTeam.getName();

        String prefix = Config.playertag_prefix.isEmpty() ? "" :
                Config.playertag_prefix
                        .replace("{color}", color)
                        .replace("{color_initials}", colorInitials)
                        .replace("{color_name}", colorName)
                        .replace("{team_initials}", teamInitials)
                        .replace("{team}", teamName);

        String suffix = Config.playertag_suffix.isEmpty() ? "" :
                Config.playertag_suffix
                        .replace("{color}", color)
                        .replace("{color_initials}", colorInitials)
                        .replace("{color_name}", colorName)
                        .replace("{team_initials}", teamInitials)
                        .replace("{team}", teamName);

        bukkitTeam.setPrefix(ColorUtil.color(PlaceholderAPIUtil.setPlaceholders(player, prefix)));
        bukkitTeam.setSuffix(ColorUtil.color(PlaceholderAPIUtil.setPlaceholders(player, suffix)));
        bukkitTeam.setAllowFriendlyFire(false);
    }

    private static void configurePlayerListNames(Player player, Team bedwarsTeam) {
        String color = bedwarsTeam.getChatColor().toString();
        String colorInitials = bedwarsTeam.getChatColor().name().substring(0, 1);
        String colorName = upperInitials(bedwarsTeam.getChatColor().name());
        String teamInitials = bedwarsTeam.getName().substring(0, 1);
        String teamName = bedwarsTeam.getName();

        String prefix = Config.playerlist_prefix.isEmpty() ? "" :
                Config.playerlist_prefix
                        .replace("{color}", color)
                        .replace("{color_initials}", colorInitials)
                        .replace("{color_name}", colorName)
                        .replace("{team_initials}", teamInitials)
                        .replace("{team}", teamName);

        String suffix = Config.playerlist_suffix.isEmpty() ? "" :
                Config.playerlist_suffix
                        .replace("{color}", color)
                        .replace("{color_initials}", colorInitials)
                        .replace("{color_name}", colorName)
                        .replace("{team_initials}", teamInitials)
                        .replace("{team}", teamName);

        for (Player teamPlayer : bedwarsTeam.getPlayers()) {
            String playerName = ColorUtil.color(PlaceholderAPIUtil.setPlaceholders(teamPlayer, prefix)) +
                    teamPlayer.getName() +
                    ColorUtil.color(PlaceholderAPIUtil.setPlaceholders(teamPlayer, suffix));
            teamPlayer.setPlayerListName(playerName);
        }
    }

    private static void sendShowHealthPacket(Player player) {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

        if (Config.tab_health) {
            sendTabHealthPackets(protocolManager, player);
        }

        if (Config.tag_health) {
            sendTagHealthPackets(protocolManager, player);
        }
    }

    private static void sendTabHealthPackets(ProtocolManager protocolManager, Player player) {
        try {
            // 创建目标
            PacketContainer objectivePacket = protocolManager.createPacket(PacketType.Play.Server.SCOREBOARD_OBJECTIVE);
            objectivePacket.getIntegers().write(0, 0);
            objectivePacket.getStrings().write(0, "bwsba-game-list");
            objectivePacket.getStrings().write(1, "bwsba-game-list");
            protocolManager.sendServerPacket(player, objectivePacket);

            // 显示目标
            PacketContainer displayPacket = protocolManager.createPacket(PacketType.Play.Server.SCOREBOARD_DISPLAY_OBJECTIVE);
            displayPacket.getIntegers().write(0, 0);
            displayPacket.getStrings().write(0, "bwsba-game-list");
            protocolManager.sendServerPacket(player, displayPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendTagHealthPackets(ProtocolManager protocolManager, Player player) {
        try {
            // 创建目标
            PacketContainer objectivePacket = protocolManager.createPacket(PacketType.Play.Server.SCOREBOARD_OBJECTIVE);
            objectivePacket.getIntegers().write(0, 0);
            objectivePacket.getStrings().write(0, "bwsba-game-name");
            objectivePacket.getStrings().write(1, "bwsba-game-name");
            protocolManager.sendServerPacket(player, objectivePacket);

            // 显示目标
            PacketContainer displayPacket = protocolManager.createPacket(PacketType.Play.Server.SCOREBOARD_DISPLAY_OBJECTIVE);
            displayPacket.getIntegers().write(0, 2);
            displayPacket.getStrings().write(0, "bwsba-game-name");
            protocolManager.sendServerPacket(player, displayPacket);

            // 设置图标
            PacketContainer iconPacket = protocolManager.createPacket(PacketType.Play.Server.SCOREBOARD_OBJECTIVE);
            iconPacket.getIntegers().write(0, 2);
            iconPacket.getStrings().write(0, "bwsba-game-name");
            iconPacket.getStrings().write(1, "§4❤");
            protocolManager.sendServerPacket(player, iconPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendHealthValuePacket(Player player, Player target, int value) {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

        if (Config.tab_health) {
            try {
                PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.SCOREBOARD_SCORE);
                packet.getIntegers().write(0, value);
                packet.getStrings().write(0, target.getName());
                packet.getStrings().write(1, "bwsba-game-list");
                protocolManager.sendServerPacket(player, packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (Config.tag_health) {
            try {
                PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.SCOREBOARD_SCORE);
                packet.getIntegers().write(0, value);
                packet.getStrings().write(0, target.getName());
                packet.getStrings().write(1, "bwsba-game-name");
                protocolManager.sendServerPacket(player, packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void setGameScoreboard(Player player, String title, List<String> lines, Game game) {
        boolean exists = scoreboards.containsKey(player);
        if (!exists) {
            scoreboards.put(player, Bukkit.getScoreboardManager().getNewScoreboard());
        }

        String[] elements = toElementArray(title, lines);
        Scoreboard scoreboard = scoreboards.get(player);

        try {
            updateScoreboardObjective(scoreboard, "bwsba-game", elements);

            if ((player.getScoreboard() == null || !player.getScoreboard().equals(scoreboard)) && !exists) {
                sendShowHealthPacket(player);
            }

            updatePlayerHealthTracking(player, game);

            Team playerTeam = game.getPlayerTeam(player);
            List<UUID> invisiblePlayers = getInvisiblePlayers(game);

            setupTeamsForScoreboard(player, game, scoreboard);

            updateTeamMembership(player, game, scoreboard, playerTeam, invisiblePlayers);

            hideInvisiblePlayerTags(player, game, playerTeam, invisiblePlayers);

            if (player.getScoreboard() == null || !player.getScoreboard().equals(scoreboard)) {
                player.setScoreboard(scoreboard);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updatePlayerHealthTracking(Player player, Game game) {
        playerHealth.putIfAbsent(player, new ConcurrentHashMap<>());
        Map<Player, Integer> healthMap = playerHealth.get(player);

        for (Player target : game.getPlayers()) {
            int health = Integer.parseInt(decimalFormat.format(target.getHealth()));
            if (!Objects.equals(healthMap.get(target), health)) {
                sendHealthValuePacket(player, target, health);
                healthMap.put(target, health);
            }
        }
    }

    private static List<UUID> getInvisiblePlayers(Game game) {
        if (Main.getInstance().getArenaManager().getArena(game.getName()) != null) {
            return Main.getInstance().getArenaManager().getArena(game.getName())
                    .getInvisiblePlayer().getPlayers();
        }
        return Collections.emptyList();
    }

    private static void updateTeamMembership(Player player, Game game, Scoreboard scoreboard,
                                             Team playerTeam, List<UUID> invisiblePlayers) {
        for (Team bedwarsTeam : game.getTeams().values()) {
            String teamId = game.getName() + ":" + bedwarsTeam.getName();
            org.bukkit.scoreboard.Team bukkitTeam = scoreboard.getTeam(teamId);

            String color = bedwarsTeam.getChatColor().toString();
            String colorInitials = bedwarsTeam.getChatColor().name().substring(0, 1);
            String colorName = upperInitials(bedwarsTeam.getChatColor().name());
            String teamInitials = bedwarsTeam.getName().substring(0, 1);
            String teamName = bedwarsTeam.getName();

            String prefix = Config.playerlist_prefix.isEmpty() ? "" :
                    Config.playerlist_prefix
                            .replace("{color}", color)
                            .replace("{color_initials}", colorInitials)
                            .replace("{color_name}", colorName)
                            .replace("{team_initials}", teamInitials)
                            .replace("{team}", teamName);

            String suffix = Config.playerlist_suffix.isEmpty() ? "" :
                    Config.playerlist_suffix
                            .replace("{color}", color)
                            .replace("{color_initials}", colorInitials)
                            .replace("{color_name}", colorName)
                            .replace("{team_initials}", teamInitials)
                            .replace("{team}", teamName);

            for (Player teamPlayer : bedwarsTeam.getPlayers()) {
                String playerName = ColorUtil.color(PlaceholderAPIUtil.setPlaceholders(player, prefix)) +
                        teamPlayer.getName() +
                        ColorUtil.color(PlaceholderAPIUtil.setPlaceholders(player, suffix));
                teamPlayer.setPlayerListName(playerName);

                if (!bukkitTeam.hasPlayer(teamPlayer)) {
                    boolean canAddToTeam = !invisiblePlayers.contains(teamPlayer.getUniqueId()) ||
                            (playerTeam != null && playerTeam.getPlayers().contains(teamPlayer));

                    if (canAddToTeam) {
                        bukkitTeam.addPlayer(teamPlayer);
                    } else {
                        teamPlayer.setPlayerListName(playerName);
                    }
                }
            }
        }
    }

    private static void hideInvisiblePlayerTags(Player player, Game game, Team playerTeam, List<UUID> invisiblePlayers) {
        if (playerTeam != null && invisiblePlayers.contains(player.getUniqueId())) {
            for (Team otherTeam : game.getTeams().values()) {
                if (!otherTeam.getName().equals(playerTeam.getName())) {
                    for (Player otherPlayer : otherTeam.getPlayers()) {
                        Scoreboard otherScoreboard = otherPlayer.getScoreboard();
                        if (otherScoreboard != null) {
                            for (org.bukkit.scoreboard.Team team : otherScoreboard.getTeams()) {
                                if (playerTeam.getPlayers().contains(player)) {
                                    team.removePlayer(player);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static String upperInitials(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
