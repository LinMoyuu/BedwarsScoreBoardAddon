package me.ram.bedwarsscoreboardaddon.storage;

import io.github.bedwarsrel.game.Game;
import lombok.Getter;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class PlayerGameStorage {

    private final Arena arena;
    @Getter
    private final Map<String, Integer> totalkills;
    @Getter
    private final Map<String, Integer> kills;
    @Getter
    private final Map<String, Integer> finalkills;
    @Getter
    private final Map<String, Integer> dies;
    @Getter
    private final Map<String, Integer> beds;
    @Getter
    private final Map<String, Integer> killStreaks;
    @Getter
    private final Map<String, Integer> highestKillStreaks;

    public PlayerGameStorage(Arena arena) {
        this.arena = arena;
        totalkills = new HashMap<>();
        kills = new HashMap<>();
        finalkills = new HashMap<>();
        dies = new HashMap<>();
        beds = new HashMap<>();
        killStreaks = new HashMap<>();
        highestKillStreaks = new HashMap<>();
    }

    public Game getGame() {
        return arena.getGame();
    }

    // 获取当前连杀数
    public int getKillStreaks(String playerName) {
        return killStreaks.getOrDefault(playerName, 0);
    }

    public void onKill(Player player, Player killer, boolean isFinal) {
        addTotalKills(killer.getName(), 1);

        if (isFinal) {
            // 最终击杀
            addFinalKills(killer.getName(), 1);
        } else {
            // 普通击杀
            addKills(killer.getName(), 1);
        }

        // 连杀
        addKillStreak(killer.getName()); // 增加连杀
        // 重置被击杀者 连杀
        if (getKillStreaks(player.getName()) > 0) {
            resetKillStreak(player.getName());
        }
    }

    public void onDie(Player player) {
        addDies(player.getName(), 1);
        setHighestKillStreak(player.getName(), 0);
    }

    // 增加连杀数
    public void addKillStreak(String playerName) {
        int currentStreak = getKillStreaks(playerName) + 1;
        killStreaks.put(playerName, currentStreak);

        // 检查并更新最高连杀记录
        if (currentStreak > getHighestKillStreak(playerName)) {
            setHighestKillStreak(playerName, currentStreak);
        }
    }

    // 重置连杀数
    public void resetKillStreak(String playerName) {
        killStreaks.put(playerName, 0);
    }

    // 获取最高连杀记录
    public int getHighestKillStreak(String playerName) {
        return highestKillStreaks.getOrDefault(playerName, 0);
    }

    // 设置最高连杀记录
    public void setHighestKillStreak(String playerName, int streak) {
        highestKillStreaks.put(playerName, streak);
    }

    // 总击杀相关方法
    public void addTotalKills(String playerName, int amount) {
        int current = getTotalKills(playerName);
        totalkills.put(playerName, current + amount);
    }

    public int getTotalKills(String playerName) {
        return totalkills.getOrDefault(playerName, 0);
    }

    public void setTotalKills(String playerName, int value) {
        totalkills.put(playerName, value);
    }

    // 普通击杀相关方法
    public void addKills(String playerName, int amount) {
        int current = getKills(playerName);
        kills.put(playerName, current + amount);
    }

    public int getKills(String playerName) {
        return kills.getOrDefault(playerName, 0);
    }

    public void setKills(String playerName, int value) {
        kills.put(playerName, value);
    }

    // 最终击杀相关方法
    public void addFinalKills(String playerName, int amount) {
        int current = getFinalKills(playerName);
        finalkills.put(playerName, current + amount);
    }

    public int getFinalKills(String playerName) {
        return finalkills.getOrDefault(playerName, 0);
    }

    public void setFinalKills(String playerName, int value) {
        finalkills.put(playerName, value);
    }

    // 死亡相关方法
    public void addDies(String playerName, int amount) {
        int current = getDies(playerName);
        dies.put(playerName, current + amount);
    }

    public int getDies(String playerName) {
        return dies.getOrDefault(playerName, 0);
    }

    public void setDies(String playerName, int value) {
        dies.put(playerName, value);
    }

    // 床破坏相关方法
    public void addBeds(String playerName, int amount) {
        int current = getBeds(playerName);
        beds.put(playerName, current + amount);
    }

    public int getBeds(String playerName) {
        return beds.getOrDefault(playerName, 0);
    }

    public void setBeds(String playerName, int value) {
        beds.put(playerName, value);
    }
}
