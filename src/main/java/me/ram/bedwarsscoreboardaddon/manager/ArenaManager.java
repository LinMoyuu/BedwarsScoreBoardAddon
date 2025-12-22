package me.ram.bedwarsscoreboardaddon.manager;

import lombok.Getter;
import me.ram.bedwarsscoreboardaddon.arena.Arena;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ArenaManager {

    private final Map<String, Arena> arenas = new HashMap<>();

    public void addArena(String game, Arena arena) {
        arenas.put(game, arena);
    }

    public void removeArena(String game) {
        arenas.remove(game);
    }

    public Arena getArena(String game) {
        return arenas.get(game);
    }

}
