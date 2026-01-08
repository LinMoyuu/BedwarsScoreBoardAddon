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
import lombok.Setter;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.addon.*;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.storage.PlayerGameStorage;
import me.ram.bedwarsscoreboardaddon.utils.BedwarsUtil;
import me.ram.bedwarsscoreboardaddon.utils.ColorUtil;
import me.ram.bedwarsscoreboardaddon.utils.PlaceholderAPIUtil;
import me.ram.bedwarsscoreboardaddon.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class Arena {
    @Getter
    private final Game game;
    @Getter
    private final ScoreBoard scoreBoard;
    @Getter
    private PlayerGameStorage playerGameStorage;
    @Getter
    private DeathMode deathMode;
    @Getter
    private HealthLevel healthLevel;
    @Getter
    private NoBreakBed noBreakBed;
    @Getter
    private ResourceUpgrade resourceUpgrade;
    @Getter
    private Holographic holographic;
    @Getter
    private InvisibilityPlayer invisiblePlayer;
    @Getter
    private LobbyBlock lobbyBlock;
    @Getter
    private Respawn respawn;
    @Getter
    private Actionbar actionbar;
    @Getter
    private Graffiti graffiti;
    @Getter
    private GameChest gameChest;
    @Getter
    private Rejoin rejoin;
    @Getter
    private TimeTask timeTask;
    private List<BukkitTask> gameTasks;
    @Getter
    private Shop shop;
    @Getter
    private boolean isOver = false;
    @Getter
    @Setter
    private boolean enabledWitherBow = false;
    @Getter
    private KillStreak killStreak;
    // 用于获取最终结算时 排列玩家击杀数 标题“最终击杀”的队伍颜色...==
    @Getter
    private Map<String, Team> playerNameTeams = new HashMap<>();
    @Getter
    private RandomEvents randomEventsManager;
    // 破坏队友脚下方块次数
    private HashMap<Player, Integer> friendlyBreakCount;
    // 随机传送
    @Getter
    private TeleportTask teleportTask;

    public Arena(Game game) {
        Main.getInstance().getArenaManager().addArena(game.getName(), this);
        this.game = game;
        World gameWorld = game.getRegion().getWorld();
        gameWorld.setGameRuleValue("doDaylightCycle", "false");
        gameWorld.setGameRuleValue("doWeatherCycle", "false");
        gameWorld.setGameRuleValue("doFireTick", "false");
        gameWorld.setGameRuleValue("doMobSpawning", "false");
        gameTasks = new ArrayList<>();
        playerGameStorage = new PlayerGameStorage(this);
        scoreBoard = new ScoreBoard(this);
        deathMode = new DeathMode(this);
        healthLevel = new HealthLevel(this);
        noBreakBed = new NoBreakBed(this);
        resourceUpgrade = new ResourceUpgrade(this);
        holographic = new Holographic(this, resourceUpgrade);
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
        killStreak = new KillStreak(this);
        randomEventsManager = new RandomEvents(this);

        friendlyBreakCount = new HashMap<>();
        teleportTask = new TeleportTask(this);
    }

    public void addGameTask(BukkitTask task) {
        gameTasks.add(task);
    }

    public void onTargetBlockDestroyed(BedwarsTargetBlockDestroyedEvent e) {
        if (!isAlivePlayer(e.getPlayer())) {
            return;
        }
        Map<String, Integer> beds = playerGameStorage.getPlayerBeds();
        Player player = e.getPlayer();
        beds.put(player.getName(), beds.getOrDefault(player.getName(), 0) + 1);
        holographic.onTargetBlockDestroyed(e);
    }

    public void onDeath(Player player) {
        invisiblePlayer.removePlayer(player);
        if (!isGamePlayer(player)) {
            return;
        }
        Map<String, Integer> dies = playerGameStorage.getPlayerDies();
        dies.put(player.getName(), dies.getOrDefault(player.getName(), 0) + 1);
        PlaySound.playSound(player, Config.play_sound_sound_death);
        killStreak.resetKillStreak(player.getUniqueId());
    }

    public void onDamage(EntityDamageEvent e) {
        respawn.onDamage(e);
    }

    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        respawn.onPlayerAttack(e);
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

    public void onFriendlyBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Team team = game.getPlayerTeam(player);
        if (team == null) return;
        friendlyBreakCount.put(player, friendlyBreakCount.getOrDefault(player, 0) + 1);
        int friendlyBreaks = friendlyBreakCount.getOrDefault(player, 0);
        int max_breaks = Config.friendlybreak_kick_max_breaks;

        player.sendMessage(ColorUtil.color(Config.friendlybreak_warning_message
                .replace("{bwprefix}", Config.bwrelPrefix)
                .replace("{breakcount}", String.valueOf(friendlyBreaks))
                .replace("{max_breaks}", String.valueOf(max_breaks))));

        if (friendlyBreaks >= max_breaks) {
            friendlyBreakCount.remove(player);
            player.kickPlayer(ColorUtil.color(Config.friendlybreak_kick_message
                    .replace("{bwprefix}", Config.bwrelPrefix)
                    .replace("{breakcount}", String.valueOf(friendlyBreaks))
                    .replace("{max_breaks}", String.valueOf(max_breaks))));

            String broadCastMessage = ColorUtil.color(Config.friendlybreak_broadcast_message
                    .replace("{bwprefix}", Config.bwrelPrefix)
                    .replace("{breakcount}", String.valueOf(friendlyBreaks))
                    .replace("{max_breaks}", String.valueOf(max_breaks))
                    .replace("{playername}", player.getName())
                    .replace("{playerdisplayname}", player.getDisplayName())
                    .replace("{team}", team.getDisplayName())
                    .replace("{teamcolor}", team.getChatColor().toString()));
            for (Player gamePlayer : game.getPlayers()) {
                gamePlayer.sendMessage(broadCastMessage);
            }
        }
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
            kills.put(killer.getName(), kills.getOrDefault(killer.getName(), 0) + 1);
        }
        if (game.getPlayerTeam(player).isDead(game)) {
            finalkills.put(killer.getName(), finalkills.getOrDefault(killer.getName(), 0) + 1);
        }
        totalkills.put(killer.getName(), totalkills.getOrDefault(killer.getName(), 0) + 1);
        PlaySound.playSound(killer, Config.play_sound_sound_kill);
        killStreak.onKill(player, killer);
    }

    public void onOver(BedwarsGameOverEvent e) {
        isOver = true;
        timeTask.refresh();
        if (!e.getGame().getName().equals(this.game.getName())) return;
        if (Config.overstats_enabled && e.getWinner() != null) {
            Team winner = e.getWinner();
            Map<String, Integer> totalkills = playerGameStorage.getPlayerTotalKills();
            Map<Integer, List<String>> player_kills = new HashMap<>();
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
            long baseDelay = 120L;
            long delayBetween = 80L;
            // 第一阶段
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> game.getPlayers().forEach(player ->
                    Utils.sendTitle(player, 20, 40, 20, "&e游戏结束", "&e正在统计本局比赛..")), baseDelay);

            // 第二阶段
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> game.getPlayers().forEach(player ->
                    Utils.sendTitle(player, 20, 40, 20,
                            "&c最高连杀： " + killStreak.getKillStreaks(player.getUniqueId()),
                            "&eKDA： &c" + calculateSpecialKda(player))), baseDelay + delayBetween);

            // 第三阶段
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                String firstKillerName = player_rank_name.get(0);
                ChatColor firstKillerTeamColor = ChatColor.WHITE;
                Team firstKillerTeam = playerNameTeams.get(firstKillerName);
                if (firstKillerTeam != null) {
                    firstKillerTeamColor = firstKillerTeam.getChatColor();
                }

                final ChatColor finalColor = firstKillerTeamColor;
                game.getPlayers().forEach(player ->
                        Utils.sendTitle(player, 20, 40, 20,
                                finalColor + firstKillerName,
                                "&d&l全&e&l场&c&l最&b&l佳"));
            }, baseDelay + 2 * delayBetween);

            for (Player player : game.getPlayers()) {
                for (String msg : Config.overstats_message) {
                    if (msg.isEmpty()) break;
                    msg = PlaceholderAPIUtil.setPlaceholders(player, msg);
                    player.sendMessage(msg.replace("{color}", winner.getChatColor() + "").replace("{win_team}", winner.getName()).replace("{win_team_players}", win_team_player_list.toString()).replace("{first_1_kills_player}", player_rank_name.get(0)).replace("{first_2_kills_player}", player_rank_name.get(1)).replace("{first_3_kills_player}", player_rank_name.get(2)).replace("{first_1_kills}", player_rank_kills.get(0) + "").replace("{first_2_kills}", player_rank_kills.get(1) + "").replace("{first_3_kills}", player_rank_kills.get(2) + ""));
                }
            }
        }
    }

    public void onEnd() {
        gameTasks.forEach(BukkitTask::cancel);
        noBreakBed.onEnd();
        holographic.remove();
        if (Main.getInstance().isEnabledCitizens()) {
            shop.remove();
        }
        invisiblePlayer.onEnd();
        graffiti.reset();
        gameChest.clearChest();
        teleportTask.stopTask();
        playerGameStorage = null;
        deathMode = null;
        healthLevel = null;
        resourceUpgrade = null;
        lobbyBlock = null;
        respawn = null;
        gameTasks = null;
        actionbar = null;
        invisiblePlayer = null;
        noBreakBed = null;
        holographic = null;
        gameChest = null;
        shop = null;
        graffiti = null;
        rejoin = null;
        timeTask = null;
        playerNameTeams = null;
        killStreak = null;
        friendlyBreakCount = null;
        teleportTask = null;
        randomEventsManager = null;
        Main.getInstance().getArenaManager().removeArena(game.getName());
    }

    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
        holographic.onArmorStandManipulate(e);
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
        gameChest.onPlayerLeave(player);
        respawn.onPlayerLeave(player);
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
