package me.ram.bedwarsscoreboardaddon.addon;

import io.github.bedwarsrel.game.Game;
import lombok.Getter;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.utils.BedwarsUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class TeleportTask {

    private static BukkitTask teleportTask;
    @Getter
    private final Game game;
    @Getter
    private final Arena arena;
    private final List<Integer> warningTimes = Arrays.asList(15, 10, 5, 4, 3, 2, 1);

    public TeleportTask(Arena arena) {
        this.arena = arena;
        this.game = arena.getGame();
    }

    public void startTask() {
        if (teleportTask != null) return;
        teleportTask = new BukkitRunnable() {
            int countdown = 75;

            @Override
            public void run() {
                countdown--;
                if (warningTimes.contains(countdown)) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendMessage("§e§l" + countdown + "§c§l秒后将集体绝杀传送!");
                    }
                }

                if (countdown <= 0) {
                    teleportToRandomPlayer();
                    countdown = 75;
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 20L); // 每秒执行一次
    }

    private void teleportToRandomPlayer() {
        List<Player> alivePlayers = game.getPlayers().stream()
                .filter(player -> !BedwarsUtil.isSpectator(player))
                .collect(Collectors.toList());

        if (alivePlayers.isEmpty()) {
            stopTask();
            return;
        }

        Player target = alivePlayers.get(new Random().nextInt(alivePlayers.size()));
        for (Player player : alivePlayers) {
            if (player.equals(target) || BedwarsUtil.isSpectator(player)) continue;
            player.teleport(target.getLocation());
        }
    }

    public void stopTask() {
        if (teleportTask != null) {
            teleportTask.cancel();
        }
    }
}