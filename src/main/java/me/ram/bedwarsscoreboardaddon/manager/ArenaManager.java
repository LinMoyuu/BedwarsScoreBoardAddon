package me.ram.bedwarsscoreboardaddon.manager;

import me.ram.bedwarsscoreboardaddon.arena.Arena;

import java.util.HashMap;
import java.util.Map;

public class ArenaManager {

    private final Map<String, Arena> arenas = new HashMap<String, Arena>();

    public void addArena(String game, Arena arena) {
        arenas.put(game, arena);
    }

    public void removeArena(String game) {
        arenas.remove(game);
    }

    public Arena getArena(String game) {
        return arenas.get(game);
    }

    public Map<String, Arena> getArenas() {
        return arenas;
    }
}
