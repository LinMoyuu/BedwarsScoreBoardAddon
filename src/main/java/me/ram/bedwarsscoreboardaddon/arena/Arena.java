package me.ram.bedwarsscoreboardaddon.arena;

import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.events.BedwarsGameOverEvent;
import io.github.bedwarsrel.events.BedwarsOpenShopEvent;
import io.github.bedwarsrel.events.BedwarsPlayerKilledEvent;
import io.github.bedwarsrel.events.BedwarsTargetBlockDestroyedEvent;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.GameState;
import io.github.bedwarsrel.game.Team;
import lombok.Getter;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.addon.*;
import me.ram.bedwarsscoreboardaddon.addon.teamshop.TeamShop;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.storage.PlayerGameStorage;
import me.ram.bedwarsscoreboardaddon.utils.BedwarsUtil;
import me.ram.bedwarsscoreboardaddon.utils.PlaceholderAPIUtil;
import me.ram.bedwarsscoreboardaddon.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class Arena {

    @Getter
    private final Game game;
    @Getter
    private final ScoreBoard scoreBoard;
    @Getter
    private final PlayerGameStorage playerGameStorage;
    @Getter
    private final DeathMode deathMode;
    @Getter
    private final HealthLevel healthLevel;
    @Getter
    private final NoBreakBed noBreakBed;
    @Getter
    private final ResourceUpgrade resourceUpgrade;
    @Getter
    private final Holographic holographic;
    @Getter
    private final TeamShop teamShop;
    @Getter
    private final InvisibilityPlayer invisiblePlayer;
    @Getter
    private final LobbyBlock lobbyBlock;
    @Getter
    private final Respawn respawn;
    @Getter
    private final Actionbar actionbar;
    @Getter
    private final Graffiti graffiti;
    @Getter
    private final GameChest gameChest;
    @Getter
    private final Rejoin rejoin;
    @Getter
    private final TimeTask timeTask;
    private final List<BukkitTask> gameTasks;
    @Getter
    private Shop shop;
    private Boolean isOver;
    // 用于获取最终结算时 排列玩家击杀数 标题“最终击杀”的队伍颜色...==
    @Getter
    private Map<String, Team> playerNameTeams = new HashMap<>();
    // 连杀记录
    @Getter
    private Map<UUID, Integer> killStreaks;
    @Getter
    private Map<UUID, Integer> highestKillStreaks;

    public Arena(Game game) {
        Main.getInstance().getArenaManager().addArena(game.getName(), this);
        this.game = game;
        gameTasks = new ArrayList<BukkitTask>();
        playerGameStorage = new PlayerGameStorage(this);
        scoreBoard = new ScoreBoard(this);
        deathMode = new DeathMode(this);
        healthLevel = new HealthLevel(this);
        noBreakBed = new NoBreakBed(this);
        resourceUpgrade = new ResourceUpgrade(this);
        holographic = new Holographic(this, resourceUpgrade);
        teamShop = new TeamShop(this);
        invisiblePlayer = new InvisibilityPlayer(this);
        lobbyBlock = new LobbyBlock(this);
        respawn = new Respawn(this);
        actionbar = new Actionbar(this);
        graffiti = new Graffiti(this);
        gameChest = new GameChest(this);
        rejoin = new Rejoin(this);
        if (Main.getInstance().isEnabledCitizens()) {
            shop = new Shop(this);
        }
        timeTask = new TimeTask(this);
        isOver = false;
        addGameTask(new BukkitRunnable() {
            @Override
            public void run() {
                if (!game.getState().equals(GameState.RUNNING)) {
                    onOver(new BedwarsGameOverEvent(game, null));
                    onEnd();
                }
            }
        }.runTaskTimer(Main.getInstance(), 1L, 1L));
        for (Player player : game.getPlayers()) {
            playerNameTeams.put(player.getName(), game.getPlayerTeam(player));
        }
        killStreaks = new HashMap<>();
        highestKillStreaks = new HashMap<>();
    }

    public void addGameTask(BukkitTask task) {
        gameTasks.add(task);
    }

    public Boolean isOver() {
        return isOver;
    }

    public void onTargetBlockDestroyed(BedwarsTargetBlockDestroyedEvent e) {
        if (!isAlivePlayer(e.getPlayer())) {
            return;
        }
        Map<String, Integer> beds = playerGameStorage.getPlayerBeds();
        Player player = e.getPlayer();
        if (beds.containsKey(player.getName())) {
            beds.put(player.getName(), beds.get(player.getName()) + 1);
        } else {
            beds.put(player.getName(), 1);
        }
        holographic.onTargetBlockDestroyed(e);
    }

    public void onDeath(Player player) {
        invisiblePlayer.removePlayer(player);
        if (!isGamePlayer(player)) {
            return;
        }
        Map<String, Integer> dies = playerGameStorage.getPlayerDies();
        if (dies.containsKey(player.getName())) {
            dies.put(player.getName(), dies.get(player.getName()) + 1);
        } else {
            dies.put(player.getName(), 1);
        }
        PlaySound.playSound(player, Config.play_sound_sound_death);
        teamShop.removeImmunePlayer(player);
    }

    public void onDamage(EntityDamageEvent e) {
        respawn.onDamage(e);
    }

    public void onInteractEntity(PlayerInteractEntityEvent e) {
        graffiti.onInteractEntity(e);
    }

    public void onInteract(PlayerInteractEvent e) {
        gameChest.onInteract(e);
    }

    public void onHangingBreak(HangingBreakEvent e) {
        graffiti.onHangingBreak(e);
    }

    public void onRespawn(Player player) {
        if (!isGamePlayer(player)) {
            return;
        }
        respawn.onRespawn(player, false);
    }

    public void onPlayerKilled(BedwarsPlayerKilledEvent e) {
        if (!isGamePlayer(e.getPlayer()) || !isGamePlayer(e.getKiller())) {
            return;
        }
        Player player = e.getPlayer();
        Player killer = e.getKiller();
        if (!game.getPlayers().contains(player) || !game.getPlayers().contains(killer) || game.isSpectator(player) || game.isSpectator(killer)) {
            return;
        }
        Map<String, Integer> totalkills = playerGameStorage.getPlayerTotalKills();
        Map<String, Integer> kills = playerGameStorage.getPlayerKills();
        Map<String, Integer> finalkills = playerGameStorage.getPlayerFinalKills();
        if (!game.getPlayerTeam(player).isDead(game)) {
            if (kills.containsKey(killer.getName())) {
                kills.put(killer.getName(), kills.get(killer.getName()) + 1);
            } else {
                kills.put(killer.getName(), 1);
            }
        }
        if (game.getPlayerTeam(player).isDead(game)) {
            if (finalkills.containsKey(killer.getName())) {
                finalkills.put(killer.getName(), finalkills.get(killer.getName()) + 1);
            } else {
                finalkills.put(killer.getName(), 1);
            }
        }
        if (totalkills.containsKey(killer.getName())) {
            totalkills.put(killer.getName(), totalkills.get(killer.getName()) + 1);
        } else {
            totalkills.put(killer.getName(), 1);
        }
        PlaySound.playSound(killer, Config.play_sound_sound_kill);

        // 连杀
        this.addKillStreak(killer.getUniqueId()); // 增加连杀

        // 重置被击杀者 连杀
        if (this.getKillStreak(player.getUniqueId()) > 0) {
            this.resetKillStreak(player.getUniqueId());
        }
    }

    public void onOver(BedwarsGameOverEvent e) {
        if (e.getGame().getName().equals(this.game.getName())) {
            isOver = true;
            if (Config.overstats_enabled && e.getWinner() != null) {
                Team winner = e.getWinner();
                Map<String, Integer> totalkills = playerGameStorage.getPlayerTotalKills();
                Map<Integer, List<String>> player_kills = new HashMap<Integer, List<String>>();
                totalkills.forEach((name, kills) -> {
                    List<String> players = player_kills.getOrDefault(kills, new ArrayList<>());
                    players.add(name);
                    player_kills.put(kills, players);
                });
                List<Integer> kills_top = new ArrayList<>(player_kills.keySet());
                Collections.sort(kills_top);
                Collections.reverse(kills_top);
                List<String> player_rank_name = new ArrayList<>();
                List<Integer> player_rank_kills = new ArrayList<>();
                for (Integer kills : kills_top) {
                    for (String name : player_kills.get(kills)) {
                        if (player_rank_name.size() < 3) {
                            player_rank_name.add(name);
                            player_rank_kills.add(kills);
                        } else {
                            break;
                        }
                    }
                }
                int size = player_rank_name.size();
                for (int i = 0; i < 3 - size; i++) {
                    player_rank_name.add("无");
                    player_rank_kills.add(0);
                }
                StringBuilder win_team_player_list = new StringBuilder();
                for (Player player : winner.getPlayers()) {
                    win_team_player_list.append((win_team_player_list.length() > 0) ? ", " + player.getName() : player.getName());
                }

                // 结算 Title
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (Player player : game.getPlayers()) {
                            Utils.sendTitle(player, 20, 40, 20, "&e游戏结束", "&e正在统计本局比赛..");
                        }
                    }
                }.runTaskLater(Main.getInstance(), 120L);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (Player player : game.getPlayers()) {
                            Utils.sendTitle(player, 20, 40, 20, "&c最高连杀： " + getHighestKillStreak(player.getUniqueId()), "&eKDA： &c" + calculateSpecialKda(player));
                        }
                    }
                }.runTaskLater(Main.getInstance(), 200L);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        String firstKillerName = player_rank_name.get(0);
                        ChatColor firstKillerTeamColor = ChatColor.WHITE;
                        Team firstKillerTeam = playerNameTeams.get(firstKillerName);
                        if (firstKillerTeam != null) {
                            firstKillerTeamColor = firstKillerTeam.getChatColor();
                        }
                        for (Player player : game.getPlayers()) {
                            Utils.sendTitle(player, 20, 40, 20, firstKillerTeamColor + firstKillerName, "&d&l全&e&l场&c&l最&b&l佳");
                        }
                    }
                }.runTaskLater(Main.getInstance(), 280L);

                for (Player player : game.getPlayers()) {
                    for (String msg : Config.overstats_message) {
                        if (msg.isEmpty()) break;
                        msg = PlaceholderAPIUtil.setPlaceholders(player, msg);
                        player.sendMessage(msg.replace("{color}", winner.getChatColor() + "").replace("{win_team}", winner.getName()).replace("{win_team_players}", win_team_player_list.toString()).replace("{first_1_kills_player}", player_rank_name.get(0)).replace("{first_2_kills_player}", player_rank_name.get(1)).replace("{first_3_kills_player}", player_rank_name.get(2)).replace("{first_1_kills}", player_rank_kills.get(0) + "").replace("{first_2_kills}", player_rank_kills.get(1) + "").replace("{first_3_kills}", player_rank_kills.get(2) + ""));
                    }
                }
            }
        }
    }

    public void onEnd() {
        gameTasks.forEach(BukkitTask::cancel);
        teamShop.onEnd();
        noBreakBed.onEnd();
        holographic.remove();
        if (Main.getInstance().isEnabledCitizens()) {
            shop.remove();
        }
        graffiti.reset();
        gameChest.clearChest();
        playerNameTeams = null;
        killStreaks = null;
        highestKillStreaks = null;
    }

    public void onDisable() {
        holographic.remove();
        if (Main.getInstance().isEnabledCitizens()) {
            shop.remove();
        }
        graffiti.reset();
        gameChest.clearChest();
    }

    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
        holographic.onArmorStandManipulate(e);
    }

    public void onClick(InventoryClickEvent e) {
        teamShop.onClick(e);
    }

    public void onItemMerge(ItemMergeEvent e) {
        if (!Config.item_merge && game.getRegion().isInRegion(e.getEntity().getLocation())) {
            e.setCancelled(true);
        }
    }

    public void onPlayerLeave(Player player) {
        holographic.onPlayerLeave(player);
        if (Config.rejoin_enabled) {
            if (game.getState() == GameState.RUNNING && !game.isSpectator(player)) {
                Team team = game.getPlayerTeam(player);
                if (team != null) {
                    if (team.getPlayers().size() > 1 && !team.isDead(game)) {
                        rejoin.addPlayer(player);
                        return;
                    }
                }
            }
            rejoin.removePlayer(player.getName());
        }
        respawn.onPlayerLeave(player);
        // teamShop.removeTriggeredPlayer(player);
        teamShop.removeImmunePlayer(player);
    }

    public void onPlayerJoined(Player player) {
        if (Config.rejoin_enabled) {
            rejoin.rejoin(player);
        }
        respawn.onPlayerJoined(player);
        holographic.onPlayerJoin(player);
        graffiti.onPlayerJoin(player);
        if (Main.getInstance().isEnabledCitizens()) {
            shop.onPlayerJoined(player);
        }
    }

    public void onOpenShop(BedwarsOpenShopEvent e) {
        if (Main.getInstance().isEnabledCitizens()) {
            shop.onOpenShop(e);
        }
    }

    private Boolean isGamePlayer(Player player) {
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null) {
            return false;
        }
        if (!game.getName().equals(this.game.getName())) {
            return false;
        }
        return !game.isSpectator(player);
    }

    private Boolean isAlivePlayer(Player player) {
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null) {
            return false;
        }
        if (!game.getName().equals(this.game.getName())) {
            return false;
        }
        return !BedwarsUtil.isSpectator(game, player);
    }

    // 获取当前连杀数
    public int getKillStreak(UUID playerId) {
        return killStreaks.getOrDefault(playerId, 0);
    }

    // 增加连杀数
    public void addKillStreak(UUID playerId) {
        int currentStreak = getKillStreak(playerId) + 1;
        killStreaks.put(playerId, currentStreak);

        // 检查并更新最高连杀记录
        if (currentStreak > getHighestKillStreak(playerId)) {
            setHighestKillStreak(playerId, currentStreak);
        }
    }

    // 重置连杀数
    public void resetKillStreak(UUID playerId) {
        killStreaks.put(playerId, 0);
    }

    // 获取最高连杀记录
    public int getHighestKillStreak(UUID playerId) {
        return highestKillStreaks.getOrDefault(playerId, 0);
    }

    // 设置最高连杀记录
    public void setHighestKillStreak(UUID playerId, int streak) {
        highestKillStreaks.put(playerId, streak);
    }

    // 哈基米给的 我实在没能想出来花雨庭怎么算出来的KDA
    // “我正在玩一款游戏 9杀 1死 最高4连杀 5张床 请帮我想一想她是怎么得出我的KDA是34.0的”
    // “所以，你在该场游戏中的数据很可能是：9次击杀、1次死亡和25次助攻。”
    // “可我没有25次助攻”
    // “让我们来做一个合理的推测：在这个游戏中，破坏一张床（Bed Destroyed）会被换算成等同于数次击杀（Kill）的分数。”
    // 可能最终击杀分数也有影响？

    /**
     * 计算特殊模式下的KDA，其中“破床”有额外加分。
     * <p>
     * 在这个算法中：
     * - 每次“击杀”得1分。
     * - 每次“破床”得5分。
     * - KDA = (总得分) / 死亡次数
     * <p>
     * //     * @param kills          击杀数。
     * //     * @param deaths         死亡数。
     * //     * @param bedsDestroyed  破床数。
     *
     * @return 计算出的KDA值。
     */
//    public static double calculateSpecialKda(int kills, int deaths, int bedsDestroyed) {
    public double calculateSpecialKda(Player player) {
        int kills = playerGameStorage.getPlayerTotalKills().getOrDefault(player.getName(), 0);
        int deaths = playerGameStorage.getPlayerDies().getOrDefault(player.getName(), 0);
        int bedsDestroyed = playerGameStorage.getPlayerBeds().getOrDefault(player.getName(), 0);
        // 计算总分，破床数乘以5，然后加上击杀数
        int totalScore = kills + (bedsDestroyed * 5);

        // 为了避免除以零的错误，如果死亡数为0，KDA就等于总分
        if (deaths == 0) {
            return totalScore;
        } else {
            // 否则，KDA = 总分 / 死亡数
            // 注意：需要将其中一个操作数转换为double，以确保结果是浮点数而不是整数
            double rawKda = (double) totalScore / deaths;
            return Math.round(rawKda * 100.0) / 100.0;
        }
    }
}
