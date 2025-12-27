package me.ram.bedwarsscoreboardaddon.addon;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers.EntityUseAction;
import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.events.BedwarsGameStartedEvent;
import io.github.bedwarsrel.events.BedwarsOpenShopEvent;
import io.github.bedwarsrel.events.BedwarsPlayerLeaveEvent;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.GameState;
import io.github.bedwarsrel.game.Team;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.utils.BedwarsUtil;
import me.ram.bedwarsscoreboardaddon.utils.LocationUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class Spectator implements Listener {

    private final List<Player> players;
    private final List<Material> resitems;

    public Spectator() {
        players = new ArrayList<>();
        resitems = new ArrayList<>();
        onPacketReceiving();
    }

    private void onPacketReceiving() {
        ProtocolManager pm = ProtocolLibrary.getProtocolManager();
        pm.addPacketListener(new PacketAdapter(Main.getInstance(), ListenerPriority.HIGHEST, PacketType.Play.Client.USE_ENTITY, PacketType.Play.Client.BLOCK_PLACE, PacketType.Play.Client.BLOCK_DIG, PacketType.Play.Client.USE_ITEM) {
            public void onPacketReceiving(PacketEvent e) {
                Player player = e.getPlayer();
                PacketContainer packet = e.getPacket();
                Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
                if (game == null || !game.getState().equals(GameState.RUNNING)) {
                    return;
                }
                if (e.getPacketType().equals(PacketType.Play.Client.USE_ENTITY)) {
                    if (packet.getEntityUseActions().read(0).equals(EntityUseAction.ATTACK) && BedwarsUtil.isSpectator(game, player)) {
                        e.setCancelled(true);
                    }
                    if (packet.getEntityUseActions().read(0).equals(EntityUseAction.INTERACT_AT) || BedwarsUtil.isSpectator(game, player)) {
                        return;
                    }
                    int id = packet.getIntegers().read(0);
                    Player target = null;
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getEntityId() == id) {
                            target = p;
                            break;
                        }
                    }
                    if (target == null) {
                        return;
                    }
                    if (!BedwarsUtil.isSpectator(game, target) && !BedwarsUtil.isRespawning(game, target)) {
                        return;
                    }
                    if (!game.isInGame(target) || !BedwarsUtil.isSpectator(game, target)) {
                        return;
                    }
                    target.teleport(target.getLocation().add(0, 5, 0));
                } else if (e.getPacketType().equals(PacketType.Play.Client.BLOCK_PLACE) || e.getPacketType().equals(PacketType.Play.Client.USE_ITEM)) {
                    if (BedwarsUtil.isRespawning(game, player)) {
                        e.setCancelled(true);
                    }
                } else if (e.getPacketType().equals(PacketType.Play.Client.BLOCK_DIG)) {
                    if (!BedwarsUtil.isRespawning(game, player)) {
                        return;
                    }
                    BlockPosition position = packet.getBlockPositionModifier().read(0);
                    Block block = new Location(player.getWorld(), position.getX(), position.getY(), position.getZ()).getBlock();
                    e.setCancelled(true);
                    block.getState().update();
                }
            }
        });
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
        return new Location(world, (x / Double.valueOf(i)), Config.respawn_centre_height, (z / Double.valueOf(i)));
    }

    private boolean isInRegion(Game game, Location location) {
        Location loc1 = game.getLoc1();
        Location loc2 = game.getLoc2();
        double x = location.getX();
        double z = location.getZ();
        return x <= Math.max(loc1.getBlockX(), loc2.getBlockX()) && x >= Math.min(loc1.getBlockX(), loc2.getBlockX()) && z <= Math.max(loc1.getBlockZ(), loc2.getBlockZ()) && z >= Math.min(loc1.getBlockZ(), loc2.getBlockZ());
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
        if (!isInRegion(game, from)) {
            player.setVelocity(new Vector());
            player.teleport(getMapCentre(game));
            return;
        }
        if (!isInRegion(game, to)) {
            Location loc = from.clone();
            loc.setYaw(to.getYaw());
            loc.setPitch(to.getPitch());
            e.setTo(loc);
        }
        if (to.getY() < 0) {
            Location loc = player.getLocation();
            loc.setY(Config.respawn_centre_height);
            player.teleport(loc);
        }
    }

    private List<Material> getResource() {
        List<Material> items = new ArrayList<>();
        ConfigurationSection config = BedwarsRel.getInstance().getConfig().getConfigurationSection("resource");
        for (String res : config.getKeys(false)) {
            List<Map<String, Object>> list = (List<Map<String, Object>>) BedwarsRel.getInstance().getConfig().getList("resource." + res + ".item");
            for (Map<String, Object> resource : list) {
                ItemStack itemStack = ItemStack.deserialize(resource);
                items.add(itemStack.getType());
            }
        }
        return items;
    }

    @EventHandler
    public void onStarted(BedwarsGameStartedEvent e) {
        for (Player player : e.getGame().getPlayers()) {
            player.setFlySpeed(0.1f);
            player.setWalkSpeed(0.2f);
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
        resitems.clear();
        resitems.addAll(getResource());
        Timer logTimer = new Timer();
        TimerTask task = new TimerTask() {
            final Game game = e.getGame();
            @Override
            public void run() {
                if (game.getState() == GameState.RUNNING) {
                    for (Player player : game.getPlayers()) {
                        if (!BedwarsUtil.isSpectator(game, player) && !BedwarsUtil.isRespawning(player)) {
                            continue;
                        }
                        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 2, 3.5, 2)) {
                            if (entity instanceof Projectile && player.getGameMode() != GameMode.SPECTATOR) {
                                player.teleport(LocationUtil.getPosition(player.getLocation(), entity.getLocation()));
                                player.setVelocity(LocationUtil.getPositionVector(player.getLocation(), entity.getLocation()).multiply(0.07));
                                break;
                            }
                        }
                    }
                } else {
                    this.cancel();
                }
            }
        };
        logTimer.scheduleAtFixedRate(task, 500, 10);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteractRespawn(PlayerInteractEvent e) {
        if (BedwarsUtil.isRespawning(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteractSpectator(PlayerInteractEvent e) {
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK) || e.isCancelled()) {
            return;
        }
        Player player = e.getPlayer();
        if (!BedwarsUtil.isRespawning(player)) {
            return;
        }
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null) {
            return;
        }
        if (!game.isInGame(player)) {
            return;
        }
        if (game.getState() != GameState.RUNNING) {
            return;
        }
        if (BedwarsUtil.isSpectator(game, player)) {
            return;
        }
        ItemStack itemStack = e.getItem();
        if (itemStack == null) {
            return;
        }
        if (!itemStack.getType().isBlock()) {
            return;
        }
        Location location = e.getClickedBlock().getRelative(e.getBlockFace()).getLocation().add(0.5, 0.5, 0.5);
        for (Entity entity : location.getWorld().getNearbyEntities(location, 0.51, 1.5, 0.51)) {
            if (entity instanceof Player) {
                Player p = (Player) entity;
                if (game.isInGame(p) && BedwarsUtil.isSpectator(game, p)) {
                    p.teleport(p.getLocation().clone().add(0, 2, 0));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onOpenShop(BedwarsOpenShopEvent e) {
        if (!(e.getPlayer() instanceof Player)) {
            return;
        }
        if (BedwarsUtil.isSpectator((Player) e.getPlayer())) {
            e.setCancelled(true);
        }
    }
}
