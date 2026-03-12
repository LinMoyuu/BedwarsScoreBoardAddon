package me.ram.bedwarsscoreboardaddon.addon;

import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.GameState;
import io.github.bedwarsrel.game.ResourceSpawner;
import io.github.bedwarsrel.game.Team;
import me.ram.bedwarsscoreboardaddon.config.Config;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

public class SpawnNoBuild implements Listener {

    /**
     * 阻止在保护区域内放置方块
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null || game.getState() != GameState.RUNNING) {
            return;
        }

        Block block = e.getBlockPlaced();
        Location loc = block.getLocation().add(0.5, 0, 0.5);

        if (isInProtectedArea(loc, game)) {
            e.setCancelled(true);
            player.sendMessage(Config.spawn_no_build_message);
        }
    }

    /**
     * 阻止在保护区域内使用水桶倒液体
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(e.getPlayer());
        if (game == null || game.getState() != GameState.RUNNING) {
            return;
        }

        Location targetLoc = e.getBlockClicked().getLocation().add(
                e.getBlockFace().getModX(),
                e.getBlockFace().getModY(),
                e.getBlockFace().getModZ()
        ).add(0.5, 0, 0.5);

        if (isInProtectedArea(targetLoc, game)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onLiquidFlow(BlockFromToEvent event) {
        Block toBlock = event.getToBlock();
        Block fromBlock = event.getBlock();
        Game game = BedwarsRel.getInstance().getGameManager().getGameByLocation(toBlock.getLocation());
        if (game == null || game.getState() != GameState.RUNNING) {
            return;
        }

        if (isInProtectedArea(toBlock.getLocation(), game)) {
            Material material = fromBlock.getType();
            if (material == Material.WATER || material == Material.LAVA || material == Material.STATIONARY_WATER || material == Material.STATIONARY_LAVA) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * 检查位置是否在游戏的保护区域内
     *
     * @param loc  要检查的位置
     * @param game 游戏实例
     * @return 如果在保护区域内则返回 true
     */
    private boolean isInProtectedArea(Location loc, Game game) {
        // 检查出生点保护区域
        if (Config.spawn_no_build_spawn_enabled) {
            for (Team team : game.getTeams().values()) {
                Location spawnLoc = team.getSpawnLocation();
                double dx = loc.getX() - spawnLoc.getX();
                double dy = loc.getY() - spawnLoc.getY();
                double dz = loc.getZ() - spawnLoc.getZ();
                double distSq = dx * dx + dy * dy + dz * dz;

                if (distSq <= Config.spawn_no_build_spawn_range * Config.spawn_no_build_spawn_range) {
                    return true;
                }
            }
        }

        // 检查资源点保护区域
        if (Config.spawn_no_build_resource_enabled) {
            for (ResourceSpawner spawner : game.getResourceSpawners()) {
                Location spawnerLoc = spawner.getLocation();
                double dx = loc.getX() - spawnerLoc.getX();
                double dy = loc.getY() - spawnerLoc.getY();
                double dz = loc.getZ() - spawnerLoc.getZ();
                double distSq = dx * dx + dy * dy + dz * dz;

                if (distSq <= Config.spawn_no_build_resource_range * Config.spawn_no_build_resource_range) {
                    return true;
                }
            }
        }

        return false;
    }
}
