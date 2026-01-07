package me.ram.bedwarsscoreboardaddon.addon;

import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.GameState;
import io.github.bedwarsrel.game.Team;
import lombok.Getter;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.events.BoardAddonPlayerRespawnEvent;
import me.ram.bedwarsscoreboardaddon.utils.ColorUtil;
import me.ram.bedwarsscoreboardaddon.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Respawn {

    @Getter
    private final Game game;
    @Getter
    private final Arena arena;
    private final List<Player> players;
    private final Map<Player, Long> protected_time;

    public Respawn(Arena arena) {
        this.arena = arena;
        this.game = arena.getGame();
        players = new ArrayList<>();
        protected_time = new HashMap<>();
    }

    public boolean isRespawning(Player player) {
        return players.contains(player);
    }

    private void addRespawningPlayer(Player player) {
        if (!players.contains(player)) {
            players.add(player);
        }
//        game.getPlayers().forEach(p -> {
//            hidePlayer(p, player);
//        });
        player.setGameMode(GameMode.SPECTATOR);
    }

    private void removeRespawningPlayer(Player player) {
        players.remove(player);
        game.getPlayers().forEach(p -> showPlayer(p, player));
    }

    public void onPlayerLeave(Player player) {
        removeRespawningPlayer(player);
        protected_time.remove(player);
    }

    public void onPlayerJoined(Player player) {
        players.forEach(p -> hidePlayer(player, p));
    }

    public void onRespawn(Player player, boolean rejoin) {
        if (!Config.respawn_enabled || game.isSpectator(player) || (game.getPlayerTeam(player).isDead(game) && !rejoin) || players.contains(player)) {
            return;
        }
        int ateams = 0;
        for (Team team : game.getTeams().values()) {
            if (!(team.isDead(game) && team.getPlayers().isEmpty())) {
                ateams++;
            }
        }
        if (ateams <= 1) {
            return;
        }
        addRespawningPlayer(player);
//        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0), true);
        Location location = game.getPlayerTeam(player).getSpawnLocation();
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            if (!game.getState().equals(GameState.RUNNING)) {
                return;
            }
            if (!player.isOnline() || !players.contains(player)) {
                return;
            }
            player.setVelocity(new Vector(0, 0, 0));
            player.teleport(location);
            player.setAllowFlight(true);
            player.setFlying(true);
            player.setGameMode(GameMode.SPECTATOR);
            arena.addGameTask(new BukkitRunnable() {
                int respawntime = Config.respawn_respawn_delay;

                @Override
                public void run() {
                    if (!player.isOnline()) {
                        cancel();
                        return;
                    }
                    if (!players.contains(player)) {
                        cancel();
                        player.setGameMode(GameMode.SURVIVAL);
                        player.setAllowFlight(false);
                        player.removePotionEffect(PotionEffectType.INVISIBILITY);
                        player.updateInventory();
                        return;
                    }
                    if (game.getPlayerTeam(player) == null) {
                        cancel();
                        player.setGameMode(GameMode.SURVIVAL);
                        player.setAllowFlight(false);
                        player.removePotionEffect(PotionEffectType.INVISIBILITY);
                        player.updateInventory();
                        return;
                    }
                    if (respawntime <= Config.respawn_respawn_delay && respawntime > 0) {
                        if (!Config.respawn_countdown_title.isEmpty() || !Config.respawn_countdown_subtitle.isEmpty()) {
                            Utils.sendTitle(player, 0, 40, 0, Config.respawn_countdown_title.replace("{respawntime}", String.valueOf(respawntime)), Config.respawn_countdown_subtitle.replace("{respawntime}", String.valueOf(respawntime)));
                        }
                        if (!Config.respawn_countdown_message.isEmpty()) {
                            player.sendMessage(Config.respawn_countdown_message.replace("{respawntime}", String.valueOf(respawntime)));
                        }
                    }
                    if (respawntime <= 0) {
                        cancel();
                        removeRespawningPlayer(player);
                        protected_time.put(player, System.currentTimeMillis());
                        player.teleport(game.getPlayerTeam(player).getSpawnLocation());
                        player.setVelocity(new Vector(0, 0, 0));
                        player.setAllowFlight(false);
                        player.setGameMode(GameMode.SURVIVAL);
                        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
                        player.setFoodLevel(20);
                        player.updateInventory();
                        Utils.clearTitle(player);
                        if (!Config.respawn_respawn_title.isEmpty() || !Config.respawn_respawn_subtitle.isEmpty()) {
                            Utils.sendTitle(player, 10, 30, 10, Config.respawn_respawn_title, Config.respawn_respawn_subtitle);
                        }
                        if (!Config.respawn_respawn_message.isEmpty()) {
                            player.sendMessage(Config.respawn_respawn_message);
                        }
                        int respawn_protectedTime = Config.respawn_protected_enabled ? Config.respawn_protected_time : 0;
                        if (respawn_protectedTime > 0) {
                            player.sendMessage(ColorUtil.color(Config.bwrelPrefix + "&a您获得了" + respawn_protectedTime + "秒的无敌时间!"));
                        }
                        Bukkit.getPluginManager().callEvent(new BoardAddonPlayerRespawnEvent(game, player));
                        return;
                    }
                    respawntime--;
                }
            }.runTaskTimer(Main.getInstance(), 0L, 20L));
        }, 1L);
    }

    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) e.getEntity();
        if (!game.isInGame(player)) {
            return;
        }
        if (!protected_time.containsKey(player)) {
            return;
        }
        int respawn_protectedTime = Config.respawn_protected_enabled ? Config.respawn_protected_time * 1000 : 0;
        if ((System.currentTimeMillis() - protected_time.get(player)) < respawn_protectedTime) {
            e.setCancelled(true);
            return;
        }
        protected_time.remove(player);
    }

    public void onPlayerAttack(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player) || !(e.getDamager() instanceof Player)) {
            return;
        }
        Player player = (Player) e.getEntity();
        Player damager = (Player) e.getDamager();
        if (!game.isInGame(player) || !game.isInGame(damager)) {
            return;
        }

        int respawn_protectedTime = Config.respawn_protected_enabled ? Config.respawn_protected_time * 1000 : 0;

        // 检查被攻击玩家是否在保护时间内
        if (protected_time.containsKey(player)) {
            if ((System.currentTimeMillis() - protected_time.get(player)) < respawn_protectedTime) {
                e.setCancelled(true);
                damager.sendMessage(ColorUtil.color(Config.bwrelPrefix + "&a该玩家正处于无敌时间!"));
                return;
            }
            protected_time.remove(player);
        }

        // 检查攻击玩家是否在保护时间内
        if (protected_time.containsKey(damager)) {
            if ((System.currentTimeMillis() - protected_time.get(damager)) < respawn_protectedTime) {
                e.setCancelled(true);
                damager.sendMessage(ColorUtil.color(Config.bwrelPrefix + "&a无敌时间，无法攻击！"));
                return;
            }
            protected_time.remove(damager);
        }
    }


    private void hidePlayer(Player p1, Player p2) {
        if (p1.getUniqueId().equals(p2.getUniqueId())) {
            return;
        }
        if (BedwarsRel.getInstance().getCurrentVersion().startsWith("v1_12")) {
            p1.hidePlayer(Main.getInstance(), p2);
        } else {
            p1.hidePlayer(p2);
        }
    }

    private void showPlayer(Player p1, Player p2) {
        if (p1.getUniqueId().equals(p2.getUniqueId())) {
            return;
        }
        if (BedwarsRel.getInstance().getCurrentVersion().startsWith("v1_12")) {
            p1.showPlayer(Main.getInstance(), p2);
        } else {
            p1.showPlayer(p2);
        }
    }
}
