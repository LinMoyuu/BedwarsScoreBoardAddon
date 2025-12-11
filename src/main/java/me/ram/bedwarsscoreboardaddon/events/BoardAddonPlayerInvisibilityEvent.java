package me.ram.bedwarsscoreboardaddon.events;

import io.github.bedwarsrel.game.Game;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BoardAddonPlayerInvisibilityEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Game game;
    private final Player player;
    private Boolean cancelled = false;

    public BoardAddonPlayerInvisibilityEvent(Game game, Player player) {
        this.game = game;
        this.player = player;
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
