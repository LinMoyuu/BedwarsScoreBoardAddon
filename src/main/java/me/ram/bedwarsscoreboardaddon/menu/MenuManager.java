package me.ram.bedwarsscoreboardaddon.menu;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuManager {

    private final Map<Player, MenuType> players;
    private final Map<Player, Inventory> player_inv;
    private final Map<Player, Map<String, Object>> player_value;

    public MenuManager() {
        players = new HashMap<>();
        player_inv = new HashMap<>();
        player_value = new HashMap<>();
    }

    public void addPlayer(Player player, MenuType type, Inventory inventory) {
        Inventory inv = player.getOpenInventory().getTopInventory();
        if (inv == null || !inv.equals(inventory)) {
            return;
        }
        players.put(player, type);
        player_inv.put(player, inventory);
    }

    public void addPlayer(Player player, MenuType type, Inventory inventory, Map<String, Object> value) {
        Inventory inv = player.getOpenInventory().getTopInventory();
        if (inv == null || !inv.equals(inventory)) {
            return;
        }
        players.put(player, type);
        player_inv.put(player, inventory);
        player_value.put(player, value);
    }

    public void removePlayer(Player player) {
        players.remove(player);
        player_inv.remove(player);
        player_value.remove(player);
    }

    public Inventory getInventory(Player player) {
        if (player_inv.containsKey(player)) {
            return player_inv.get(player);
        }
        return null;
    }

    public Map<String, Object> getValue(Player player) {
        if (player_value.containsKey(player)) {
            return player_value.get(player);
        }
        return null;
    }

    public boolean isOpen(Player player) {
        if (!players.containsKey(player)) {
            return false;
        }
        if (!player_inv.containsKey(player)) {
            return false;
        }
        Inventory inv = player.getOpenInventory().getTopInventory();
        return inv != null && inv.equals(player_inv.get(player));
    }

    public boolean isOpen(Player player, MenuType type) {
        if (!isOpen(player)) {
            return false;
        }
        return players.get(player).equals(type);
    }

    public List<Player> getPlayers() {
        List<Player> list = new ArrayList<>();
        list.addAll(players.keySet());
        return list;
    }
}
