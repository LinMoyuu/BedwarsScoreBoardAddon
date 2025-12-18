package me.ram.bedwarsscoreboardaddon.addon;

import io.github.bedwarsrel.game.Game;
import lombok.Getter;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;

public class TimeTask {

    @Getter
    private final Game game;
    @Getter
    private final Arena arena;
    @Getter
    private int gameLeft;

    public TimeTask(Arena arena) {
        this.arena = arena;
        this.game = arena.getGame();
        this.gameLeft = arena.getGameLeft();

        // TimeCommand 开局指令
        for (String cmd : Config.timecommand_startcommand) {
            if (cmd.isEmpty()) {
                continue;
            }
            if (cmd.contains("{player}")) {
                for (Player player : game.getPlayers()) {
                    Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), ColorUtil.color(cmd.replace("{player}", player.getName())));
                }
            } else if (cmd.contains("{gamename}")) {
                Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), ColorUtil.color(cmd.replace("{gamename}", game.getName())));
            } else {
                Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), ColorUtil.color(cmd));
            }
        }

        // 这段包有问题的
        arena.addGameTask(new BukkitRunnable() {
            @Override
            public void run() {
                if (arena.isOver()) {
                    this.cancel();
                    return;
                }
                gameLeft--;
                arena.setGameLeft(gameLeft);
            }
        }.runTaskTimer(Main.getInstance(), 0L, 20L));

        ScoreBoard scoreBoard = arena.getScoreBoard();
        // 刷新 自定义任务
        Map<String, String> plan_infos = scoreBoard.getPlan_infos();
        arena.addGameTask(new BukkitRunnable() {
            @Override
            public void run() {
                if (arena.isOver()) {
                    if (game.getTimeLeft() <= 1 || arena.isOver()) {
                        for (String key : plan_infos.keySet()) {
                            if (key.endsWith("_2")) continue;
                            plan_infos.put(key, "");
                        }
                    }
                    this.cancel();
                    return;
                }

                for (String plan : Config.planinfo) {
                    if (gameLeft <= Main.getInstance().getConfig().getInt("planinfo." + plan + ".start_time") && gameLeft > Main.getInstance().getConfig().getInt("planinfo." + plan + ".end_time")) {
                        for (String key : Main.getInstance().getConfig().getConfigurationSection("planinfo." + plan + ".plans").getKeys(false)) {
                            plan_infos.put(key, Main.getInstance().getConfig().getString("planinfo." + plan + ".plans." + key));
                        }
                    }
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 20L));

        // 刷新 自定义倒计时
        for (String id : Config.timer.keySet()) {
            arena.addGameTask(new BukkitRunnable() {
                int i = Config.timer.get(id);

                @Override
                public void run() {
                    if (arena.isOver()) {
                        i = 0;
                        this.cancel();
                    }

                    String format = i / 60 + ":" + ((i % 60 < 10) ? ("0" + i % 60) : (i % 60));
                    scoreBoard.getTimer_placeholder().put("{timer_" + id + "}", format);
                    scoreBoard.getTimer_placeholder().put("{timer_sec_" + id + "}", String.valueOf(i));
                    i--;
                }
            }.runTaskTimer(Main.getInstance(), 0L, 20L));
        }
        // 刷新 TimeCommand
        for (String cmds : Main.getInstance().getConfig().getConfigurationSection("timecommand").getKeys(false)) {
            arena.addGameTask(new BukkitRunnable() {
                final int gametime = Main.getInstance().getConfig().getInt("timecommand." + cmds + ".gametime");
                final List<String> cmdlist = Main.getInstance().getConfig().getStringList("timecommand." + cmds + ".command");

                @Override
                public void run() {
                    if (gameLeft <= gametime) {
                        for (String cmd : cmdlist) {
                            if (cmd.isEmpty()) {
                                continue;
                            }
                            if (cmd.contains("{player}")) {
                                for (Player player : game.getPlayers()) {
                                    Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), ColorUtil.color(cmd.replace("{player}", player.getName())));
                                }
                            } else if (cmd.contains("{gamename}")) {
                                Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), ColorUtil.color(cmd.replace("{gamename}", game.getName())));
                            } else {
                                Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), ColorUtil.color(cmd));
                            }
                        }
                        cancel();
                    }
                }
            }.runTaskTimer(Main.getInstance(), 0L, 20L));
        }
    }
}
