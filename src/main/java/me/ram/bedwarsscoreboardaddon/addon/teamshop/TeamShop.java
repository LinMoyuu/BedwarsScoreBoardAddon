package me.ram.bedwarsscoreboardaddon.addon.teamshop;

import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.Team;
import lombok.Getter;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeamShop {

    @Getter
    private final Game game;
    @Getter
    private final Arena arena;
    private final List<Listener> listeners;
    @Getter
    private HashMap<Team, Integer> teamSharpnessLevel;
    @Getter
    private HashMap<Team, Integer> teamLeggingsProtectionLevel;
    @Getter
    private HashMap<Team, Integer> teamBootsProtectionLevel;

    public TeamShop(Arena arena) {
        this.arena = arena;
        this.game = arena.getGame();
        listeners = new ArrayList<>();
        teamSharpnessLevel = new HashMap<>();
        teamLeggingsProtectionLevel = new HashMap<>();
        teamBootsProtectionLevel = new HashMap<>();
    }

    public void onEnd() {
        listeners.forEach(HandlerList::unregisterAll);
    }

    private List<String> replaceLore(List<String> lore, String... args) {
        List<String> list = new ArrayList<>();
        if (lore == null || lore.isEmpty()) {
            return list;
        }
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < (args.length / 2); i++) {
            int j = i * 2;
            map.put(args[j], args[j + 1]);
        }
        lore.forEach(line -> {
            for (String key : map.keySet()) {
                line = line.replace(key, map.get(key));
            }
            list.add(line);
        });
        return list;
    }

    private String getLevel(int i) {
        switch (i) {
            case 1:
                return "I";
            case 2:
                return "II";
            case 3:
                return "III";
            case 4:
                return "IV";
            default:
                return "";
        }
    }

    private String getItemName(List<String> list) {
        if (!list.isEmpty()) {
            return list.get(0);
        }
        return "§f";
    }

    private List<String> getItemLore(List<String> list) {
        List<String> lore = new ArrayList<>();
        if (list.size() > 1) {
            lore.addAll(list);
            lore.remove(0);
        }
        return lore;
    }
}
