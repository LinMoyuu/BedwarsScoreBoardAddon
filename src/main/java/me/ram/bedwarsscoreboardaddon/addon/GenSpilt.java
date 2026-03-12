package me.ram.bedwarsscoreboardaddon.addon;

import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.Team;
import lombok.Getter;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// https://github.com/tomkeuper/BedWars2023/blob/development/bedwars-plugin/src/main/java/com/tomkeuper/bedwars/arena/feature/GenSplitFeature.java
public class GenSpilt implements Listener {

    @Getter
    private final Game game;
    @Getter
    private final Arena arena;
    private List<Listener> listeners;

    public GenSpilt(Arena arena) {
        this.arena = arena;
        this.game = arena.getGame();
        listeners = new ArrayList<>();
        listeners.add(this);
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    public void onEnd() {
        listeners.forEach(HandlerList::unregisterAll);
        listeners = null;
    }

    @EventHandler
    public void onResPickup(PlayerPickupItemEvent e) {
        if (!Config.resource_genspilt_enabled) return;
        if (e.isCancelled()) return;
        if (e.getItem() == null) return;
        ItemStack itemStack = e.getItem().getItemStack();
        if (itemStack == null) return;

        boolean shouldCoutiune = false;
        for (String items : Config.resource_genspilt_items) {
            try {
                if (itemStack.getType().equals(Material.valueOf(items))) {
                    shouldCoutiune = true;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (!shouldCoutiune) return;
        Player player = e.getPlayer();

        Location playerLocation = e.getPlayer().getLocation();
        double splitRange = Config.resource_genspilt_range;
        Collection<Entity> nearbyEntities = playerLocation.getWorld().getNearbyEntities(playerLocation, splitRange, splitRange, 2.0);

        for (Entity entity : playerLocation.getWorld().getEntities()) {
            if (nearbyEntities.contains(entity) && entity instanceof Player) {
                Player pickupPlayer = (Player) entity;
                if (pickupPlayer.getUniqueId() != player.getUniqueId()) {
                    Team team = game.getPlayerTeam(player);
                    Team rt = game.getPlayerTeam(pickupPlayer);
                    if (!arena.isAlivePlayer(game, player) || !arena.isAlivePlayer(game, pickupPlayer) || team != rt)
                        continue;
                    pickupPlayer.getInventory().addItem(itemStack.clone());
                }
            }
        }
    }

}
