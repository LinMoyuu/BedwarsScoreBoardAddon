package me.ram.bedwarsscoreboardaddon.addon.teamshop;

import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.GameState;
import lombok.Getter;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeamShop {

    @Getter
    private final Game game;
    @Getter
    private final Arena arena;
    private final List<Listener> listeners;
    @Getter
    private final HashMap<Player, Integer> playerSharpnessLevel;
    @Getter
    private final HashMap<Player, Integer> playerLeggingsProtectionLevel;
    @Getter
    private final HashMap<Player, Integer> playerBootsProtectionLevel;

    public TeamShop(Arena arena) {
        this.arena = arena;
        this.game = arena.getGame();
        listeners = new ArrayList<>();
        playerSharpnessLevel = new HashMap<>();
        playerLeggingsProtectionLevel = new HashMap<>();
        playerBootsProtectionLevel = new HashMap<>();
    }

    public void onEnd() {
        listeners.forEach(HandlerList::unregisterAll);
    }

    // https://github.com/BedwarsRel/BedwarsRel/blob/master/common/src/main/java/io/github/bedwarsrel/shop/NewItemShop.java#L473C1-L502C1
    public void onOpen(InventoryOpenEvent e) {
        Player player = (Player) e.getPlayer();
        Inventory inventory = e.getInventory();
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null || !inventory.getTitle().equals(BedwarsRel._l(player, "ingame.shop.name"))) {
            return;
        }
        // 有人写死指定SLIME_BALL为切换了
        ItemStack slime = new ItemStack(Material.SLIME_BALL, 1);
        ItemMeta slimeMeta = slime.getItemMeta();

        slimeMeta.setDisplayName(BedwarsRel._l(player, "ingame.shop.oldshop"));
        slimeMeta.setLore(new ArrayList<>());
        slime.setItemMeta(slimeMeta);
        inventory.remove(slime);
        // 有人写死指定BUCKET为切换了
        ItemStack stack = null;
        if (game.getPlayerSettings(player).oneStackPerShift()) {
            stack = new ItemStack(Material.BUCKET, 1);
            ItemMeta meta = stack.getItemMeta();

            meta.setDisplayName(
                    ChatColor.AQUA + BedwarsRel._l(player, "default.currently") + ": " + ChatColor.WHITE
                            + BedwarsRel._l(player, "ingame.shop.onestackpershift"));
            meta.setLore(new ArrayList<>());
            stack.setItemMeta(meta);
        } else {
            stack = new ItemStack(Material.LAVA_BUCKET, 1);
            ItemMeta meta = stack.getItemMeta();

            meta.setDisplayName(
                    ChatColor.AQUA + BedwarsRel._l(player, "default.currently") + ": " + ChatColor.WHITE
                            + BedwarsRel._l(player, "ingame.shop.fullstackpershift"));
            meta.setLore(new ArrayList<>());
            stack.setItemMeta(meta);
        }
        inventory.remove(stack);
    }

    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) e.getWhoClicked();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!game.getState().equals(GameState.RUNNING)) {
                    return;
                }
                // 点击后 获取背包是否有特殊物品Lore 有就是购买成功 成功了就替换 Rel原版是直接给物品了
                // 由于都是颜色符号 不显示 但是会显示个空行
                // 怪聪明的... 效果差强人意吧(会显示个空行) 真要把这个改掉估计得自己写商店或者改Rel。
                ItemStack[] stacks = player.getInventory().getContents();
                for (int i = 0; i < stacks.length; i++) {
                    ItemStack stack = stacks[i];
                    if (stack == null) {
                        continue;
                    }
                    ItemMeta meta = stack.getItemMeta();
                    if (!meta.hasLore()) {
                        continue;
                    }
                    PlayerInventory inventory = player.getInventory();
                    // 物品是否是特属装备 是的话自动装备
                    if (meta.getLore().contains("§a§r§m§o§r§0§0§1") || meta.getLore().contains("§a§r§m§o§r§0§0§2") || meta.getLore().contains("§a§r§m§o§r§0§0§3")) {
                        stack.setType(Material.AIR);
                        inventory.setItem(i, stack);
                        player.updateInventory();
                        ItemStack leggings = new ItemStack(Material.CHAINMAIL_LEGGINGS);
                        ItemStack boots = new ItemStack(Material.CHAINMAIL_BOOTS);
                        if (meta.getLore().contains("§a§r§m§o§r§0§0§1")) {
                            leggings = new ItemStack(Material.CHAINMAIL_LEGGINGS);
                            boots = new ItemStack(Material.CHAINMAIL_BOOTS);
                        } else if (meta.getLore().contains("§a§r§m§o§r§0§0§2")) {
                            leggings = new ItemStack(Material.IRON_LEGGINGS);
                            boots = new ItemStack(Material.IRON_BOOTS);
                        } else if (meta.getLore().contains("§a§r§m§o§r§0§0§3")) {
                            leggings = new ItemStack(Material.DIAMOND_LEGGINGS);
                            boots = new ItemStack(Material.DIAMOND_BOOTS);
                        }
                        ItemMeta leggingsMeta = leggings.getItemMeta();
                        ItemMeta bootsMeta = boots.getItemMeta();
                        leggingsMeta.spigot().setUnbreakable(true);
                        bootsMeta.spigot().setUnbreakable(true);
                        leggings.setItemMeta(leggingsMeta);
                        boots.setItemMeta(bootsMeta);
                        inventory.setLeggings(leggings);
                        inventory.setBoots(boots);
                        int playerLeggings = arena.getTeamShop().getPlayerLeggingsProtectionLevel().getOrDefault(player, 0);
                        if (playerLeggings != 0) {
                            Utils.giveLeggingsProtection(player, playerLeggings);
                        }

                        int playerBoots = arena.getTeamShop().getPlayerBootsProtectionLevel().getOrDefault(player, 0);
                        if (playerBoots != 0) {
                            Utils.giveBootsProtection(player, playerBoots);
                        }
                        break;
                    } else if (stack.getType().name().endsWith("_SWORD") && meta.getLore().contains("§s§w§o§r§d")) {
                        List<String> lore = meta.getLore();
                        lore.remove("§s§w§o§r§d");
                        meta.setLore(lore);
                        stack.setItemMeta(meta);
                        ItemStack newSword = stack.clone();

                        // 在购买剑后 花雨庭会将之前的剑放进背包内
                        // 没能想出什么方法 也没见过背包满的花雨庭怎么处理的 就这么生草了
                        // 在Hotbar中寻找玩家当前的剑
                        ItemStack oldSword = null;
                        int oldSwordSlot = -1;
                        for (int j = 0; j < 9; j++) {
                            ItemStack itemInHotbar = inventory.getItem(j);
                            if (itemInHotbar != null && itemInHotbar.getType().name().endsWith("_SWORD")) {
                                oldSword = itemInHotbar.clone();
                                oldSwordSlot = j;
                                break;
                            }
                        }
                        // 清除商店GUI中的临时物品?
                        inventory.setItem(i, new ItemStack(Material.AIR));

                        // 将新剑放到旧剑的位置上. 如果没找到旧剑 则直接添加
                        if (oldSwordSlot != -1) {
                            inventory.setItem(oldSwordSlot, newSword);
                        } else {
                            inventory.addItem(newSword);
                        }

                        // 将旧剑移动到背包
                        if (oldSword != null) {
                            boolean placedInInventory = false;
                            // 遍历主背包区域寻找空位
                            for (int j = 9; j <= 35; j++) {
                                if (inventory.getItem(j) == null || inventory.getItem(j).getType() == Material.AIR) {
                                    inventory.setItem(j, oldSword); // 放入空位
                                    placedInInventory = true;
                                    break;
                                }
                            }
                            // 如果背包满了 旧剑直接掉落
                            if (!placedInInventory) {
                                player.getWorld().dropItemNaturally(player.getLocation(), oldSword);
                            }
                        }
                        int playerSharpness = arena.getTeamShop().getPlayerSharpnessLevel().getOrDefault(player, 0);
                        if (playerSharpness != 0) {
                            Utils.givePlayerSharpness(player, playerSharpness);
                        }

                        player.updateInventory();
                        break;
                    } else if (stack.getType().equals(Material.BOOK) && (
                            meta.getLore().contains("§s§o§u§l§s§1") || meta.getLore().contains("§s§o§u§l§s§2")
                                    || meta.getLore().contains("§s§o§u§l§l§1") || meta.getLore().contains("§s§o§u§l§l§2")
                                    || meta.getLore().contains("§s§o§u§l§b§1") || meta.getLore().contains("§s§o§u§l§b§2"))) {
                        stack.setType(Material.AIR);
                        inventory.setItem(i, stack);
                        player.updateInventory();
                        // §s§o§u§l soul
                        // §s = sharpness §l = leggings §b = boots
                        // §1 = level etc...
                        if (meta.getLore().contains("§s§o§u§l§s§1")) { // 锋利
                            playerSharpnessLevel.put(player, 1);
                            Utils.givePlayerSharpness(player, 1);
                        } else if (meta.getLore().contains("§s§o§u§l§s§2")) {
                            playerSharpnessLevel.put(player, 2);
                            Utils.givePlayerSharpness(player, 2);
                        } else if (meta.getLore().contains("§s§o§u§l§l§1")) { // 护腿
                            playerLeggingsProtectionLevel.put(player, 1);
                            Utils.giveLeggingsProtection(player, 1);
                        } else if (meta.getLore().contains("§s§o§u§l§l§2")) {
                            playerLeggingsProtectionLevel.put(player, 2);
                            Utils.giveLeggingsProtection(player, 2);
                        } else if (meta.getLore().contains("§s§o§u§l§b§1")) { // 鞋子
                            playerBootsProtectionLevel.put(player, 1);
                            Utils.giveBootsProtection(player, 1);
                        } else if (meta.getLore().contains("§s§o§u§l§b§2")) {
                            playerBootsProtectionLevel.put(player, 2);
                            Utils.giveBootsProtection(player, 2);
                        }
                        break;
                    }
                }
            }
        }.runTaskLater(Main.getInstance(), 1L);
    }

    private List<String> replaceLore(List<String> lore, String... args) {
        List<String> list = new ArrayList<>();
        if (lore == null || lore.isEmpty()) {
            return list;
        }
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < (args.length / 2); i++) {
            int j = i * 2;
            map.put(args[j], args[j + 1]);
        }
        lore.forEach(line -> {
            for (String key : map.keySet()) {
                line = line.replace(key, map.get(key));
            }
            list.add(line);
        });
        return list;
    }

    private String getLevel(int i) {
        switch (i) {
            case 1:
                return "I";
            case 2:
                return "II";
            case 3:
                return "III";
            case 4:
                return "IV";
            default:
                return "";
        }
    }

    private String getItemName(List<String> list) {
        if (!list.isEmpty()) {
            return list.get(0);
        }
        return "§f";
    }

    private List<String> getItemLore(List<String> list) {
        List<String> lore = new ArrayList<>();
        if (list.size() > 1) {
            lore.addAll(list);
            lore.remove(0);
        }
        return lore;
    }
}
