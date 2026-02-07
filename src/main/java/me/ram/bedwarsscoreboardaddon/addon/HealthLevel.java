package me.ram.bedwarsscoreboardaddon.addon;

import io.github.bedwarsrel.game.Game;
import lombok.Getter;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.events.BoardAddonSetHealthEvent;
import me.ram.bedwarsscoreboardaddon.utils.ColorUtil;
import me.ram.bedwarsscoreboardaddon.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HealthLevel {

    @Getter
    private final Game game;
    @Getter
    private final Arena arena;
    @Getter
    private final Map<String, String> levelTime;
    private final Set<String> executedHealthStages = new HashSet<>();
    @Getter
    private Integer nowHealth;

    public HealthLevel(Arena arena) {
        this.arena = arena;
        this.game = arena.getGame();
        levelTime = new HashMap<>();
        nowHealth = 20;
        if (Config.sethealth_start_enabled) {
            nowHealth = Config.sethealth_start_health;
            for (Player player : game.getPlayers()) {
                player.setMaxHealth(Config.sethealth_start_health);
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    public void checkHealth() {
        for (String sh : Main.getInstance().getConfig().getConfigurationSection("sethealth").getKeys(false)) {
            if (sh.equals("start")) {
                continue;
            }

            if (executedHealthStages.contains(sh)) {
                continue;
            }

            final int gametime = Main.getInstance().getConfig().getInt("sethealth." + sh + ".gametime");
            final int timeLeft = game.getTimeLeft();

            int remtime = timeLeft - gametime;
            String formatremtime = remtime / 60 + ":" + ((remtime % 60 < 10) ? ("0" + remtime % 60) : (remtime % 60));
            levelTime.put(sh, formatremtime);

            if (timeLeft <= gametime) {
                executedHealthStages.add(sh);

                BoardAddonSetHealthEvent setHealthEvent = new BoardAddonSetHealthEvent(game);
                Bukkit.getPluginManager().callEvent(setHealthEvent);
                if (setHealthEvent.isCancelled()) {
                    continue;
                }

                final int maxhealth = Main.getInstance().getConfig().getInt("sethealth." + sh + ".health");
                final String title = Main.getInstance().getConfig().getString("sethealth." + sh + ".title");
                final String subtitle = Main.getInstance().getConfig().getString("sethealth." + sh + ".subtitle");
                final String message = Main.getInstance().getConfig().getString("sethealth." + sh + ".message");

                nowHealth = maxhealth;
                for (Player player : game.getPlayers()) {
                    double dhealth = maxhealth - player.getMaxHealth();
                    player.setMaxHealth(maxhealth);

                    if (dhealth > 0) {
                        double nhealth = player.getHealth() + dhealth;
                        nhealth = nhealth > maxhealth ? maxhealth : nhealth;
                        player.setHealth(nhealth);
                    }

                    if (title != null && !title.isEmpty() || subtitle != null && !subtitle.isEmpty()) {
                        Utils.sendTitle(player, 10, 50, 10, ColorUtil.color(title), ColorUtil.color(subtitle));
                    }
                    if (message != null && !message.isEmpty()) {
                        player.sendMessage(ColorUtil.color(message));
                    }
                }

                PlaySound.playSound(game, Config.play_sound_sound_sethealth);
            }
        }
    }
}
