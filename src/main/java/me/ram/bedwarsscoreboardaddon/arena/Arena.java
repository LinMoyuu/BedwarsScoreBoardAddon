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
    @Getter
    private boolean isOver;
    //
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
    //
    @Getter
    private Actionbar actionbar;
    @Getter
    private ScoreBoard scoreBoard;
    @Getter
    private TimeTask timeTask;

    // 缝隙拆床
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
    // 连杀
    @Getter
    private KillStreak killStreak;
    // 用于获取最终结算时 排列玩家击杀数 标题"最终击杀"的队伍颜色...
    @Getter
    private Map<String, Team> playerNameTeams = new HashMap<>();
    // 恶意破坏方块
    @Getter
    private FriendlyBreak friendlyBreak;
    // 随机传送
    @Getter
    private TeleportTask teleportTask;

    public Arena(Game game) {
        Main.getInstance().getArenaManager().addArena(game.getName(), this);
        this.game = game;
        this.isOver = false;
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
        isOver = true;
        // 刷新...
        timeTask.refresh();
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), scoreBoard::updateScoreboard, 20L);
        // 恢复边界
        deathMode.onOver();
        // 战绩结算
        if (Config.overstats_enabled && e.getWinner() != null) {
            Team winner = e.getWinner();

            // 计算击杀数排名
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
            List<String> player_kill_rank_name = new ArrayList<>();
            List<Integer> player_kill_rank_kills = new ArrayList<>();
            for (Integer kills : kills_top) {
                for (String name : player_kills.get(kills)) {
                    if (player_kill_rank_name.size() < 3) {
                        player_kill_rank_name.add(name);
                        player_kill_rank_kills.add(kills);
                    } else {
                        break;
                    }
                }
            }

            // 计算KDA
            Set<String> allPlayers = new HashSet<>();
            allPlayers.addAll(totalkills.keySet());
            allPlayers.addAll(playerGameStorage.getPlayerBeds().keySet());
            Map<String, Double> playerKdas = new HashMap<>();
            for (String playerName : allPlayers) {
                double kda = calculateSpecialKda(playerName);
                playerKdas.put(playerName, kda);
            }

            // 按KDA排序获取前三名
            List<Map.Entry<String, Double>> sortedKdas = new ArrayList<>(playerKdas.entrySet());
            sortedKdas.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

            List<String> player_kda_rank_name = new ArrayList<>();
            List<Double> player_kda_rank_kda = new ArrayList<>();

            for (int i = 0; i < Math.min(3, sortedKdas.size()); i++) {
                Map.Entry<String, Double> entry = sortedKdas.get(i);
                player_kda_rank_name.add(entry.getKey());
                player_kda_rank_kda.add(entry.getValue());
            }

            // 填充不足3人的空位
            int killSize = player_kill_rank_name.size();
            for (int i = 0; i < 3 - killSize; i++) {
                player_kill_rank_name.add("无");
                player_kill_rank_kills.add(0);
            }

            int kdaSize = player_kda_rank_name.size();
            for (int i = 0; i < 3 - kdaSize; i++) {
                player_kda_rank_name.add("无");
                player_kda_rank_kda.add(0.0);
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
                            "&eKDA： &c" + calculateSpecialKda(player.getName()))), baseDelay + delayBetween);

            // 第三阶段
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                String firstKdaPlayerName = player_kda_rank_name.get(0);

                ChatColor firstKdaPlayerTeamColor = ChatColor.WHITE;
                Team firstKdaPlayerTeam = playerNameTeams.get(firstKdaPlayerName);
                if (firstKdaPlayerTeam != null) {
                    firstKdaPlayerTeamColor = firstKdaPlayerTeam.getChatColor();
                }

                final ChatColor finalColor = firstKdaPlayerTeamColor;
                game.getPlayers().forEach(player ->
                        Utils.sendTitle(player, 20, 40, 20,
                                finalColor + firstKdaPlayerName,
                                "&d&l全&e&l场&c&l最&b&l佳"));
            }, baseDelay + 2 * delayBetween);

            if (!Config.overstats_message.isEmpty()) {
                for (Player player : game.getPlayers()) {
                    for (String msg : Config.overstats_message) {
                        msg = PlaceholderAPIUtil.setPlaceholders(player, msg);
                        player.sendMessage(msg.replace("{color}", winner.getChatColor() + "")
                                .replace("{win_team}", winner.getName())
                                .replace("{win_team_players}", win_team_player_list.toString())
                                .replace("{first_1_kills_player}", player_kill_rank_name.get(0))
                                .replace("{first_2_kills_player}", player_kill_rank_name.get(1))
                                .replace("{first_3_kills_player}", player_kill_rank_name.get(2))
                                .replace("{first_1_kills}", player_kill_rank_kills.get(0) + "")
                                .replace("{first_2_kills}", player_kill_rank_kills.get(1) + "")
                                .replace("{first_3_kills}", player_kill_rank_kills.get(2) + "")
                                .replace("{first_1_kda_player}", player_kda_rank_name.get(0))
                                .replace("{first_2_kda_player}", player_kda_rank_name.get(1))
                                .replace("{first_3_kda_player}", player_kda_rank_name.get(2))
                                .replace("{first_1_kda}", String.format("%.2f", player_kda_rank_kda.get(0)))
                                .replace("{first_2_kda}", String.format("%.2f", player_kda_rank_kda.get(1)))
                                .replace("{first_3_kda}", String.format("%.2f", player_kda_rank_kda.get(2))));
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
        killStreak.onEnd();
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


    // 疑似一张床5分, 但不除以死亡数...
//    public static double calculateSpecialKda(int kills, int deaths, int bedsDestroyed) {
    public double calculateSpecialKda(String playerName) {
        int kills = playerGameStorage.getPlayerTotalKills().getOrDefault(playerName, 0);
        int bedsDestroyed = playerGameStorage.getPlayerBeds().getOrDefault(playerName, 0);
        return kills + (bedsDestroyed * 5);
    }
}
