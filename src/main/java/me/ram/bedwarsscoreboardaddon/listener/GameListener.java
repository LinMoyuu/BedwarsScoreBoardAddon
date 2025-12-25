package me.ram.bedwarsscoreboardaddon.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.events.*;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.GameState;
import io.github.bedwarsrel.game.Team;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.addon.SelectTeam;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.events.BedwarsTeamDeadEvent;
import me.ram.bedwarsscoreboardaddon.utils.BedwarsUtil;
import me.ram.bedwarsscoreboardaddon.utils.ColorUtil;
import me.ram.bedwarsscoreboardaddon.utils.ScoreboardUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Map;

public class GameListener implements Listener {

    @EventHandler
    public void onStart(BedwarsGameStartEvent e) {
        Game game = e.getGame();
        if (!BedwarsUtil.isXpMode(game)) return;
        for (Player player : game.getPlayers()) {
            player.sendMessage(ColorUtil.color(Config.bwrelPrefix + "&a正在加载配置，请耐心等待!"));
            player.sendMessage(ColorUtil.color(Config.bwrelPrefix + "&a加载完成，玩的愉快！"));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onStarted(BedwarsGameStartedEvent e) {
        Game game = e.getGame();
        Map<Player, Scoreboard> scoreboards = ScoreboardUtil.getScoreboards();
        for (Player player : game.getPlayers()) {
            if (scoreboards.containsKey(player)) {
                ScoreboardUtil.removePlayer(player);
            }
        }
        new Arena(game);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (Main.getInstance().getArenaManager().getArenas().containsKey(game.getName())) {
                    Main.getInstance().getArenaManager().getArenas().get(game.getName()).getScoreBoard().updateScoreboard();
                }
            }
        }.runTaskLater(Main.getInstance(), 2L);
    }

    @EventHandler
    public void onOver(BedwarsGameOverEvent e) {
        Game game = e.getGame();
        if (Main.getInstance().getArenaManager().getArenas().containsKey(game.getName())) {
            Main.getInstance().getArenaManager().getArenas().get(game.getName()).onOver(e);
        }
    }

    @EventHandler
    public void onPlayerJoined(BedwarsPlayerJoinedEvent e) {
        Player player = e.getPlayer();
        Game game = e.getGame();
        if (Main.getInstance().getArenaManager().getArenas().containsKey(game.getName())) {
            Main.getInstance().getArenaManager().getArenas().get(game.getName()).onPlayerJoined(player);
        }
        if (game.getState() == GameState.WAITING) {
            for (Arena arena : Main.getInstance().getArenaManager().getArenas().values()) {
                arena.getRejoin().removePlayer(player.getName());
            }
        }
        Main.getInstance().getEditHolographicManager().remove(player);
    }

    @EventHandler
    public void onPlayerLeave(BedwarsPlayerLeaveEvent e) {
        Game game = e.getGame();
        Team team = e.getTeam();
        if (team == null) {
            return;
        }
        Player player = e.getPlayer();
        int players = 0;
        for (Player p : team.getPlayers()) {
            if (!game.isSpectator(p)) {
                players++;
            }
        }
        if (game.getState() == GameState.RUNNING && !game.isSpectator(player) && players <= 1) {
            Bukkit.getPluginManager().callEvent(new BedwarsTeamDeadEvent(game, team));
//            if (Config.rejoin_enabled) {
//                destroyBlock(game, team);
//                if (Main.getInstance().getArenaManager().getArenas().containsKey(game.getName())) {
//                    Main.getInstance().getArenaManager().getArenas().get(game.getName()).getRejoin().removeTeam(team.getName());
//                }
//            }
        }
        if (Main.getInstance().getArenaManager().getArenas().containsKey(game.getName())) {
            Main.getInstance().getArenaManager().getArenas().get(game.getName()).onPlayerLeave(e.getPlayer());
        }
        if (player.isOnline()) {
            ProtocolManager m = ProtocolLibrary.getProtocolManager();
            try {
                PacketContainer packet = m.createPacket(PacketType.Play.Server.SCOREBOARD_OBJECTIVE);
                packet.getIntegers().write(0, 1);
                packet.getStrings().write(0, "bwsba-game-list");
                packet.getStrings().write(1, "bwsba-game-list");
                m.sendServerPacket(player, packet);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            try {
                PacketContainer packet = m.createPacket(PacketType.Play.Server.SCOREBOARD_OBJECTIVE);
                packet.getIntegers().write(0, 1);
                packet.getStrings().write(0, "bwsba-game-name");
                packet.getStrings().write(1, "bwsba-game-name");
                m.sendServerPacket(player, packet);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        Main.getInstance().getEditHolographicManager().remove(player);
        ScoreboardUtil.removePlayer(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEnd(BedwarsGameEndEvent e) {
        Game game = e.getGame();
        if (Main.getInstance().getArenaManager().getArenas().containsKey(game.getName())) {
            Main.getInstance().getArenaManager().getArenas().get(game.getName()).onEnd();
        }
        Main.getInstance().getArenaManager().removeArena(game.getName());
        for (Player player : game.getPlayers()) {
            if (player == null) continue;
            game.playerLeave(player, false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onOpenShop(BedwarsOpenShopEvent e) {
        Game game = e.getGame();
        if (Main.getInstance().getArenaManager().getArenas().containsKey(game.getName())) {
            Main.getInstance().getArenaManager().getArenas().get(game.getName()).onOpenShop(e);
        }
    }

    @EventHandler
    public void onTargetBlockDestroyed(BedwarsTargetBlockDestroyedEvent e) {
        Game game = e.getGame();
        if (Main.getInstance().getArenaManager().getArenas().containsKey(game.getName())) {
            Main.getInstance().getArenaManager().getArenas().get(game.getName()).onTargetBlockDestroyed(e);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (Main.getInstance().getArenaManager().getArenas().containsKey(game.getName())) {
                        Main.getInstance().getArenaManager().getArenas().get(game.getName()).getScoreBoard().updateScoreboard();
                    }
                }
            }.runTaskLater(Main.getInstance(), 1L);
        }
    }

    @EventHandler
    public void onPlayerKilled(BedwarsPlayerKilledEvent e) {
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(e.getKiller());
        if (game != null && Main.getInstance().getArenaManager().getArenas().containsKey(game.getName())) {
            Main.getInstance().getArenaManager().getArenas().get(game.getName()).onPlayerKilled(e);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onOpenTeamSelection(BedwarsOpenTeamSelectionEvent e) {
        if (!Config.select_team_enabled) {
            return;
        }
        e.setCancelled(true);
        SelectTeam.openSelectTeam(e.getGame(), (Player) e.getPlayer());
    }
}
