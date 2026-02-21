package me.ram.bedwarsscoreboardaddon.addon;

import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.events.BedwarsGameOverEvent;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.GameState;
import io.github.bedwarsrel.game.Team;
import lombok.Getter;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.manager.PlaceholderManager;
import me.ram.bedwarsscoreboardaddon.storage.PlayerGameStorage;
import me.ram.bedwarsscoreboardaddon.utils.PlaceholderAPIUtil;
import me.ram.bedwarsscoreboardaddon.utils.ScoreboardUtil;
import me.ram.bedwarsscoreboardaddon.utils.Utils;
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

    public ScoreBoard(Arena arena) {
        this.arena = arena;
        game = arena.getGame();
        placeholderManager = new PlaceholderManager(game);
        team_status = new HashMap<>();
        // 这块刷新在TimeTask
        timer_placeholder = new HashMap<>();
        plan_infos = new HashMap<>();
        // 刷新计分板任务
        // 如果不是20再进行自定义刷新, 否则交给下方倒计时执行 避免刷新不同步
        if (Config.scoreboard_interval != 20) {
            arena.addGameTask(new BukkitRunnable() {
                @Override
                public void run() {
                    updateScoreboard();
                }
            }.runTaskTimer(Main.getInstance(), 0L, Config.scoreboard_interval));
        }
        arena.addGameTask(new BukkitRunnable() {
            @Override
            public void run() {
                for (BukkitTask task : game.getRunningTasks()) {
                    task.cancel();
                }
                game.getRunningTasks().clear();
                startTimerCountdown(game);
            }
        }.runTaskLater(Main.getInstance(), 20L));
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
        game.setTimeLeft(BedwarsRel.getInstance().getMaxLength());
        game.addRunningTask(new BukkitRunnable() {
            public void run() {
                if (game.getTimeLeft() == 0) {
                    game.setOver(true);
                    game.getCycle().checkGameOver();
                    cancel();
                    return;
                }
                if (game.getState() != GameState.RUNNING || game.getPlayers().isEmpty()) {
                    arena.onOver(new BedwarsGameOverEvent(game, null));
                    arena.onEnd();
                    cancel();
                    return;
                }
                game.setTimeLeft(game.getTimeLeft() - 1);
                arena.getTimeTask().refresh();
                if (Config.scoreboard_interval == 20) updateScoreboard();
            }
        }.runTaskTimer(BedwarsRel.getInstance(), 0L, 20L));
    }

    public void updateScoreboard() {
        // 计算队伍数量
        int alive_teams = 0;
        int remain_teams = 0;
        Map<String, Team> teams = game.getTeams();

        for (Team team : teams.values()) {
            if (!team.isDead(game)) {
                alive_teams++;
            }
            if (!team.getPlayers().isEmpty()) {
                remain_teams++;
            }
        }

        // 计算凋零弓时间
        int wither = game.getTimeLeft() - Config.witherbow_gametime;
        String bowtime = arena.getWitherBow().isWitherbowEnabled()
                ? Config.witherbow_already_start
                : String.format("%d:%02d", wither / 60, wither % 60);

        if (title_index >= Config.scoreboard_title.size()) {
            title_index = 0;
        }
        String formattedTime = Utils.getFormattedTimeLeft(game.getTimeLeft());
        String score_title = Config.scoreboard_title.isEmpty()
                ? "BedWars"
                : Config.scoreboard_title.get(title_index)
                .replace("{game}", game.getName())
                .replace("{time}", formattedTime);
        title_index++;

        // 获取计分板行配置
        String teams_count = String.valueOf(teams.size());
        List<String> scoreboard_lines = Config.scoreboard_lines.getOrDefault(
                teams_count,
                Config.scoreboard_lines.getOrDefault("default", Arrays.asList("", "{team_status}", ""))
        );

        // 计算存活玩家数量
        int alive_players = (int) game.getPlayers().stream()
                .filter(p -> !game.isSpectator(p))
                .count();

        // 为每个玩家构建计分板
        for (Player player : game.getPlayers()) {
            List<String> lines = new ArrayList<>();

            // 获取玩家队伍信息
            Team player_team = game.getPlayerTeam(player);
            PlayerGameStorage playerGameStorage = arena.getPlayerGameStorage();
            String playerName = player.getName();
            String player_total_kills = playerGameStorage.getTotalKills(playerName) + "";
            String player_kills = playerGameStorage.getKills(playerName) + "";
            String player_final_kills = playerGameStorage.getFinalKills(playerName) + "";
            String player_dies = playerGameStorage.getDies(playerName) + "";
            String player_beds = playerGameStorage.getBeds(playerName) + "";
            String playerkillStreaks = playerGameStorage.getKillStreaks(playerName) + "";
            String playerHighestkillStreaks = playerGameStorage.getHighestKillStreak(playerName) + "";
            String player_team_color = "§f";
            String player_team_players = "";
            String player_team_name = "";
            String player_team_bed_status = "";

            if (player_team != null) {
                player_team_color = player_team.getChatColor() + "";
                player_team_players = player_team.getPlayers().size() + "";
                player_team_name = player_team.getName();
                player_team_bed_status = getTeamBedStatus(game, player_team);
            }

            for (String ls : scoreboard_lines) {
                if (ls.contains("{team_status}")) {
                    // 队伍状态行
                    for (Team t : teams.values()) {
                        String you = (game.getPlayerTeam(player) == t) ? Config.scoreboard_you : "";
                        if (team_status.containsKey(t.getName())) {
                            lines.add(team_status.get(t.getName()).replace("{you}", you));
                        } else {
                            lines.add(ls.replace("{team_status}", getTeamStatusFormat(game, t).replace("{you}", you)));
                        }
                    }
                } else {
                    String date = new SimpleDateFormat(Config.date_format).format(new Date());
                    String add_line = ls;

                    // 事件信息替换
                    for (String key : plan_infos.keySet()) {
                        add_line = add_line.replace("{plan_" + key + "}", plan_infos.get(key));
                    }

                    // 随机事件替换
                    String randomevent = "";
                    Optional<RandomEvents> event = arena.getRandomEventsManager().getNextEvent();
                    if (event.isPresent()) {
                        randomevent = event.get().getEventName();
                    }
                    add_line = add_line.replace("{randomevent}", randomevent);

                    // 变量替换
                    add_line = add_line.replace("{death_mode}", arena.getDeathMode().getDeathmode_time())
                            .replace("{remain_teams}", remain_teams + "")
                            .replace("{alive_teams}", alive_teams + "")
                            .replace("{alive_players}", alive_players + "")
                            .replace("{teams}", teams.size() + "")
                            .replace("{color}", player_team_color)
                            .replace("{team_peoples}", player_team_players)
                            .replace("{player_name}", playerName)
                            .replace("{team}", player_team_name)
                            .replace("{beds}", player_beds)
                            .replace("{dies}", player_dies)
                            .replace("{totalkills}", player_total_kills)
                            .replace("{finalkills}", player_final_kills)
                            .replace("{kills}", player_kills)
                            .replace("{killstreaks}", playerkillStreaks)
                            .replace("{highestkillstreaks}", playerHighestkillStreaks)
                            .replace("{time}", getGameTime(game.getTimeLeft()))
                            .replace("{formattime}", formattedTime)
                            .replace("{game}", game.getName())
                            .replace("{date}", date)
                            .replace("{online}", game.getPlayers().size() + "")
                            .replace("{bowtime}", bowtime)
                            .replace("{team_bed_status}", player_team_bed_status)
                            .replace("{no_break_bed}", arena.getNoBreakBed().getTime());

                    // 生命等级
                    for (String key : arena.getHealthLevel().getLevelTime().keySet()) {
                        add_line = add_line.replace("{sethealthtime_" + key + "}", arena.getHealthLevel().getLevelTime().get(key));
                    }

                    // 资源升级替换
                    for (String key : arena.getResourceUpgrade().getUpgTime().keySet()) {
                        add_line = add_line.replace("{resource_upgrade_" + key + "}", arena.getResourceUpgrade().getUpgTime().get(key));
                    }

                    // 游戏占位符替换
                    for (String key : placeholderManager.getGamePlaceholder().keySet()) {
                        add_line = add_line.replace(key, placeholderManager.getGamePlaceholder().get(key).onGamePlaceholderRequest(game));
                    }

                    // 队伍特定占位符替换
                    for (Team t : teams.values()) {
                        String team_name = t.getName();
                        if (add_line.contains("{team_" + team_name + "_status}")) {
                            String stf = getTeamStatusFormat(game, t);
                            String you_indicator = (game.getPlayerTeam(player) == null) ? "" :
                                    (game.getPlayerTeam(player) == t) ? Config.scoreboard_you : "";
                            stf = stf.replace("{you}", you_indicator);
                            add_line = add_line.replace("{team_" + team_name + "_status}", stf);
                        }
                        if (add_line.contains("{team_" + team_name + "_bed_status}")) {
                            add_line = add_line.replace("{team_" + team_name + "_bed_status}", getTeamBedStatus(game, t));
                        }
                        if (add_line.contains("{team_" + team_name + "_peoples}")) {
                            add_line = add_line.replace("{team_" + team_name + "_peoples}", t.getPlayers().size() + "");
                        }
                    }

                    // 队伍占位符替换
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

                    // 玩家占位符替换
                    if (placeholderManager.getPlayerPlaceholders().containsKey(playerName)) {
                        for (String identifier : placeholderManager.getPlayerPlaceholder(playerName).keySet()) {
                            add_line = add_line.replace(identifier, placeholderManager.getPlayerPlaceholder(playerName).get(identifier).onPlayerPlaceholderRequest(game, player));
                        }
                    } else {
                        for (String playername : placeholderManager.getPlayerPlaceholders().keySet()) {
                            for (String placeholder : placeholderManager.getPlayerPlaceholders().get(playername).keySet()) {
                                add_line = add_line.replace(placeholder, "");
                            }
                        }
                    }

                    // 计时器占位符替换
                    for (String placeholder : timer_placeholder.keySet()) {
                        add_line = add_line.replace(placeholder, timer_placeholder.get(placeholder));
                    }

                    // 最终占位符处理
                    add_line = PlaceholderAPIUtil.setPlaceholders(player, add_line);
                    lines.add(add_line);
                }
            }
            String player_name = playerName.toLowerCase();
            if (player_name.equals("yukiend") || player_name.equals("linmoyu_") || player_name.startsWith("lmy_")) {
                lines.add("BWSBA Modified By @YukiEnd");
            }

            // 设置计分板
            String title = PlaceholderAPIUtil.setPlaceholders(player, score_title);
            ScoreboardUtil.setGameScoreboard(player, title, lines, game);
        }
    }

    private String getTeamBedStatus(Game game, Team team) {
        if (team.isDead(game)) {
            return Config.scoreboard_team_bed_status_bed_destroyed;
        } else if (!team.isDead(game) && team.getPlayers().isEmpty()) {
            return Config.scoreboard_team_bed_status_bed_alive_empty;
        } else {
            return Config.scoreboard_team_bed_status_bed_alive;
        }
    }

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
