package me.ram.bedwarsscoreboardaddon.addon;

import io.github.bedwarsrel.events.BedwarsPlayerKilledEvent;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.GameState;
import lombok.Getter;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.utils.BedwarsUtil;
import me.ram.bedwarsscoreboardaddon.utils.ColorUtil;
import me.ram.bedwarsscoreboardaddon.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.*;

public class KillStreak implements Listener {
    @Getter
    private final Game game;
    @Getter
    private final Arena arena;
    private final List<Listener> listeners;
    // 连杀记录
    @Getter
    private final Map<UUID, Integer> killStreaks;
    @Getter
    private final Map<UUID, Integer> highestKillStreaks;

    public KillStreak(Arena arena) {
        this.arena = arena;
        this.game = arena.getGame();
        killStreaks = new HashMap<>();
        highestKillStreaks = new HashMap<>();
        listeners = new ArrayList<>();
        listeners.add(this);
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    @EventHandler
    public void onPlayerKilled(BedwarsPlayerKilledEvent e) {
        if (e.getPlayer() == null || e.getKiller() == null) {
            return;
        }
        Player killer = e.getKiller();
        Player player = e.getPlayer();
        Game game = e.getGame();
        if (game.getState() != GameState.RUNNING) {
            return;
        }
        if (game.getPlayerTeam(player) == null || game.getPlayerTeam(killer) == null) {
            return;
        }
        Arena arena = Main.getInstance().getArenaManager().getArena(e.getGame().getName());
        int killStreak = arena.getKillStreak().getKillStreaks(killer.getUniqueId());
        boolean needSendTitle = !BedwarsUtil.isXpMode(game) || BedwarsUtil.isXpMode(game) && BedwarsUtil.getPlayerXP(game, player) == 0;

        // 没人知道为什么花雨庭没有11 杀
        if (killStreak <= 10) {
            if (needSendTitle) {
                Utils.sendMainTitle(killer, 0, 60, 20, "&a&l" + killStreak + " &a&l杀");
            }
            if (!killStreakMessage(killer, killStreak).isEmpty()) {
                for (Player gamePlayers : game.getPlayers()) {
                    gamePlayers.sendMessage(ColorUtil.color(killStreakMessage(killer, killStreak)));
                }
            }
        } else {
            if (needSendTitle) {
                Utils.sendMainTitle(killer, 0, 60, 20, "&a&l" + 10 + " &a&l杀");
            }
            if (!killStreakMessage(killer, 10).isEmpty()) {
                for (Player gamePlayers : game.getPlayers()) {
                    gamePlayers.sendMessage(ColorUtil.color(killStreakMessage(killer, 10)));
                }
            }
        }
        // 神秘花雨庭会给被击杀者发送一个换行
        killer.sendMessage("§k§i§e§n§d");
    }

    public String killStreakMessage(Player killer, int killStreak) {
        String message = "";

        switch (killStreak) {
            case 3:
                return "&a" + killer.getDisplayName() + " &6正在大杀特杀！";
            case 5:
                //    return "&a" + killer.getDisplayName() + " &6杀人如麻！";
                return "&a" + killer.getDisplayName() + " &6无人可挡！";
            case 7:
                return "&a" + killer.getDisplayName() + " &6主宰服务器！";
            case 9:
                return "&a" + killer.getDisplayName() + " &6如同神一般！";
            case 10:
                return "&a" + killer.getDisplayName() + " &6已经超越神了！拜托谁去杀了他吧！";
            default:
                break;
        }
        return message;
    }

    public void onEnd() {
        listeners.forEach(HandlerList::unregisterAll);
    }

    // 获取当前连杀数
    public int getKillStreaks(UUID playerId) {
        return killStreaks.getOrDefault(playerId, 0);
    }

    public void onKill(Player player, Player killer) {
        // 连杀
        addKillStreak(killer.getUniqueId()); // 增加连杀
        // 重置被击杀者 连杀
        if (getKillStreaks(player.getUniqueId()) > 0) {
            resetKillStreak(player.getUniqueId());
        }
    }

    // 增加连杀数
    public void addKillStreak(UUID playerId) {
        int currentStreak = getKillStreaks(playerId) + 1;
        killStreaks.put(playerId, currentStreak);

        // 检查并更新最高连杀记录
        if (currentStreak > getHighestKillStreak(playerId)) {
            setHighestKillStreak(playerId, currentStreak);
        }
    }

    // 重置连杀数
    public void resetKillStreak(UUID playerId) {
        killStreaks.put(playerId, 0);
    }

    // 获取最高连杀记录
    public int getHighestKillStreak(UUID playerId) {
        return highestKillStreaks.getOrDefault(playerId, 0);
    }

    // 设置最高连杀记录
    public void setHighestKillStreak(UUID playerId, int streak) {
        highestKillStreaks.put(playerId, streak);
    }
}