package me.ram.bedwarsscoreboardaddon.addon;

import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.Team;
import lombok.Getter;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.manager.PlaceholderManager;
import me.ram.bedwarsscoreboardaddon.utils.PlaceholderAPIUtil;
import me.ram.bedwarsscoreboardaddon.utils.Utils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Actionbar {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(Config.date_format);
    @Getter
    private final Game game;
    @Getter
    private final PlaceholderManager placeholderManager;
    @Getter
    private final Arena arena;

    public Actionbar(Arena arena) {
        this.arena = arena;
        this.game = arena.getGame();
        this.placeholderManager = new PlaceholderManager(game);
    }

    public void sendActionbar() {
        String actionbarConfig = arena.getWitherBow().isWitherbowEnabled()
                ? Config.actionbar_witherbow
                : Config.actionbar;

        if (actionbarConfig == null || actionbarConfig.isEmpty()) return;

        int wither = game.getTimeLeft() - Config.witherbow_gametime;
        String bowtime;
        if (arena.getWitherBow().isWitherbowEnabled()) {
            bowtime = Config.witherbow_already_start;
        } else {
            int minutes = wither / 60;
            int seconds = wither % 60;
            bowtime = minutes + ":" + (seconds < 10 ? "0" + seconds : String.valueOf(seconds));
        }

        int alivePlayers = 0;
        for (Player p : game.getPlayers()) {
            if (!game.isSpectator(p)) {
                alivePlayers++;
            }
        }

        Map<String, String> globalPlaceholders = new HashMap<>();
        globalPlaceholders.put("{bowtime}", bowtime);
        globalPlaceholders.put("{time}", String.valueOf(game.getTimeLeft() / 60));
        globalPlaceholders.put("{formattime}", Utils.getFormattedTimeLeft(game.getTimeLeft()));
        globalPlaceholders.put("{game}", game.getName());
        globalPlaceholders.put("{date}", DATE_FORMAT.format(new Date()));
        globalPlaceholders.put("{online}", String.valueOf(game.getPlayers().size()));
        globalPlaceholders.put("{alive_players}", String.valueOf(alivePlayers));

        placeholderManager.getGamePlaceholder().forEach((identifier, placeholder) -> {
            globalPlaceholders.put(identifier, placeholder.onGamePlaceholderRequest(game));
        });

        for (Player player : game.getPlayers()) {
            Team playerTeam = game.getPlayerTeam(player);
            if (playerTeam == null || game.isSpectator(player)) continue;

            if (!player.getLocation().getWorld().equals(playerTeam.getSpawnLocation().getWorld())) {
                continue;
            }

            // 创建动作栏字符串
            String actionbar = applyActionbarPlaceholders(actionbarConfig, player, playerTeam, globalPlaceholders);
            Utils.sendPlayerActionbar(player, actionbar);
        }
    }

    /**
     * 应用所有占位符到动作栏字符串
     */
    private String applyActionbarPlaceholders(String template, Player player, Team playerTeam, Map<String, String> globalPlaceholders) {
        StringBuilder result = new StringBuilder(template);
        for (Map.Entry<String, String> entry : globalPlaceholders.entrySet()) {
            int index;
            while ((index = result.indexOf(entry.getKey())) != -1) {
                result.replace(index, index + entry.getKey().length(), entry.getValue());
            }
        }
        String directionArrow = getDirectionArrow(player.getLocation(), playerTeam.getSpawnLocation());
        applyTeamPlaceholders(result, playerTeam);
        applyPlayerPlaceholders(result, player);
        String processed = result.toString()
                .replace("{team_peoples}", String.valueOf(playerTeam.getPlayers().size()))
                .replace("{color}", String.valueOf(playerTeam.getChatColor()))
                .replace("{team}", playerTeam.getName())
                .replace("{range}", String.valueOf((int) player.getLocation().distance(playerTeam.getSpawnLocation())))
                .replace("{direction}", ("§6" + directionArrow));
        processed = PlaceholderAPIUtil.setPlaceholders(player, processed);

        return processed;
    }

    /**
     * 应用团队占位符
     */
    private void applyTeamPlaceholders(StringBuilder result, Team playerTeam) {
        Map<String, String> replacements = new HashMap<>();

        // 如果团队有占位符
        if (placeholderManager.getTeamPlaceholder(playerTeam.getName()) != null) {
            placeholderManager.getTeamPlaceholder(playerTeam.getName()).forEach((identifier, placeholder) -> {
                replacements.put(identifier, placeholder.onTeamPlaceholderRequest(playerTeam));
            });
        } else {
            placeholderManager.getTeamPlaceholders().values().forEach(teamPlaceholders -> {
                teamPlaceholders.keySet().forEach(placeholder -> {
                    replacements.put(placeholder, "");
                });
            });
        }

        applyReplacements(result, replacements);
    }

    /**
     * 应用玩家占位符
     */
    private void applyPlayerPlaceholders(StringBuilder result, Player player) {
        Map<String, String> replacements = new HashMap<>();

        // 如果玩家有占位符
        if (placeholderManager.getPlayerPlaceholder(player.getName()) != null) {
            placeholderManager.getPlayerPlaceholder(player.getName()).forEach((identifier, placeholder) -> {
                replacements.put(identifier, placeholder.onPlayerPlaceholderRequest(game, player));
            });
        } else {
            placeholderManager.getPlayerPlaceholders().values().forEach(playerPlaceholders -> {
                playerPlaceholders.keySet().forEach(placeholder -> {
                    replacements.put(placeholder, "");
                });
            });
        }

        applyReplacements(result, replacements);
    }

    /**
     * 在 StringBuilder 中应用替换
     */
    private void applyReplacements(StringBuilder result, Map<String, String> replacements) {
        StringBuilder temp = new StringBuilder(result.toString());

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            int index;
            while ((index = temp.indexOf(entry.getKey())) != -1) {
                temp.replace(index, index + entry.getKey().length(), entry.getValue());
            }
        }

        // 清空原结果并设置新值
        result.setLength(0);
        result.append(temp);
    }

    /**
     * 获取玩家到出生点的方向箭头
     *
     * @param playerLocation 玩家位置
     * @param spawnLocation  出生点位置
     * @return 方向箭头
     */
    private String getDirectionArrow(Location playerLocation, Location spawnLocation) {
        Vector direction = spawnLocation.toVector().subtract(playerLocation.toVector());
        float playerYaw = ((playerLocation.getYaw() % 360) + 360) % 360;
        double angle = Math.toDegrees(Math.atan2(direction.getX(), direction.getZ()));
        angle = ((angle % 360) + 360) % 360;
        double relativeAngle = angle - playerYaw;
        if (relativeAngle > 180) {
            relativeAngle -= 360;
        } else if (relativeAngle < -180) {
            relativeAngle += 360;
        }
        if (Math.abs(relativeAngle) <= 45) {
            return "^";  // 前方
        } else if (relativeAngle > 45 && relativeAngle <= 135) {
            return "<";  // 左方
        } else if (relativeAngle < -45 && relativeAngle >= -135) {
            return ">";  // 右方
        } else {
            return "V";  // 后方
        }
    }
}