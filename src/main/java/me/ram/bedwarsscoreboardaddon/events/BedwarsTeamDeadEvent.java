package me.ram.bedwarsscoreboardaddon.events;

import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.Team;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BedwarsTeamDeadEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Game game;
    private final Team team;

    public BedwarsTeamDeadEvent(Game game, Team team) {
        this.game = game;
        this.team = team;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Game getGame() {
        return game;
    }

    public Team getTeam() {
        return team;
    }

    public HandlerList getHandlers() {
        return handlers;
    }
}
