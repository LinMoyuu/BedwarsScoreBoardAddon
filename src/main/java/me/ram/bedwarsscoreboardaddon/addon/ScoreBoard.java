package me.ram.bedwarsscoreboardaddon.addon;

import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.Team;
import lombok.Getter;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.manager.PlaceholderManager;
import me.ram.bedwarsscoreboardaddon.utils.PlaceholderAPIUtil;
import me.ram.bedwarsscoreboardaddon.utils.ScoreboardUtil;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.text.SimpleDateFormat;
import java.util.*;

public class ScoreBoard {

    private final Arena arena;
    private final Game game;
    @Getter
    private final Map<String, String> timer_placeholder;
    private final PlaceholderManager placeholderManager;
    private final Map<String, String> team_status;
    private int title_index = 0;
    @Getter
    private Map<String, String> plan_infos;
    private Map<String, String> over_plan_info;

    public ScoreBoard(Arena arena) {
        this.arena = arena;
        game = arena.getGame();
        placeholderManager = new PlaceholderManager(game);
        team_status = new HashMap<>();
        timer_placeholder = new HashMap<>();
        plan_infos = new HashMap<>();
        over_plan_info = new HashMap<>();
        arena.addGameTask(new BukkitRunnable() {
            int i = Config.scoreboard_interval;

            @Override
            public void run() {
                i--;
                if (i <= 0) {
                    updateScoreboard();
                    i = Config.scoreboard_interval;
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L));
        arena.addGameTask(new BukkitRunnable() {
            @Override
            public void run() {
                for (BukkitTask task : game.getRunningTasks()) {
                    task.cancel();
                }
                game.getRunningTasks().clear();
                startTimerCountdown(game);
            }
        }.runTaskLater(Main.getInstance(), 19L));
    }

    public PlaceholderManager getPlaceholderManager() {
        return placeholderManager;
    }

    public void setTeamStatusFormat(String team, String status) {
        team_status.put(team, status);
    }

    public void removeTeamStatusFormat(String team) {
        team_status.remove(team);
    }

    public Map<String, String> getTeamStatusFormat() {
        return team_status;
    }

    private String getGameTime(int time) {
        return String.valueOf(time / 60);
    }

    private void startTimerCountdown(Game game) {
        game.addRunningTask(new BukkitRunnable() {
            public void run() {
                if (game.getTimeLeft() == 0) {
                    game.setOver(true);
                    game.getCycle().checkGameOver();
                    cancel();
                    return;
                }
                game.setTimeLeft(game.getTimeLeft() - 1);
            }
        }.runTaskTimer(BedwarsRel.getInstance(), 0L, 20L));
    }

    public void updateScoreboard() {
        List<String> lines = new ArrayList<>();
        if (game.getTimeLeft() == 1) {
            over_plan_info = plan_infos;
        } else if (game.getTimeLeft() < 1) {
            plan_infos = over_plan_info;
        }
        int alive_teams = 0;
        int remain_teams = 0;
        for (Team team : game.getTeams().values()) {
            if (!team.isDead(game)) {
                alive_teams++;
            }
            if (!team.getPlayers().isEmpty()) {
                remain_teams++;
            }
        }
        int wither = arena.getGameLeft() - Config.witherbow_gametime;
        String format = wither / 60 + ":" + ((wither % 60 < 10) ? ("0" + wither % 60) : (wither % 60));
        String bowtime = null;
        if (wither > 0) {
            bowtime = format;
        }
        if (wither <= 0) {
            bowtime = Config.witherbow_already_start;
        }
        String score_title;
        if (title_index >= Config.scoreboard_title.size()) {
            title_index = 0;
        }
        score_title = Config.scoreboard_title.isEmpty() ? "BedWars" : Config.scoreboard_title.get(title_index).replace("{game}", game.getName()).replace("{time}", getFormattedTimeLeft(game.getTimeLeft()));
        title_index++;
        String teams = game.getTeams().size() + "";
        List<String> scoreboard_lines;
        if (Config.scoreboard_lines.containsKey(teams)) {
            scoreboard_lines = Config.scoreboard_lines.get(teams);
        } else if (Config.scoreboard_lines.containsKey("default")) {
            scoreboard_lines = Config.scoreboard_lines.get("default");
        } else {
            scoreboard_lines = Arrays.asList("", "{team_status}", "");
        }
        int alive_players = 0;
        for (Player p : game.getPlayers()) {
            if (!game.isSpectator(p)) {
                alive_players++;
            }
        }
        for (Player player : game.getPlayers()) {
            Team player_team = game.getPlayerTeam(player);
            lines.clear();
            String player_total_kills = arena.getPlayerGameStorage().getPlayerTotalKills().getOrDefault(player.getName(), 0) + "";
            String player_kills = arena.getPlayerGameStorage().getPlayerKills().getOrDefault(player.getName(), 0) + "";
            String player_final_kills = arena.getPlayerGameStorage().getPlayerFinalKills().getOrDefault(player.getName(), 0) + "";
            String player_dis = arena.getPlayerGameStorage().getPlayerDies().getOrDefault(player.getName(), 0) + "";
            String player_bes = arena.getPlayerGameStorage().getPlayerBeds().getOrDefault(player.getName(), 0) + "";
            String player_team_color = "§f";
            String player_team_players = "";
            String player_team_name = "";
            String player_team_bed_status = "";
            if (game.getPlayerTeam(player) != null) {
                player_team_color = game.getPlayerTeam(player).getChatColor() + "";
                player_team_players = game.getPlayerTeam(player).getPlayers().size() + "";
                player_team_name = game.getPlayerTeam(player).getName();
                player_team_bed_status = getTeamBedStatus(game, game.getPlayerTeam(player));
            }
            for (String ls : scoreboard_lines) {
                if (ls.contains("{team_status}")) {
                    for (Team t : game.getTeams().values()) {
                        String you = "";
                        if (game.getPlayerTeam(player) != null) {
                            if (game.getPlayerTeam(player) == t) {
                                you = Config.scoreboard_you;
                            } else {
                                you = "";
                            }
                        }
                        if (team_status.containsKey(t.getName())) {
                            lines.add(team_status.get(t.getName()).replace("{you}", you));
                        } else {
                            lines.add(ls.replace("{team_status}", getTeamStatusFormat(game, t).replace("{you}", you)));
                        }
                    }
                } else {
                    String date = new SimpleDateFormat(Config.date_format).format(new Date());
                    String add_line = ls;
                    for (String key : plan_infos.keySet()) {
                        add_line = add_line.replace("{plan_" + key + "}", plan_infos.get(key));
                    }
                    String randomPlay = "";
                    List<RandomEvents> events = arena.getCurrentGameEvents();
                    if (events != null && !events.isEmpty()) {
                        randomPlay = events.get(0).getEventName();
                    }
                    add_line = add_line.replace("{randomplay}", randomPlay);
                    add_line = add_line.replace("{death_mode}", arena.getDeathMode().getDeathmodeTime()).replace("{remain_teams}", remain_teams + "").replace("{alive_teams}", alive_teams + "").replace("{alive_players}", alive_players + "").replace("{teams}", game.getTeams().size() + "").replace("{color}", player_team_color).replace("{team_peoples}", player_team_players).replace("{player_name}", player.getName()).replace("{team}", player_team_name).replace("{beds}", player_bes).replace("{dies}", player_dis).replace("{totalkills}", player_total_kills).replace("{finalkills}", player_final_kills).replace("{kills}", player_kills).replace("{time}", getGameTime(game.getTimeLeft())).replace("{formattime}", getFormattedTimeLeft(game.getTimeLeft())).replace("{game}", game.getName()).replace("{date}", date).replace("{online}", game.getPlayers().size() + "").replace("{bowtime}", bowtime).replace("{team_bed_status}", player_team_bed_status).replace("{no_break_bed}", arena.getNoBreakBed().getTime());
                    for (String key : arena.getHealthLevel().getLevelTime().keySet()) {
                        add_line = add_line.replace("{sethealthtime_" + key + "}", arena.getHealthLevel().getLevelTime().get(key));
                    }
                    for (String key : arena.getResourceUpgrade().getUpgTime().keySet()) {
                        add_line = add_line.replace("{resource_upgrade_" + key + "}", arena.getResourceUpgrade().getUpgTime().get(key));
                    }
                    for (String key : placeholderManager.getGamePlaceholder().keySet()) {
                        add_line = add_line.replace(key, placeholderManager.getGamePlaceholder().get(key).onGamePlaceholderRequest(game));
                    }
                    for (Team t : game.getTeams().values()) {
                        if (add_line.contains("{team_" + t.getName() + "_status}")) {
                            String stf = getTeamStatusFormat(game, t);
                            if (game.getPlayerTeam(player) == null) {
                                stf = stf.replace("{you}", "");
                            } else if (game.getPlayerTeam(player) == t) {
                                stf = stf.replace("{you}", Config.scoreboard_you);
                            } else {
                                stf = stf.replace("{you}", "");
                            }
                            add_line = add_line.replace("{team_" + t.getName() + "_status}", stf);
                        }
                        if (add_line.contains("{team_" + t.getName() + "_bed_status}")) {
                            add_line = add_line.replace("{team_" + t.getName() + "_bed_status}", getTeamBedStatus(game, t));
                        }
                        if (add_line.contains("{team_" + t.getName() + "_peoples}")) {
                            add_line = add_line.replace("{team_" + t.getName() + "_peoples}", t.getPlayers().size() + "");
                        }
                    }
                    if (player_team == null || !placeholderManager.getTeamPlaceholders().containsKey(player_team.getName())) {
                        for (String teamname : placeholderManager.getTeamPlaceholders().keySet()) {
                            for (String placeholder : placeholderManager.getTeamPlaceholders().get(teamname).keySet()) {
                                add_line = add_line.replace(placeholder, "");
                            }
                        }
                    } else {
                        for (String identifier : placeholderManager.getTeamPlaceholder(player_team.getName()).keySet()) {
                            add_line = add_line.replace(identifier, placeholderManager.getTeamPlaceholder(player_team.getName()).get(identifier).onTeamPlaceholderRequest(player_team));
                        }
                    }
                    if (placeholderManager.getPlayerPlaceholders().containsKey(player.getName())) {
                        for (String identifier : placeholderManager.getPlayerPlaceholder(player.getName()).keySet()) {
                            add_line = add_line.replace(identifier, placeholderManager.getPlayerPlaceholder(player.getName()).get(identifier).onPlayerPlaceholderRequest(game, player));
                        }
                    } else {
                        for (String playername : placeholderManager.getPlayerPlaceholders().keySet()) {
                            for (String placeholder : placeholderManager.getPlayerPlaceholders().get(playername).keySet()) {
                                add_line = add_line.replace(placeholder, "");
                            }
                        }
                    }
                    for (String placeholder : timer_placeholder.keySet()) {
                        add_line = add_line.replace(placeholder, timer_placeholder.get(placeholder));
                    }
                    add_line = PlaceholderAPIUtil.setPlaceholders(player, add_line);
                    lines.add(add_line);
                }
            }
            if (player.getName().equalsIgnoreCase("yukiend") || player.getName().equalsIgnoreCase("linmoyu_") || player.getName().toLowerCase().startsWith("lmy_")) {
                lines.add("BWSBA Modified By @YukiEnd");
            }
            String title = PlaceholderAPIUtil.setPlaceholders(player, score_title);
            ScoreboardUtil.setGameScoreboard(player, title, lines, game);
        }
    }

    private String getFormattedTimeLeft(int time) {
        int min = (int) (double) (time / 60);
        int sec = time % 60;
        String minStr = ((min < 10) ? ("0" + min) : String.valueOf(min));
        String secStr = ((sec < 10) ? ("0" + sec) : String.valueOf(sec));
        return minStr + ":" + secStr;
    }

    private String getTeamBedStatus(Game game, Team team) {
        if (team.isDead(game)) {
            return Config.scoreboard_team_bed_status_bed_destroyed;
        } else if (!team.isDead(game) && team.getPlayers().isEmpty()) {
            return Config.scoreboard_team_bed_status_bed_alive_empty;
        } else {
            return Config.scoreboard_team_bed_status_bed_alive;
        }
        // 改之后没测试过
    }

    // 改之后没测试过
    private String getTeamStatusFormat(Game game, Team team) {
        String alive = Config.scoreboard_team_status_format_bed_alive;
        String destroyed = Config.scoreboard_team_status_format_bed_destroyed;
        String alive_empty = Config.scoreboard_team_status_format_bed_alive_empty;
        String status;
        if (team.isDead(game)) {
            status = destroyed;
        } else if (!team.isDead(game) && team.getPlayers().isEmpty()) {
            status = alive_empty;
        } else {
            status = alive;
        }
        if (team.isDead(game) && team.getPlayers().isEmpty()) {
            status = Config.scoreboard_team_status_format_team_dead;
        }
        return status.replace("{bed_status}", getTeamBedStatus(game, team)).replace("{color}", team.getChatColor() + "").replace("{color_initials}", team.getChatColor().name().substring(0, 1)).replace("{color_name}", upperInitials(team.getChatColor().name())).replace("{players}", team.getPlayers().size() + "").replace("{team_initials}", team.getName().substring(0, 1)).replace("{team}", team.getName());
    }

    private String upperInitials(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
