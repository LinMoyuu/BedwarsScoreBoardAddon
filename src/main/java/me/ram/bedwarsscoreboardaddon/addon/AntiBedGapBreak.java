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

import java.util.*;

public class AntiBedGapBreak {

    @Getter
    private final Game game;
    @Getter
    private final Arena arena;
    private PacketListener packetListener;

    private static final Set<Material> TRANSPARENT_MATERIALS;

    static {
        Set<Material> tempSet = new HashSet<>();
        tempSet.add(Material.AIR);
        tempSet.add(Material.WATER);
        tempSet.add(Material.STATIONARY_WATER);
        tempSet.add(Material.LAVA);
        tempSet.add(Material.STATIONARY_LAVA);
        tempSet.add(Material.CARPET);
        tempSet.add(Material.SNOW);

        TRANSPARENT_MATERIALS = Collections.unmodifiableSet(tempSet);
    }

    public AntiBedGapBreak(Arena arena) {
        this.arena = arena;
        this.game = arena.getGame();
        if (!Config.anti_gap_breakbed_enabled) return;
        registerPacketListener();
    }

    public void onEnd() {
        if (packetListener != null) {
            ProtocolLibrary.getProtocolManager().removePacketListener(packetListener);
            packetListener = null;
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
                if (player == null || !arena.isAlivePlayer(player)) {
                    return;
                }

                PacketContainer packet = e.getPacket();
                EnumWrappers.PlayerDigType digType = packet.getPlayerDigTypes().read(0);

                if (digType != EnumWrappers.PlayerDigType.STOP_DESTROY_BLOCK) {
                    return;
                }

                BlockPosition pos = packet.getBlockPositionModifier().read(0);
                Location blockLocation = pos.toLocation(player.getWorld());
                Block brokenBlock = blockLocation.getBlock();

                // 只检测床
                if (brokenBlock.getType() != Material.BED_BLOCK) {
                    return;
                }

                List<Block> lineOfSight = player.getLineOfSight(TRANSPARENT_MATERIALS, Config.anti_gap_breakbed_distance);

                if (lineOfSight.isEmpty()) {
                    return;
                }

                Block lastVisibleBlock = lineOfSight.get(lineOfSight.size() - 1);

                if (lastVisibleBlock.equals(brokenBlock)) {
                    return;
                }

                if (lastVisibleBlock.getType() == Material.BED_BLOCK) {
                    if (isAdjacent(brokenBlock, lastVisibleBlock)) {
                        return;
                    }
                }

                e.setCancelled(true);
                brokenBlock.getState().update(true);

                String gap_break_message = Config.anti_gap_breakbed_message;
                if (!gap_break_message.isEmpty()) {
                    player.sendMessage(gap_break_message);
                }
            }
        };
        ProtocolLibrary.getProtocolManager().addPacketListener(packetListener);
    }

    /**
     * 检查两个方块是否在六个方向上相邻
     */
    private boolean isAdjacent(Block b1, Block b2) {
        int x1 = b1.getX(), y1 = b1.getY(), z1 = b1.getZ();
        int x2 = b2.getX(), y2 = b2.getY(), z2 = b2.getZ();

        int dx = Math.abs(x1 - x2);
        int dy = Math.abs(y1 - y2);
        int dz = Math.abs(z1 - z2);

        return (dx + dy + dz) == 1;
    }
}