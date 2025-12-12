package me.ram.bedwarsscoreboardaddon.addon;

import io.github.bedwarsrel.events.BedwarsPlayerKilledEvent;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.GameState;
import me.ram.bedwarsscoreboardaddon.utils.ColorUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

public class SoulItem implements Listener {

    @EventHandler
    public void onPlayerKilled(BedwarsPlayerKilledEvent e) {
        if (e.getKiller() == null || e.getPlayer() == null) {
            return;
        }
        Player killer = e.getKiller();
        Player player = e.getPlayer();
        Game game = e.getGame();
        if (game.getState() != GameState.RUNNING) {
            return;
        }
        if (game.getPlayerTeam(player) == null || game.getPlayerTeam(killer) == null) {
            return;
        }
        if (game.isSpectator(killer)) {
            return;
        }
        // 灵魂
        ItemStack soul = new ItemStack(Material.NETHER_STAR);
        ItemMeta soulMeta = soul.getItemMeta();
        soulMeta.setDisplayName("灵魂");
        soulMeta.setLore(Collections.singletonList(ColorUtil.color("&4死亡不掉落")));
        soul.setItemMeta(soulMeta);
        killer.getInventory().addItem(soul);
    }
}
