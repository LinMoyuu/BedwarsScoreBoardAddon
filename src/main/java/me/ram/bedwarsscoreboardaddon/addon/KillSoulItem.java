package me.ram.bedwarsscoreboardaddon.addon;

import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.events.BedwarsPlayerKilledEvent;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.GameState;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.utils.BedwarsUtil;
import me.ram.bedwarsscoreboardaddon.utils.ColorUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KillSoulItem implements Listener {

    private final Map<UUID, Map<Integer, ItemStack>> itemsToKeep = new HashMap<>();

    @EventHandler
    public void onPlayerKilled(BedwarsPlayerKilledEvent e) {
        if (!Config.killsoul_enabled || e.getKiller() == null || e.getPlayer() == null) {
            return;
        }
        Player killer = e.getKiller();
        Player player = e.getPlayer();
        Game game = e.getGame();
        Arena arena = Main.getInstance().getArenaManager().getArena(game.getName());
        if (game.getState() != GameState.RUNNING || arena == null|| game.isSpectator(killer)) {
            return;
        }
        if (game.getPlayerTeam(player) == null || game.getPlayerTeam(killer) == null) {
            return;
        }
        int killStreaks = arena.getPlayerGameStorage().getKillStreaks(killer.getName());
        // 判断条件
        boolean shouldGiveSoul = false;
        if (Config.killsoul_autodetect) {
            // 是否是经验模式 如果是直接不执行后续语句
            if (BedwarsUtil.isXpMode(game)) return;
            // 判断是否队伍数量为8队 或 判断连杀数是否>=3 给予灵魂
            if (game.getTeams().size() >= 8 || killStreaks >= 3) shouldGiveSoul = true;
        } else if (killStreaks >= Config.killsoul_onkillstreak) {
            shouldGiveSoul = true;
        }
        // 灵魂物品
        ItemStack soul = new ItemStack(Material.NETHER_STAR);
        ItemMeta soulMeta = soul.getItemMeta();
        soulMeta.setDisplayName(ColorUtil.color("&4灵魂"));
        soulMeta.setLore(Collections.singletonList(ColorUtil.color("死亡不掉落")));
        soul.setItemMeta(soulMeta);
        if (shouldGiveSoul) {
            killer.getInventory().addItem(soul);
        }
    }

    // 死亡后特定物品不掉落 疑似只有开“死亡不掉落”然后自己造掉落才行了
    // 所以就这么生草 交给哈基米写了
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity().getPlayer();
        if (player == null) return;
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null) {
            return;
        }
        Arena arena = Main.getInstance().getArenaManager().getArena(game.getName());
        if (arena == null) {
            return;
        }
        if (BedwarsUtil.isXpMode(game) || game.getPlayerTeam(player) == null || game.isSpectator(player)) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        Map<Integer, ItemStack> keptItems = new HashMap<>();

        // 1. 遍历玩家的整个物品栏（包括盔甲和副手）
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);

            // 检查物品是否为“灵魂绑定”
            // 哈基米没写空指针检查 无敌
            if (item != null && item.getType().equals(Material.NETHER_STAR)) {
                // 记录物品和它所在的格子编号
                keptItems.put(i, item);
                // 从掉落列表中移除该物品，防止它掉在地上
                event.getDrops().remove(item);
            }
        }
        if (!keptItems.isEmpty()) {
            itemsToKeep.put(player.getUniqueId(), keptItems);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null) return;
        if (BedwarsUtil.isXpMode(game) || game.getPlayerTeam(player) == null || game.isSpectator(player)) {
            return;
        }
        if (itemsToKeep.containsKey(playerUUID)) {
            Map<Integer, ItemStack> keptItems = itemsToKeep.get(playerUUID);
            PlayerInventory inventory = player.getInventory();

            // 4. 遍历暂存的物品，并使用 setItem 方法放回原位
            for (Map.Entry<Integer, ItemStack> entry : keptItems.entrySet()) {
                int slot = entry.getKey();
                ItemStack item = entry.getValue();
                inventory.setItem(slot, item); // 这是关键！
            }

            itemsToKeep.remove(playerUUID);
        }
    }
}
