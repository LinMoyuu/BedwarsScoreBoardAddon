package me.ram.bedwarsscoreboardaddon.storage;

import io.github.bedwarsrel.game.Game;
import me.ram.bedwarsscoreboardaddon.arena.Arena;

import java.util.HashMap;
import java.util.Map;

public class PlayerGameStorage {

    private final Arena arena;
    private final Map<String, Integer> totalkills;
    private final Map<String, Integer> kills;
    private final Map<String, Integer> finalkills;
    private final Map<String, Integer> dies;
    private final Map<String, Integer> beds;

    public PlayerGameStorage(Arena arena) {
        this.arena = arena;
        totalkills = new HashMap<String, Integer>();
        kills = new HashMap<String, Integer>();
        finalkills = new HashMap<String, Integer>();
        dies = new HashMap<String, Integer>();
        beds = new HashMap<String, Integer>();
    }

    public Arena getArena() {
        return arena;
    }

    public Game getGame() {
        return arena.getGame();
    }

    public Map<String, Integer> getPlayerTotalKills() {
        return totalkills;
    }

    public Map<String, Integer> getPlayerKills() {
        return kills;
    }

    public Map<String, Integer> getPlayerFinalKills() {
        return finalkills;
    }

    public Map<String, Integer> getPlayerDies() {
        return dies;
    }

    public Map<String, Integer> getPlayerBeds() {
        return beds;
    }
}
