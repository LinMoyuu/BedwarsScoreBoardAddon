package me.ram.bedwarsscoreboardaddon.events;

import io.github.bedwarsrel.game.Game;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BoardAddonSetHealthEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Game game;
    private Boolean cancelled = false;

    public BoardAddonSetHealthEvent(Game game) {
        this.game = game;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Game getGame() {
        return game;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    public HandlerList getHandlers() {
        return handlers;
    }
}
