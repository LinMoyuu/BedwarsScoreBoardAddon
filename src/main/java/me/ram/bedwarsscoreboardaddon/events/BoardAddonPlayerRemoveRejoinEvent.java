package me.ram.bedwarsscoreboardaddon.events;

import io.github.bedwarsrel.game.Game;
import me.ram.bedwarsscoreboardaddon.addon.Rejoin;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BoardAddonPlayerRemoveRejoinEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Game game;
    private final String player;
    private final Rejoin rejoin;

    public BoardAddonPlayerRemoveRejoinEvent(Game game, String player, Rejoin rejoin) {
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

    public String getPlayer() {
        return player;
    }

    public Rejoin getRejoin() {
        return rejoin;
    }

    public HandlerList getHandlers() {
        return handlers;
    }
}
