package me.ram.bedwarsscoreboardaddon.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.google.common.collect.ImmutableMap;
import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.events.BedwarsGameEndEvent;
import io.github.bedwarsrel.events.BedwarsGameOverEvent;
import io.github.bedwarsrel.events.BedwarsGameStartedEvent;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.GameState;
import io.github.bedwarsrel.game.Team;
import io.github.bedwarsrel.utils.ChatWriter;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.utils.BedwarsUtil;
import me.ram.bedwarsscoreboardaddon.utils.ColorUtil;
import me.ram.bedwarsscoreboardaddon.utils.PlaceholderAPIUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 能改成这样也可以请高人了
public class GameMessageListener implements Listener {

    private final Map<String, Map<Event, PacketListener>> deathEvents = new HashMap<>();
    private final Map<String, Map<Event, PacketListener>> killedEvents = new HashMap<>();
    private final Map<String, Map<Event, PacketListener>> bedEvents = new HashMap<>();
    private final Map<String, Map<Event, PacketListener>> quitevents = new HashMap<>();
    private final Map<Player, Game> playerGameCache = new HashMap<>();

    // 死亡消息部分
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeathLowest(PlayerDeathEvent e) {
        if (!Config.death_chat_enabled) {
            return;
        }
        Player player = e.getEntity();
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null || game.getState() != GameState.RUNNING) {
            return;
        }
        Player killer = player.getKiller();
        if (killer != null) {
            return;
        }
        Player damager = game.getPlayerDamager(player);
        if (damager != null) {
            return;
        }
        Map<Event, PacketListener> map = deathEvents.getOrDefault(game.getName(), new HashMap<>());
        map.put(e, registerDeathPacketListener(player, game.getPlayerTeam(player)));
        deathEvents.put(game.getName(), map);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeathHighest(PlayerDeathEvent e) {
        if (!Config.death_chat_enabled) {
            return;
        }
        Player player = e.getEntity();
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null || game.getState() != GameState.RUNNING) {
            return;
        }
        Player killer = player.getKiller();
        if (killer != null) {
            return;
        }
        Player damager = game.getPlayerDamager(player);
        if (damager != null) {
            return;
        }
        Map<Event, PacketListener> map = deathEvents.getOrDefault(game.getName(), new HashMap<>());
        if (!map.containsKey(e)) {
            return;
        }
        ProtocolLibrary.getProtocolManager().removePacketListener(map.get(e));
        map.remove(e);
        deathEvents.put(game.getName(), map);
        String finalkilled = "";
        Team playerTeam = game.getPlayerTeam(player);
        if (game.isSpectator(player) || playerTeam.isDead(game)) {
            finalkilled = ColorUtil.color(Config.killed_chat_final);
        }

        String string = ColorUtil.color(PlaceholderAPIUtil.setPlaceholders(player, Config.death_chat_message
                .replace("{bwprefix}", Config.bwrelPrefix)
                .replace("{playerTeamString}", ChatColor.GOLD + "(" + playerTeam.getDisplayName() + ChatColor.GOLD + ")")
                .replace("{playerTeamColor}", playerTeam.getChatColor().toString())
                .replace("{playerTeam}", playerTeam.getDisplayName())
                .replace("{player}", player.getDisplayName())
                .replace("{final}", finalkilled)));
        for (Player p : game.getPlayers()) {
            if (p.isOnline()) {
                p.sendMessage(string);
            }
        }
    }

    private PacketListener registerDeathPacketListener(Player player, Team deathTeam) {
        PacketListener listener = new PacketAdapter(Main.getInstance(), PacketType.Play.Server.CHAT) {
            public void onPacketSending(PacketEvent e) {
                WrappedChatComponent chat = e.getPacket().getChatComponents().read(0);
                WrappedChatComponent[] chats = WrappedChatComponent.fromChatMessage(ChatWriter.pluginMessage(
                        ChatColor.GOLD + BedwarsRel._l(player, "ingame.player.died", ImmutableMap
                                .of("player",
                                        Game.getPlayerWithTeamString(player, deathTeam, ChatColor.GOLD)))));

                for (WrappedChatComponent c : chats) {
                    if (chat.getJson().equals(c.getJson())) {
                        e.setCancelled(true);
                        break;
                    }
                }
            }
        };
        ProtocolLibrary.getProtocolManager().addPacketListener(listener);
        return listener;
    }

    // 死亡消息部分结束
    // 击杀消息部分

    @EventHandler(priority = EventPriority.LOWEST)
    public void onKilledLowest(PlayerDeathEvent e) {
        if (!Config.killed_chat_enabled) {
            return;
        }
        Player player = e.getEntity();
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null || game.getState() != GameState.RUNNING) {
            return;
        }
        Player killer = player.getKiller();
        if (killer == null) {
            killer = game.getPlayerDamager(player);
            if (killer == null) {
                return;
            }
        }
        Map<Event, PacketListener> map = killedEvents.getOrDefault(game.getName(), new HashMap<>());
        map.put(e, registerKilledPacketListener(killer, game.getPlayerTeam(killer), player, game.getPlayerTeam(player)));
        killedEvents.put(game.getName(), map);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onKilledHighest(PlayerDeathEvent e) {
        if (!Config.killed_chat_enabled) {
            return;
        }
        Player player = e.getEntity();
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null || game.getState() != GameState.RUNNING) {
            return;
        }
        Player killer = player.getKiller();
        if (killer == null) {
            killer = game.getPlayerDamager(player);
            if (killer == null) {
                return;
            }
        }
        Map<Event, PacketListener> map = killedEvents.getOrDefault(game.getName(), new HashMap<>());
        if (!map.containsKey(e)) {
            return;
        }
        ProtocolLibrary.getProtocolManager().removePacketListener(map.get(e));
        map.remove(e);
        killedEvents.put(game.getName(), map);
        String finalkilled = "";
        Team playerTeam = game.getPlayerTeam(player);
        Team killerTeam = game.getPlayerTeam(killer);
        if (game.isSpectator(player) || playerTeam.isDead(game)) {
            finalkilled = ColorUtil.color(Config.killed_chat_final);
        }

        String string = Config.killed_chat_message
                .replace("{bwprefix}", Config.bwrelPrefix)
                .replace("{playerTeamString}", ChatColor.GOLD + "(" + playerTeam.getDisplayName() + ChatColor.GOLD + ")")
                .replace("{killerTeamString}", ChatColor.GOLD + "(" + killerTeam.getDisplayName() + ChatColor.GOLD + ")")
                .replace("{playerTeamColor}", playerTeam.getChatColor().toString())
                .replace("{killerTeamColor}", killerTeam.getChatColor().toString())
                .replace("{playerTeam}", playerTeam.getDisplayName())
                .replace("{killerTeam}", killerTeam.getDisplayName())
                .replace("{hearts}", BedwarsUtil.getHealthsString(killer))
                .replace("{player}", player.getDisplayName())
                .replace("{killer}", killer.getDisplayName())
                .replace("{final}", finalkilled);

        string = PlaceholderAPIUtil.replacePlaceholdersWithPrefix(string, "{player:", player);
        string = PlaceholderAPIUtil.replacePlaceholdersWithPrefix(string, "{killer:", killer);
        string = ColorUtil.color(string);

        for (Player p : game.getPlayers()) {
            if (p.isOnline()) {
                p.sendMessage(string);
            }
        }
    }

    private PacketListener registerKilledPacketListener(Player killer, Team killerTeam, Player player, Team deathTeam) {
        PacketListener listener = new PacketAdapter(Main.getInstance(), PacketType.Play.Server.CHAT) {
            public void onPacketSending(PacketEvent e) {
                Player p = e.getPlayer();
                WrappedChatComponent chat = e.getPacket().getChatComponents().read(0);
                String hearts = "";
                DecimalFormat format = new DecimalFormat("#");
                double health = killer.getHealth() / killer.getMaxHealth() * killer.getHealthScale();
                if (!BedwarsRel.getInstance().getBooleanConfig("hearts-in-halfs", true)) {
                    format = new DecimalFormat("#.#");
                    health /= 2.0;
                }
                if (BedwarsRel.getInstance().getBooleanConfig("hearts-on-death", true)) {
                    hearts = "[" + ChatColor.RED + "❤" + format.format(health) + ChatColor.GOLD + "]";
                }
                WrappedChatComponent[] chats = WrappedChatComponent.fromChatMessage(ChatWriter.pluginMessage(ChatColor.GOLD + BedwarsRel._l(p, "ingame.player.killed", ImmutableMap.of("killer", Game.getPlayerWithTeamString(killer, killerTeam, ChatColor.GOLD, hearts), "player", Game.getPlayerWithTeamString(player, deathTeam, ChatColor.GOLD)))));
                for (WrappedChatComponent c : chats) {
                    if (chat.getJson().equals(c.getJson())) {
                        e.setCancelled(true);
                        break;
                    }
                }
            }
        };
        ProtocolLibrary.getProtocolManager().addPacketListener(listener);
        return listener;
    }

    // 击杀消息部分结束
    // 破坏床部分

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBedLowest(BlockBreakEvent e) {
        if (!Config.bedbreak_chat_enabled) {
            return;
        }
        Player player = e.getPlayer();
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null || game.getState() != GameState.RUNNING) {
            return;
        }
        Map<Event, PacketListener> map = bedEvents.getOrDefault(game.getName(), new HashMap<>());
        Team breakTeam = BedwarsUtil.getTeamOfBed(e.getBlock(), game, player);
        if (breakTeam == null) return;
        map.put(e, registerBedPacketListener(player, game.getPlayerTeam(player), breakTeam));
        bedEvents.put(game.getName(), map);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBedDestoryed(BlockBreakEvent e) {
        if (!Config.bedbreak_chat_enabled) {
            return;
        }
        Player player = e.getPlayer();
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null || game.getState() != GameState.RUNNING) {
            return;
        }
        Map<Event, PacketListener> map = bedEvents.getOrDefault(game.getName(), new HashMap<>());
        if (!map.containsKey(e)) {
            return;
        }
        ProtocolLibrary.getProtocolManager().removePacketListener(map.get(e));
        map.remove(e);
        bedEvents.put(game.getName(), map);
        Team playerTeam = game.getPlayerTeam(player);
        Team breakTeam = BedwarsUtil.getTeamOfBed(e.getBlock(), game, player);
        if (breakTeam == null) return;

        String string = ColorUtil.color(PlaceholderAPIUtil.setPlaceholders(player, Config.bedbreak_chat_message
                .replace("{bwprefix}", Config.bwrelPrefix)
                .replace("{playerTeamString}", ChatColor.GOLD + "(" + playerTeam.getDisplayName() + ChatColor.GOLD + ")")
                .replace("{playerTeamColor}", playerTeam.getChatColor().toString())
                .replace("{playerTeam}", playerTeam.getDisplayName())
                .replace("{player}", player.getDisplayName())
                .replace("{deathTeam}", breakTeam.getDisplayName())
                .replace("{deathTeamColor}", breakTeam.getChatColor().toString())));
        for (Player p : game.getPlayers()) {
            if (p.isOnline()) {
                p.sendMessage(string);
            }
        }
    }

    private PacketListener registerBedPacketListener(Player player, Team playerTeam, Team destoryedTeam) {
        PacketListener listener = new PacketAdapter(Main.getInstance(), PacketType.Play.Server.CHAT) {
            public void onPacketSending(PacketEvent e) {
                WrappedChatComponent chat = e.getPacket().getChatComponents().read(0);
                WrappedChatComponent[] chats = WrappedChatComponent.fromChatMessage(ChatWriter.pluginMessage(ChatColor.RED + BedwarsRel
                        ._l(player, "ingame.blocks.beddestroyed",
                                ImmutableMap.of("team",
                                        destoryedTeam.getChatColor() + destoryedTeam.getName() + ChatColor.RED,
                                        "player",
                                        Game.getPlayerWithTeamString(player, playerTeam, ChatColor.RED)))));

                for (WrappedChatComponent c : chats) {
                    if (chat.getJson().equals(c.getJson())) {
                        e.setCancelled(true);
                        break;
                    }
                }
            }
        };
        ProtocolLibrary.getProtocolManager().addPacketListener(listener);
        return listener;
    }

    // 破坏床消息部分结束
    // 离开游戏部分

    // 做缓存, 否则会空
    @EventHandler
    public void onGameStarted(BedwarsGameStartedEvent event) {
        if (!Config.quit_chat_enabled) {
            return;
        }
        for (Player player : event.getGame().getPlayers()) {
            playerGameCache.put(player, event.getGame());
        }
    }

    @EventHandler
    public void onGameEnd(BedwarsGameEndEvent event) {
        if (!Config.quit_chat_enabled) {
            return;
        }
        Game endedGame = event.getGame();
        playerGameCache.entrySet().removeIf(entry -> entry.getValue().equals(endedGame));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuitLowest(PlayerQuitEvent e) {
        if (!Config.quit_chat_enabled) {
            return;
        }
        Player player = e.getPlayer();
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null) {
            game = playerGameCache.getOrDefault(player, null);
            if (game == null) return;
        }
        if (BedwarsUtil.isSpectator(player)) return;
        Team team = game.getPlayerTeam(player);
        if (team == null) return;
        Map<Event, PacketListener> map = quitevents.getOrDefault(game.getName(), new HashMap<>());
        map.put(e, registerQuitPacketListener(player, team));
        quitevents.put(game.getName(), map);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuitHighest(PlayerQuitEvent e) {
        if (!Config.quit_chat_enabled) {
            return;
        }
        Player player = e.getPlayer();
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null) {
            game = playerGameCache.getOrDefault(player, null);
            if (game == null) return;
        }
        if (BedwarsUtil.isSpectator(player)) return;
        Map<Event, PacketListener> map = quitevents.getOrDefault(game.getName(), new HashMap<>());
        if (!map.containsKey(e)) {
            return;
        }
        ProtocolLibrary.getProtocolManager().removePacketListener(map.get(e));
        map.remove(e);
        quitevents.put(game.getName(), map);
        Team playerTeam = game.getPlayerTeam(player);
        if (playerTeam == null) {
            Arena arena = Main.getInstance().getArenaManager().getArena(game.getName());
            if (arena == null) return;
            playerTeam = arena.getPlayerNameTeams().getOrDefault(player.getName(), null);
            if (playerTeam == null) return;
        }

        String string = ColorUtil.color(PlaceholderAPIUtil.setPlaceholders(player, Config.quit_chat_message
                .replace("{bwprefix}", Config.bwrelPrefix)
                .replace("{playerTeamString}", ChatColor.GOLD + "(" + playerTeam.getDisplayName() + ChatColor.GOLD + ")")
                .replace("{playerTeamColor}", playerTeam.getChatColor().toString())
                .replace("{playerTeam}", playerTeam.getDisplayName())
                .replace("{player}", player.getDisplayName())));
        for (Player p : game.getPlayers()) {
            if (p.isOnline()) {
                p.sendMessage(string);
            }
        }
    }

    private PacketListener registerQuitPacketListener(Player player, Team playerTeam) {
        PacketListener listener = new PacketAdapter(Main.getInstance(), PacketType.Play.Server.CHAT) {
            public void onPacketSending(PacketEvent e) {
                WrappedChatComponent chat = e.getPacket().getChatComponents().read(0);
                String chatJson = chat.getJson();

                WrappedChatComponent[] chats = WrappedChatComponent.fromChatMessage(ChatWriter.pluginMessage(
                        ChatColor.RED + BedwarsRel
                                ._l(player, "ingame.player.left", ImmutableMap.of("player",
                                        Game.getPlayerWithTeamString(player, playerTeam, ChatColor.RED)
                                                + ChatColor.RED)))
                );

                for (WrappedChatComponent c : chats) {
                    if (chatJson.equals(c.getJson())) {
                        e.setCancelled(true);
                        break;
                    }
                }
            }
        };
        ProtocolLibrary.getProtocolManager().addPacketListener(listener);
        return listener;
    }

    // 游戏重启消息部分

    @EventHandler
    public void onGameOver(BedwarsGameOverEvent event) {
        if (!BedwarsRel.getInstance().isBungee()) return;
        if (Config.isBedwarsXPEnabled) return;
        registerRestartMessageListener();
    }

    private void registerRestartMessageListener() {
        PacketListener packetListener = new PacketAdapter(Main.getInstance(), ListenerPriority.HIGHEST, PacketType.Play.Server.CHAT) {
            public void onPacketSending(PacketEvent e) {
                if (e.getPacketType() != PacketType.Play.Server.CHAT) return;

                PacketContainer originalPacket = e.getPacket();
                WrappedChatComponent originalChat = originalPacket.getChatComponents().read(0);
                String originalMessage = originalChat.getJson();
                // 目前没有想法修改 没有想法在不动REL的情况下获取BungeeCycle倒计时
                if (originalMessage.contains("服务器将在") && originalMessage.contains("秒后重置地图!")) {
                    e.setCancelled(true);
                }
            }
        };
        ProtocolLibrary.getProtocolManager().addPacketListener(packetListener);
    }
}
