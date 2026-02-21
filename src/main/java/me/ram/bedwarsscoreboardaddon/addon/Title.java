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
import me.ram.bedwarsscoreboardaddon.utils.PlaceholderAPIUtil;
import me.ram.bedwarsscoreboardaddon.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
        Team destoryedTeam = e.getTeam();

        // 为床被破坏的队伍发送
        String destroyed_title_title = ColorUtil.color(PlaceholderAPIUtil.setPlaceholders(player, Config.destroyed_title_title
                .replace("{player}", player.getDisplayName())
                .replace("{playerTeam}", playerTeam.getDisplayName())
                .replace("{playerTeamColor}", playerTeam.getChatColor().toString())
                .replace("{destoryedTeam}", destoryedTeam.getName())
                .replace("{destoryedTeamColor}", destoryedTeam.getChatColor().toString())));
        String destroyed_title_subtitle = ColorUtil.color(PlaceholderAPIUtil.setPlaceholders(player, Config.destroyed_title_subtitle
                .replace("{player}", player.getDisplayName())
                .replace("{playerTeam}", playerTeam.getDisplayName())
                .replace("{playerTeamColor}", playerTeam.getChatColor().toString())
                .replace("{destoryedTeam}", destoryedTeam.getName())
                .replace("{destoryedTeamColor}", destoryedTeam.getChatColor().toString())));
        for (Player wasBrokenPlayers : e.getTeam().getPlayers()) {
            Utils.sendTitle(wasBrokenPlayers, 0, 60, 20, destroyed_title_title, destroyed_title_subtitle);
        }

        // 为且非床破坏者其他队伍发送
        for (Player gamePlayers : game.getPlayers()) {
            if (gamePlayers.equals(player)) continue; // 破坏者跳过
            Team gamePlayersTeam = game.getPlayerTeam(gamePlayers);
            if (gamePlayersTeam != null && gamePlayersTeam.equals(e.getTeam())) continue;
            String other_title = ColorUtil.color(PlaceholderAPIUtil.setPlaceholders(player, Config.destroyed_other_title
                    .replace("{player}", player.getDisplayName())
                    .replace("{playerTeam}", playerTeam.getDisplayName())
                    .replace("{playerTeamColor}", playerTeam.getChatColor().toString())
                    .replace("{destoryedTeam}", destoryedTeam.getName())
                    .replace("{destoryedTeamColor}", destoryedTeam.getChatColor().toString())));
            String other_subtitle = ColorUtil.color(PlaceholderAPIUtil.setPlaceholders(player, Config.destroyed_other_subtitle
                    .replace("{player}", player.getDisplayName())
                    .replace("{playerTeam}", playerTeam.getDisplayName())
                    .replace("{playerTeamColor}", playerTeam.getChatColor().toString())
                    .replace("{destoryedTeam}", destoryedTeam.getName())
                    .replace("{destoryedTeamColor}", destoryedTeam.getChatColor().toString())));
            Utils.sendTitle(gamePlayers, 0, 60, 20, other_title, other_subtitle);
        }
        // 为破坏者发送
        String destroyer_title = ColorUtil.color(PlaceholderAPIUtil.setPlaceholders(player, Config.destroyed_destroyer_title
                .replace("{player}", player.getDisplayName())
                .replace("{playerTeam}", playerTeam.getDisplayName())
                .replace("{playerTeamColor}", playerTeam.getChatColor().toString())
                .replace("{destoryedTeam}", destoryedTeam.getName())
                .replace("{destoryedTeamColor}", destoryedTeam.getChatColor().toString())));
        String destroyer_subtitle = ColorUtil.color(PlaceholderAPIUtil.setPlaceholders(player, Config.destroyed_destroyer_subtitle
                .replace("{player}", player.getDisplayName())
                .replace("{playerTeam}", playerTeam.getDisplayName())
                .replace("{playerTeamColor}", playerTeam.getChatColor().toString())
                .replace("{destoryedTeam}", destoryedTeam.getName())
                .replace("{destoryedTeamColor}", destoryedTeam.getChatColor().toString())));
        Utils.sendTitle(player, 0, 60, 20, destroyer_title, destroyer_subtitle);
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
        Player newPlayer = e.getPlayer();
        if (newPlayer.getName().contains(",") || newPlayer.getName().contains("[") || newPlayer.getName().contains("]")) {
            newPlayer.kickPlayer("");
            return;
        }
        if (!(game.getState() == GameState.WAITING && Config.jointitle_enabled)) return;
        int needplayers = game.getMinPlayers() - game.getPlayers().size();
        needplayers = Math.max(needplayers, 0);
        String status = Config.jointitle_status_waiting.replace("{count}", String.valueOf(needplayers));
        if (game.getLobbyCountdown() != null) {
            status = Config.jointitle_status_starting;
        }
        String title = ColorUtil.color(PlaceholderAPIUtil.setPlaceholders(newPlayer, Config.jointitle_title
                .replace("{player}", newPlayer.getDisplayName())
                .replace("{status}", status)));
        String subtitle = ColorUtil.color(PlaceholderAPIUtil.setPlaceholders(newPlayer, Config.jointitle_subtitle
                .replace("{player}", newPlayer.getDisplayName())
                .replace("{status}", status)));
        if (Config.jointitle_broadcast) {
            for (Player player : game.getPlayers()) {
                Utils.sendTitle(newPlayer, player, 5, 60, 5, title, subtitle);
            }
        } else {
            Utils.sendTitle(newPlayer, newPlayer, 5, 60, 5, title, subtitle);
        }
    }

    @EventHandler
    public void onLeave(BedwarsPlayerLeaveEvent e) {
        Game game = e.getGame();
        if (!(game.getState() == GameState.WAITING && Config.moreplayer_title_enabled)) return;
        if (game.getLobbyCountdown() != null) {
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                int needplayers = game.getMinPlayers() - game.getPlayers().size();
                needplayers = Math.max(needplayers, 0);
                if (needplayers == 0) return;
                String title = ColorUtil.color(Config.jointitle_title
                        .replace("{count}", String.valueOf(needplayers)));
                String subtitle = ColorUtil.color(Config.jointitle_subtitle
                        .replace("{count}", String.valueOf(needplayers)));
                for (Player player : game.getPlayers()) {
                    Utils.sendTitle(player, player, 5, 60, 5, title, subtitle);
                }
            }, 1L);
        }
    }

    @EventHandler
    public void onPlayerKillStreak(BedwarsPlayerKilledEvent e) {
        Player player = e.getPlayer();
        Player killer = e.getKiller();
        if (e.getPlayer() == null || e.getKiller() == null) {
            return;
        }
        Game game = e.getGame();
        Arena arena = Main.getInstance().getArenaManager().getArena(game.getName());
        if (arena == null) return;
        if (game.getState() != GameState.RUNNING) {
            return;
        }
        if (game.getPlayerTeam(player) == null || game.getPlayerTeam(killer) == null) {
            return;
        }
        int killStreak = arena.getPlayerGameStorage().getKillStreaks().getOrDefault(killer.getName(), 0);
        // 不启用修复 默认此播报连杀数最高为10
        if (killStreak >= 10 && !Config.killstreak_fix_display_count) killStreak = 10;
        // 是否非经验起床模式 / 是经验模式 但经验为0 避免与killxp冲突
        boolean autoDetectSendTitle = !BedwarsUtil.isXpMode(game) || BedwarsUtil.isXpMode(game) && BedwarsUtil.getPlayerXP(game, player) == 0;
        // 自动判断 并且符合发送标题的条件 / 未启用自动判断 并且启用了始终发送标题
        if (Config.killstreak_title_autodetect && autoDetectSendTitle || !Config.killstreak_title_autodetect && Config.killstreak_title_enabled) {
            Utils.sendMainTitle(killer, 0, 60, 20, ColorUtil.color(PlaceholderAPIUtil.setPlaceholders(player, Config.killstreak_title_text
                    .replace("{count}", killStreak + "")
                    .replace("{kills}", arena.getPlayerGameStorage().getKills(player.getName()) + ""))));
        }
        if (Config.killstreak_message_enabled) {
            Team playerTeam = game.getPlayerTeam(player);
            Team killerTeam = game.getPlayerTeam(killer);
            String message = ColorUtil.color(PlaceholderAPIUtil.setPlaceholders(player, Config.killstreak_message_broadcasts.getOrDefault(killStreak, "")
                    .replace("{count}", killStreak + "")
                    .replace("{kills}", arena.getPlayerGameStorage().getKills(player.getName()) + "")
                    .replace("{killerTeamString}", ChatColor.GOLD + "(" + killerTeam.getDisplayName() + ChatColor.GOLD + ")")
                    .replace("{playerTeamColor}", playerTeam.getChatColor().toString())
                    .replace("{killerTeamColor}", killerTeam.getChatColor().toString())
                    .replace("{playerTeam}", playerTeam.getDisplayName())
                    .replace("{killerTeam}", killerTeam.getDisplayName())
                    .replace("{hearts}", BedwarsUtil.getHealthsString(killer))
                    .replace("{player}", player.getDisplayName())
                    .replace("{killer}", killer.getDisplayName())
            ));
            if (!message.isEmpty()) {
                for (Player gamePlayers : game.getPlayers()) {
                    gamePlayers.sendMessage(message);
                }
            }
            // 神秘花雨庭会给被击杀者发送一个换行
            if (Config.killstreak_message_send_killer_empty_message) killer.sendMessage("§k§i§e§n§d");
        }
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
