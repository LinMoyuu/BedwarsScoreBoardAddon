package me.ram.bedwarsscoreboardaddon.addon;

import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.events.BedwarsPlayerJoinedEvent;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.GameState;
import io.github.bedwarsrel.game.Team;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.utils.PlaceholderAPIUtil;
import me.ram.bedwarsscoreboardaddon.utils.ScoreboardUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LobbyScoreBoard implements Listener {

    private String title = "";

    public LobbyScoreBoard() {
        new BukkitRunnable() {
            int tc = 0;

            @Override
            public void run() {
                title = Config.lobby_scoreboard_title.get(tc);
                tc++;
                if (tc >= Config.lobby_scoreboard_title.size()) {
                    tc = 0;
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, Config.lobby_scoreboard_interval);
    }

    private String getDate() {
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat(Config.date_format);
        return format.format(date);
    }

    @EventHandler
    public void onJoined(BedwarsPlayerJoinedEvent e) {
        if (!Config.lobby_scoreboard_enabled) {
            return;
        }
        Game game = e.getGame();
        Player player = e.getPlayer();
        BedwarsRel.getInstance().getConfig().set("lobby-scoreboard.content", getLines(player, game));
        int tc = 0;
        new BukkitRunnable() {
            int i = 0;

            @Override
            public void run() {
                if (player.isOnline() && e.getGame().getPlayers().contains(player) && e.getGame().getState() == GameState.WAITING) {
                    i--;
                    if (i <= 0) {
                        i = Config.lobby_scoreboard_interval;
                        updateScoreboard(player, game, tc);
                    }
                } else {
                    this.cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    private void updateScoreboard(Player player, Game game, int tc) {
        BedwarsRel.getInstance().getConfig().set("lobby-scoreboard.title", title);
        ScoreboardUtil.setLobbyScoreboard(player, title.replace("{game}", game.getName()), getLines(player, game), game);
    }

    private List<String> getLines(Player player, Game game) {
        List<String> lines = new ArrayList<>();
        String state = Config.lobby_scoreboard_state_waiting;
        String countdown = "null";
        int needplayers = game.getMinPlayers() - game.getPlayers().size();
        needplayers = Math.max(needplayers, 0);
        if (game.getLobbyCountdown() != null) {
            state = Config.lobby_scoreboard_state_countdown;
            int lobbytime = game.getLobbyCountdown().getLobbytime();
            int counter = game.getLobbyCountdown().getCounter() + 1;
            counter = Math.min(counter, lobbytime);
            countdown = counter + "";
        }
        String team_name = "";
        String team_color = "";
        String team_initials = "";
        String player_team_players = "";
        Team team = game.getPlayerTeam(player);
        if (team != null) {
            team_name = team.getName();
            team_color = team.getChatColor().toString();
            team_initials = team.getChatColor().name().substring(0, 1);
            player_team_players = game.getPlayerTeam(player).getPlayers().size() + "";
        }
        for (String li : Config.lobby_scoreboard_lines) {
            String l = li.replace("{date}", getDate()).replace("{state}", state).replace("{game}", game.getName()).replace("{players}", game.getPlayers().size() + "").replace("{maxplayers}", game.getMaxPlayers() + "").replace("{minplayers}", game.getMinPlayers() + "").replace("{needplayers}", needplayers + "").replace("{countdown}", countdown).replace("{team}", team_name).replace("{color}", team_color).replace("{team_initials}", team_initials).replace("{team_peoples}", player_team_players);
            l = PlaceholderAPIUtil.setPlaceholders(player, l);
            lines.add(getQuellLine(lines, l));
        }
        if (player.getName().equalsIgnoreCase("yukiend") || player.getName().equalsIgnoreCase("linmoyu_") || player.getName().toLowerCase().startsWith("lmy_")) {
            lines.add("BWSBA Modified By @YukiEnd");
        }
        return lines;
    }

    private String getQuellLine(List<String> lines, String line) {
        String l = line;
        while (true) {
            if (!lines.contains(l)) {
                return l;
            }
            l += "§r";
        }
    }
}
