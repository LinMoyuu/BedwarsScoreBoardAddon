package me.ram.bedwarsscoreboardaddon.addon;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import io.github.bedwarsrel.game.Game;
import lombok.Getter;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class AntiBedGapBreak {

    @Getter
    private final Game game;
    @Getter
    private final Arena arena;
    private PacketListener packetListener;

    public AntiBedGapBreak(Arena arena) {
        this.arena = arena;
        this.game = arena.getGame();
        if (!Config.anti_gap_breakbed_enabled) return;
        registerPacketListener();
    }

    public void onEnd() {
        if (packetListener != null) {
            ProtocolLibrary.getProtocolManager().removePacketListener(packetListener);
        }
    }

    private void registerPacketListener() {
        packetListener = new PacketAdapter(Main.getInstance(), ListenerPriority.HIGHEST, PacketType.Play.Client.BLOCK_DIG) {
            public void onPacketReceiving(PacketEvent e) {
                if (!Config.anti_gap_breakbed_enabled) return;
                if (e.getPacketType() != PacketType.Play.Client.BLOCK_DIG) {
                    return;
                }
                Player player = e.getPlayer();
                if (!arena.isGamePlayer(player))
                    return;
                PacketContainer packet = e.getPacket();
                EnumWrappers.PlayerDigType digType = packet.getPlayerDigTypes().read(0);
                if (digType != EnumWrappers.PlayerDigType.STOP_DESTROY_BLOCK) {
                    return;
                }
                BlockPosition pos = packet.getBlockPositionModifier().read(0);
                Location blockLocation = pos.toLocation(player.getWorld());

                Block brokenBlock = blockLocation.getBlock();
                if (brokenBlock.getType() != Material.BED_BLOCK) {
                    return;
                }
                Block targetBlock = player.getTargetBlock(null, Config.anti_gap_breakbed_distance);

                String gap_break_message = Config.anti_gap_breakbed_message;
                if (targetBlock != null && !targetBlock.equals(brokenBlock)) {
                    e.setCancelled(true);
                    brokenBlock.getState().update(true);
                    if (!gap_break_message.isEmpty()) player.sendMessage(gap_break_message);
                }
            }
        };
        ProtocolLibrary.getProtocolManager().addPacketListener(packetListener);
    }
}
