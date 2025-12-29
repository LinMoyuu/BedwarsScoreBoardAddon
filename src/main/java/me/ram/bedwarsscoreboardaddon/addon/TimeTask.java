package me.ram.bedwarsscoreboardaddon.addon;

import io.github.bedwarsrel.game.Game;
import lombok.Getter;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.utils.ColorUtil;
import me.ram.bedwarsscoreboardaddon.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimeTask {

    @Getter
    private final Game game;
    @Getter
    private final Arena arena;
    private final ScoreBoard scoreBoard;

    // 用于存储与时间相关的命令，避免每次都从配置文件读取
    private final Map<Integer, List<String>> timedCommands;

    public TimeTask(Arena arena) {
        this.arena = arena;
        this.game = arena.getGame();
        this.scoreBoard = arena.getScoreBoard();

        // 预加载时间命令
        this.timedCommands = preloadTimedCommands();
        // 执行开局指令
        dispatchCommands(Config.timecommand_startcommand);
        refresh();
    }

    public void refresh() {
        checkPlans();
        checkTimeCommands();
        checkWitherBow();
        arena.getActionbar().sendActionbar();
        arena.getHealthLevel().checkHealth();
        arena.getNoBreakBed().checkBedBreakState();
        arena.getResourceUpgrade().checkResourceUpgrade();
        arena.getDeathMode().checkDeathMode();
    }

    /**
     * 预加载所有与时间相关的命令到 Map 中，以游戏剩余时间为键。
     *
     * @return 一个包含时间点和对应命令列表的 Map。
     */
    public Map<Integer, List<String>> preloadTimedCommands() {
        ConfigurationSection timeCommandSection = Main.getInstance().getConfig().getConfigurationSection("timecommand");
        if (timeCommandSection == null) {
            return java.util.Collections.emptyMap();
        }

        if (timedCommands != null) {
            timedCommands.clear();
        }

        Map<Integer, List<String>> result = new HashMap<>();

        for (String key : timeCommandSection.getKeys(false)) {
            int gameTime = timeCommandSection.getInt(key + ".gametime");
            List<String> commands = timeCommandSection.getStringList(key + ".command");

            // 如果已存在相同时间的条目，合并命令列表
            if (result.containsKey(gameTime)) {
                List<String> existingCommands = result.get(gameTime);
                List<String> mergedCommands = new ArrayList<>(existingCommands);
                mergedCommands.addAll(commands);
                result.put(gameTime, mergedCommands);
            } else {
                result.put(gameTime, new ArrayList<>(commands));
            }
        }

        return result;
    }

    /**
     * 检查并执行当前时间点的命令。
     */
    public void checkTimeCommands() {
        if (timedCommands.containsKey(game.getTimeLeft())) {
            dispatchCommands(timedCommands.get(game.getTimeLeft()));
        }
    }

    /**
     * 刷新计分板上的自定义任务。
     */
    public void checkPlans() {
        if (arena.isOver()) {
            // 游戏结束，将所有计时器清零
            scoreBoard.getPlan_infos().forEach((key, value) -> {
                if (key.endsWith("_1")) {
                    scoreBoard.getPlan_infos().put(key, "");
                }
            });
            for (String key : new ArrayList<>(scoreBoard.getTimer_placeholder().keySet())) {
                if (key.contains("_sec_")) {
                    scoreBoard.getTimer_placeholder().put(key, "0");
                } else {
                    scoreBoard.getTimer_placeholder().put(key, "0:00");
                }
            }
            return;
        }

        ConfigurationSection planInfoSection = Main.getInstance().getConfig().getConfigurationSection("planinfo");
        if (planInfoSection == null) return;

        int currentTimeLeft = game.getTimeLeft();
        for (String planName : planInfoSection.getKeys(false)) {
            ConfigurationSection planSection = planInfoSection.getConfigurationSection(planName);
            if (planSection == null) continue;

            int startTime = planSection.getInt("start_time");
            int endTime = planSection.getInt("end_time");

            int remainingSeconds = Math.max(0, currentTimeLeft - endTime);

            ConfigurationSection plans = planSection.getConfigurationSection("plans");
            if (plans != null) {
                String format = String.format("%d:%02d", remainingSeconds / 60, remainingSeconds % 60);

                scoreBoard.getTimer_placeholder().put("{plan_timer_" + planName + "}", format);
                scoreBoard.getTimer_placeholder().put("{plan_timer_sec_" + planName + "}", String.valueOf(remainingSeconds));

                if (currentTimeLeft <= startTime && currentTimeLeft > endTime) {
                    for (String key : plans.getKeys(false)) {
                        scoreBoard.getPlan_infos().put(key, plans.getString(key));
                    }
                }
            }
        }
    }

    public void checkWitherBow() {
        if (!Config.witherbow_enabled || arena.isEnabledWitherBow()) return;
        int enableAfterSec = (game.getTimeLeft() - Config.witherbow_gametime);

        if (Config.witherbow_remind_times != null && !Config.witherbow_remind_times.isEmpty()) {
            if (Config.witherbow_remind_times.contains(enableAfterSec)) {
                for (Player player : game.getPlayers()) {
                    player.sendMessage(WitherBow.formatMessage(enableAfterSec));
                }
            }
        }

        if (game.getTimeLeft() <= Config.witherbow_gametime) {
            if (!Config.witherbow_title.isEmpty() || !Config.witherbow_subtitle.isEmpty()) {
                game.getPlayers().forEach(player -> Utils.sendTitle(player, 10, 50, 10, Config.witherbow_title, Config.witherbow_subtitle));
            }
            if (!Config.witherbow_message.isEmpty()) {
                game.getPlayers().forEach(player -> player.sendMessage(Config.witherbow_message.replace("{bwprefix}", Config.bwrelPrefix)));
            }
            PlaySound.playSound(game, Config.play_sound_sound_enable_witherbow);
            arena.setEnabledWitherBow(true);
        }
    }

    /**
     * 一个通用的方法，用于分发指令。
     *
     * @param commands 要执行的指令列表。
     */
    private void dispatchCommands(List<String> commands) {
        for (String cmd : commands) {
            if (cmd == null || cmd.isEmpty()) {
                continue;
            }
            String coloredCmd = ColorUtil.color(cmd);
            if (cmd.contains("{player}")) {
                for (Player player : game.getPlayers()) {
                    dispatch(coloredCmd.replace("{player}", player.getName()));
                }
            } else if (cmd.contains("{gamename}")) {
                dispatch(coloredCmd.replace("{gamename}", game.getName()));
            } else {
                dispatch(coloredCmd);
            }
        }
    }

    /**
     * 封装 Bukkit 的 dispatchCommand。
     *
     * @param command
     */
    private void dispatch(String command) {
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command);
    }
}
