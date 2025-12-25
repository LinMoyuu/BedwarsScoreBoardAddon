package me.ram.bedwarsscoreboardaddon.utils;

import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.Team;
import ldcr.BedwarsXP.api.XPManager;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;
import org.bukkit.entity.Player;

public class BedwarsUtil {

    public static boolean isRespawning(Player player) {
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null) {
            return false;
        }
        return isRespawning(game, player);
    }

    public static boolean isRespawning(Game game, Player player) {
        Arena arena = Main.getInstance().getArenaManager().getArena(game.getName());
        if (arena == null) {
            return false;
        }
        return arena.getRespawn().isRespawning(player);
    }

    public static boolean isSpectator(Player player) {
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null) {
            return false;
        }
        return isSpectator(game, player);
    }

    public static boolean isSpectator(Game game, Player player) {
        return game.isSpectator(player) || isRespawning(game, player);
    }

    public static boolean isDieOut(Game game, Team team) {
        if (!team.isDead(game)) {
            return false;
        }
        for (Player player : team.getPlayers()) {
            if (!game.isSpectator(player)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isXpMode(Game game) {
        return Config.isBedwarsXPEnabled && ldcr.BedwarsXP.Config.isGameEnabledXP(game.getName());
    }

    public static int getPlayerXP(Game game, Player player) {
        if (!isXpMode(game)) return 0;
        return XPManager.getXPManager(game.getName()).getXP(player);
    }
}
