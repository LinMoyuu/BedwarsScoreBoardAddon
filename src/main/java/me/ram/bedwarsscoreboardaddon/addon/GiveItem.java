package me.ram.bedwarsscoreboardaddon.addon;

import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.events.BedwarsGameStartedEvent;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.GameState;
import io.github.bedwarsrel.game.Team;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.events.BoardAddonPlayerRespawnEvent;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class GiveItem implements Listener {

    /**
     * 用于给玩家发放盔甲和物品。
     *
     * @param player  目标玩家
     * @param team    玩家所在的队伍
     * @param respawn 玩家是否是重生状态
     */
    public static void giveItem(Player player, Team team, boolean respawn) {
        processArmor(player, team, respawn);
        processInventoryItems(player, respawn);
    }

    /**
     * 处理所有盔甲的发放
     */
    private static void processArmor(Player player, Team team, boolean respawn) {
        PlayerInventory inventory = player.getInventory();

        Map<String, Map<String, Object>> armorConfigs = new LinkedHashMap<>();
        armorConfigs.put("HELMET", Config.giveitem_armor_helmet_item);
        armorConfigs.put("CHESTPLATE", Config.giveitem_armor_chestplate_item);
        armorConfigs.put("LEGGINGS", Config.giveitem_armor_leggings_item);
        armorConfigs.put("BOOTS", Config.giveitem_armor_boots_item);

        Map<String, String> giveOptions = new HashMap<>();
        giveOptions.put("HELMET", Config.giveitem_armor_helmet_give);
        giveOptions.put("CHESTPLATE", Config.giveitem_armor_chestplate_give);
        giveOptions.put("LEGGINGS", Config.giveitem_armor_leggings_give);
        giveOptions.put("BOOTS", Config.giveitem_armor_boots_give);

        Map<String, Supplier<ItemStack>> slotGetters = new HashMap<>();
        slotGetters.put("HELMET", inventory::getHelmet);
        slotGetters.put("CHESTPLATE", inventory::getChestplate);
        slotGetters.put("LEGGINGS", inventory::getLeggings);
        slotGetters.put("BOOTS", inventory::getBoots);

        Map<String, Consumer<ItemStack>> slotSetters = new HashMap<>();
        slotSetters.put("HELMET", inventory::setHelmet);
        slotSetters.put("CHESTPLATE", inventory::setChestplate);
        slotSetters.put("LEGGINGS", inventory::setLeggings);
        slotSetters.put("BOOTS", inventory::setBoots);

        for (Map.Entry<String, Map<String, Object>> entry : armorConfigs.entrySet()) {
            String armorType = entry.getKey();
            String option = giveOptions.get(armorType);

            if (option != null && shouldGiveItem(option, respawn) && slotGetters.containsKey(armorType) && slotGetters.get(armorType).get() == null) {
                ItemStack armorPiece = createArmorPiece(entry.getValue(), armorType, team.getColor().getColor());
                if (armorPiece != null && slotSetters.containsKey(armorType)) {
                    slotSetters.get(armorType).accept(armorPiece);
                }
            }
        }
    }

    /**
     * 处理配置文件中 "giveitem.item" 下的其他物品。
     */
    private static void processInventoryItems(Player player, boolean isRespawn) {
        ConfigurationSection itemsSection = Main.getInstance().getConfig().getConfigurationSection("giveitem.item");
        if (itemsSection == null) return;

        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection itemConfig = itemsSection.getConfigurationSection(key);
            if (itemConfig == null || !shouldGiveItem(itemConfig.getString("give", "true"), isRespawn)) {
                continue;
            }

            try {
                List<?> itemList = itemConfig.getList("item");
                if (itemList != null && !itemList.isEmpty() && itemList.get(0) instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> itemMap = (Map<String, Object>) itemList.get(0);
                    ItemStack itemStack = ItemStack.deserialize(itemMap);

                    if (isRespawn) {
                        player.getInventory().addItem(itemStack);
                    } else {
                        player.getInventory().setItem(itemConfig.getInt("slot"), itemStack);
                    }
                }
            } catch (Exception e) {
                Main.getInstance().getLogger().severe("无法创建或给予物品: " + key);
                e.printStackTrace();
            }
        }
    }

    /**
     * 根据配置创建并为团队盔甲着色。
     *
     * @param config    物品配置Map
     * @param armorType 盔甲类型 (e.g., "HELMET")
     * @param teamColor 团队颜色
     * @return 创建好的 ItemStack，失败则返回 null
     */
    private static ItemStack createArmorPiece(Map<String, Object> config, String armorType, Color teamColor) {
        // 创建副本以防修改原始 Map
        Map<String, Object> itemMap = new HashMap<>(config);
        boolean isTeamArmor = "TEAM_ARMOR".equals(itemMap.get("type"));

        if (isTeamArmor) {
            itemMap.put("type", "LEATHER_" + armorType);
        }

        try {
            ItemStack armorPiece = ItemStack.deserialize(itemMap);
            // 确保 getItemMeta() 不为 null
            if (isTeamArmor && armorPiece.hasItemMeta() && armorPiece.getItemMeta() instanceof LeatherArmorMeta) {
                LeatherArmorMeta meta = (LeatherArmorMeta) armorPiece.getItemMeta();
                meta.setColor(teamColor);
                armorPiece.setItemMeta(meta);
            }
            return armorPiece;
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("反序列化盔甲失败: " + armorType);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 根据配置字符串和重生状态判断是否应该给予物品。
     *
     * @param giveOption 配置选项 ("true", "start", "respawn")
     * @param isRespawn  玩家是否重生
     * @return 如果应该给予物品，则返回 true
     */
    private static boolean shouldGiveItem(String giveOption, boolean isRespawn) {
        return "true".equalsIgnoreCase(giveOption) ||
                ("start".equalsIgnoreCase(giveOption) && !isRespawn) ||
                ("respawn".equalsIgnoreCase(giveOption) && isRespawn);
    }

    @EventHandler
    public void onStarted(BedwarsGameStartedEvent e) {
        for (Player player : e.getGame().getPlayers()) {
            Team team = e.getGame().getPlayerTeam(player);
            GiveItem.giveItem(player, team, false);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer((Player) e.getWhoClicked());
        if (game == null || game.getState() != GameState.RUNNING) {
            return;
        }
        Player player = (Player) e.getWhoClicked();
        if (game.getPlayerTeam(player) == null) {
            return;
        }
        Inventory inventory = e.getInventory();
        if (inventory.getHolder() == null) {
            return;
        }
        if (!(inventory.getHolder().equals(player.getInventory().getHolder()) && (inventory.getTitle().equals("container.crafting") || inventory.getTitle().equals("container.inventory")))) {
            return;
        }
        if (e.getRawSlot() == 5 && !Config.giveitem_armor_helmet_move) {
            e.setCancelled(true);
            player.sendMessage("§4§l你无法移动该物品!");
            return;
        }
        if (e.getRawSlot() == 6 && !Config.giveitem_armor_chestplate_move) {
            e.setCancelled(true);
            player.sendMessage("§4§l你无法移动该物品!");
            return;
        }
        if (e.getRawSlot() == 7 && !Config.giveitem_armor_leggings_move) {
            e.setCancelled(true);
            player.sendMessage("§4§l你无法移动该物品!");
            return;
        }
        if (e.getRawSlot() == 8 && !Config.giveitem_armor_boots_move) {
            e.setCancelled(true);
            player.sendMessage("§4§l你无法移动该物品!");
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        if (player == null) return;
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(e.getEntity());
        if (game == null) {
            return;
        }
        Arena arena = Main.getInstance().getArenaManager().getArena(game.getName());
        if (arena == null) {
            return;
        }
        if (game.getPlayerTeam(player) == null) {
            return;
        }
        if (game.getPlayerTeam(player).isDead(game)) {
            return;
        }

        arena.addGameTask(new BukkitRunnable() {
            final ItemStack stack1 = player.getInventory().getHelmet();
            final ItemStack stack2 = player.getInventory().getChestplate();
            final ItemStack stack3 = player.getInventory().getLeggings();
            final ItemStack stack4 = player.getInventory().getBoots();

            @Override
            public void run() {
                if (Config.giveitem_keeparmor) {
                    if (stack1 != null) {
                        player.getInventory().setHelmet(stack1);
                    }
                    if (stack2 != null) {
                        player.getInventory().setChestplate(stack2);
                    }
                    if (stack3 != null) {
                        player.getInventory().setLeggings(stack3);
                    }
                    if (stack4 != null) {
                        player.getInventory().setBoots(stack4);
                    }
                }
            }
        }.runTaskLater(Main.getInstance(), 1L));
    }

    @EventHandler
    public void onRespawn(BoardAddonPlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Game game = event.getGame();
        Team team = game.getPlayerTeam(player);
        GiveItem.giveItem(player, team, true);
    }
}
