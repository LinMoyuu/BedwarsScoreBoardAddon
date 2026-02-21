package me.ram.bedwarsscoreboardaddon.addon;

import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.Team;
import lombok.Getter;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FriendlyBreak implements Listener {

    @Getter
    private final Game game;
    @Getter
    private final Arena arena;
    @Getter
    private final HashMap<Player, Integer> friendlyBreakCount;
    private List<Listener> listeners;

    public FriendlyBreak(Arena arena) {
        this.arena = arena;
        this.game = arena.getGame();
        friendlyBreakCount = new HashMap<>();
        listeners = new ArrayList<>();
        listeners.add(this);
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFriendlyBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!arena.isAlivePlayer(player)) return;
        if (!Config.friendlybreak_kick_enabled || BedwarsRel.getInstance().getBooleanConfig("friendlybreak", true))
            return;
        Team playerTeam = game.getPlayerTeam(player);
        if (playerTeam == null) return;
        for (Player teamPlayer : playerTeam.getPlayers()) {
            if (player.equals(teamPlayer)) {
                continue;
            }

            if (!teamPlayer.getLocation().getBlock().getRelative(BlockFace.DOWN).equals(event.getBlock())) {
                continue;
            }

            friendlyBreakCount.put(player, friendlyBreakCount.getOrDefault(player, 0) + 1);
            int friendlyBreaks = friendlyBreakCount.getOrDefault(player, 0);
            int max_breaks = Config.friendlybreak_kick_max_breaks;

            player.sendMessage(ColorUtil.color(Config.friendlybreak_warning_message
                    .replace("{bwprefix}", Config.bwrelPrefix)
                    .replace("{breakcount}", String.valueOf(friendlyBreaks))
                    .replace("{max_breaks}", String.valueOf(max_breaks))));

            if (friendlyBreaks >= max_breaks) {
                friendlyBreakCount.remove(player);
                player.kickPlayer(ColorUtil.color(Config.friendlybreak_kick_message
                        .replace("{bwprefix}", Config.bwrelPrefix)
                        .replace("{breakcount}", String.valueOf(friendlyBreaks))
                        .replace("{max_breaks}", String.valueOf(max_breaks))));

                String broadCastMessage = ColorUtil.color(Config.friendlybreak_broadcast_message
                        .replace("{bwprefix}", Config.bwrelPrefix)
                        .replace("{breakcount}", String.valueOf(friendlyBreaks))
                        .replace("{max_breaks}", String.valueOf(max_breaks))
                        .replace("{playername}", player.getName())
                        .replace("{playerdisplayname}", player.getDisplayName())
                        .replace("{team}", playerTeam.getDisplayName())
                        .replace("{teamcolor}", playerTeam.getChatColor().toString()));
                for (Player gamePlayer : game.getPlayers()) {
                    gamePlayer.sendMessage(broadCastMessage);
                }
            }
            break;
        }
    }

    public void onEnd() {
        listeners.forEach(HandlerList::unregisterAll);
        listeners = null;
    }
}
