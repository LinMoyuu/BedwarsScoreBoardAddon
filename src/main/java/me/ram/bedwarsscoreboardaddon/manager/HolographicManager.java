package me.ram.bedwarsscoreboardaddon.manager;

import me.ram.bedwarsscoreboardaddon.api.HolographicAPI;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class HolographicManager {

    private final List<HolographicAPI> holographics;

    public HolographicManager() {
        holographics = new ArrayList<>();
    }

    public void addHolographic(HolographicAPI holo) {
        if (!holographics.contains(holo)) {
            holographics.add(holo);
        }
    }

    public void removeHolographic(HolographicAPI holo) {
        holographics.remove(holo);
    }

    public void deleteHolographic(HolographicAPI holo) {
        if (holographics.contains(holo)) {
            holo.remove();
            holographics.remove(holo);
        }
    }

    public List<HolographicAPI> getPlayerHolographic(Player player) {
        List<HolographicAPI> list = new ArrayList<>();
        for (HolographicAPI holo : holographics) {
            if (holo.getPlayers().contains(player)) {
                list.add(holo);
            }
        }
        return list;
    }
}
