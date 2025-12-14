package me.ram.bedwarsscoreboardaddon.addon;

import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.events.*;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.GameState;
import io.github.bedwarsrel.game.Team;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.utils.BedwarsUtil;
import me.ram.bedwarsscoreboardaddon.utils.ColorUtil;
import me.ram.bedwarsscoreboardaddon.utils.Utils;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class Title implements Listener {

    private final Map<String, Integer> Times = new HashMap<>();

    @EventHandler
    public void onStarted(BedwarsGameStartedEvent e) {
        Game game = e.getGame();
        Arena arena = Main.getInstance().getArenaManager().getArena(game.getName());
        Times.put(e.getGame().getName(), e.getGame().getTimeLeft());
        if (Config.start_title_enabled) {
            for (Player player : e.getGame().getPlayers()) {
                Utils.clearTitle(player);
            }
            int delay = game.getRegion().getWorld().getName().equals(game.getLobby().getWorld().getName()) ? 5 : 30;
            arena.addGameTask(new BukkitRunnable() {
                int rn = 0;

                @Override
                public void run() {
                    if (rn < Config.start_title_title.size()) {
                        for (Player player : e.getGame().getPlayers()) {
                            Utils.sendTitle(player, 0, 80, 5, Config.start_title_title.get(rn), Config.start_title_subtitle);
                        }
                        rn++;
                    } else {
                        this.cancel();
                    }
                }
            }.runTaskTimer(Main.getInstance(), delay, 0L));
        }
        if (game.getLobby().getWorld().equals(game.getRegion().getWorld())) {
            PlaySound.playSound(e.getGame(), Config.play_sound_sound_start);
        } else {
            arena.addGameTask(new BukkitRunnable() {
                @Override
                public void run() {
                    PlaySound.playSound(e.getGame(), Config.play_sound_sound_start);
                }
            }.runTaskLater(Main.getInstance(), 30L));
        }
    }

    @EventHandler
    public void onDestroyed(BedwarsTargetBlockDestroyedEvent e) {
        if (!Config.destroyed_title_enabled) return;
        Game game = e.getGame();
        Player player = e.getPlayer();
        Team playerTeam = game.getPlayerTeam(player);
        // 为床被破坏的队伍发送
        for (Player wasBrokenPlayers : e.getTeam().getPlayers()) {
            Utils.sendTitle(wasBrokenPlayers, 1, 30, 1, Config.destroyed_title_title, Config.destroyed_title_subtitle.replace("{player}", playerTeam.getChatColor() + player.getDisplayName()));
        }
        // 为且非床破坏者其他队伍发送
        for (Player gamePlayers : e.getGame().getPlayers()) {
            if (gamePlayers.equals(player)) continue; // 破坏者跳过
            if (game.getPlayerTeam(gamePlayers).equals(e.getTeam())) continue; // 是被破坏的跳过
            Utils.sendTitle(player, 1, 60, 1, e.getTeam().getDisplayName() + "&c床已被摧毁", "&7破坏者： " +
                    playerTeam.getChatColor() + playerTeam.getDisplayName() + player.getDisplayName());
        }
        // 为破坏者发送
        Utils.sendTitle(player, 1, 60, 1, e.getTeam().getDisplayName(), "&a床已被你摧毁");
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null) {
            return;
        }
        if (Config.die_out_title_enabled) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (game.getState() == GameState.RUNNING && game.isSpectator(player)) {
                        Utils.sendTitle(player, 0, 80, 10, Config.die_out_title_title, Config.die_out_title_subtitle);
                    }
                }
            }.runTaskLater(Main.getInstance(), 5L);
        }
    }

    @EventHandler
    public void onOver(BedwarsGameOverEvent e) {
        if (Config.victory_title_enabled) {
            Game game = e.getGame();
            Arena arena = Main.getInstance().getArenaManager().getArena(game.getName());
            Team team = e.getWinner();
            int time = Times.getOrDefault(e.getGame().getName(), 3600) - e.getGame().getTimeLeft();
            String formattime = time / 60 + ":" + ((time % 60 < 10) ? ("0" + time % 60) : (time % 60));
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (team != null && team.getPlayers() != null) {
                        for (Player player : team.getPlayers()) {
                            if (player.isOnline()) {
                                Utils.clearTitle(player);
                            }
                        }
                    }
                }
            }.runTaskLater(Main.getInstance(), 1L);
            arena.addGameTask(new BukkitRunnable() {
                int rn = 0;

                @Override
                public void run() {
                    if (rn < Config.victory_title_title.size()) {
                        if (team != null && team.getPlayers() != null) {
                            for (Player player : team.getPlayers()) {
                                if (player.isOnline()) {
                                    Utils.sendTitle(player, 0, 80, 5, Config.victory_title_title.get(rn).replace("{time}", formattime).replace("{color}", team.getChatColor() + "").replace("{team}", team.getName()), Config.victory_title_subtitle.replace("{time}", formattime).replace("{color}", team.getChatColor() + "").replace("{team}", team.getName()));
                                }
                            }
                            rn++;
                        } else {
                            this.cancel();
                        }
                    } else {
                        this.cancel();
                    }
                }
            }.runTaskTimer(Main.getInstance(), 40L, 0L));
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                PlaySound.playSound(e.getGame(), Config.play_sound_sound_over);
            }
        }.runTaskLater(Main.getInstance(), 40L);
    }

    @EventHandler
    public void onJoined(BedwarsPlayerJoinedEvent e) {
        Game game = e.getGame();
        for (Player player : game.getPlayers()) {
            if (player.getName().contains(",") || player.getName().contains("[") || player.getName().contains("]")) {
                player.kickPlayer("");
            }
            if (!(e.getGame().getState() != GameState.WAITING && e.getGame().getState() == GameState.RUNNING)) {
                if (Config.jointitle_enabled) {
                    int needplayers = game.getMinPlayers() - game.getPlayers().size();
                    needplayers = Math.max(needplayers, 0);
                    String status = "&f还需 " + needplayers + " 个玩家";
                    if (game.getLobbyCountdown() != null) {
                        status = "游戏马上开始";
                    }
                    String title = Config.jointitle_title.replace("{player}", player.getDisplayName()).replace("{status}", status);
                    String subtitle = Config.jointitle_subtitle.replace("{player}", player.getDisplayName()).replace("{status}", status);
                    Utils.sendTitle(player, e.getPlayer(), 5, 50, 5, title, subtitle);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerKilled(BedwarsPlayerKilledEvent e) {
        if (e.getKiller() == null || e.getPlayer() == null) {
            return;
        }
        Player killer = e.getKiller();
        Player player = e.getPlayer();
        Game game = e.getGame();
        if (game.getState() != GameState.RUNNING) {
            return;
        }
        if (game.getPlayerTeam(player) == null || game.getPlayerTeam(killer) == null) {
            return;
        }
        Arena arena = Main.getInstance().getArenaManager().getArena(e.getGame().getName());
        int killStreak = arena.getKillStreak(killer.getUniqueId());
        // 没人知道为什么花雨庭没有11 杀
        if (killStreak <= 10) {
            Utils.sendTitle(killer, 0, 60, 20, "&a&l" + killStreak + " &a&l杀", "");
            if (!killStreakMessage(killer, killStreak).isEmpty()) {
                for (Player gamePlayers : game.getPlayers()) {
                    gamePlayers.sendMessage(ColorUtil.color(killStreakMessage(killer, killStreak)));
                }
            }
        } else {
            Utils.sendTitle(killer, 0, 60, 20, "&a&l" + 10 + " &a&l杀", "");
            if (!killStreakMessage(killer, 10).isEmpty()) {
                for (Player gamePlayers : game.getPlayers()) {
                    gamePlayers.sendMessage(ColorUtil.color(killStreakMessage(killer, 10)));
                }
            }
        }
    }

    public String killStreakMessage(Player killer, int killStreak) {
        String message = "";

        switch (killStreak) {
            case 3:
                return "&a" + killer.getDisplayName() + " &6正在大杀特杀！";
            case 5:
                //    return "&a" + killer.getDisplayName() + " &6杀人如麻！";
                return "&a" + killer.getDisplayName() + " &6无人可挡！";
            case 7:
                return "&a" + killer.getDisplayName() + " &6主宰服务器！";
            case 9:
                return "&a" + killer.getDisplayName() + " &6如同神一般！";
            case 10:
                return "&a" + killer.getDisplayName() + " &6已经超越神了！拜托谁去杀了他吧！";
            case 1:
            case 2:
            case 4:
            case 6:
            case 8:
            default:
                break;
        }
        return message;
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamageTitle(EntityDamageByEntityEvent e) {
        if (!Config.damagetitle_enabled || e.isCancelled() || !(e.getDamager() instanceof Player) || !(e.getEntity() instanceof Player)) {
            return;
        }
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer((Player) e.getDamager());
        if (game == null || game.getState() != GameState.RUNNING) {
            return;
        }
        if (!(game.getPlayers().contains((Player) e.getDamager()) && game.getPlayers().contains((Player) e.getEntity()))) {
            return;
        }
        Player player = (Player) e.getEntity();
        Player damager = (Player) e.getDamager();
        if (BedwarsUtil.isSpectator(game, damager) || BedwarsUtil.isSpectator(game, player)) {
            return;
        }
        if (!Config.damagetitle_title.isEmpty() || !Config.damagetitle_subtitle.isEmpty()) {
            DecimalFormat df = new DecimalFormat("0.00");
            DecimalFormat df2 = new DecimalFormat("#");
            double health = player.getHealth() - e.getFinalDamage();
            health = health < 0 ? 0 : health;
            Utils.sendTitle((Player) e.getDamager(), player, 0, 20, 0, Config.damagetitle_title.replace("{player}", player.getName()).replace("{damage}", df.format(e.getDamage())).replace("{health}", df2.format(health)).replace("{maxhealth}", df2.format(player.getMaxHealth())), Config.damagetitle_subtitle.replace("{player}", player.getName()).replace("{damage}", df.format(e.getDamage())).replace("{health}", df2.format(health)).replace("{maxhealth}", df2.format(player.getMaxHealth())));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBowDamage(EntityDamageByEntityEvent e) {
        if (!Config.bowdamage_enabled || e.isCancelled()) {
            return;
        }
        if (!(e.getDamager() instanceof Arrow) || !(e.getEntity() instanceof Player)) {
            return;
        }
        Arrow arrow = (Arrow) e.getDamager();
        if (!(arrow.getShooter() instanceof Player)) {
            return;
        }
        Player shooter = (Player) arrow.getShooter();
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(shooter);
        if (game == null) {
            return;
        }
        if (game.getState() != GameState.RUNNING) {
            return;
        }
        Player player = (Player) e.getEntity();
        Integer damage = (int) e.getFinalDamage();
        if (game.getPlayerTeam(shooter) == game.getPlayerTeam(player)) {
            e.setCancelled(true);
        }
        if (player.isDead()) {
            return;
        }
        double health = player.getHealth() - e.getFinalDamage();
        health = health < 0 ? 0 : health;
        DecimalFormat df = new DecimalFormat("#");
        if (!Config.bowdamage_title.isEmpty() || !Config.bowdamage_subtitle.isEmpty()) {
            Utils.sendTitle(shooter, player, 0, 20, 0, Config.bowdamage_title.replace("{player}", player.getName()).replace("{damage}", damage + "").replace("{health}", df.format(health)).replace("{maxhealth}", df.format(player.getMaxHealth())), Config.bowdamage_subtitle.replace("{player}", player.getName()).replace("{damage}", damage + "").replace("{health}", df.format(health)).replace("{maxhealth}", df.format(player.getMaxHealth())));
        }
        if (!Config.bowdamage_message.isEmpty()) {
            Utils.sendMessage(shooter, player, Config.bowdamage_message.replace("{player}", player.getName()).replace("{damage}", damage + "").replace("{health}", df.format(health)).replace("{maxhealth}", df.format(player.getMaxHealth())));
        }
    }
}
