package me.ram.bedwarsscoreboardaddon.addon;

import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.events.BedwarsGameStartedEvent;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.GameState;
import lombok.Getter;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.events.BoardAddonPlayerShootWitherBowEvent;
import me.ram.bedwarsscoreboardaddon.utils.ColorUtil;
import me.ram.bedwarsscoreboardaddon.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class WitherBow implements Listener {

    @Getter
    private final Game game;
    @Getter
    private final Arena arena;
    private List<Listener> listeners;
    @Getter
    private boolean witherbowEnabled;

    public WitherBow(Arena arena) {
        this.arena = arena;
        this.game = arena.getGame();
        this.witherbowEnabled = false;
        listeners = new ArrayList<>();
        listeners.add(this);
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent e) {
        if (!Config.witherbow_enabled) {
            return;
        }
        if (!(e.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) e.getEntity();
        if (arena == null || game.getState() != GameState.RUNNING || !arena.isAlivePlayer(player) || !this.isWitherbowEnabled()) {
            return;
        }
        WitherSkull skull = player.launchProjectile(WitherSkull.class, e.getProjectile().getVelocity().multiply(0.3));
        BoardAddonPlayerShootWitherBowEvent shootWitherBowEvent = new BoardAddonPlayerShootWitherBowEvent(game, player, skull);
        BedwarsRel.getInstance().getServer().getPluginManager().callEvent(shootWitherBowEvent);
        if (shootWitherBowEvent.isCancelled()) {
            skull.remove();
            return;
        }
        player.getWorld().playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 5);
        PlaySound.playSound(player, Config.play_sound_sound_witherbow);
        skull.setYield(4.0f);
        skull.setShooter(player);
        e.setCancelled(true);
        player.updateInventory();
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        Entity entity = e.getEntity();
        Entity damager = e.getDamager();
        if (!(entity instanceof Player) || !(damager instanceof WitherSkull)) {
            return;
        }
        WitherSkull skull = (WitherSkull) damager;
        if (skull.getShooter() == null) {
            return;
        }
        Player shooter = (Player) skull.getShooter();
        Player player = (Player) entity;
        if (arena == null || game.getState() != GameState.RUNNING || !arena.isAlivePlayer(player) || !arena.isAlivePlayer(shooter) || !this.isWitherbowEnabled()) {
            return;
        }
        if (game.getPlayerTeam(shooter).getName().equals(game.getPlayerTeam(player).getName())) {
            e.setCancelled(true);
            return;
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1));
    }

    @EventHandler
    public void onGameStarted(BedwarsGameStartedEvent event) {
        if (!arena.isGame(event.getGame())) return;
        if (Config.witherbow_enabled && Config.witherbow_tips_on_start) {
            int enableAfterSec = BedwarsRel.getInstance().getMaxLength() - Config.witherbow_gametime;
            for (Player player : game.getPlayers()) {
                player.sendMessage(ColorUtil.color(WitherBow.formatMessage(enableAfterSec)));
            }
        }
    }

    public void checkWitherBow() {
        if (!Config.witherbow_enabled || this.witherbowEnabled) return;
        int enableAfterSec = (game.getTimeLeft() - Config.witherbow_gametime);

        if (Config.witherbow_remind_times != null && !Config.witherbow_remind_times.isEmpty()) {
            if (Config.witherbow_remind_times.contains(enableAfterSec)) {
                for (Player player : game.getPlayers()) {
                    player.sendMessage(WitherBow.formatMessage(enableAfterSec));
                }
            }
        }

        if (game.getTimeLeft() <= Config.witherbow_gametime) {
            if (!Config.witherbow_title.isEmpty() || !Config.witherbow_subtitle.isEmpty()) {
                game.getPlayers().forEach(player -> Utils.sendTitle(player, 10, 50, 10, Config.witherbow_title, Config.witherbow_subtitle));
            }
            if (!Config.witherbow_message.isEmpty()) {
                game.getPlayers().forEach(player -> player.sendMessage(Config.witherbow_message.replace("{bwprefix}", Config.bwrelPrefix)));
            }
            PlaySound.playSound(game, Config.play_sound_sound_enable_witherbow);
            this.witherbowEnabled = true;
        }
    }

    /**
     * 格式化倒计时消息
     *
     * @param seconds 剩余秒数
     * @return 格式化后的消息
     */
    public static String formatMessage(int seconds) {
        String message = Config.witherbow_countdown_message;

        String time;
        String unit;

        if (seconds >= 60) {
            int minutes = seconds / 60;
            time = String.valueOf(minutes);
            unit = "分钟";
        } else {
            time = String.valueOf(seconds);
            unit = "秒钟";
        }

        // 替换占位符
        String formatted = message
                .replace("{bwprefix}", Config.bwrelPrefix)
                .replace("{time}", time)
                .replace("{unit}", unit);

        return ColorUtil.color(formatted);
    }

    public void onEnd() {
        listeners.forEach(HandlerList::unregisterAll);
        listeners = null;
    }
}
