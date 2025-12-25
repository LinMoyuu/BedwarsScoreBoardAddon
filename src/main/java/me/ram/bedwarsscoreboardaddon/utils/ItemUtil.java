package me.ram.bedwarsscoreboardaddon.utils;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Set;

public class ItemUtil {

    public static ItemStack createItem(String name) {
        try {
            return new ItemStack(Material.valueOf(name));
        } catch (Exception e) {
        }
        return new ItemStack(Material.AIR);
    }

    public static ItemStack createItem(String name, int amount) {
        try {
            return new ItemStack(Material.valueOf(name), amount);
        } catch (Exception e) {
        }
        return new ItemStack(Material.AIR);
    }

    public static ItemStack createItem(String name, int amount, short damage) {
        try {
            return new ItemStack(Material.valueOf(name), amount, damage);
        } catch (Exception e) {
        }
        return new ItemStack(Material.AIR);
    }

    public static void setItemName(ItemStack item, String name) {
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
    }

    public static void setItemLore(ItemStack item, List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    public static void setItemUnbreakable(ItemStack item, boolean unbreak) {
        ItemMeta meta = item.getItemMeta();
        try {
            meta.setUnbreakable(unbreak);
        } catch (Exception e) {
            meta.spigot().setUnbreakable(unbreak);
        }
        item.setItemMeta(meta);
    }

    public static void addItemFlags(ItemStack item, ItemFlag... flags) {
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(flags);
        item.setItemMeta(meta);
    }

    public String getItemName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) {
            return item.getType().name();
        }
        return item.getItemMeta().getDisplayName();
    }

    public List<String> getItemLore(ItemStack item) {
        return item.getItemMeta().getLore();
    }

    public Set<ItemFlag> getItemFlags(ItemStack item) {
        return item.getItemMeta().getItemFlags();
    }


    public static void giveLeggingsProtection(Player player, int level) {
        PlayerInventory playerInventory = player.getInventory();
        ItemStack leggings = playerInventory.getLeggings();
        if (leggings == null) return;
        leggings.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, level);
        ItemMeta leggingsMeta = leggings.getItemMeta();
        leggingsMeta.spigot().setUnbreakable(true);
        playerInventory.setLeggings(leggings);
        player.updateInventory();
    }

    public static void giveBootsProtection(Player player, int level) {
        PlayerInventory playerInventory = player.getInventory();
        ItemStack boots = playerInventory.getBoots();
        if (boots == null) return;
        boots.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, level);
        ItemMeta bootsMeta = boots.getItemMeta();
        bootsMeta.spigot().setUnbreakable(true);
        playerInventory.setBoots(boots);
        player.updateInventory();
    }

    public static void givePlayerSharpness(Player player, int level) {
        PlayerInventory playerInventory = player.getInventory();
        for (int i = 0; i < playerInventory.getSize(); i++) {
            ItemStack stack = playerInventory.getItem(i);
            if (!isSword(stack)) continue;
            if (stack.getEnchantmentLevel(Enchantment.DAMAGE_ALL) > level) continue;
            stack.addEnchantment(Enchantment.DAMAGE_ALL, level);
        }
        player.updateInventory();
    }

    public static boolean isSword(ItemStack item) {
        if (item == null) return false;

        Material type = item.getType();
        return type == Material.WOOD_SWORD ||
                type == Material.STONE_SWORD ||
                type == Material.IRON_SWORD ||
                type == Material.GOLD_SWORD ||
                type == Material.DIAMOND_SWORD;
    }
}
