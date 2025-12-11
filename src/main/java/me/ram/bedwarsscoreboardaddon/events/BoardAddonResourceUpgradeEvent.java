package me.ram.bedwarsscoreboardaddon.events;

import io.github.bedwarsrel.game.Game;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;

public class BoardAddonResourceUpgradeEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Game game;
    private List<String> upgrade;
    private Boolean cancelled = false;

    public BoardAddonResourceUpgradeEvent(Game game, List<String> upgrade) {
        this.game = game;
        this.upgrade = upgrade;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Game getGame() {
        return game;
    }

    public List<String> getUpgrade() {
        return upgrade;
    }

    public void setUpgrade(List<String> upgrade) {
        this.upgrade = upgrade;
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
