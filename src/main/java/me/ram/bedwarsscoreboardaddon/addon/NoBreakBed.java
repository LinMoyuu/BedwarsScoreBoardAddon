package me.ram.bedwarsscoreboardaddon.addon;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.GameState;
import lombok.Getter;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.utils.BedwarsUtil;
import me.ram.bedwarsscoreboardaddon.utils.Utils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class NoBreakBed {

    @Getter
    private final Game game;
    @Getter
    private final Arena arena;
    private boolean bre;
    private String formattime = "00:00";
    private PacketListener packetListener;

    public NoBreakBed(Arena arena) {
        this.arena = arena;
        this.game = arena.getGame();
        bre = false;
        if (!Config.nobreakbed_enabled) {
            return;
        }
        registerPacketListener();
        arena.addGameTask(new BukkitRunnable() {
            @Override
            public void run() {
                int time = arena.getGameLeft() - Config.nobreakbed_gametime;
                formattime = time / 60 + ":" + ((time % 60 < 10) ? ("0" + time % 60) : (time % 60));
                if (arena.getGameLeft() <= Config.nobreakbed_gametime) {
                    bre = true;
                    if (Config.nobreakbed_enabled) {
                        for (Player player : game.getPlayers()) {
                            if (!Config.nobreakbed_title.isEmpty() || !Config.nobreakbed_subtitle.isEmpty()) {
                                Utils.sendTitle(player, 10, 50, 10, Config.nobreakbed_title, Config.nobreakbed_subtitle);
                            }
                            if (!Config.nobreakbed_message.isEmpty()) {
                                player.sendMessage(Config.nobreakbed_message);
                            }
                        }
                    }
                    cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 20L));
    }

    public String getTime() {
        return formattime;
    }

    public void onEnd() {
        if (packetListener != null) {
            ProtocolLibrary.getProtocolManager().removePacketListener(packetListener);
        }
    }

    private void registerPacketListener() {
        packetListener = new PacketAdapter(Main.getInstance(), ListenerPriority.HIGHEST, PacketType.Play.Client.BLOCK_DIG) {
            public void onPacketReceiving(PacketEvent e) {
                if (!Config.nobreakbed_enabled) {
                    return;
                }
                Player player = e.getPlayer();
                if (BedwarsUtil.isSpectator(game, player) || game.getState() != GameState.RUNNING) {
                    return;
                }
                if (!bre && e.getPacketType() == PacketType.Play.Client.BLOCK_DIG) {
                    PacketContainer packet = e.getPacket();
                    BlockPosition position = packet.getBlockPositionModifier().read(0);
                    Block block = new Location(player.getWorld(), position.getX(), position.getY(), position.getZ()).getBlock();
                    if (!block.getType().equals(game.getTargetMaterial())) {
                        return;
                    }
                    if (!packet.getPlayerDigTypes().read(0).equals(EnumWrappers.PlayerDigType.STOP_DESTROY_BLOCK)) {
                        return;
                    }
                    player.sendMessage(Config.nobreakbed_nobreakmessage);
                    e.setCancelled(true);
                    block.getState().update();
                }
            }
        };
        ProtocolLibrary.getProtocolManager().addPacketListener(packetListener);
    }
}
