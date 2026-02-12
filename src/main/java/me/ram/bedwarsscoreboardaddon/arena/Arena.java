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
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.storage.PlayerGameStorage;
import me.ram.bedwarsscoreboardaddon.utils.BedwarsUtil;
import me.ram.bedwarsscoreboardaddon.utils.PlaceholderAPIUtil;
import me.ram.bedwarsscoreboardaddon.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
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
    // 需要刷新的游戏资讯类 优先级较高
    @Getter
    private PlayerGameStorage playerGameStorage;
    @Getter
    private DeathMode deathMode;
    @Getter
    private HealthBar healthBar;
    @Getter
    private NoBreakBed noBreakBed;
    @Getter
    private ResourceUpgrade resourceUpgrade;
    @Getter
    private Holographic holographic;
    @Getter
    private Graffiti graffiti;
    @Getter
    private RandomEvents randomEventsManager;
    @Getter
    private WitherBow witherBow;
    @Getter
    private Respawn respawn;
    @Getter
    private HealthLevel healthLevel;
    @Getter
    private InvisibilityPlayer invisiblePlayer;

    @Getter
    private Actionbar actionbar;
    @Getter
    private ScoreBoard scoreBoard;
    @Getter
    private TimeTask timeTask;

    //
    @Getter
    private AntiBedGapBreak antiBedGapBreak;
    @Getter
    private LobbyBlock lobbyBlock;
    @Getter
    private GameChest gameChest;
    @Getter
    private Rejoin rejoin;
    private List<BukkitTask> gameTasks;
    @Getter
    private Shop shop;
    @Getter
    private boolean isOver = false;
    @Getter
    private KillStreak killStreak;
    // 用于获取最终结算时 排列玩家击杀数 标题"最终击杀"的队伍颜色...
    @Getter
    private Map<String, Team> playerNameTeams = new HashMap<>();
    @Getter
    private FriendlyBreak friendlyBreak;
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

        // 需要刷新的游戏资讯类 优先级较高
        playerGameStorage = new PlayerGameStorage(this);
        deathMode = new DeathMode(this);
        healthBar = new HealthBar(this);
        noBreakBed = new NoBreakBed(this);
        resourceUpgrade = new ResourceUpgrade(this);
        holographic = new Holographic(this, resourceUpgrade);
        graffiti = new Graffiti(this);
        randomEventsManager = new RandomEvents(this);
        witherBow = new WitherBow(this);
        respawn = new Respawn(this);
        healthLevel = new HealthLevel(this);
        invisiblePlayer = new InvisibilityPlayer(this);

        // 游戏资讯刷新 务必将其放在靠后位置 否则容易空指针
        actionbar = new Actionbar(this);
        scoreBoard = new ScoreBoard(this);
        timeTask = new TimeTask(this);
        //
        antiBedGapBreak = new AntiBedGapBreak(this);
        lobbyBlock = new LobbyBlock(this);
        gameChest = new GameChest(this);
        rejoin = new Rejoin(this);
        if (Main.getInstance().isEnabledCitizens()) {
            shop = new Shop(this);
        }
        killStreak = new KillStreak(this);
        friendlyBreak = new FriendlyBreak(this);
        teleportTask = new TeleportTask(this);
    }

    public void addGameTask(BukkitTask task) {
        gameTasks.add(task);
    }

    public void onTargetBlockDestroyed(BedwarsTargetBlockDestroyedEvent e) {
        Player player = e.getPlayer();
        if (!isAlivePlayer(player)) {
            return;
        }
        Map<String, Integer> beds = playerGameStorage.getPlayerBeds();
        beds.put(player.getName(), beds.getOrDefault(player.getName(), 0) + 1);
        holographic.onTargetBlockDestroyed(e);
        scoreBoard.updateScoreboard();
    }

    public void onDeath(Player player) {
        invisiblePlayer.removePlayer(player);
        Map<String, Integer> dies = playerGameStorage.getPlayerDies();
        dies.put(player.getName(), dies.getOrDefault(player.getName(), 0) + 1);
        PlaySound.playSound(player, Config.play_sound_sound_death);
        killStreak.resetKillStreak(player.getUniqueId());
        scoreBoard.updateScoreboard();
    }

    public void onDamage(EntityDamageEvent e) {
        if (isAlivePlayer(game, (Player) e.getEntity())) {
            respawn.onDamage(e);
        }
    }

    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (isAlivePlayer(game, (Player) e.getEntity()) && isAlivePlayer(game, (Player) e.getDamager())) {
            respawn.onPlayerAttack(e);
        }
    }

    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (isAlivePlayer(game, e.getPlayer())) {
            graffiti.onInteractEntity(e);
        }
    }

    public void onInteract(PlayerInteractEvent e) {
        if (isAlivePlayer(game, e.getPlayer())) {
            gameChest.onInteract(e);
        }
    }

    public void onHangingBreak(HangingBreakEvent e) {
        graffiti.onHangingBreak(e);
    }

    public void onRespawn(Player player) {
        respawn.onRespawn(player, false);
    }

    public void onPlayerKilled(BedwarsPlayerKilledEvent e) {
        Player player = e.getPlayer();
        Player killer = e.getKiller();
        if (!isAlivePlayer(player) || !isAlivePlayer(killer)) {
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
        scoreBoard.updateScoreboard();
    }

    public void onOver(BedwarsGameOverEvent e) {
        if (!e.getGame().getName().equals(this.game.getName())) return;
        isOver = true;
        timeTask.refresh();
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), scoreBoard::updateScoreboard, 20L);
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

            if (!Config.overstats_message.isEmpty()) {
                for (Player player : game.getPlayers()) {
                    for (String msg : Config.overstats_message) {
                        msg = PlaceholderAPIUtil.setPlaceholders(player, msg);
                        player.sendMessage(msg.replace("{color}", winner.getChatColor() + "").replace("{win_team}", winner.getName()).replace("{win_team_players}", win_team_player_list.toString()).replace("{first_1_kills_player}", player_rank_name.get(0)).replace("{first_2_kills_player}", player_rank_name.get(1)).replace("{first_3_kills_player}", player_rank_name.get(2)).replace("{first_1_kills}", player_rank_kills.get(0) + "").replace("{first_2_kills}", player_rank_kills.get(1) + "").replace("{first_3_kills}", player_rank_kills.get(2) + ""));
                    }
                }
            }
        }
    }

    public void onEnd() {
        gameTasks.forEach(BukkitTask::cancel);
        gameTasks = null;

        playerGameStorage = null;
        deathMode = null;
        healthBar.onEnd();
        healthBar = null;
        noBreakBed.onEnd();
        noBreakBed = null;
        resourceUpgrade = null;
        holographic.remove();
        holographic = null;
        graffiti.reset();
        graffiti = null;
        randomEventsManager = null;
        witherBow.onEnd();
        witherBow = null;
        respawn = null;
        healthLevel = null;
        invisiblePlayer.onEnd();
        invisiblePlayer = null;

        actionbar = null;
        scoreBoard = null;
        timeTask = null;

        antiBedGapBreak.onEnd();
        antiBedGapBreak = null;
        lobbyBlock = null;
        gameChest.clearChest();
        gameChest = null;
        rejoin = null;
        if (Main.getInstance().isEnabledCitizens()) {
            shop.remove();
            shop = null;
        }
        killStreak = null;
        playerNameTeams = null;
        friendlyBreak.onEnd();
        friendlyBreak = null;
        teleportTask.stopTask();
        teleportTask = null;

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

    public Boolean isGame(Game game) {
        return game != null && game.getName().equals(this.game.getName());
    }

    public Boolean isGamePlayer(Player player) {
        return isGame(BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player)) && !this.game.isSpectator(player);
    }

    public Boolean isAlivePlayer(Player player) {
        return isGame(BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player)) && !BedwarsUtil.isSpectator(this.game, player);
    }

    public Boolean isGamePlayer(Game game, Player player) {
        return isGame(game) && !this.game.isSpectator(player);
    }

    public Boolean isAlivePlayer(Game game, Player player) {
        return isGame(game) && !BedwarsUtil.isSpectator(this.game, player);
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
