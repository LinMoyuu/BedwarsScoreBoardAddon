package me.ram.bedwarsscoreboardaddon.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.config.Config;
import org.bukkit.Bukkit;

public class ServerRestartMessageListener extends PacketAdapter {

    public ServerRestartMessageListener(Main plugin) {
        super(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.CHAT);
    }

    @Override
    public void onPacketSending(PacketEvent e) {
        if (Config.isBedwarsXPEnabled) return;
        if (e.getPacketType() != PacketType.Play.Server.CHAT) return;

        PacketContainer originalPacket = e.getPacket();
        WrappedChatComponent originalChat = originalPacket.getChatComponents().read(0);
        String originalMessage = originalChat.getJson();
        if (originalMessage.contains("起床战争") && originalMessage.contains("服务器将在") && originalMessage.contains("秒后重置地图!")) {
            e.setCancelled(true);
        }
    }

}
