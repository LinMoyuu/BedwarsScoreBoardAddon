package me.ram.bedwarsscoreboardaddon.addon;

import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.GameState;
import io.github.bedwarsrel.game.ResourceSpawner;
import io.github.bedwarsrel.game.Team;
import me.ram.bedwarsscoreboardaddon.config.Config;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class SpawnNoBuild implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlace(BlockPlaceEvent e) {
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(e.getPlayer());
        if (game == null) {
            return;
        }
        if (game.getState() != GameState.RUNNING) {
            return;
        }
        Block block = e.getBlock();
        Player player = e.getPlayer();
        if (e.getBlock() != null && e.getBlock().getType() == Material.TNT) return;
        if (Config.spawn_no_build_spawn_enabled) {
            for (Team team : game.getTeams().values()) {
                if (team.getSpawnLocation().distanceSquared(block.getLocation().clone().add(0.5, 0, 0.5)) <= Math.pow(Config.spawn_no_build_spawn_range, 2)) {
                    e.setCancelled(true);
                    player.sendMessage(Config.spawn_no_build_message);
                    return;
                }
            }
        }
        if (Config.spawn_no_build_resource_enabled) {
            for (ResourceSpawner spawner : game.getResourceSpawners()) {
                if (spawner.getLocation().distanceSquared(block.getLocation().clone().add(0.5, 0, 0.5)) <= Math.pow(Config.spawn_no_build_resource_range, 2)) {
                    e.setCancelled(true);
                    player.sendMessage(Config.spawn_no_build_message);
                    return;
                }
            }
        }
    }
}
