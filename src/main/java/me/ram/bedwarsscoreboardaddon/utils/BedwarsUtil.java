package me.ram.bedwarsscoreboardaddon.utils;

import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.Team;
import ldcr.BedwarsXP.api.XPManager;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.material.Bed;

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


    public static Team getTeamOfBed(Block block, Game game, Player player) {
        Team playerTeam = null;
        if (player != null) {
            // 假设有方法获取玩家的队伍
            playerTeam = game.getPlayerTeam(player);
        }

        // 处理床方块的特殊情况
        if (block.getType().equals(Material.BED_BLOCK)) {
            Block bedHead = block;
            Block bedFoot = null;

            // 获取床数据
            Bed bedData = (Bed) block.getState().getData();

            // 找到床头（床的头部方块）
            if (!bedData.isHeadOfBed()) {
                // 当前方块是床尾，找到床头
                bedHead = io.github.bedwarsrel.utils.Utils.getBedNeighbor(block);
                bedFoot = block;
            } else {
                // 当前方块是床头，找到床尾
                bedFoot = io.github.bedwarsrel.utils.Utils.getBedNeighbor(block);
            }

            // 如果玩家有队伍，检查是否是自己队伍的床
            if (playerTeam != null) {
                Block teamBed = playerTeam.getHeadTarget();
                if (teamBed != null && (teamBed.equals(bedHead) || (teamBed.equals(bedFoot)))) {
                    return null; // 是自己队伍的床，返回null
                }
            }

            // 查找哪个队伍的床是这个方块
            for (Team team : game.getTeams().values()) {
                Block teamHead = team.getHeadTarget();
                Block teamFoot = team.getFeetTarget();

                if (teamHead != null && (teamHead.equals(bedHead) || teamHead.equals(bedFoot))) {
                    return team;
                }
                if (teamFoot != null && (teamFoot.equals(bedHead) || teamFoot.equals(bedFoot))) {
                    return team;
                }
            }
        } else {
            // 非床方块的处理
            if (playerTeam != null) {
                Block teamBed = playerTeam.getHeadTarget();
                if (teamBed != null && teamBed.equals(block)) {
                    return null; // 是自己队伍的床，返回null
                }
            }

            // 查找哪个队伍的床是这个方块
            for (Team team : game.getTeams().values()) {
                Block teamHead = team.getHeadTarget();
                Block teamFoot = team.getFeetTarget();

                if ((teamHead != null && teamHead.equals(block)) ||
                        (teamFoot != null && teamFoot.equals(block))) {
                    return team;
                }
            }
        }

        return null; // 不是任何队伍的床
    }
}
