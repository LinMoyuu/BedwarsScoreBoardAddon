package me.ram.bedwarsscoreboardaddon.listener;

import ldcr.BedwarsXP.EventListeners;
import me.ram.bedwarsscoreboardaddon.utils.BedwarsUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;

public class XPEventListener extends EventListeners implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onItemPickup(PlayerPickupItemEvent e) {
        if (e.getPlayer() == null) {
            return;
        }
        Player player = e.getPlayer();
        if (BedwarsUtil.isSpectator(player) || BedwarsUtil.isRespawning(player)) {
            return;
        }
        super.onItemPickup(e);
    }
}
