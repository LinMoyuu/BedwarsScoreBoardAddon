package me.ram.bedwarsscoreboardaddon.addon;

import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.Team;
import lombok.Getter;
import lombok.Setter;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.events.BoardAddonDeathModeEvent;
import me.ram.bedwarsscoreboardaddon.utils.Utils;
import org.bukkit.*;
import org.bukkit.entity.Player;

public class DeathMode {

    @Getter
    private final Game game;
    @Getter
    private final Arena arena;
    private String deathmode_time;
    @Getter
    @Setter
    private boolean enabledDeathMode = false;

    public DeathMode(Arena arena) {
        this.arena = arena;
        this.game = arena.getGame();
    }

    public void checkDeathMode() {
        if (!Config.deathmode_enabled || isEnabledDeathMode()) return;
        int deathmodetime = game.getTimeLeft() - Config.deathmode_gametime;
        deathmode_time = deathmodetime / 60 + ":" + ((deathmodetime % 60 < 10) ? ("0" + deathmodetime % 60) : (deathmodetime % 60));
        if (game.getTimeLeft() <= Config.deathmode_gametime) {
            BoardAddonDeathModeEvent deathModeEvent = new BoardAddonDeathModeEvent(game);
            Bukkit.getPluginManager().callEvent(deathModeEvent);
            if (deathModeEvent.isCancelled()) {
                return;
            }
            setEnabledDeathMode(true);
            for (Player player : game.getPlayers()) {
                if (!Config.deathmode_title.isEmpty() || !Config.deathmode_subtitle.isEmpty()) {
                    Utils.sendTitle(player, 10, 80, 10, Config.deathmode_title, Config.deathmode_subtitle);
                }
                if (!Config.deathmode_message.isEmpty()) {
                    player.sendMessage(Config.deathmode_message);
                }
            }
            for (Team team : game.getTeams().values()) {
                destroyBlock(game, team);
            }
            PlaySound.playSound(game, Config.play_sound_sound_deathmode);
            startShrinking();
        }
    }

    public String getDeathmodeTime() {
        return deathmode_time;
    }

    private void destroyBlock(Game game, Team team) {
        Material type = team.getTargetHeadBlock().getBlock().getType();
        if (type.equals(game.getTargetMaterial())) {
            if (type.equals(Material.BED_BLOCK)) {
                if (BedwarsRel.getInstance().getCurrentVersion().startsWith("v1_8")) {
                    team.getTargetFeetBlock().getBlock().setType(Material.AIR);
                } else {
                    team.getTargetHeadBlock().getBlock().setType(Material.AIR);
                }
            } else {
                team.getTargetHeadBlock().getBlock().setType(Material.AIR);
            }
        }
    }

    public void startShrinking() {
        World world = game.getLoc1().getWorld();
        Location loc1 = game.getLoc1();
        Location loc2 = game.getLoc2();
        // 计算中心点
        double centerX = (loc1.getX() + loc2.getX()) / 2;
        double centerZ = (loc1.getZ() + loc2.getZ()) / 2;
        // 计算初始边界的直径 (取X和Z中较大的值，确保整个区域都在圈内)
        double sizeX = Math.abs(loc1.getX() - loc2.getX());
        double sizeZ = Math.abs(loc1.getZ() - loc2.getZ());
        double initialSize = Math.max(sizeX, sizeZ);
        WorldBorder border = world.getWorldBorder();

        // 设置初始边界
        border.setCenter(centerX, centerZ);
        border.setSize(initialSize);
        border.setDamageAmount(0.1); // 边界外的伤害
        border.setWarningDistance(10); // 靠近边界10格时开始警告

        long shrinkDurationInSeconds = 90; // 缩短秒数
        double targetSize = 25.0; // 最终大小

        // 设置缩小
        border.setSize(targetSize, shrinkDurationInSeconds);
    }
}
