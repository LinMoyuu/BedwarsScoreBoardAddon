package me.ram.bedwarsscoreboardaddon.addon;

import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.events.BedwarsGameStartedEvent;
import io.github.bedwarsrel.events.BedwarsOpenShopEvent;
import io.github.bedwarsrel.events.BedwarsPlayerLeaveEvent;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.GameState;
import io.github.bedwarsrel.game.Team;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.utils.BedwarsUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class Spectator implements Listener {

    @EventHandler
    public void onStarted(BedwarsGameStartedEvent e) {
        for (Player player : e.getGame().getPlayers()) {
            player.setFlySpeed(0.1f);
            player.setWalkSpeed(0.2f);
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        player.setFlySpeed(0.1f);
        player.setWalkSpeed(0.2f);
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
    }

    @EventHandler
    public void onLeave(BedwarsPlayerLeaveEvent e) {
        Player player = e.getPlayer();
        player.setFlySpeed(0.1f);
        player.setWalkSpeed(0.2f);
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
    }

    private Location getMapCentre(Game game) {
        World world = game.getRegion().getWorld();
        int i = 0;
        double x = 0;
        double z = 0;
        for (Team team : game.getTeams().values()) {
            if (team.getSpawnLocation().getWorld().getName().equals(world.getName())) {
                x += team.getSpawnLocation().getX();
                z += team.getSpawnLocation().getZ();
                i++;
            }
        }
        return new Location(world, (x / (double) i), Config.respawn_centre_height, (z / (double) i));
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null || !game.getState().equals(GameState.RUNNING) || !BedwarsUtil.isSpectator(game, player)) {
            return;
        }
        Location from = e.getFrom();
        Location to = e.getTo();
        if (!game.getRegion().isInRegion(from) || !game.getRegion().isInRegion(to)) {
            player.setVelocity(new Vector());
            player.teleport(getMapCentre(game));
            return;
        }
        if (to.getY() < 0) {
            Location loc = player.getLocation();
            loc.setY(Config.respawn_centre_height);
            player.teleport(loc);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onOpenShop(BedwarsOpenShopEvent e) {
        if (!(e.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) e.getPlayer();
        if (BedwarsUtil.isSpectator(player)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) {
            return;
        }
        Player player = (Player) e.getDamager();
        if (BedwarsUtil.isSpectator(player)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (BedwarsUtil.isSpectator(player)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent e) {
        if (e.getPlayer() == null) {
            return;
        }
        Player player = e.getPlayer();
        if (BedwarsUtil.isSpectator(player)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (e.getPlayer() == null) {
            return;
        }
        Player player = e.getPlayer();
        if (BedwarsUtil.isSpectator(player)) {
            e.setCancelled(true);
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemPickUp(PlayerPickupItemEvent e) {
        if (e.getPlayer() == null) {
            return;
        }
        Player player = e.getPlayer();
        if (BedwarsUtil.isSpectator(player)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBucketFill(PlayerBucketFillEvent e) {
        if (e.getPlayer() == null) {
            return;
        }
        Player player = e.getPlayer();
        if (BedwarsUtil.isSpectator(player)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        if (e.getPlayer() == null) {
            return;
        }
        Player player = e.getPlayer();
        if (BedwarsUtil.isSpectator(player)) {
            e.setCancelled(true);
        }
    }
}
