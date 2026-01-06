package me.ram.bedwarsscoreboardaddon.addon;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.events.BedwarsOpenShopEvent;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.GameState;
import io.github.bedwarsrel.shop.NewItemShop;
import lombok.Getter;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.api.HolographicAPI;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.events.BoardAddonPlayerOpenItemShopEvent;
import me.ram.bedwarsscoreboardaddon.utils.BedwarsUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.npc.skin.SkinnableEntity;
import net.citizensnpcs.trait.Gravity;
import net.citizensnpcs.trait.LookClose;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Shop {

    @Getter
    private final Game game;
    @Getter
    private final Arena arena;
    private final List<NPC> shops;
    private final List<HolographicAPI> titles;
    private final List<Integer> npcid;
    private WrappedDataWatcher.Serializer booleanserializer;

    public Shop(Arena arena) {
        this.arena = arena;
        this.game = arena.getGame();
        shops = new ArrayList<>();
        titles = new ArrayList<>();
        npcid = new ArrayList<>();
        if (!BedwarsRel.getInstance().getCurrentVersion().startsWith("v1_8")) {
            booleanserializer = WrappedDataWatcher.Registry.get(Boolean.class);
        }
        if (Config.shop_enabled) {
            if (Config.game_shop_item.containsKey(game.getName())) {
                for (String loc : Config.game_shop_item.get(game.getName())) {
                    Location location = toLocation(loc);
                    if (location != null) {
                        shops.add(spawnShop(location.clone(), Config.shop_item_shop_look, Config.shop_item_shop_type, Config.shop_item_shop_skin));
                        if (!Config.shop_item_shop_name.get(0).isEmpty()) {
                            setTitle(location.clone().add(0, -0.1, 0), Config.shop_item_shop_name);
                        }
                    }
                }
            }
        }
    }

    public boolean isShopNPC(int id) {
        return npcid.contains(id);
    }

    public Boolean onNPCClick(Player player, NPC npc, Boolean isCancelled) {
        if (!Config.shop_enabled) {
            return isCancelled;
        }
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game != null) {
            if (shops.contains(npc)) {
                if (isGamePlayer(player)) {
                    isCancelled = true;
                    BoardAddonPlayerOpenItemShopEvent openItemShopEvent = new BoardAddonPlayerOpenItemShopEvent(game, player);
                    Bukkit.getPluginManager().callEvent(openItemShopEvent);
                    if (!openItemShopEvent.isCancelled() && Config.shop_enabled_addonopen) {
                        player.closeInventory();
                        NewItemShop itemShop = game.openNewItemShop(player);
                        itemShop.setCurrentCategory(null);
                        itemShop.openCategoryInventory(player);
                    }
                }
            }
        }
        return isCancelled;
    }

    public void onOpenShop(BedwarsOpenShopEvent e) {
        if (!Config.shop_enabled) {
            return;
        }
        Player player = (Player) e.getPlayer();
        if (player.getGameMode().equals(GameMode.SPECTATOR)) {
            e.setCancelled(true);
        }
    }

    public void onPlayerJoined(Player player) {
        if (game.getState() == GameState.RUNNING) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (game.getState() == GameState.RUNNING && player.isOnline() && game.getPlayers().contains(player)) {
                        for (HolographicAPI holo : titles) {
                            holo.display(player);
                        }
                    }
                }
            }.runTaskLater(Main.getInstance(), 10L);
        }
    }

    private Boolean isGamePlayer(Player player) {
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null) {
            return false;
        }
        if (BedwarsUtil.isSpectator(game, player)) {
            return false;
        }
        return player.getGameMode() != GameMode.SPECTATOR;
    }

    private NPC spawnShop(Location location, boolean look, String type, String skin) {
        if (!location.getBlock().getChunk().isLoaded()) {
            location.getBlock().getChunk().load(true);
        }
        NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, "");
        npc.setProtected(true);
        npc.getTrait(Gravity.class).toggle();
        if (look) {
            npc.getTrait(LookClose.class).toggle();
        }
        npc.spawn(location);
        try {
            EntityType entityType = EntityType.valueOf(type);
            npc.setBukkitEntityType(entityType);
        } catch (Exception e) {
        }
        npc.data().setPersistent("silent-sounds", true);
        npcid.add(npc.getEntity().getEntityId());
        if (!Config.shop_item_shop_name.get(0).isEmpty()) {
            hideEntityTag(npc.getEntity());
        } else {
            npc.setName("商店");
        }
        Config.addShopNPC(npc.getId());
        try {
            if (npc.isSpawned() && npc.getEntity() instanceof SkinnableEntity) {
                SkinnableEntity skinnable = (SkinnableEntity) npc.getEntity();
                skinnable.setSkinName(skin, true);
            }
        } catch (Exception e) {
        }
        return npc;
    }

    private void hideEntityTag(Entity entity) {
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            ProtocolManager man = ProtocolLibrary.getProtocolManager();
            PacketContainer packet = man.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            packet.getIntegers().write(0, entity.getEntityId());
            WrappedDataWatcher wrappedDataWatcher = new WrappedDataWatcher();
            if (BedwarsRel.getInstance().getCurrentVersion().startsWith("v1_8")) {
                wrappedDataWatcher.setObject(3, (byte) 0);
            } else {
                wrappedDataWatcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(3, booleanserializer), false);
            }
            packet.getWatchableCollectionModifier().write(0, wrappedDataWatcher.getWatchableObjects());
            for (Player player : game.getPlayers()) {
                man.sendServerPacket(player, packet, false);
            }
        }, 1L);
    }

    private void setTitle(Location location, List<String> title) {
        Location loc = location.clone();
        if (!loc.getBlock().getChunk().isLoaded()) {
            loc.getBlock().getChunk().load(true);
        }
        List<String> list = new ArrayList<>(title);
        Collections.reverse(list);
        for (String line : list) {
            HolographicAPI holo = new HolographicAPI(loc, line);
            titles.add(holo);
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Player player : game.getPlayers()) {
                        holo.display(player);
                    }
                }
            }.runTaskLater(Main.getInstance(), 20L);
            loc.add(0, 0.3, 0);
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                if (game.getState() == GameState.RUNNING) {
                    if (!loc.getBlock().getChunk().isLoaded()) {
                        loc.getBlock().getChunk().load(true);
                    }
                } else {
                    this.cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 0L);
    }

    private Location toLocation(String loc) {
        try {
            String[] ary = loc.split(", ");
            if (Bukkit.getWorld(ary[0]) != null) {
                Location location = new Location(Bukkit.getWorld(ary[0]), Double.parseDouble(ary[1]), Double.parseDouble(ary[2]), Double.parseDouble(ary[3]));
                if (ary.length > 4) {
                    location.setYaw(Float.parseFloat(ary[4]));
                    location.setPitch(Float.parseFloat(ary[5]));
                }
                return location;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public void remove() {
        for (NPC npc : shops) {
            CitizensAPI.getNPCRegistry().deregister(npc);
        }
        for (HolographicAPI holo : titles) {
            holo.remove();
        }
    }
}
