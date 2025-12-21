package me.ram.bedwarsscoreboardaddon.addon;

import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.events.BedwarsGameStartedEvent;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.GameState;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.events.BoardAddonPlayerShootWitherBowEvent;
import me.ram.bedwarsscoreboardaddon.utils.BedwarsUtil;
import me.ram.bedwarsscoreboardaddon.utils.ColorUtil;
import me.ram.bedwarsscoreboardaddon.utils.Utils;
import org.bukkit.Effect;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class WitherBow implements Listener {

    @EventHandler
    public void onStarted(BedwarsGameStartedEvent e) {
        Game game = e.getGame();
        Arena arena = Main.getInstance().getArenaManager().getArena(game.getName());
        int enableAfterMinutes = (BedwarsRel.getInstance().getMaxLength() - Config.witherbow_gametime) / 60;
        for (Player player : game.getPlayers()) {
            player.sendMessage(ColorUtil.color(BedwarsRel.getInstance().getConfig().getString("chat-prefix") + " §f§l凋零弓  §7将在 §a" + enableAfterMinutes + " 分钟 §7后开启!"));
        }
        arena.addGameTask(new BukkitRunnable() {
            @Override
            public void run() {
                if (arena.getGameLeft() <= Config.witherbow_gametime && Config.witherbow_enabled) {
                    if (!Config.witherbow_title.isEmpty() || !Config.witherbow_subtitle.isEmpty()) {
                        game.getPlayers().forEach(player -> Utils.sendTitle(player, 10, 50, 10, Config.witherbow_title, Config.witherbow_subtitle));
                    }
                    if (!Config.witherbow_message.isEmpty()) {
                        game.getPlayers().forEach(player -> player.sendMessage(Config.witherbow_message));
                    }
                    PlaySound.playSound(game, Config.play_sound_sound_enable_witherbow);
                    cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 20L));
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
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null) return;
        Arena arena = Main.getInstance().getArenaManager().getArena(game.getName());
        if (arena == null || game.getState() != GameState.RUNNING || BedwarsUtil.isSpectator(game, player) || arena.getGameLeft() > Config.witherbow_gametime) {
            return;
        }
        WitherSkull skull = player.launchProjectile(WitherSkull.class);
        BoardAddonPlayerShootWitherBowEvent shootWitherBowEvent = new BoardAddonPlayerShootWitherBowEvent(game, player, skull);
        BedwarsRel.getInstance().getServer().getPluginManager().callEvent(shootWitherBowEvent);
        if (shootWitherBowEvent.isCancelled()) {
            skull.remove();
            return;
        }
        player.getWorld().playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 5);
        PlaySound.playSound(player, Config.play_sound_sound_witherbow);
        skull.setYield(4.0f);
        skull.setVelocity(e.getProjectile().getVelocity());
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
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null) {
            return;
        }
        if (BedwarsUtil.isSpectator(game, player) || BedwarsUtil.isSpectator(game, shooter)) {
            e.setCancelled(true);
            return;
        }
        if (game.getPlayerTeam(shooter).getName().equals(game.getPlayerTeam(player).getName())) {
            e.setCancelled(true);
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1));
    }
}
