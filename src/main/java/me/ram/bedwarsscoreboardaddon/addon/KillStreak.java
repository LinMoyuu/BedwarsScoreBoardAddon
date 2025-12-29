package me.ram.bedwarsscoreboardaddon.addon;

import io.github.bedwarsrel.game.Game;
import lombok.Getter;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KillStreak {
    @Getter
    private final Game game;
    @Getter
    private final Arena arena;
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