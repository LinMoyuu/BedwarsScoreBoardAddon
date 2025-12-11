package me.ram.bedwarsscoreboardaddon.events;

import io.github.bedwarsrel.game.Game;
import me.ram.bedwarsscoreboardaddon.addon.Rejoin;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BoardAddonPlayerRejoinEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Game game;
    private final Player player;
    private final Rejoin rejoin;
    private Boolean cancelled = false;

    public BoardAddonPlayerRejoinEvent(Game game, Player player, Rejoin rejoin) {
        this.game = game;
        this.player = player;
        this.rejoin = rejoin;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Game getGame() {
        return game;
    }

    public Player getPlayer() {
        return player;
    }

    public Rejoin getRejoin() {
        return rejoin;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }

    public HandlerList getHandlers() {
        return handlers;
    }
}
