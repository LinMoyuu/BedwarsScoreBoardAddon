package me.ram.bedwarsscoreboardaddon.addon;

import io.github.bedwarsrel.events.BedwarsGameStartedEvent;
import io.github.bedwarsrel.game.Game;
import lombok.Getter;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.events.BoardAddonPlayerRespawnEvent;
import me.ram.bedwarsscoreboardaddon.utils.ScoreboardUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HealthBar implements Listener {

    @Getter
    private final Game game;
    @Getter
    private final Arena arena;
    @Getter
    private final HashMap<Player, Integer> friendlyBreakCount;
    private final List<Listener> listeners;

    public HealthBar(Arena arena) {
        this.arena = arena;
        this.game = arena.getGame();
        this.friendlyBreakCount = new HashMap<>();
        listeners = new ArrayList<>();
        listeners.add(this);
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHbEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        if (!arena.isGamePlayer(game, player)) return;
        int health = (int) Math.max(0, Math.ceil((player.getHealth() - event.getFinalDamage())));
        for (Player target : game.getPlayers()) {
            if (target == null || !target.isOnline()) {
                continue;
            }
            ScoreboardUtil.sendHealthValuePacket(target, player, health);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHbEntityRegain(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        if (!arena.isGamePlayer(game, player)) return;
        int health = (int) Math.ceil(player.getHealth() + event.getAmount());
        for (Player target : game.getPlayers()) {
            if (target == null || !target.isOnline()) {
                continue;
            }
            ScoreboardUtil.sendHealthValuePacket(target, player, health);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHbRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!arena.isGamePlayer(game, player)) return;
        for (Player target : game.getPlayers()) {
            if (target == null || !target.isOnline()) {
                continue;
            }
            ScoreboardUtil.sendHealthValuePacket(target, player, (int) Math.ceil(player.getHealth()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHbScoreboardRespawn(BoardAddonPlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!arena.isGamePlayer(game, player)) return;
        for (Player target : game.getPlayers()) {
            if (target == null || !target.isOnline()) {
                continue;
            }
            ScoreboardUtil.sendHealthValuePacket(target, player, (int) Math.ceil(player.getHealth()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHbStarted(BedwarsGameStartedEvent event) {
        if (!arena.isGame(event.getGame())) return;
        for (Player player : game.getPlayers()) {
            if (player == null || !player.isOnline()) {
                continue;
            }
            int currentHealth = (int) Math.max(0, Math.ceil(player.getHealth()));

            for (Player target : game.getPlayers()) {
                if (target != null && target.isOnline() && !target.equals(player)) {
                    ScoreboardUtil.sendHealthValuePacket(target, player, currentHealth);
                }
            }
            ScoreboardUtil.sendHealthValuePacket(player, player, currentHealth);
        }
    }

    public void onEnd() {
        listeners.forEach(HandlerList::unregisterAll);
    }
}
