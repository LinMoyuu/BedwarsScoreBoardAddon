package me.ram.bedwarsscoreboardaddon.listener;

import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.GameState;
import io.github.bedwarsrel.game.Team;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.addon.RandomEvents;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.edit.EditGame;
import me.ram.bedwarsscoreboardaddon.events.BedwarsTeamDeadEvent;
import me.ram.bedwarsscoreboardaddon.menu.MenuManager;
import me.ram.bedwarsscoreboardaddon.utils.BedwarsUtil;
import me.ram.bedwarsscoreboardaddon.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Optional;

public class EventListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent e) {
        Player player = e.getPlayer();
        String message = e.getMessage();
        String[] args = message.split(" ");
        // 配置时 传送
        if (args[0].equalsIgnoreCase("/bwsbatp")) {
            e.setCancelled(true);
            if (args.length == 8 && player.hasPermission("bedwarsscoreboardaddon.teleport")) {
                String loc = message.substring(10 + args[1].length());
                Location location = toLocation(loc);
                if (location != null) {
                    player.teleport(location);
                    Main.getInstance().getEditHolographicManager().displayGameLocation(player, args[1]);
                }
            }
            return;
        }
        // 使用bwrel自带创建地图命令时 打开创建地图配置菜单 (原来这么生草?)
        if (args[0].equalsIgnoreCase("/bw") || args[0].equalsIgnoreCase("/bedwarsrel:bw")) {
            if (args.length > 3) {
                if (args[1].equalsIgnoreCase("addgame")) {
                    try {
                        Integer.valueOf(args[3]);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                Game game = BedwarsRel.getInstance().getGameManager().getGame(args[2]);
                                EditGame.editGame(player, game);
                            }
                        }.runTask(Main.getInstance());
                    } catch (Exception ex) {
                    }
                }
            }
            return;
        }
        // rejoin相关
        if (!args[0].equalsIgnoreCase("/rejoin")) {
            return;
        }
        e.setCancelled(true);
        if (!Config.rejoin_enabled) {
            return;
        }
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game != null) {
            return;
        }
        for (Arena arena : Main.getInstance().getArenaManager().getArenas().values()) {
            if (arena.getRejoin().getPlayers().containsKey(player.getName())) {
                arena.getGame().playerJoins(player);
                return;
            }
        }
        player.sendMessage(Config.rejoin_message_error);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null) {
            return;
        }
        if (Main.getInstance().getArenaManager().getArenas().containsKey(game.getName())) {
            Main.getInstance().getArenaManager().getArenas().get(game.getName()).onDeath(player);
        }
        Team team = game.getPlayerTeam(player);
        if (team == null) {
            return;
        }
        int players = 0;
        for (Player p : team.getPlayers()) {
            if (!game.isSpectator(p)) {
                players++;
            }
        }
        if (game.getState() == GameState.RUNNING && players <= 1 && !game.isSpectator(player) && team.isDead(game)) {
            Bukkit.getPluginManager().callEvent(new BedwarsTeamDeadEvent(game, team));
            if (Main.getInstance().getArenaManager().getArenas().containsKey(game.getName())) {
                Main.getInstance().getArenaManager().getArenas().get(game.getName()).getRejoin().removeTeam(team.getName());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game != null && Main.getInstance().getArenaManager().getArenas().containsKey(game.getName())) {
            Main.getInstance().getArenaManager().getArenas().get(game.getName()).onRespawn(player);
        }
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            if (player.isOnline()) {
                Main.getInstance().getHolographicManager().getPlayerHolographic(player).forEach(holo -> holo.display(player));
            }
        }, 5L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.isCancelled()) {
            return;
        }
        Player player = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            if (player.isOnline()) {
                Main.getInstance().getHolographicManager().getPlayerHolographic(player).forEach(holo -> holo.display(player));
            }
        }, 1L);
    }

    // https://github.com/BedwarsRel/BedwarsRel/blob/master/common/src/main/java/io/github/bedwarsrel/listener/PlayerListener.java#L492-L511
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onToggleFlight(PlayerToggleFlightEvent e) {
        Player player = e.getPlayer();
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null || game.getState() != GameState.RUNNING || game.isSpectator(player) || !game.getPlayers().contains(player)) {
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE) e.setCancelled(false);
    }

    @EventHandler
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
        for (Arena arena : Main.getInstance().getArenaManager().getArenas().values()) {
            arena.onArmorStandManipulate(e);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInvisDamageByEntity(EntityDamageByEntityEvent e) {
        if (!Config.invisibility_player_enabled) {
            return;
        }
        if (e.isCancelled()) {
            return;
        }
        if (!(e.getEntity() instanceof Player && e.getDamager() instanceof Player)) {
            return;
        }
        Player player = (Player) e.getEntity();
        if (!player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            return;
        }
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null) {
            return;
        }
        if (game.getState() != GameState.RUNNING) {
            return;
        }
        if (BedwarsUtil.isSpectator(game, player)) {
            return;
        }
        if (!Main.getInstance().getArenaManager().getArenas().containsKey(game.getName())) {
            return;
        }
        if (Config.invisibility_player_damage_show_player) {
            Main.getInstance().getArenaManager().getArenas().get(game.getName()).getInvisiblePlayer().removePlayer(player);
        } else {
            Main.getInstance().getArenaManager().getArenas().get(game.getName()).getInvisiblePlayer().showPlayerArmor(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemMerge(ItemMergeEvent e) {
        for (Arena arena : Main.getInstance().getArenaManager().getArenas().values()) {
            arena.onItemMerge(e);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFoodLevelChange(FoodLevelChangeEvent e) {
        if (Config.hunger_change) {
            return;
        }
        if (!(e.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) e.getEntity();
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null) {
            return;
        }
        if (game.getState() != GameState.RUNNING) {
            return;
        }
        e.setFoodLevel(20);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerItemConsume(PlayerItemConsumeEvent e) {
        Player player = e.getPlayer();
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null || game.getState() != GameState.RUNNING || BedwarsUtil.isSpectator(game, player)) {
            return;
        }
        Arena arena = Main.getInstance().getArenaManager().getArenas().get(game.getName());
        if (arena == null) {
            return;
        }
        if (!e.getItem().getType().equals(Material.POTION)) return;
        ItemStack itemStack = e.getItem().clone();
        PotionMeta potionMeta = (PotionMeta) itemStack.getItemMeta();
        if (potionMeta == null) return;

        // 物品冷却
        if (Config.do_not_drink_randomevent_same_effect && arena.getRandomEventsManager().hasActiveEvent()) {
            Optional<RandomEvents> currentEventOptional = arena.getRandomEventsManager().getCurrentEvent();
            if (currentEventOptional.isPresent()) {
                RandomEvents currentEvent = currentEventOptional.get();
                PotionEffectType eventEffectType = currentEvent.getEffectType();

                if (Utils.hasEffectOfType(potionMeta, itemStack, eventEffectType) && player.hasPotionEffect(eventEffectType)) {
                    e.setCancelled(true);
                    player.sendMessage("物品冷却中");
                    return;
                }
            }
        }

        // 清空药水瓶
        if (Config.clear_bottle) {
            Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
                ItemStack glassBottle = new ItemStack(Material.GLASS_BOTTLE);
                if (BedwarsRel.getInstance().getCurrentVersion().startsWith("v1_8")) {
                    if (player.getInventory().getItemInHand().isSimilar(glassBottle)) {
                        player.getInventory().setItemInHand(new ItemStack(Material.AIR));
                    }
                } else if (player.getInventory().getItemInMainHand().isSimilar(glassBottle)) {
                    player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                } else if (player.getInventory().getItemInOffHand().isSimilar(glassBottle)) {
                    player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
                }
            });
        }

        // 隐藏隐身玩家药水粒子效果
        if (Config.invisibility_player_enabled && Utils.hasEffectOfType(potionMeta, itemStack, PotionEffectType.INVISIBILITY)) {
            arena.getInvisiblePlayer().hidePlayer(player);
            if (Config.invisibility_player_hide_particles) {
                for (PotionEffect effect : player.getActivePotionEffects()) {
                    player.addPotionEffect(new PotionEffect(effect.getType(), effect.getDuration(), effect.getAmplifier(), true, false), true);
                }
            }
        }

        // 隐藏隐身玩家其他药水粒子效果
        if (Config.invisibility_player_enabled && Config.invisibility_player_hide_particles && arena.getInvisiblePlayer().isInvisiblePlayer(player) && (e.getItem().getType() == Material.POTION || e.getItem().getType() == Material.GOLDEN_APPLE || e.getItem().getType() == Material.ROTTEN_FLESH || e.getItem().getType() == Material.RAW_FISH || e.getItem().getType() == Material.RAW_CHICKEN || e.getItem().getType() == Material.SPIDER_EYE || e.getItem().getType() == Material.POISONOUS_POTATO)) {
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                for (PotionEffect effect : player.getActivePotionEffects()) {
                    player.addPotionEffect(new PotionEffect(effect.getType(), effect.getDuration(), effect.getAmplifier(), true, false), true);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null) return;
        if (Main.getInstance().getArenaManager().getArenas().containsKey(game.getName())) {
            Main.getInstance().getArenaManager().getArenas().get(game.getName()).onInteract(e);
        }
        if (e.isCancelled()) {
            return;
        }
        if (e.getItem() == null || !(e.getItem().getType() == Material.WATER_BUCKET || e.getItem().getType() == Material.LAVA_BUCKET) || e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (BedwarsUtil.isSpectator(game, player) || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        game.getRegion().addPlacedBlock(e.getClickedBlock().getRelative(e.getBlockFace()), null);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        Player player = e.getPlayer();
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null) return;
        if (Main.getInstance().getArenaManager().getArenas().containsKey(game.getName())) {
            Main.getInstance().getArenaManager().getArenas().get(game.getName()).onInteractEntity(e);
        }
    }

    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent e) {
        Main.getInstance().getEditHolographicManager().remove(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Main.getInstance().getEditHolographicManager().remove(e.getPlayer());
        Player player = e.getPlayer();
        MenuManager man = Main.getInstance().getMenuManager();
        man.removePlayer(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player player = (Player) e.getEntity();
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null) return;
        if (Main.getInstance().getArenaManager().getArenas().containsKey(game.getName())) {
            Main.getInstance().getArenaManager().getArenas().get(game.getName()).onDamage(e);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamageEntity(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player) || !(e.getDamager() instanceof Player)) return;
        Player player = (Player) e.getEntity();
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null) return;
        if (Main.getInstance().getArenaManager().getArenas().containsKey(game.getName())) {
            Main.getInstance().getArenaManager().getArenas().get(game.getName()).onEntityDamageByEntity(e);
        }
    }

    @EventHandler
    public void onPearlDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof EnderPearl)) return;
        Player player = (Player) event.getEntity();
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null || game.getState() != GameState.RUNNING) return;
        if (Config.pearlnodamage_autodetect) {
            if (!BedwarsUtil.isXpMode(game)) event.setCancelled(true);
        } else if (Config.pearlnodamage_enabled) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHangingBreak(HangingBreakEvent e) {
        Main.getInstance().getArenaManager().getArenas().values().forEach(arena -> {
            arena.onHangingBreak(e);
        });
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Player player = (Player) e.getPlayer();
        MenuManager man = Main.getInstance().getMenuManager();
        man.removePlayer(player);
    }

    private Location toLocation(String loc) {
        try {
            String[] ary = loc.split(", ");
            if (Bukkit.getWorld(ary[0]) != null) {
                Location location = new Location(Bukkit.getWorld(ary[0]), Double.parseDouble(ary[1]), Double.parseDouble(ary[2]), Double.parseDouble(ary[3]));
                if (ary.length > 4) {
                    location.setYaw(Float.parseFloat(ary[4]));
                    location.setPitch(Float.parseFloat(ary[5]));
                }
                return location;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}
